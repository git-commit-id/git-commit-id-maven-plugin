/*
 * This file is part of git-commit-id-plugin by Konrad Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.maven.git;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import pl.project13.maven.git.GitDataLoader.GitDataLoaderBuilder;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * Goal which puts git build-time information into property files or maven's properties.
 *
 * @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 * @goal revision
 * @phase initialize
 * @requiresProject
 * @threadSafe true
 * @since 1.0
 */
public class GitCommitIdMojo extends AbstractMojo {

//  // these properties will be exposed to maven
//  public static final String BRANCH = "branch";
//  public static final String COMMIT_ID = "commit.id";
//  public static final String COMMIT_ID_ABBREV = "commit.id.abbrev";
//  public static final String COMMIT_DESCRIBE = "commit.id.describe";
//  public static final String BUILD_AUTHOR_NAME = "build.user.name";
//  public static final String BUILD_AUTHOR_EMAIL = "build.user.email";
  public static final String BUILD_TIME = "build.time";
//  public static final String COMMIT_AUTHOR_NAME = "commit.user.name";
//  public static final String COMMIT_AUTHOR_EMAIL = "commit.user.email";
//  public static final String COMMIT_MESSAGE_FULL = "commit.message.full";
//  public static final String COMMIT_MESSAGE_SHORT = "commit.message.short";
//  public static final String COMMIT_TIME = "commit.time";
//  public static final String REMOTE_ORIGIN_URL = "remote.origin.url";

  /**
   * The maven project.
   *
   * @parameter property="project"
   * @readonly
   */
  MavenProject project;

  /**
   * Contains the full list of projects in the reactor.
   *
   * @parameter property="reactorProjects"
   * @readonly
   */
  private List<MavenProject> reactorProjects;

  /**
   * Tell git-commit-id to inject the git properties into all
   * reactor projects not just the current one.
   * <p/>
   * For details about why you might want to skip this, read this issue: https://github.com/ktoso/maven-git-commit-id-plugin/pull/65
   * Basically, injecting into all projects may slow down the build and you don't always need this feature.
   *
   * @parameter default-value="true"
   */
  private boolean injectAllReactorProjects;

  /**
   * Specifies whether the goal runs in verbose mode.
   * To be more specific, this means more info being printed out while scanning for paths and also
   * it will make git-commit-id "eat it's own dog food" :-)
   *
   * @parameter default-value="false"
   */
  private boolean verbose;

  /**
   * Specifies whether the execution in pom projects should be skipped.
   * Override this value to false if you want to force the plugin to run on 'pom' packaged projects.
   *
   * @parameter parameter="git.skipPoms" default-value="true"
   */
  private boolean skipPoms;

  /**
   * Specifies whether plugin should generate properties file.
   * By default it will not generate any additional file,
   * and only add properties to maven project's properties for further filtering
   * <p/>
   * If set to "true" properties will be fully generated with no placeholders inside.
   *
   * @parameter default-value="false"
   */
  private boolean generateGitPropertiesFile;

  /**
   * Decide where to generate the git.properties file. By default, the ${project.build.outputDirectory}/git.properties
   * file will be updated - of course you must first set generateGitPropertiesFile = true to force git-commit-id
   * into generateFile mode.
   * <p/>
   * The path here is relative to your projects src directory.
   *
   * @parameter default-value="${project.build.outputDirectory}/git.properties"
   */
  private String generateGitPropertiesFilename;

  /**
   * The root directory of the repository we want to check
   *
   * @parameter default-value="${project.basedir}/.git"
   */
  private File dotGitDirectory;

  /**
   * Configuration for the <pre>git-describe</pre> command.
   * You can modify the dirty marker, abbrev length and other options here.
   * <p/>
   * If not specified, default values will be used.
   *
   * @parameter
   */
  private GitDescribeConfig gitDescribe;

  /**
   * <p>
   * Configure the "git.commit.id.abbrev" property to be at least of length N.
   * N must be in the range of 2 to 40 (inclusive), other values will result in an Exception.
   * </p>
   * <p/>
   * <p>
   * An Abbreviated commit is a shorter version of the commit id, it is guaranteed to be unique though.
   * To keep this contract, the plugin may decide to print an abbrev version that is longer than the value specified here.
   * </p>
   * <p/>
   * <b>Example:</b>
   * <p>
   * You have a very big repository, yet you set this value to 2. It's very probable that you'll end up getting a 4 or 7 char
   * long abbrev version of the commit id. If your repository, on the other hand, has just 4 commits, you'll probably get a 2 char long abbrev.
   * </p>
   *
   * @parameter default-value=7
   */
  private int abbrevLength;

