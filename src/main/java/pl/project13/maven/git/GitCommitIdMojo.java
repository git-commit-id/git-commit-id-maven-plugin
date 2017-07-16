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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import java.io.OutputStream;

import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;
import pl.project13.maven.git.util.PropertyManager;
import pl.project13.maven.git.util.SortedProperties;

/**
 * Puts git build-time information into property files or maven's properties.
 *
 * @since 1.0
 */
@Mojo(name = "revision", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class GitCommitIdMojo extends AbstractMojo {
  // TODO fix access modifier
  /**
   * The Maven Project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  MavenProject project;

  /**
   * The list of projects in the reactor.
   */
  @Parameter(defaultValue = "${reactorProjects}", readonly = true)
  private List<MavenProject> reactorProjects;

  /**
   * The Maven Session Object.
   */
  @Parameter(property = "session", required = true, readonly = true)
  private MavenSession session;

  /**
   * <p>Set this to {@code 'true'} to inject git properties into all reactor projects, not just the current one.</p>
   *
   * <p>Injecting into all projects may slow down the build and you don't always need this feature.
   * See <a href="https://github.com/ktoso/maven-git-commit-id-plugin/pull/65">pull #65</a> for details about why you might want to skip this.
   * </p>
   */
  @Parameter(defaultValue = "false")
  private boolean injectAllReactorProjects;

  /**
   * Set this to {@code 'true'} to print more info while scanning for paths.
   * It will make git-commit-id "eat its own dog food" :-)
   */
  @Parameter(defaultValue = "false")
  private boolean verbose;

  /**
   * Set this to {@code 'false'} to execute plugin in 'pom' packaged projects.
   */
  @Parameter(defaultValue = "true")
  private boolean skipPoms;

  /**
   * Set this to {@code 'true'} to generate {@code 'git.properties'} file.
   * By default plugin only adds properties to maven project properties.
   */
  @Parameter(defaultValue = "false")
  private boolean generateGitPropertiesFile;

  /**
   * <p>The location of {@code 'git.properties'} file. Set {@code 'generateGitPropertiesFile'} to {@code 'true'}
   * to generate this file.</p>
   *
   * <p>The path here is relative to your project src directory.</p>
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}/git.properties")
  private String generateGitPropertiesFilename;

  /**
   * The root directory of the repository we want to check.
   */
  @Parameter(defaultValue = "${project.basedir}/.git")
  private File dotGitDirectory;

  /**
   * Configuration for the {@code 'git-describe'} command.
   * You can modify the dirty marker, abbrev length and other options here.
   */
  @Parameter
  private GitDescribeConfig gitDescribe;

  /**
   * <p>Minimum length of {@code 'git.commit.id.abbrev'} property.
   * Value must be from 2 to 40 (inclusive), other values will result in an exception.</p>
   *
   * <p>An abbreviated commit is a shorter version of commit id. However, it is guaranteed to be unique.
   * To keep this contract, the plugin may decide to print an abbreviated version
   * that is longer than the value specified here.</p>
   *
   * <p><b>Example:</b> You have a very big repository, yet you set this value to 2. It's very probable that you'll end up
   * getting a 4 or 7 char long abbrev version of the commit id. If your repository, on the other hand,
   * has just 4 commits, you'll probably get a 2 char long abbreviation.</p>
   *
   */
  @Parameter(defaultValue = "7")
  private int abbrevLength;

  /**
   * The format to save properties in: {@code 'properties'} or {@code 'json'}.
   */
  @Parameter(defaultValue = "properties")
  private String format;

  /**
   * The prefix to expose the properties on. For example {@code 'git'} would allow you to access {@code ${git.branch}}.
   */
  @Parameter(defaultValue = "git")
  private String prefix;
  // prefix with dot appended if needed
  private String prefixDot = "";

  /**
   * The date format to be used for any dates exported by this plugin. It should be a valid {@link SimpleDateFormat} string.
   */
  @Parameter(defaultValue = "dd.MM.yyyy '@' HH:mm:ss z")
  private String dateFormat;

  /**
   * <p>The timezone used in the date format of dates exported by this plugin.
   * It should be a valid Timezone string such as {@code 'America/Los_Angeles'}, {@code 'GMT+10'} or {@code 'PST'}.</p>
   *
   * <p>Try to avoid three-letter time zone IDs because the same abbreviation is often used for multiple time zones.
   * Please review <a href="https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html">https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html</a> for more information on this issue.</p>
   */
  @Parameter
  private String dateFormatTimeZone;

  /**
   * Set this to {@code 'false'} to continue the build on missing {@code '.git'} directory.
   */
  @Parameter(defaultValue = "true")
  private boolean failOnNoGitDirectory;

  /**
   * <p>Set this to {@code 'false'} to continue the build even if unable to get enough data for a complete run.
   * This may be useful during CI builds if the CI server does weird things to the repository.</p>
   *
   * <p>Setting this value to {@code 'false'} causes the plugin to gracefully tell you "I did my best"
   * and abort its execution if unable to obtain git meta data - yet the build will continue to run without failing.</p>
   *
   * <p>See <a href="https://github.com/ktoso/maven-git-commit-id-plugin/issues/63">issue #63</a>
   * for a rationale behind this flag.</p>
   */
  @Parameter(defaultValue = "true")
  private boolean failOnUnableToExtractRepoInfo;

  /**
   * Set this to {@code 'true'} to use native Git executable to fetch information about the repository.
   * It is in most cases faster but requires a git executable to be installed in system.
   * By default the plugin will use jGit implementation as a source of information about the repository.
   * @since 2.1.9
   */
  @Parameter(defaultValue = "false")
  private boolean useNativeGit;

  /**
   * Set this to {@code 'true'} to skip plugin execution.
   * @since 2.1.8
   */
  @Parameter(property = "maven.gitcommitid.skip", defaultValue = "false")
  private boolean skip;

  /**
   * <p>Set this to {@code 'true'} to only run once in a multi-module build.  This probably won't "do the right thing"
   * if your project has more than one git repository.  If you use this with {@code 'generateGitPropertiesFile'},
   * it will only generate (or update) the file in the directory where you started your build.</p>
   *
   * <p>The git.* maven properties are available in all modules.</p>
   * @since 2.1.12
   */
  @Parameter(defaultValue = "false")
  private boolean runOnlyOnce;

  /**
   * <p>List of properties to exclude from the resulting file.
   * May be useful when you want to hide {@code 'git.remote.origin.url'} (maybe because it contains your repo password?)
   * or the email of the committer.</p>
   *
   * <p>Supports wildcards: you can write {@code 'git.commit.user.*'} to exclude both the {@code 'name'}
   * as well as {@code 'email'} properties from being emitted into the resulting files.</p>
   *
   * <p><b>Note:</b> The strings here are Java regular expressions: {@code '.*'} is a wildcard, not plain {@code '*'}.</p>
   * @since 2.1.9
   */
  @Parameter
  private List<String> excludeProperties;

  /**
   * <p>List of properties to include into the resulting file. Only properties specified here will be included.
   * This list will be overruled by the {@code 'excludeProperties'}.</p>
   *
   * <p>Supports wildcards: you can write {@code 'git.commit.user.*'} to include both the {@code 'name'}
   * as well as {@code 'email'} properties into the resulting files.</p>
   *
   * <p><b>Note:</b> The strings here are Java regular expressions: {@code '.*'} is a wildcard, not plain {@code '*'}.</p>
   * @since 2.1.14
   */
  @Parameter
  private List<String> includeOnlyProperties;

  /**
   * <p>The mode of {@code 'git.commit.id'} property generation.</p>
   *
   * <p>{@code 'git.commit.id'} property name is incompatible with json export
   * (see <a href="https://github.com/ktoso/maven-git-commit-id-plugin/issues/122">issue #122</a>).
   * This property allows one either to preserve backward compatibility or to enable fully valid json export:
   *
   * <ol>
   * <li>{@code 'flat'} (default) generates the property {@code 'git.commit.id'}, preserving backwards compatibility.</li>
   * <li>{@code 'full'} generates the property {@code 'git.commit.id.full'}, enabling fully valid json object export.</li>
   * </ol>
   * </p>
   *
   * <p><b>Note:</b> Depending on your plugin configuration you obviously can choose the `prefix` of your properties
   * by setting it accordingly in the plugin's configuration. As a result this is therefore only an illustration
   * what the switch means when the 'prefix' is set to it's default value.</p>
   * <p><b>Note:</b> If you set the value to something that's not equal to {@code 'flat'} or {@code 'full'} (ignoring the case)
   * the plugin will output a warning and will fallback to the default {@code 'flat'} mode.</p>
   * @since 2.2.0
   */
  @Parameter(defaultValue = "flat")
  private String commitIdGenerationMode;
  private CommitIdGenerationMode commitIdGenerationModeEnum;

  /**
   * The properties we store our data in and then expose them.
   */
  private Properties properties;

  /**
   * Charset to read-write project sources.
   */
  private Charset sourceCharset = StandardCharsets.UTF_8;

  @NotNull
  private final LoggerBridge log = new MavenLoggerBridge(this, false);

  @Override
  public void execute() throws MojoExecutionException {
    try {
      // Set the verbose setting: now it should be correctly loaded from maven.
      log.setVerbose(verbose);

      // read source encoding from project properties for those who still doesn't use UTF-8
      String sourceEncoding = project.getProperties().getProperty("project.build.sourceEncoding");
      if (null != sourceEncoding) {
        sourceCharset = Charset.forName(sourceEncoding);
      } else {
        sourceCharset = Charset.defaultCharset();
      }

      if (skip) {
        log.info("skip is enabled, skipping execution!");
        return;
      }

      if (runOnlyOnce) {
        if (!session.getExecutionRootDirectory().equals(session.getCurrentProject().getBasedir().getAbsolutePath())) {
          log.info("runOnlyOnce is enabled and this project is not the top level project, skipping execution!");
          return;
        }
      }

      if (isPomProject(project) && skipPoms) {
        log.info("isPomProject is true and skipPoms is true, return");
        return;
      }

      dotGitDirectory = lookupGitDirectory();
      if (failOnNoGitDirectory && !directoryExists(dotGitDirectory)) {
        throw new GitCommitIdExecutionException(".git directory is not found! Please specify a valid [dotGitDirectory] in your pom.xml");
      }

      if (gitDescribe == null) {
        gitDescribe = new GitDescribeConfig();
      }

      if (dotGitDirectory != null) {
        log.info("dotGitDirectory {}", dotGitDirectory.getAbsolutePath());
      } else {
        log.info("dotGitDirectory is null, aborting execution!");
        return;
      }

      try {
        try {
          commitIdGenerationModeEnum = CommitIdGenerationMode.valueOf(commitIdGenerationMode.toUpperCase());
        } catch (IllegalArgumentException e) {
          log.warn("Detected wrong setting for 'commitIdGenerationMode'. Falling back to default 'flat' mode!");
          commitIdGenerationModeEnum = CommitIdGenerationMode.FLAT;
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
          project.getProperties().putAll(properties); // add to maven project properties also when file is generated
        }

        if (injectAllReactorProjects) {
          appendPropertiesToReactorProjects(properties, prefixDot);
        }
      } catch (Exception e) {
        handlePluginFailure(e);
      }
    } catch (GitCommitIdExecutionException e) {
      throw new MojoExecutionException(e.getMessage(), e);
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
        log.debug("shouldExclude.apply({}) = {}", key, shouldExclude.apply(key));
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
        log.debug("!shouldInclude.apply({}) = {}", key, shouldInclude.apply(key));
        properties.remove(key);
      }
    }
  }

  /**
   * Reacts to an exception based on the {@code failOnUnableToExtractRepoInfo} setting.
   * If it's true, an GitCommitIdExecutionException will be thrown, otherwise we just log an error message.
   *
   * @throws GitCommitIdExecutionException which will be thrown in case the
   *                                {@code failOnUnableToExtractRepoInfo} setting was set to true.
   */
  private void handlePluginFailure(Exception e) throws GitCommitIdExecutionException {
    if (failOnUnableToExtractRepoInfo) {
      throw new GitCommitIdExecutionException("Could not complete Mojo execution...", e);
    } else {
      log.error(e.getMessage(), e);
    }
  }

  private void appendPropertiesToReactorProjects(@NotNull Properties properties, @NotNull String trimmedPrefixWithDot) {
    for (MavenProject mavenProject : reactorProjects) {
      Properties mavenProperties = mavenProject.getProperties();

      // TODO check message
      log.info("{}] project {}", mavenProject.getName(), mavenProject.getName());

      for (Object key : properties.keySet()) {
        if (key.toString().startsWith(trimmedPrefixWithDot)) {
            mavenProperties.put(key, properties.get(key));
        }
      }
    }
  }

  /**
   * Find the git directory of the currently used project.
   * If it's not already specified, this method will try to find it.
   *
   * @return the File representation of the .git directory
   */
  @VisibleForTesting File lookupGitDirectory() throws GitCommitIdExecutionException {
    return new GitDirLocator(project, reactorProjects).lookupGitDirectory(dotGitDirectory);
  }

  private Properties initProperties() throws GitCommitIdExecutionException {
    if (generateGitPropertiesFile) {
      return properties = new Properties();
    } else {
      return properties = project.getProperties();
    }
  }

  private void logProperties(@NotNull Properties properties) {
    for (Object key : properties.keySet()) {
      String keyString = key.toString();
      if (isOurProperty(keyString)) {
        log.info("found property {}", keyString);
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
    put(properties, GitCommitPropertyConstant.BUILD_TIME, smf.format(buildDate));
    put(properties, GitCommitPropertyConstant.BUILD_VERSION, project.getVersion());
  }

  void loadBuildHostData(@NotNull Properties properties) {
    String buildHost = null;
    try {
      buildHost = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      log.info("Unable to get build host, skipping property {}. Error message: {}", GitCommitPropertyConstant.BUILD_HOST, e.getMessage());
    }
    put(properties, GitCommitPropertyConstant.BUILD_HOST, buildHost);
  }

  void loadShortDescribe(@NotNull Properties properties) {
    //removes git hash part from describe
    String commitDescribe = properties.getProperty(prefixDot + GitCommitPropertyConstant.COMMIT_DESCRIBE);

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
        put(properties, GitCommitPropertyConstant.COMMIT_SHORT_DESCRIBE, commitShortDescribe);
      } else {
        put(properties, GitCommitPropertyConstant.COMMIT_SHORT_DESCRIBE, commitDescribe);
      }
    }
  }

  void loadGitData(@NotNull Properties properties) throws GitCommitIdExecutionException {
    if (useNativeGit) {
      loadGitDataWithNativeGit(properties);
    } else {
      loadGitDataWithJGit(properties);
    }
  }

  void loadGitDataWithNativeGit(@NotNull Properties properties) throws GitCommitIdExecutionException {
    try {
      final File basedir = project.getBasedir().getCanonicalFile();

      GitDataProvider nativeGitProvider = NativeGitProvider
              .on(basedir, log)
              .setPrefixDot(prefixDot)
              .setAbbrevLength(abbrevLength)
              .setDateFormat(dateFormat)
              .setDateFormatTimeZone(dateFormatTimeZone)
              .setGitDescribe(gitDescribe)
              .setCommitIdGenerationMode(commitIdGenerationModeEnum);

      nativeGitProvider.loadGitData(properties);
    } catch (IOException e) {
      throw new GitCommitIdExecutionException(e);
    }
  }

  void loadGitDataWithJGit(@NotNull Properties properties) throws GitCommitIdExecutionException {
    GitDataProvider jGitProvider = JGitProvider
      .on(dotGitDirectory, log)
      .setPrefixDot(prefixDot)
      .setAbbrevLength(abbrevLength)
      .setDateFormat(dateFormat)
      .setDateFormatTimeZone(dateFormatTimeZone)
      .setGitDescribe(gitDescribe)
      .setCommitIdGenerationMode(commitIdGenerationModeEnum);

    jGitProvider.loadGitData(properties);
  }

  void maybeGeneratePropertiesFile(@NotNull Properties localProperties, File base, String propertiesFilename) throws GitCommitIdExecutionException {
    try {
      final File gitPropsFile = craftPropertiesOutputFile(base, propertiesFilename);
      final boolean isJsonFormat = "json".equalsIgnoreCase(format);

      boolean shouldGenerate = true;

      if (gitPropsFile.exists()) {
        final Properties persistedProperties;

        try {
          if (isJsonFormat) {
            log.info("Reading existing json file [{}] (for module {})...", gitPropsFile.getAbsolutePath(), project.getName());

            persistedProperties = readJsonProperties(gitPropsFile);
          } else {
            log.info("Reading existing properties file [{}] (for module {})...", gitPropsFile.getAbsolutePath(), project.getName());

            persistedProperties = readProperties(gitPropsFile);
          }

          final Properties propertiesCopy = (Properties) localProperties.clone();

          final String buildTimeProperty = prefixDot + GitCommitPropertyConstant.BUILD_TIME;

          propertiesCopy.remove(buildTimeProperty);
          persistedProperties.remove(buildTimeProperty);

          shouldGenerate = !propertiesCopy.equals(persistedProperties);
        } catch (CannotReadFileException ex) {
          // Read has failed, regenerate file
          log.info("Cannot read properties file [{}] (for module {})...", gitPropsFile.getAbsolutePath(), project.getName());
          shouldGenerate = true;
        }
      }

      if (shouldGenerate) {
        Files.createParentDirs(gitPropsFile);
        Writer outputWriter = null;
        OutputStream outputStream = null;
        boolean threw = true;

        try {
          outputStream = new FileOutputStream(gitPropsFile);
          SortedProperties sortedLocalProperties = new SortedProperties();
          sortedLocalProperties.putAll(localProperties);
          if (isJsonFormat) {
            outputWriter = new OutputStreamWriter(outputStream, sourceCharset);
            log.info("Writing json file to [{}] (for module {})...", gitPropsFile.getAbsolutePath(), project.getName());
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputWriter, sortedLocalProperties);
          } else {
            log.info("Writing properties file to [{}] (for module {})...", gitPropsFile.getAbsolutePath(), project.getName());
            // using outputStream directly instead of outputWriter this way the UTF-8 characters appears in unicode escaped form
            sortedLocalProperties.store(outputStream, "Generated by Git-Commit-Id-Plugin");
          }
          threw = false;
        } catch (final IOException ex) {
          throw new RuntimeException("Cannot create custom git properties file: " + gitPropsFile, ex);
        } finally {
          if (outputWriter == null) {
            Closeables.close(outputStream, threw);
          } else {
            Closeables.close(outputWriter, threw);
          }
        }
      } else {
        log.info("Properties file [{}] is up-to-date (for module {})...", gitPropsFile.getAbsolutePath(), project.getName());
      }
    } catch (IOException e) {
      throw new GitCommitIdExecutionException(e);
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
    log.info(keyWithPrefix + " " + value);
    PropertyManager.putWithoutPrefix(properties, keyWithPrefix, value);
  }

  private boolean directoryExists(@Nullable File fileLocation) {
    return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory();
  }

  @SuppressWarnings( "resource" )
  private Properties readJsonProperties(@NotNull File jsonFile) throws CannotReadFileException {
    final HashMap<String, Object> propertiesMap;

    {
      Closeable closeable = null;

      try {
        boolean threw = true;
        try {
          final FileInputStream fis = new FileInputStream(jsonFile);
          closeable = fis;

          final InputStreamReader reader = new InputStreamReader(fis, sourceCharset);
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
  private Properties readProperties(@NotNull File propertiesFile) throws CannotReadFileException {
    Closeable closeable = null;

    try {
      boolean threw = true;
      try {
        final FileInputStream fis = new FileInputStream(propertiesFile);
        closeable = fis;

        final InputStreamReader reader = new InputStreamReader(fis, sourceCharset);
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

  static class CannotReadFileException extends Exception {
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
}
