/*
 * This file is part of git-commit-id-plugin by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;
import pl.project13.maven.git.util.PropertyManager;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Goal which puts git build-time information into property files or maven's properties.
 *
 * @goal revision
 * @phase initialize
 * @requiresProject
 * @threadSafe true
 * @since 1.0
 */
@SuppressWarnings({"JavaDoc"})
public class GitCommitIdMojo extends AbstractMojo {

  // these properties will be exposed to maven
  public static final String BRANCH = "branch";
  public static final String DIRTY = "dirty";
  public static final String DIRTY_MARK = "dirty.mark";
  public static final String COMMIT_ID_FLAT = "commit.id";
  public static final String COMMIT_ID_FULL = "commit.id.full";
  public static String COMMIT_ID = COMMIT_ID_FLAT;

  public static final String COMMIT_ID_ABBREV = "commit.id.abbrev";
  public static final String COMMIT_DESCRIBE = "commit.id.describe";
  public static final String COMMIT_SHORT_DESCRIBE = "commit.id.describe-short";
  public static final String BUILD_AUTHOR_NAME = "build.user.name";
  public static final String BUILD_AUTHOR_EMAIL = "build.user.email";
  public static final String BUILD_TIME = "build.time";
  public static final String BUILD_VERSION = "build.version";
  public static final String BUILD_HOST = "build.host";
  public static final String COMMIT_AUTHOR_NAME = "commit.user.name";
  public static final String COMMIT_AUTHOR_EMAIL = "commit.user.email";
  public static final String COMMIT_MESSAGE_FULL = "commit.message.full";
  public static final String COMMIT_MESSAGE_SHORT = "commit.message.short";
  public static final String COMMIT_TIME = "commit.time";
  public static final String REMOTE_ORIGIN_URL = "remote.origin.url";
  public static final String TAGS = "tags";
  public static final String CLOSEST_TAG_NAME = "closest.tag.name";
  public static final String CLOSEST_TAG_COMMIT_COUNT = "closest.tag.commit.count";

  /**
   * The maven project.
   *
   * @parameter property="project" default-value="${project}"
   * @readonly
   */
  @SuppressWarnings("UnusedDeclaration")
  MavenProject project;

  /**
   * Contains the full list of projects in the reactor.
   *
   * @parameter property="reactorProjects" default-value="${reactorProjects}"
   * @readonly
   */
  @SuppressWarnings("UnusedDeclaration")
  private List<MavenProject> reactorProjects;

  /**
   * Tell git-commit-id to inject the git properties into all
   * reactor projects not just the current one.
   *
   * For details about why you might want to skip this, read this issue: https://github.com/ktoso/maven-git-commit-id-plugin/pull/65
   * Basically, injecting into all projects may slow down the build and you don't always need this feature.
   *
   * @parameter default-value="false"
   */
  @SuppressWarnings("UnusedDeclaration")
  private boolean injectAllReactorProjects;

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
   * @parameter parameter="git.skipPoms" default-value="true"
   */
  @SuppressWarnings("UnusedDeclaration")
  private boolean skipPoms;

  /**
   * Specifies whether plugin should generate properties file.
   * By default it will not generate any additional file,
   * and only add properties to maven project's properties for further filtering
   *
   * If set to "true" properties will be fully generated with no placeholders inside.
   *
   * @parameter default-value="false"
   */
  @SuppressWarnings("UnusedDeclaration")
  private boolean generateGitPropertiesFile;

  /**
   * Decide where to generate the git.properties file. By default, the ${project.build.outputDirectory}/git.properties
   * file will be updated - of course you must first set generateGitPropertiesFile = true to force git-commit-id
   * into generateFile mode.
   *
   * The path here is relative to your projects src directory.
   *
   * @parameter default-value="${project.build.outputDirectory}/git.properties"
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
   *
   * If not specified, default values will be used.
   *
   * @parameter
   */
  @SuppressWarnings("UnusedDeclaration")
  private GitDescribeConfig gitDescribe;