  /**
   * The format to save properties in. Valid options are "properties" (default) and "json".
   *
   * @parameter default-value="properties"
   */
  private String format;

  /**
   * The prefix to expose the properties on, for example 'git' would allow you to access '${git.branch}'
   *
   * @parameter default-value="git"
   */
  private String prefix;
  private String prefixDot;

  /**
   * The date format to be used for any dates exported by this plugin.
   * It should be a valid SimpleDateFormat string.
   *
   * @parameter default-value="dd.MM.yyyy '@' HH:mm:ss z"
   */
  private String dateFormat;

  /**
   * Specifies whether the plugin should fail if it can't find the .git directory. The default
   * value is true.
   *
   * @parameter default-value="true"
   */
  private boolean failOnNoGitDirectory;

  /**
   * By default the plugin will fail the build if unable to obtain enough data for a complete run,
   * if you don't care about this - for example it's not needed during your CI builds and the CI server does weird
   * things to the repository, you may want to set this value to false.
   * <p/>
   * Setting this value to `false`, causes the plugin to gracefully tell you "I did my best" and abort it's execution
   * if unable to obtain git meta data - yet the build will continue to run (without failing).
   * <p/>
   * See https://github.com/ktoso/maven-git-commit-id-plugin/issues/63 for a rationale behing this flag.
   *
   * @parameter default-value="true"
   */
  private boolean failOnUnableToExtractRepoInfo;

  /**
   * Skip the plugin execution.
   *
   * @parameter default-value="false"
   * @since 2.1.8
   */
  private boolean skip = false;

  /**
   * Can be used to exclude certain properties from being emited into the resulting file.
   * May be useful when you want to hide {@code git.remote.origin.url} (maybe because it contains your repo password?),
   * or the email of the committer etc.
   *
   * Each value may be globbing, that is, you can write {@code git.commit.user.*} to exclude both, the {@code name},
   * as well as {@code email} properties from being emitted into the resulting files.
   *
   * Please note that the strings here are Java regexes ({@code .*} is globbing, not plain {@code *}).
   *
   * @parameter
   * @since 2.1.9
   */
  private List<String> excludeProperties = Collections.emptyList();


  /**
   * The properties we store our data in and then expose them
   */
  private Properties properties;

  boolean runningTests = false;

  @Nonnull
  LoggerBridge loggerBridge = new MavenLoggerBridge(getLog(), true);

  public void execute() throws MojoExecutionException {
    // Set the verbose setting now it should be correctly loaded from maven.
    loggerBridge.setVerbose(verbose);

    if (skip) {
      log("skip is true, return");
      return;
    }

    if (isPomProject(project) && skipPoms) {
      log("isPomProject is true and skipPoms is true, return");
      return;
    }

    dotGitDirectory = lookupGitDirectory();
    throwWhenRequiredDirectoryNotFound(dotGitDirectory, failOnNoGitDirectory, ".git directory could not be found! Please specify a valid [dotGitDirectory] in your pom.xml");

    if (dotGitDirectory != null) {
      log("dotGitDirectory", dotGitDirectory.getAbsolutePath());
    } else {
      log("dotGitDirectory is null, aborting execution!");
      return;
    }

    try {
      properties = initProperties();
      prefixDot = prefix + ".";

//      loadGitData(properties);

	  GitDataLoader gitDataLoader = new GitDataLoaderBuilder()
	  								.withWorkingTreeDirectory(dotGitDirectory.getParentFile())
	  								.withAbbrevationLength(abbrevLength)
	  								.withDateFormat(dateFormat)
	  								.withGitDescribe(gitDescribe)
	  								.withPrefixDot(prefixDot)
	  								.withLoggerBridge(loggerBridge)
	  								.build();
      
      try {
		gitDataLoader.loadGitData(properties);
	} catch (GitDataLoaderException e) {
		throw new MojoExecutionException(e.getMessage(), e);
	}
      
//      filterNot(properties, excludeProperties);
      PropertiesExcludeFilter.filterNot(properties, excludeProperties);
      
      // now in GitDataLoader
//      loadBuildTimeData(properties);
      
      logProperties(properties);

      if (generateGitPropertiesFile) {
        generatePropertiesFile(properties, project.getBasedir(), generateGitPropertiesFilename);
      }

      if (injectAllReactorProjects) {
        appendPropertiesToReactorProjects(properties);
      }
    } catch (IOException e) {
      handlePluginFailure(e);
    }

  }

