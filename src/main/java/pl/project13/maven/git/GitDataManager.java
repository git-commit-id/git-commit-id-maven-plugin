package pl.project13.maven.git;

import pl.project13.core.*;
import pl.project13.core.cibuild.BuildServerDataProvider;
import pl.project13.core.git.GitDescribeConfig;
import pl.project13.core.log.LogInterface;
import pl.project13.core.util.BuildFileChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class GitDataManager {
  public enum PropertiesOutputFormat {

    PROPERTIES,

    JSON,
  }

  public interface DataProvider {
    @Nonnull
    LogInterface getLogInterface();

    @Nullable
    String getPrefixDot();

    @Nullable
    List<String> getExcludeProperties();

    @Nullable
    List<String> getIncludeOnlyProperties();

    boolean shouldGeneratePropertiesFile();

    @Nonnull
    String getGenerateGitPropertiesFilename();

    @Nonnull
    Charset getPropertiesOutputFileSourceCharset();

    @Nonnull
    PropertiesOutputFormat getPropertiesOutputFormat();

    @Nonnull
    File getProjectBaseDir();

    @Nonnull
    String getProjectName();

    @Nonnull
    String getProjectVersion();

    @Nonnull
    String getProjectBuildOutputTimestamp();

    @Nonnull
    void callBeforePropertiesAreWrittenToFile(final Properties p);

    @Nonnull
    BuildFileChangeListener getBuildFileChangeListener();

    @Nonnull
    String getDateFormat();

    @Nonnull
    String getDateFormatTimeZone();

    long getNativeGitTimeoutInMs();

    int getAbbrevLength();

    boolean shouldRunWithNativeGit();

    GitDescribeConfig getGitDescribeConfig();

    CommitIdGenerationMode getCommitIdGenerationMode();

    boolean shouldUseBranchNameFromBuildEnvironment();

    boolean shouldRunOffline();

    String getEvaluateOnCommit();

    File getDotGitDirectory();
  }


  private LogInterface log;

  private String prefixDot;

  private List<String> includeOnlyProperties;

  private List<String> excludeProperties;

  private File projectBaseDir;

  private String projectVersion;

  private String projectName;

  private String projectBuildOutputTimestamp;

  private String dateFormat;

  private String dateFormatTimeZone;

  private boolean shouldGeneratePropertiesFile;

  private BuildFileChangeListener buildFileChangeListener;

  private PropertiesOutputFormat propertiesOutputFormat;

  private String generateGitPropertiesFilename;

  private Charset propertiesOutputFileSourceCharset;

  private long nativeGitTimeoutInMs;

  private int abbrevLength;

  private boolean shouldRunWithNativeGit;

  private GitDescribeConfig gitDescribeConfig;

  private CommitIdGenerationMode commitIdGenerationMode;

  private boolean useBranchNameFromBuildEnvironment;

  private boolean shouldRunOffline;

  private String evaluateOnCommit;

  private File dotGitDirectory;

  private Consumer<Properties> callbackBeforePropertiesAreWrittenToFile;


  public GitDataManager(@Nonnull DataProvider dp) {
    Objects.requireNonNull(dp);
    this.log = Objects.requireNonNull(dp.getLogInterface());
    this.prefixDot = Optional.ofNullable(dp.getPrefixDot()).orElseGet(() -> "");
    this.includeOnlyProperties = Optional.ofNullable(dp.getIncludeOnlyProperties()).orElseGet(() -> new ArrayList<>());
    this.excludeProperties = Optional.ofNullable(dp.getExcludeProperties()).orElseGet(() -> new ArrayList<>());
    this.projectBaseDir = dp.getProjectBaseDir();
    this.projectVersion = dp.getProjectVersion();
    this.projectName = dp.getProjectName();
    this.dateFormat = dp.getDateFormat();
    this.dateFormatTimeZone = dp.getDateFormatTimeZone();
    this.projectBuildOutputTimestamp = dp.getProjectBuildOutputTimestamp();
    this.shouldGeneratePropertiesFile = dp.shouldGeneratePropertiesFile();
    this.buildFileChangeListener = dp.getBuildFileChangeListener();
    this.propertiesOutputFormat = dp.getPropertiesOutputFormat();
    this.generateGitPropertiesFilename = dp.getGenerateGitPropertiesFilename();
    this.propertiesOutputFileSourceCharset = dp.getPropertiesOutputFileSourceCharset();
    this.nativeGitTimeoutInMs = dp.getNativeGitTimeoutInMs();
    this.abbrevLength = dp.getAbbrevLength();
    this.shouldRunWithNativeGit = dp.shouldRunWithNativeGit();
    this.gitDescribeConfig = dp.getGitDescribeConfig();
    this.commitIdGenerationMode = dp.getCommitIdGenerationMode();
    this.useBranchNameFromBuildEnvironment = dp.shouldUseBranchNameFromBuildEnvironment();
    this.shouldRunOffline = dp.shouldRunOffline();
    this.evaluateOnCommit = dp.getEvaluateOnCommit();
    this.dotGitDirectory = dp.getDotGitDirectory();
    this.callbackBeforePropertiesAreWrittenToFile = dp::callBeforePropertiesAreWrittenToFile;
  }

  public Properties gatherGitData(@Nullable Properties existingProperties) throws GitCommitIdExecutionException {
    final Properties properties = Optional.ofNullable(existingProperties).orElseGet(() -> new Properties());


    loadGitData(properties);
    loadBuildData(properties);

    PropertiesFilterer propertiesFilterer = new PropertiesFilterer(log);
    // first round of publication and filtering (we need to make variables available for the ParameterExpressionEvaluator
    propertiesFilterer.filter(properties, includeOnlyProperties, prefixDot);
    propertiesFilterer.filterNot(properties, excludeProperties, prefixDot);

    callbackBeforePropertiesAreWrittenToFile.accept(properties);
    logProperties(log, properties);

    if (shouldGeneratePropertiesFile) {
      new PropertiesFileGenerator(
              log,
              buildFileChangeListener,
              // TODO: This should be an enum
              propertiesOutputFormat.toString().toLowerCase(),
              prefixDot,
              projectName
      ).maybeGeneratePropertiesFile(
              properties,
              projectBaseDir,
              generateGitPropertiesFilename,
              propertiesOutputFileSourceCharset
      );
    }
    return properties;
  }

  private void loadBuildData(Properties existingProperties) throws GitCommitIdExecutionException {
    Map<String, Supplier<String>> additionalProperties = Collections.singletonMap(
            GitCommitPropertyConstant.BUILD_VERSION, () -> projectVersion);
    BuildServerDataProvider buildServerDataProvider = BuildServerDataProvider.getBuildServerProvider(
            System.getenv(), log);
    buildServerDataProvider
            .setDateFormat(dateFormat)
            .setDateFormatTimeZone(dateFormatTimeZone)
            .setAdditionalProperties(additionalProperties)
            .setPrefixDot(prefixDot)
            .setExcludeProperties(excludeProperties)
            .setIncludeOnlyProperties(includeOnlyProperties);

    buildServerDataProvider.loadBuildData(existingProperties, parseOutputTimestamp(projectBuildOutputTimestamp));
  }

  /**
   * Parse output timestamp configured for Reproducible Builds' archive entries
   * (https://maven.apache.org/guides/mini/guide-reproducible-builds.html).
   * The value from <code>${project.build.outputTimestamp}</code> is either formatted as ISO 8601
   * <code>yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like
   * <a href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>.
   *
   * Inspired by https://github.com/apache/maven-archiver/blob/a3103d99396cd8d3440b907ef932a33563225265/src/main/java/org/apache/maven/archiver/MavenArchiver.java#L765
   *
   * @param outputTimestamp the value of <code>${project.build.outputTimestamp}</code> (may be <code>null</code>)
   * @return the parsed timestamp, may be <code>null</code> if <code>null</code> input or input contains only 1
   *         character
   */
  private Date parseOutputTimestamp(String outputTimestamp) throws GitCommitIdExecutionException {
    if (outputTimestamp != null && !outputTimestamp.trim().isEmpty() && outputTimestamp.chars().allMatch(Character::isDigit)) {
      return new Date(Long.parseLong(outputTimestamp) * 1000);
    }

    if ((outputTimestamp == null) || (outputTimestamp.length() < 2)) {
      // no timestamp configured
      return null;
    }

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    try {
      return df.parse(outputTimestamp);
    } catch (ParseException pe) {
      throw new GitCommitIdExecutionException(
              "Invalid 'project.build.outputTimestamp' value '" + outputTimestamp + "'",
              pe);
    }
  }

  private void logProperties(@Nonnull LogInterface log, @Nonnull Properties properties) {
    for (String propertyName : properties.stringPropertyNames()) {
      log.info(String.format("including property %s in results", propertyName));
    }
  }

  private void loadGitData(@Nonnull Properties properties) throws GitCommitIdExecutionException {
    if (shouldRunWithNativeGit) {
      loadGitDataWithNativeGit(properties);
    } else {
      loadGitDataWithJGit(properties);
    }
  }

  private void loadGitDataWithNativeGit(@Nonnull Properties properties) throws GitCommitIdExecutionException {
    try {
      // TODO: I guess this should use dotGitDirectory.getParentFile().getCanonicalFile()
      final File basedir = projectBaseDir.getCanonicalFile();

      GitDataProvider nativeGitProvider = NativeGitProvider
              .on(basedir, nativeGitTimeoutInMs, log)
              .setPrefixDot(prefixDot)
              .setAbbrevLength(abbrevLength)
              .setDateFormat(dateFormat)
              .setDateFormatTimeZone(dateFormatTimeZone)
              .setGitDescribe(gitDescribeConfig)
              .setCommitIdGenerationMode(commitIdGenerationMode)
              .setUseBranchNameFromBuildEnvironment(useBranchNameFromBuildEnvironment)
              .setExcludeProperties(excludeProperties)
              .setIncludeOnlyProperties(includeOnlyProperties)
              .setOffline(shouldRunOffline);

      nativeGitProvider.loadGitData(evaluateOnCommit, properties);
    } catch (IOException e) {
      throw new GitCommitIdExecutionException(e);
    }
  }

  private void loadGitDataWithJGit(@Nonnull Properties properties) throws GitCommitIdExecutionException {
    GitDataProvider jGitProvider = JGitProvider
            .on(dotGitDirectory, log)
            .setPrefixDot(prefixDot)
            .setAbbrevLength(abbrevLength)
            .setDateFormat(dateFormat)
            .setDateFormatTimeZone(dateFormatTimeZone)
            .setGitDescribe(gitDescribeConfig)
            .setCommitIdGenerationMode(commitIdGenerationMode)
            .setUseBranchNameFromBuildEnvironment(useBranchNameFromBuildEnvironment)
            .setExcludeProperties(excludeProperties)
            .setIncludeOnlyProperties(includeOnlyProperties)
            .setOffline(shouldRunOffline);

    jGitProvider.loadGitData(evaluateOnCommit, properties);
  }
}