  /**
   * <p>
   * Configure the "git.commit.id.abbrev" property to be at least of length N.
   * N must be in the range of 2 to 40 (inclusive), other values will result in an Exception.
   * </p>
   *
   * <p>
   * An Abbreviated commit is a shorter version of the commit id, it is guaranteed to be unique though.
   * To keep this contract, the plugin may decide to print an abbrev version that is longer than the value specified here.
   * </p>
   *
   * <b>Example:</b>
   * <p>
   * You have a very big repository, yet you set this value to 2. It's very probable that you'll end up getting a 4 or 7 char
   * long abbrev version of the commit id. If your repository, on the other hand, has just 4 commits, you'll probably get a 2 char long abbrev.
   * </p>
   *
   * @parameter default-value=7
   */
  @SuppressWarnings("UnusedDeclaration")
  private int abbrevLength;

  /**
   * The format to save properties in. Valid options are "properties" (default) and "json".
   *
   * @parameter default-value="properties"
   */
  @SuppressWarnings("UnusedDeclaration")
  private String format;

  /**
   * The prefix to expose the properties on, for example 'git' would allow you to access '${git.branch}'
   *
   * @parameter default-value="git"
   */
  @SuppressWarnings("UnusedDeclaration")
  private String prefix;
  private String prefixDot = "";

  /**
   * The date format to be used for any dates exported by this plugin.
   * It should be a valid SimpleDateFormat string.
   *
   * @parameter default-value="dd.MM.yyyy '@' HH:mm:ss z"
   */
  @SuppressWarnings("UnusedDeclaration")
  private String dateFormat;

  /**
   * The timezone used in the date format that's used for any dates exported by this plugin.
   * It should be a valid Timezone string (e.g. 'America/Los_Angeles', 'GMT+10', 'PST').
   * As a general warning try to avoid three-letter time zone IDs because the same abbreviation are often used for multiple time zones.
   * Please review https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html for more information on this issue.
   * will use the timezone that's shipped with java as a default (java.util.TimeZone.getDefault().getID())
   *
   * @parameter
   */
  @SuppressWarnings("UnusedDeclaration")
  private String dateFormatTimeZone;



  /**
   * Specifies whether the plugin should fail if it can't find the .git directory. The default
   * value is true.
   *
   * @parameter default-value="true"
   */
  @SuppressWarnings("UnusedDeclaration")
  private boolean failOnNoGitDirectory;

  /**
   * By default the plugin will fail the build if unable to obtain enough data for a complete run,
   * if you don't care about this - for example it's not needed during your CI builds and the CI server does weird
   * things to the repository, you may want to set this value to false.
   *
   * Setting this value to `false`, causes the plugin to gracefully tell you "I did my best" and abort it's execution
   * if unable to obtain git meta data - yet the build will continue to run (without failing).
   *
   * See https://github.com/ktoso/maven-git-commit-id-plugin/issues/63 for a rationale behing this flag.
   *
   * @parameter default-value="true"
   */
  @SuppressWarnings("UnusedDeclaration")
  private boolean failOnUnableToExtractRepoInfo;

  /**
   * By default the plugin will use a jgit implementation as a source of a information about the repository. You can
   * use a native GIT executable to fetch information about the repository, witch is in most cases faster but requires
   * a git executable to be installed in system.
   *
   * @parameter default-value="false"
   * @since 2.1.9
   */
  @SuppressWarnings("UnusedDeclaration")
  private boolean useNativeGit;

  /**
   * Skip the plugin execution.
   *
   * @parameter default-value="false"
   * @since 2.1.8
   */
  @SuppressWarnings("UnusedDeclaration")
  private boolean skip = false;

  /**
   * In a multi-module build, only run once.  This probably won't "do the right thing" if your project has more than
   * one git repository.  If you use this with the option 'generateGitPropertiesFile', it will only generate (or update)
   * the file in the directory where you started your build.
   *
   * The git.* maven properties are available in all modules.
   *
   * @parameter default-value="false"
   * @since 2.1.12
   */
  @SuppressWarnings("UnusedDeclaration")
  private boolean runOnlyOnce = false;

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
  @SuppressWarnings("UnusedDeclaration")
  private List<String> excludeProperties = Collections.emptyList();