  /**
   * Reacts to an exception based on the {@code failOnUnableToExtractRepoInfo} setting.
   * If it's true, an MojoExecutionException will be throw, otherwise we just log an error message.
   *
   * @throws MojoExecutionException which will be should be throw within an MojoException in case the
   *                                {@code failOnUnableToExtractRepoInfo} setting was set to true.
   */
  private void handlePluginFailure(Exception e) throws MojoExecutionException {
    if (failOnUnableToExtractRepoInfo) {
      throw new MojoExecutionException("Could not complete Mojo execution...", e);
    } else {
      loggerBridge.error(e.getMessage());
    }
  }

  private void appendPropertiesToReactorProjects(@Nonnull Properties properties) {
    for (MavenProject mavenProject : reactorProjects) {
      Properties mavenProperties = mavenProject.getProperties();

      log(mavenProject.getName(), "] project", mavenProject.getName());

      for (Object key : properties.keySet()) {
        mavenProperties.put(key, properties.get(key));
      }
    }
  }

  private void throwWhenRequiredDirectoryNotFound(File dotGitDirectory, Boolean required, String message) throws MojoExecutionException {
    if (required && directoryDoesNotExits(dotGitDirectory)) {
      throw new MojoExecutionException(message);
    }
  }

  /**
   * Find the git directory of the currently used project.
   * If it's not already specified, this method will try to find it.
   *
   * @return the File representation of the .git directory
   */
  private File lookupGitDirectory() throws MojoExecutionException {
    return new GitDirLocator(project, reactorProjects).lookupGitDirectory(dotGitDirectory);
  }

  private Properties initProperties() throws MojoExecutionException {
    if (generateGitPropertiesFile) {
      return properties = new Properties();
    } else if (!runningTests) {
      return properties = project.getProperties();
    } else {
      return properties = new Properties(); // that's ok for unit tests
    }
  }

  private void logProperties(@Nonnull Properties properties) {
    for (Object key : properties.keySet()) {
      String keyString = key.toString();
      if (isOurProperty(keyString)) {
        log("found property", keyString);
      }
    }
  }

  private boolean isOurProperty(@Nonnull String keyString) {
    return keyString.startsWith(prefixDot);
  }

  static int counter;

  void generatePropertiesFile(@Nonnull Properties properties, File base, String propertiesFilename) throws IOException {
    FileWriter fileWriter = null;
    File gitPropsFile = new File(base, propertiesFilename);
    try {
      Files.createParentDirs(gitPropsFile);

      fileWriter = new FileWriter(gitPropsFile);
      if ("json".equalsIgnoreCase(format)) {
        log("Writing json file to [", gitPropsFile.getAbsolutePath(), "] (for module ", project.getName() + (++counter), ")...");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(fileWriter, properties);
      } else {
        log("Writing properties file to [", gitPropsFile.getAbsolutePath(), "] (for module ", project.getName() + (++counter), ")...");
        properties.store(fileWriter, "Generated by Git-Commit-Id-Plugin");
      }

    } catch (IOException ex) {
      throw new RuntimeException("Cannot create custom git properties file: " + gitPropsFile, ex);
    } finally {
      Closeables.close(fileWriter, true);
    }
  }

  boolean isPomProject(@Nonnull MavenProject project) {
    return project.getPackaging().equalsIgnoreCase("pom");
  }

  void log(String... parts) {
    loggerBridge.log((Object[]) parts);
  }

  private boolean directoryExists(@Nullable File fileLocation) {
    return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory();
  }

  private boolean directoryDoesNotExits(File fileLocation) {
    return !directoryExists(fileLocation);
  }

  // SETTERS FOR TESTS ----------------------------------------------------

  public void setFormat(String format) {
    this.format = format;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void setDotGitDirectory(File dotGitDirectory) {
    this.dotGitDirectory = dotGitDirectory;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public Properties getProperties() {
    return properties;
  }

  public void setGitDescribe(GitDescribeConfig gitDescribe) {
    this.gitDescribe = gitDescribe;
  }

  public void setAbbrevLength(int abbrevLength) {
    this.abbrevLength = abbrevLength;
  }

  public void setExcludeProperties(List<String> excludeProperties) {
    this.excludeProperties = excludeProperties;
  }
}
