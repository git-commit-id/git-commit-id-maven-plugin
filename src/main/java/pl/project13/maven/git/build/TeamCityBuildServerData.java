package pl.project13.maven.git.build;

import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.GitCommitPropertyConstant;
import pl.project13.maven.git.log.LoggerBridge;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class TeamCityBuildServerData extends BuildServerDataProvider {

  private final Properties teamcitySystemProperties = new Properties();

  TeamCityBuildServerData(@NotNull LoggerBridge log, @NotNull Map<String, String> env) {
    super(log, env);
    if (isActiveServer(env)) {
      //https://confluence.jetbrains.com/display/TCD18/Predefined+Build+Parameters
      try {
        teamcitySystemProperties.load(new FileInputStream(env.get("TEAMCITY_BUILD_PROPERTIES_FILE")));
      } catch (IOException | NullPointerException e) {
        log.error("Failed to retrieve Teamcity properties file", e);
      }
    }
  }

  /**
   * @see <a href=https://confluence.jetbrains.com/display/TCD18/Predefined+Build+Parameters#PredefinedBuildParameters-ServerBuildProperties>TeamCity</a>
   */
  public static boolean isActiveServer(@NotNull Map<String, String> env) {
    return env.containsKey("TEAMCITY_VERSION");
  }

  @Override
  void loadBuildNumber(@NotNull Properties properties) {
    String buildNumber = env.get("BUILD_NUMBER");
    String buildNumberUnique = teamcitySystemProperties.getProperty("teamcity.build.id");

    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber == null ? "" : buildNumber);
    put(properties,
        GitCommitPropertyConstant.BUILD_NUMBER_UNIQUE,
        buildNumberUnique == null ? "" : buildNumberUnique);
  }

  @Override
  public String getBuildBranch() {
    //there is no branch environment variable in TeamCity 10 or earlier
    String environmentBasedBranch = teamcitySystemProperties.getProperty("teamcity.build.branch");
    log.info("Using property file based branch name. teamcity.build.branch = {}",
        environmentBasedBranch);
    return environmentBasedBranch;
  }
}
