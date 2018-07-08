package pl.project13.maven.git.build;

import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.GitCommitPropertyConstant;
import pl.project13.maven.git.log.LoggerBridge;

import java.util.Map;
import java.util.Properties;

public class HudsonBuildServerData extends BuildServerDataProvider {
  HudsonBuildServerData(LoggerBridge log) {
    super(log);
  }

  @Override
  public BuildEnvironmentType getBuildEnvironmentType() {
    return BuildEnvironmentType.HUDSON;
  }

  /**
   * @see <a href="http://wiki.eclipse.org/Using_Hudson/Building_a_software_project">Hudson</a>
   */
  public static boolean isActiveServer(@NotNull Map<String, String> env) {
    return env.containsKey("HUDSON_URL") || env.containsKey("HUDSON_HOME");
  }

  @Override
  void loadBuildNumber(@NotNull Map<String, String> env, @NotNull Properties properties) {
    String buildNumber = env.get("BUILD_NUMBER");

    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber == null ? "" : buildNumber);
  }

  @Override
  public String getBuildBranch(@NotNull Map<String, String> env, @NotNull LoggerBridge log) {
    return new JenkinsBuildServerData(log).getBuildBranch(env,log);
  }
}
