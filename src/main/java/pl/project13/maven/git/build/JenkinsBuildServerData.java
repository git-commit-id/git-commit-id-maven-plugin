package pl.project13.maven.git.build;

import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.GitCommitPropertyConstant;
import pl.project13.maven.git.log.LoggerBridge;

import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;

public class JenkinsBuildServerData extends BuildServerDataProvider {

  JenkinsBuildServerData(LoggerBridge log) {
    super(log);
  }

  @Override
  public BuildEnvironmentType getBuildEnvironmentType() {
    return BuildEnvironmentType.JENKINS;
  }

  /**
   * @see <a href="https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables">JenkinsSetEnvironmentVariables</a>
   */
  public static boolean isActiveServer(@NotNull Map<String, String> env) {
    return env.containsKey("JENKINS_URL") || env.containsKey("JENKINS_HOME");
  }

  @Override
  void loadBuildNumber(@NotNull Map<String, String> env, @NotNull Properties properties) {
    String buildNumber = env.get("BUILD_NUMBER");

    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber == null ? "" : buildNumber);
  }

  @Override
  public String getBuildBranch(@NotNull Map<String, String> env, @NotNull LoggerBridge log) {
    String environmentBasedLocalBranch = env.get("GIT_LOCAL_BRANCH");
    if (!isNullOrEmpty(environmentBasedLocalBranch)) {
      log.info("Using environment variable based branch name. GIT_LOCAL_BRANCH = {}",
          environmentBasedLocalBranch);
      return environmentBasedLocalBranch;
    }
    String environmentBasedBranch = env.get("GIT_BRANCH");
    log.info("Using environment variable based branch name. GIT_BRANCH = {}",
        environmentBasedBranch);
    return environmentBasedBranch;
  }
}