  /**
   * Can be used to include only certain properties into the resulting file.
   * Will be overruled by the exclude properties.
   *
   * Each value may be globbing, that is, you can write {@code git.commit.user.*} to include both, the {@code name},
   * as well as {@code email} properties into the resulting files.
   *
   * Please note that the strings here are Java regexes ({@code .*} is globbing, not plain {@code *}).
   *
   * @parameter
   * @since 2.1.14
   */
  @SuppressWarnings("UnusedDeclaration")
  private List<String> includeOnlyProperties = Collections.emptyList();

  /**
   * The option can be used to tell the plugin how it should generate the 'git.commit.id' property. Due to some naming issues when exporting the properties as an json-object (https://github.com/ktoso/maven-git-commit-id-plugin/issues/122) we needed to make it possible to export all properties as a valid json-object.
   * Due to the fact that this is one of the major properties the plugin is exporting we just don't want to change the exporting mechanism and somehow throw the backwards compatibility away.
   * We rather provide a convient switch where you can choose if you would like the properties as they always had been, or if you rather need to support full json-object compatibility.
   * In the case you need to fully support json-object we unfortunately need to change the 'git.commit.id' property from 'git.commit.id' to 'git.commit.id.full' in the exporting mechanism to allow the generation of a fully valid json object.
   *
   * Currently the switch allows two different options:
   * 1. By default this property is set to 'flat' and will generate the formerly known property 'git.commit.id' as it was in the previous versions of the plugin. Keeping it to 'flat' by default preserve backwards compatibility and does not require further adjustments by the end user.
   * 2. If you set this switch to 'full' the plugin will export the formerly known property 'git.commit.id' as 'git.commit.id.full' and therefore will generate a fully valid json object in the exporting mechanism.
   *
   * *Note*: Depending on your plugin configuration you obviously can choose the 'prefix' of your properties by setting it accordingly in the plugin's configuration. As a result this is therefore only an illustration what the switch means when the 'prefix' is set to it's default value.
   * *Note*: If you set the value to something that's not equal to 'flat' or 'full' (ignoring the case) the plugin will output a warning and will fallback to the default 'flat' mode.
   *
   * @parameter default-value="flat"
   * @since 2.2.0
   */
  private String commitIdGenerationMode;

  /**
   * The Maven Session Object
   *
   * @parameter property="session"
   * @required
   * @readonly
   */
  @SuppressWarnings("UnusedDeclaration")
  protected MavenSession session;

  /**
   * The properties we store our data in and then expose them
   */
  private Properties properties;

  boolean runningTests = false;

  @NotNull
  LoggerBridge loggerBridge = new MavenLoggerBridge(getLog(), true);

  public void execute() throws MojoExecutionException {
    // Set the verbose setting now it should be correctly loaded from maven.
    loggerBridge.setVerbose(verbose);

    if (skip) {
      log("skip is enabled, skipping execution!");
      return;
    }

    if (runOnlyOnce) {
      if (!session.getExecutionRootDirectory().equals(session.getCurrentProject().getBasedir().getAbsolutePath())) {
        log("runOnlyOnce is enabled and this project is not the top level project, skipping execution!");
        return;
      }
    }

    if (isPomProject(project) && skipPoms) {
      log("isPomProject is true and skipPoms is true, return");
      return;
    }

    dotGitDirectory = lookupGitDirectory();
    throwWhenRequiredDirectoryNotFound(dotGitDirectory, failOnNoGitDirectory, ".git directory could not be found! Please specify a valid [dotGitDirectory] in your pom.xml");

    if (gitDescribe == null) {
      gitDescribe = new GitDescribeConfig();
    }

    if (dotGitDirectory != null) {
      log("dotGitDirectory", dotGitDirectory.getAbsolutePath());
    } else {
      log("dotGitDirectory is null, aborting execution!");
      return;
    }

    try {
      switch(CommitIdGenerationModeEnum.getValue(commitIdGenerationMode)){
      default:
        loggerBridge.warn("Detected wrong setting for 'commitIdGenerationMode' will fallback to default 'flat'-Mode!");
      case FLAT:
        COMMIT_ID = COMMIT_ID_FLAT;
        break;
      case FULL:
        COMMIT_ID = COMMIT_ID_FULL;
        break;
      }

      properties = initProperties();

      String trimmedPrefix = prefix.trim();
      prefixDot = trimmedPrefix.equals("") ? "" : trimmedPrefix + ".";

      loadGitData(properties);
      loadBuildVersionAndTimeData(properties);
      loadBuildHostData(properties);
      loadShortDescribe(properties);
      filter(properties, includeOnlyProperties);
      filterNot(properties, excludeProperties);
      logProperties(properties);

      if (generateGitPropertiesFile) {
        maybeGeneratePropertiesFile(properties, project.getBasedir(), generateGitPropertiesFilename);
      }

      if (injectAllReactorProjects) {
        appendPropertiesToReactorProjects(properties, prefixDot);
      }
    } catch (Exception e) {
      e.printStackTrace();
      handlePluginFailure(e);
    }

  }

