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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import pl.project13.jgit.DescribeCommand;
import pl.project13.jgit.DescribeResult;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * Goal which puts git build-time information into property files or maven's properties.
 *
 * @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 * @goal revision
 * @phase initialize
 * @requiresProject
 * @since 1.0
 */
@SuppressWarnings({"JavaDoc"})
public class GitCommitIdMojo extends AbstractMojo {

  // these properties will be exposed to maven
  public static final String BRANCH = "branch";
  public static final String COMMIT_ID = "commit.id";
  public static final String COMMIT_ID_ABBREV = "commit.id.abbrev";
  public static final String COMMIT_DESCRIBE = "commit.id.describe";
  public static final String BUILD_AUTHOR_NAME = "build.user.name";
  public static final String BUILD_AUTHOR_EMAIL = "build.user.email";
  public static final String BUILD_TIME = "build.time";
  public static final String COMMIT_AUTHOR_NAME = "commit.user.name";
  public static final String COMMIT_AUTHOR_EMAIL = "commit.user.email";
  public static final String COMMIT_MESSAGE_FULL = "commit.message.full";
  public static final String COMMIT_MESSAGE_SHORT = "commit.message.short";
  public static final String COMMIT_TIME = "commit.time";
  public static final String REMOTE_ORIGIN_URL = "remote.origin.url";

  /**
   * The maven project.
   *
   * @parameter expression="${project}"
   * @readonly
   */
  @SuppressWarnings("UnusedDeclaration")
  MavenProject project;

  /**
   * Contains the full list of projects in the reactor.
   *
   * @parameter expression="${reactorProjects}"
   * @readonly
   */
  private List reactorProjects;

  /**
   * Specifies whether the goal runs in verbose mode.
   * To be more specific, this means more info being printed out while scanning for paths and also
   * it will make git-commit-id "eat it's own dog food" :-)
   *
   * @parameter default-value="false"
   */
  @SuppressWarnings("UnusedDeclaration")
  private boolean verbose;

  /**
   * Specifies whether the execution in pom projects should be skipped.
   * Override this value to false if you want to force the plugin to run on 'pom' packaged projects.
   *
   * @parameter expression="${git.skipPoms}" default-value="true"
   */
  @SuppressWarnings("UnusedDeclaration")
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
  @SuppressWarnings("UnusedDeclaration")
  private boolean generateGitPropertiesFile;

  /**
   * Decide where to generate the git.properties file. By default, the src/main/resources/git.properties
   * file will be updated - of course you must first set generateGitPropertiesFile = true to force git-commit-id
   * into generateFile mode.
   * <p/>
   * The path here is relative to your projects src directory.
   *
   * @parameter default-value="src/main/resources/git.properties"
   */
  @SuppressWarnings("UnusedDeclaration")
  private String generateGitPropertiesFilename;

  /**
   * The root directory of the repository we want to check
   *
   * @parameter default-value="${project.basedir}/.git"
   */
  @SuppressWarnings("UnusedDeclaration")
  private File dotGitDirectory;

  /**
   * Configuration for the <pre>git-describe</pre> command.
   * You can modify the dirty marker, abbrev length and other options here.
   * <p/>
   * If not specified, default values will be used.
   *
   * @parameter
   */
  @SuppressWarnings("UnusedDeclaration")
  private GitDescribeConfig gitDescribe;

  /**
   * <p>
   *   Configure the "git.commit.id.abbrev" property to be at least of length N.
   *   N must be in the range of 2 to 40 (inclusive), other values will result in an Exception.
   * </p>
   *
   * <p>
   *   An Abbreviated commit is a shorter version of the commit id, it is guaranteed to be unique though.
   *   To keep this contract, the plugin may decide to print an abbrev version that is longer than the value specified here.
   * </p>
   *
   * <b>Example:</b>
   * <p>
   *   You have a very big repository, yet you set this value to 2. It's very probable that you'll end up getting a 4 or 7 char
   *   long abbrev version of the commit id. If your repository, on the other hand, has just 4 commits, you'll probably get a 2 char long abbrev.
   * </p>
   *
   * @parameter default-value=7
   */
  @SuppressWarnings("UnusedDeclaration")
  private int abbrevLength;

  /**
   * The prefix to expose the properties on, for example 'git' would allow you to access '${git.branch}'
   *
   * @parameter default-value="git"
   */
  @SuppressWarnings("UnusedDeclaration")
  private String prefix;
  private String prefixDot;

  /**
   * The date format to be used for any dates exported by this plugin.
   * It should be a valid SimpleDateFormat string.
   *
   * @parameter default-value="dd.MM.yyyy '@' HH:mm:ss z"
   */
  @SuppressWarnings("UnusedDeclaration")
  private String dateFormat;

  /**
   * Specifies whether the plugin should fail if it can't find the .git directory. The default
   * value is true.
   *
   * @parameter default-value="true"
   */
  @SuppressWarnings("UnusedDeclaration")
  private boolean failOnNoGitDirectory;

