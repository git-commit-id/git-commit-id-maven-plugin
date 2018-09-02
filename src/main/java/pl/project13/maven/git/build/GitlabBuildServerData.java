package pl.project13.maven.git.build;

import pl.project13.maven.git.GitCommitPropertyConstant;
import pl.project13.maven.git.log.LoggerBridge;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Properties;

public class GitlabBuildServerData extends BuildServerDataProvider {

  GitlabBuildServerData(LoggerBridge log, @Nonnull Map<String, String> env) {
    super(log,env);
  }

  /**
   * @see <a href="https://docs.gitlab.com/ce/ci/variables/#predefined-variables-environment-variables">GitlabCIVariables</a>
   */
  public static boolean isActiveServer(Map<String, String> env) {
    return env.containsKey("CI");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    // GITLAB CI
    // CI_PIPELINE_ID will be present if in a Gitlab CI environment (Gitlab >8.10 & Gitlab CI >0.5)  and contains a server wide unique ID for a pipeline run
    String uniqueBuildNumber = env.get("CI_PIPELINE_ID");
    // CI_PIPELINE_IID will be present if in a Gitlab CI environment (Gitlab >11.0) and contains the project specific build number
    String buildNumber = env.get("CI_PIPELINE_IID");

    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber == null ? "" : buildNumber);
    put(properties,
        GitCommitPropertyConstant.BUILD_NUMBER_UNIQUE,
        uniqueBuildNumber == null ? "" : uniqueBuildNumber);
  }

  @Override
  public String getBuildBranch() {
    String environmentBasedBranch = env.get("CI_COMMIT_REF_NAME");
    log.info("Using environment variable based branch name. CI_COMMIT_REF_NAME = {}", environmentBasedBranch);
    return environmentBasedBranch;
  }
}
