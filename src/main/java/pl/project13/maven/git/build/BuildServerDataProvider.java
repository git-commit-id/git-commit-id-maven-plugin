package pl.project13.maven.git.build;

import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.GitCommitPropertyConstant;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.util.PropertyManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

public abstract class BuildServerDataProvider {

  private final LoggerBridge log;
  private String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
  private String dateFormatTimeZone = null;
  private String prefixDot = "";
  private MavenProject project = null;

  BuildServerDataProvider(LoggerBridge log) {
    this.log = log;
  }

  public BuildServerDataProvider setDateFormat(@NotNull String dateFormat) {
    this.dateFormat = dateFormat;
    return this;
  }

  public BuildServerDataProvider setDateFormatTimeZone(@NotNull String dateFormatTimeZone) {
    this.dateFormatTimeZone = dateFormatTimeZone;
    return this;
  }

  public BuildServerDataProvider setProject(@NotNull MavenProject project) {
    this.project = project;
    return this;
  }

  public BuildServerDataProvider setPrefixDot(@NotNull String prefixDot) {
    this.prefixDot = prefixDot;
    return this;
  }

  public abstract BuildEnvironmentType getBuildEnvironmentType();

  /**
   * Get the {@link BuildServerDataProvider} impelementation for the running environment
   * @param env environment variables which get used to identify the environment
   * @param log logging provider which will be used to log events
   * @return the corresponding {@link BuildServerDataProvider} for your environment or {@link UnknownBuildServerData}
   */
  public static BuildServerDataProvider getBuildServerProvider(@NotNull Map<String, String> env, LoggerBridge log) {
    switch (checkBuildEnvironmentForType(env)) {
      case JENKINS:
        return new JenkinsBuildServerData(log);
      case HUDSON:
        return new HudsonBuildServerData(log);
      case GITLAB:
        return new GitlabBuildServerData(log);
      case TRAVIS:
        return new TravisBuildServerData(log);
      case TEAM_CITY:
        return new TeamCityBuildServerData(log);
      case BAMBOO:
        return new BambooBuildServerData(log);
      case UNKNOWN:
      default:
        return new UnknownBuildServerData(log);
    }
  }

  public void loadBuildData(@NotNull Properties properties) {
    loadBuildVersionAndTimeData(properties);
    loadBuildHostData(properties);
    loadBuildNumber(System.getenv(), properties);
  }

  /**
   * Fill the properties file build number environment variables which are supplied by build servers
   * build.number is the project specific build number, if this number is not available the unique number will be used
   * build.number.unique is a server wide unique build number
   *
   * @param properties a properties instance to put the entries on
   */
  abstract void loadBuildNumber(@NotNull Map<String, String> env, @NotNull Properties properties);

  /**
   * @param env environment variables which get used to identify the environment
   * @param log logging provider which will be used to log events
   * @return the branch name provided by the server or an empty string
   */
  public abstract String getBuildBranch(@NotNull Map<String, String> env, @NotNull LoggerBridge log);

  private void loadBuildVersionAndTimeData(@NotNull Properties properties) {
    Date buildDate = new Date();
    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
    if (dateFormatTimeZone != null) {
      smf.setTimeZone(TimeZone.getTimeZone(dateFormatTimeZone));
    }
    put(properties, GitCommitPropertyConstant.BUILD_TIME, smf.format(buildDate));

    if (project != null) {
      put(properties, GitCommitPropertyConstant.BUILD_VERSION, project.getVersion());
    }
  }

  private void loadBuildHostData(@NotNull Properties properties) {
    String buildHost = null;
    try {
      buildHost = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      log.info("Unable to get build host, skipping property {}. Error message: {}",
          GitCommitPropertyConstant.BUILD_HOST,
          e.getMessage());
    }
    put(properties, GitCommitPropertyConstant.BUILD_HOST, buildHost);
  }

  protected void put(@NotNull Properties properties, String key, String value) {
    String keyWithPrefix = prefixDot + key;
    log.info("{} {}", keyWithPrefix, value);
    PropertyManager.putWithoutPrefix(properties, keyWithPrefix, value);
  }

  public static BuildEnvironmentType checkBuildEnvironmentForType(Map<String, String> env) {
    if (BambooBuildServerData.isActiveServer(env)) {
      return BuildEnvironmentType.BAMBOO;
    }
    if (GitlabBuildServerData.isActiveServer(env)) {
      return BuildEnvironmentType.GITLAB;
    }
    if (HudsonBuildServerData.isActiveServer(env)) {
      return BuildEnvironmentType.HUDSON;
    }
    if (JenkinsBuildServerData.isActiveServer(env)) {
      return BuildEnvironmentType.JENKINS;
    }
    if (TeamCityBuildServerData.isActiveServer(env)) {
      return BuildEnvironmentType.TEAM_CITY;
    }
    if (TravisBuildServerData.isActiveServer(env)) {
      return BuildEnvironmentType.TRAVIS;
    }
    return BuildEnvironmentType.UNKNOWN;
  }
}