  private void filterNot(Properties properties, @Nullable List<String> exclusions) {
    if (exclusions == null || exclusions.isEmpty()) {
      return;
    }

    List<Predicate<CharSequence>> excludePredicates = Lists.transform(exclusions, new Function<String, Predicate<CharSequence>>() {
      @Override
      public Predicate<CharSequence> apply(String exclude) {
        return Predicates.containsPattern(exclude);
      }
    });

    Predicate<CharSequence> shouldExclude = Predicates.alwaysFalse();
    for (Predicate<CharSequence> predicate : excludePredicates) {
      shouldExclude = Predicates.or(shouldExclude, predicate);
    }

    for (String key : properties.stringPropertyNames()) {
      if (shouldExclude.apply(key)) {
        loggerBridge.debug("shouldExclude.apply(" + key + ") = " + shouldExclude.apply(key));
        properties.remove(key);
      }
    }
  }

  private void filter(Properties properties, @Nullable List<String> inclusions) {
    if (inclusions == null || inclusions.isEmpty()) {
      return;
    }

    List<Predicate<CharSequence>> includePredicates = Lists.transform(inclusions, new Function<String, Predicate<CharSequence>>() {
      @Override
      public Predicate<CharSequence> apply(String exclude) {
        return Predicates.containsPattern(exclude);
      }
    });

    Predicate<CharSequence> shouldInclude = Predicates.alwaysFalse();
    for (Predicate<CharSequence> predicate : includePredicates) {
      shouldInclude = Predicates.or(shouldInclude, predicate);
    }

    for (String key : properties.stringPropertyNames()) {
      if (!shouldInclude.apply(key)) {
        loggerBridge.debug("!shouldInclude.apply(" + key + ") = " + shouldInclude.apply(key));
        properties.remove(key);
      }
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
      loggerBridge.error(e.getMessage(), com.google.common.base.Throwables.getStackTraceAsString(e));
    }
  }