  /**
   * The properties we store our data in and then expose them
   */
  private Properties properties;

  public final String logPrefix = "[GitCommitIdMojo] ";

  boolean runningTests = false;

  @NotNull
  LoggerBridge loggerBridge = new MavenLoggerBridge(getLog(), verbose);

  public void execute() throws MojoExecutionException {
    if (isPomProject(project) && skipPoms) {
      log("Skipping the execution as it is a project with packaging type: 'pom'");
      return;
    }

    dotGitDirectory = lookupGitDirectory();
    throwWhenRequiredDirectoryNotFound(dotGitDirectory, failOnNoGitDirectory, ".git directory could not be found! Please specify a valid [dotGitDirectory] in your pom.xml");

    if(dotGitDirectory != null) {
    	log("Running on '%s' repository...", dotGitDirectory.getAbsolutePath());
    } else {
    	log(".git directory could not be found, skipping execution");
        return;
    }

    try {
      properties = initProperties();
      prefixDot = prefix + ".";

      loadGitData(properties);
      loadBuildTimeData(properties);

      logProperties(properties);

      if (generateGitPropertiesFile) {
        generatePropertiesFile(properties, generateGitPropertiesFilename);
      }

      appendPropertiesToMaven(properties);
    } catch (IOException e) {
      throw new MojoExecutionException("Could not complete Mojo execution...", e);
    }

    log("Finished running.");
  }

