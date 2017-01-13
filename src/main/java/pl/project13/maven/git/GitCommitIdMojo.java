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
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import java.io.*;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.junit.Test;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;
import pl.project13.maven.git.util.PropertyManager;

/**
 * Puts git build-time information into property files or maven's properties.
 *
 * @since 1.0
 */
@Mojo(name = "revision", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class GitCommitIdMojo extends GitMojo {



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
    put(properties, BUILD_TIME, smf.format(buildDate));
    put(properties, BUILD_VERSION, project.getVersion());
  }

  void loadBuildHostData(@NotNull Properties properties) {
    String buildHost = null;
    try {
      buildHost = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      log.info("Unable to get build host, skipping property {}. Error message: {}", BUILD_HOST, e.getMessage());
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

          final String buildTimeProperty = prefixDot + BUILD_TIME;

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
        boolean threw = true;

        try {
          outputWriter = new OutputStreamWriter(new FileOutputStream(gitPropsFile), sourceCharset);
          if (isJsonFormat) {
            log.info("Writing json file to [{}] (for module {})...", gitPropsFile.getAbsolutePath(), project.getName());
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputWriter, localProperties);
          } else {
            log.info("Writing properties file to [{}] (for module {})...", gitPropsFile.getAbsolutePath(), project.getName());
            localProperties.store(outputWriter, "Generated by Git-Commit-Id-Plugin");
          }
          threw = false;
        } catch (final IOException ex) {
          throw new RuntimeException("Cannot create custom git properties file: " + gitPropsFile, ex);
        } finally {
          Closeables.close(outputWriter, threw);
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
}