package pl.project13.maven.git.build;

import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.GitCommitPropertyConstant;
import pl.project13.maven.git.log.LoggerBridge;

import java.util.Map;
import java.util.Properties;

public class BambooBuildServerData extends BuildServerDataProvider {

  BambooBuildServerData(LoggerBridge log) {
    super(log);
  }

  public static boolean isActiveServer(Map<String, String> env) {
    return env.containsKey("BAMBOO_BUILDKEY");
  }

  @Override
  public BuildEnvironmentType getBuildEnvironmentType() {
    return BuildEnvironmentType.BAMBOO;
  }

  @Override
  void loadBuildNumber(@NotNull Map<String, String> env, @NotNull Properties properties) {
    String buildNumber = env.get("BAMBOO_BUILDNUMBER");

    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber == null ? "" : buildNumber);
  }

  @Override
  public String getBuildBranch(@NotNull Map<String, String> env, @NotNull LoggerBridge log) {
    String environmentBasedBranch = env.get("BAMBOO_PLANREPOSITORY_BRANCH");
    log.info("Using environment variable based branch name. BAMBOO_PLANREPOSITORY_BRANCH = {}", environmentBasedBranch);
    return environmentBasedBranch;
  }
}