  private void appendPropertiesToMaven(@NotNull Properties properies) {
    log("Appending to Maven properties");
    
    if (project != null) {
      if (reactorProjects != null) {
        Iterator projIter = reactorProjects.iterator();

        while (projIter.hasNext()) {
          MavenProject nextProj = (MavenProject) projIter.next();
          Properties mavenProperties = nextProj.getProperties(); 
              
          log("Injecting Git properties to \"%s\" project", nextProj.getName());

          for (Object key : properties.keySet()) {
            mavenProperties.put(key, properties.get(key));
          }
        }
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
    return getGitDirLocator().lookupGitDirectory(project, dotGitDirectory);
  }

  @NotNull
  GitDirLocator getGitDirLocator() {
    return new GitDirLocator();
  }

  private Properties initProperties() throws MojoExecutionException {
    log("Initializing properties...");
    if (generateGitPropertiesFile) {
      log("Using clean properties...");
      return properties = new Properties();
    } else if (!runningTests) {
      log("Using maven project properties...");
      return properties = project.getProperties();
    } else {
      return properties = new Properties(); // that's ok for unit tests
    }
  }

  private void logProperties(@NotNull Properties properties) {
    log("------------------git properties loaded------------------");

    for (Object key : properties.keySet()) {
      String keyString = key.toString();
      if (isOurProperty(keyString)) {
        log(String.format("%s = %s", key, properties.getProperty(keyString)));
      }
    }
    log("---------------------------------------------------------");
  }

  private boolean isOurProperty(@NotNull String keyString) {
    return keyString.startsWith(prefixDot);
  }

  void loadBuildTimeData(@NotNull Properties properties) {
    Date commitDate = new Date();
    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
    put(properties, BUILD_TIME, smf.format(commitDate));
  }

  void loadGitData(@NotNull Properties properties) throws IOException, MojoExecutionException {
    log("Loading data from git repository...");
    Repository git = getGitRepository();
    ObjectReader objectReader = git.newObjectReader();

    // git.user.name
    String userName = git.getConfig().getString("user", null, "name");
    put(properties, BUILD_AUTHOR_NAME, userName);

    // git.user.email
    String userEmail = git.getConfig().getString("user", null, "email");
    put(properties, BUILD_AUTHOR_EMAIL, userEmail);

    // more details parsed out bellow
    Ref HEAD = git.getRef(Constants.HEAD);
    if (HEAD == null) {
      throw new MojoExecutionException("Could not get HEAD Ref, are you sure you've set the dotGitDirectory property of this plugin to a valid path?");
    }
    RevWalk revWalk = new RevWalk(git);
    RevCommit headCommit = revWalk.parseCommit(HEAD.getObjectId());
    revWalk.markStart(headCommit);

    try {
      // git.branch
      String branch = git.getBranch();
      put(properties, BRANCH, branch);

      // git.commit.id.describe
      maybePutGitDescribe(properties, git);

      // git.commit.id
      put(properties, COMMIT_ID, headCommit.getName());

      // git.commit.id.abbrev
      putAbbrevCommitId(objectReader, properties, headCommit, abbrevLength);

      // git.commit.author.name
      String commitAuthor = headCommit.getAuthorIdent().getName();
      put(properties, COMMIT_AUTHOR_NAME, commitAuthor);

      // git.commit.author.email
      String commitEmail = headCommit.getAuthorIdent().getEmailAddress();
      put(properties, COMMIT_AUTHOR_EMAIL, commitEmail);

      // git commit.message.full
      String fullMessage = headCommit.getFullMessage();
      put(properties, COMMIT_MESSAGE_FULL, fullMessage);

      // git commit.message.short
      String shortMessage = headCommit.getShortMessage();
      put(properties, COMMIT_MESSAGE_SHORT, shortMessage);

      long timeSinceEpoch = headCommit.getCommitTime();
      Date commitDate = new Date(timeSinceEpoch * 1000); // git is "by sec" and java is "by ms"
      SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
      put(properties, COMMIT_TIME, smf.format(commitDate));

      // git remote.origin.url
      String remoteOriginUrl = git.getConfig().getString("remote", "origin", "url");
      put(properties, REMOTE_ORIGIN_URL, remoteOriginUrl);
    } finally {
      revWalk.dispose();
    }
  }

  private void putAbbrevCommitId(ObjectReader objectReader, Properties properties, RevCommit headCommit, int abbrevLength) throws MojoExecutionException {
    if(abbrevLength < 2 || abbrevLength > 40) {
      throw new MojoExecutionException("Abbreviated commit id lenght must be between 2 and 40, inclusive! Was [%s]. " +
                                           "Please fix your configuration (the <abbrevLength/> element).");
    }

    try {
      AbbreviatedObjectId abbreviatedObjectId = objectReader.abbreviate(headCommit, abbrevLength);
      put(properties, COMMIT_ID_ABBREV, abbreviatedObjectId.name());
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to abbreviate commit id! " +
                                           "You may want to investigate the <abbrevLength/> element in your configuration.", e);
    }
  }

  void maybePutGitDescribe(@NotNull Properties properties, @NotNull Repository repository) throws MojoExecutionException {
    if (gitDescribe == null || !gitDescribe.isSkip()) {
      putGitDescribe(properties, repository);
    }
  }

  @VisibleForTesting
  void putGitDescribe(@NotNull Properties properties, @NotNull Repository repository) throws MojoExecutionException {
    try {
      DescribeResult describeResult = DescribeCommand
          .on(repository)
          .withLoggerBridge(loggerBridge)
          .setVerbose(verbose)
          .apply(gitDescribe)
          .call();

      put(properties, COMMIT_DESCRIBE, describeResult.toString());
    } catch (GitAPIException ex) {
      throw new MojoExecutionException("Unable to obtain git.commit.id.describe information", ex);
    }
  }

  void generatePropertiesFile(@NotNull Properties properties, String generateGitPropertiesFilename) throws IOException {
    String filename = project.getBasedir().getAbsolutePath() + File.separatorChar + generateGitPropertiesFilename;

    FileWriter fileWriter = null;
    try {
      File gitPropsFile = new File(filename);
      Files.createParentDirs(gitPropsFile);

      fileWriter = new FileWriter(gitPropsFile);
      properties.store(fileWriter, "Generated by Git-Commit-Id-Plugin");
    } catch (IOException ex) {
      throw new RuntimeException("Cannot create custom git properties file: " + generateGitPropertiesFilename, ex);
    } finally {
      Closeables.closeQuietly(fileWriter);
    }
  }

  boolean isPomProject(@NotNull MavenProject project) {
    return project.getPackaging().equalsIgnoreCase("pom");
  }

  @NotNull
  private Repository getGitRepository() throws MojoExecutionException {
    Repository repository;

    FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
    try {
      repository = repositoryBuilder
          .setGitDir(dotGitDirectory)
          .readEnvironment() // scan environment GIT_* variables
          .findGitDir() // scan up the file system tree
          .build();
    } catch (IOException e) {
      throw new MojoExecutionException("Could not initialize repository...", e);
    }

    if (repository == null) {
      throw new MojoExecutionException("Could not create git repository. Are you sure '" + dotGitDirectory + "' is the valid Git root for your project?");
    }

    return repository;
  }

  private void put(@NotNull Properties properties, String key, String value) {
    putWithoutPrefix(properties, prefixDot + key, value);
  }

  private void putWithoutPrefix(@NotNull Properties properties, String key, String value) {
    if (verbose) {
      String s = String.format("Storing: %s = %s", key, value);
      log(s);
    }

    if (isNotEmpty(value)) {
      properties.put(key, value);
    } else {
      properties.put(key, "Unknown");
    }
  }

  private boolean isNotEmpty(@Nullable String value) {
    return null != value && !" ".equals(value.trim().replaceAll(" ", ""));
  }

  void log(String message, String... interpolations) {
    loggerBridge.log(logPrefix + message, interpolations);
  }

  private boolean directoryExists(@Nullable File fileLocation) {
    return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory();
  }

  private boolean directoryDoesNotExits(File fileLocation) {
    return !directoryExists(fileLocation);
  }

  // SETTERS FOR TESTS ----------------------------------------------------

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
}
