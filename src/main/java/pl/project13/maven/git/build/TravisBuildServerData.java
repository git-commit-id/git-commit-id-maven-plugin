package pl.project13.maven.git.build;

import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.GitCommitPropertyConstant;
import pl.project13.maven.git.log.LoggerBridge;

import java.util.Map;
import java.util.Properties;

public class TravisBuildServerData extends BuildServerDataProvider {

  TravisBuildServerData(LoggerBridge log) {
    super(log);
  }

  @Override
  public BuildEnvironmentType getBuildEnvironmentType() {
    return BuildEnvironmentType.TRAVIS;
  }

  /**
   * @see <a href=https://docs.travis-ci.com/user/environment-variables/#Default-Environment-Variables>Travis</a>
   */
  public static boolean isActiveServer(@NotNull Map<String, String> env) {
    return env.containsKey("TRAVIS");
  }

  @Override
  void loadBuildNumber(@NotNull Map<String, String> env, @NotNull Properties properties) {
    String buildNumber = env.get("TRAVIS_BUILD_NUMBER");
    String uniqueBuildNumber = env.get("TRAVIS_BUILD_ID");

    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber == null ? "" : buildNumber);
    put(properties, GitCommitPropertyConstant.BUILD_NUMBER_UNIQUE, uniqueBuildNumber == null ? "" : uniqueBuildNumber);
  }

  @Override
  public String getBuildBranch(@NotNull Map<String, String> env, @NotNull LoggerBridge log) {
    String environmentBasedBranch = env.get("TRAVIS_BRANCH");
    log.info("Using environment variable based branch name. TRAVIS_BRANCH = {}", environmentBasedBranch);
    return environmentBasedBranch;
  }
}