  private void appendPropertiesToReactorProjects(@NotNull Properties properties, @NotNull String trimmedPrefixWithDot) {
    for (MavenProject mavenProject : reactorProjects) {
      Properties mavenProperties = mavenProject.getProperties();

      log(mavenProject.getName(), "] project", mavenProject.getName());

      for (Object key : properties.keySet()) {
        if (key.toString().startsWith(trimmedPrefixWithDot)) {
            mavenProperties.put(key, properties.get(key));
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
  @VisibleForTesting File lookupGitDirectory() throws MojoExecutionException {
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

  private void logProperties(@NotNull Properties properties) {
    for (Object key : properties.keySet()) {
      String keyString = key.toString();
      if (isOurProperty(keyString)) {
        log("found property", keyString);
      }
    }
  }

  private boolean isOurProperty(@NotNull String keyString) {
    return keyString.startsWith(prefixDot);
  }

  void loadBuildVersionAndTimeData(@NotNull Properties properties) {
    Date buildDate = new Date();
    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
    if(dateFormatTimeZone != null){
      smf.setTimeZone(TimeZone.getTimeZone(dateFormatTimeZone));
    }
    put(properties, BUILD_TIME, smf.format(buildDate));
    put(properties, BUILD_VERSION, project.getVersion());
  }

  void loadBuildHostData(@NotNull Properties properties) {
    String buildHost = null;
    try {
      buildHost = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      log("Unable to get build host, skipping property " + BUILD_HOST + ". Error message was: " + e.getMessage());
    }
    put(properties, BUILD_HOST, buildHost);
  }

  void loadShortDescribe(@NotNull Properties properties) {
    //removes git hash part from describe
    String commitDescribe = properties.getProperty(prefixDot + COMMIT_DESCRIBE);

    if (commitDescribe != null) {
      int startPos = commitDescribe.indexOf("-g");
      if (startPos > 0) {
        String commitShortDescribe;
        int endPos = commitDescribe.indexOf('-', startPos + 1);
        if (endPos < 0) {
          commitShortDescribe = commitDescribe.substring(0, startPos);
        } else {
          commitShortDescribe = commitDescribe.substring(0, startPos) + commitDescribe.substring(endPos);
        }
        put(properties, COMMIT_SHORT_DESCRIBE, commitShortDescribe);
      } else {
        put(properties, COMMIT_SHORT_DESCRIBE, commitDescribe);
      }
    }
  }

  void loadGitData(@NotNull Properties properties) throws IOException, MojoExecutionException {
    if (useNativeGit) {
      loadGitDataWithNativeGit(properties);
    } else {
      loadGitDataWithJGit(properties);
    }
  }

  void loadGitDataWithNativeGit(@NotNull Properties properties) throws IOException, MojoExecutionException {
    final File basedir = project.getBasedir().getCanonicalFile();

    GitDataProvider nativeGitProvider = NativeGitProvider
      .on(basedir, loggerBridge)
      .setVerbose(verbose)
      .setPrefixDot(prefixDot)
      .setAbbrevLength(abbrevLength)
      .setDateFormat(dateFormat)
      .setDateFormatTimeZone(dateFormatTimeZone)
      .setGitDescribe(gitDescribe);

    nativeGitProvider.loadGitData(properties);
  }

  void loadGitDataWithJGit(@NotNull Properties properties) throws IOException, MojoExecutionException {
    GitDataProvider jGitProvider = JGitProvider
      .on(dotGitDirectory, loggerBridge)
      .setVerbose(verbose)
      .setPrefixDot(prefixDot)
      .setAbbrevLength(abbrevLength)
      .setDateFormat(dateFormat)
      .setDateFormatTimeZone(dateFormatTimeZone)
      .setGitDescribe(gitDescribe);

    jGitProvider.loadGitData(properties);
  }

  void maybeGeneratePropertiesFile(@NotNull Properties localProperties, File base, String propertiesFilename) throws IOException {
    final File gitPropsFile = craftPropertiesOutputFile(base, propertiesFilename);
    final boolean isJsonFormat = "json".equalsIgnoreCase( format );

    boolean shouldGenerate = true;

    if (gitPropsFile.exists( )) {
      final Properties persistedProperties;

      try {
        if (isJsonFormat) {
          log("Reading exising json file [", gitPropsFile.getAbsolutePath(), "] (for module ", project.getName(), ")...");

          persistedProperties = readJsonProperties( gitPropsFile );
        }
        else {
          log("Reading exising properties file [", gitPropsFile.getAbsolutePath(), "] (for module ", project.getName(), ")...");

          persistedProperties = readProperties( gitPropsFile );
        }

        final Properties propertiesCopy = (Properties) localProperties.clone( );

        final String buildTimeProperty = prefixDot + BUILD_TIME;

        propertiesCopy.remove( buildTimeProperty );
        persistedProperties.remove( buildTimeProperty );

        shouldGenerate = ! propertiesCopy.equals( persistedProperties );
      }
      catch ( CannotReadFileException ex ) {
        // Read has failed, regenerate file
        log("Cannot read properties file [", gitPropsFile.getAbsolutePath(), "] (for module ", project.getName(), ")...");
        shouldGenerate = true;
      }
    }

    if (shouldGenerate) {
      Files.createParentDirs(gitPropsFile);
      Writer outputWriter = null;
      boolean threw = true;

      try {
        outputWriter = new OutputStreamWriter(new FileOutputStream(gitPropsFile), Charsets.UTF_8);
        if (isJsonFormat) {
          log("Writing json file to [", gitPropsFile.getAbsolutePath(), "] (for module ", project.getName(), ")...");
          ObjectMapper mapper = new ObjectMapper();
          mapper.writeValue(outputWriter, localProperties);
        } else {
          log("Writing properties file to [", gitPropsFile.getAbsolutePath(), "] (for module ", project.getName(), ")...");
          localProperties.store(outputWriter, "Generated by Git-Commit-Id-Plugin");
        }
        threw = false;
      } catch (final IOException ex) {
        throw new RuntimeException("Cannot create custom git properties file: " + gitPropsFile, ex);
      } finally {
        Closeables.close(outputWriter, threw);
      }
    }
    else {
      log("Properties file [", gitPropsFile.getAbsolutePath(), "] is up-to-date (for module ", project.getName(), ")...");
    }
  }

  @VisibleForTesting File craftPropertiesOutputFile(File base, String propertiesFilename) {
    File returnPath = new File(base, propertiesFilename);

    File currentPropertiesFilepath = new File(propertiesFilename);
    if (currentPropertiesFilepath.isAbsolute()) {
      returnPath = currentPropertiesFilepath;
    }

    return returnPath;
  }


  boolean isPomProject(@NotNull MavenProject project) {
    return project.getPackaging().equalsIgnoreCase("pom");
  }

  private void put(@NotNull Properties properties, String key, String value) {
    String keyWithPrefix = prefixDot + key;
    log(keyWithPrefix, value);
    PropertyManager.putWithoutPrefix(properties, keyWithPrefix, value);
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

  @SuppressWarnings( "resource" )
  static Properties readJsonProperties(@NotNull File jsonFile) throws CannotReadFileException {
    final HashMap<String, Object> propertiesMap;

    {
      Closeable closeable = null;

      try {
        boolean threw = true;
        try {
          final FileInputStream fis = new FileInputStream(jsonFile);
          closeable = fis;

          final InputStreamReader reader = new InputStreamReader(fis, Charsets.UTF_8);
          closeable = reader;

          final ObjectMapper mapper = new ObjectMapper();
          final TypeReference<HashMap<String, Object>> mapTypeRef =
                  new TypeReference<HashMap<String, Object>>() {
                  };

          propertiesMap = mapper.readValue(reader, mapTypeRef);
          threw = false;
        } finally {
          Closeables.close(closeable, threw);
        }
      } catch (final Exception ex) {
        throw new CannotReadFileException(ex);
      }
    }

    final Properties retVal = new Properties( );

    for(final Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
      retVal.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
    }

    return retVal;
  }

  @SuppressWarnings( "resource" )
  static Properties readProperties(@NotNull File propertiesFile) throws CannotReadFileException {
    Closeable closeable = null;

    try {
      boolean threw = true;
      try {
        final FileInputStream fis = new FileInputStream(propertiesFile);
        closeable = fis;

        final InputStreamReader reader = new InputStreamReader(fis, Charsets.UTF_8);
        closeable = reader;

        final Properties retVal = new Properties();

        retVal.load(reader);
        threw = false;

        return retVal;
      } finally {
        Closeables.close(closeable, threw);
      }
    }
    catch (final Exception ex) {
      throw new CannotReadFileException(ex);
    }
  }

  static class CannotReadFileException
    extends Exception
  {
    private static final long serialVersionUID = -6290782570018307756L;

    CannotReadFileException( Throwable cause )
    {
      super( cause );
    }
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

  public void setIncludeOnlyProperties(List<String> includeOnlyProperties) {
    this.includeOnlyProperties = includeOnlyProperties;
  }

  public void useNativeGit(boolean useNativeGit) {
    this.useNativeGit = useNativeGit;
  }

  public void setCommitIdGenerationMode(String commitIdGenerationMode){
    this.commitIdGenerationMode = commitIdGenerationMode;
  }

  public LoggerBridge getLoggerBridge() {
    return loggerBridge;
  }
}
