package pl.project13.maven.git.build;

import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.log.LoggerBridge;

import java.util.Map;
import java.util.Properties;

public class UnknownBuildServerData extends BuildServerDataProvider {
  public UnknownBuildServerData(LoggerBridge log) {
    super(log);
  }

  @Override
  public BuildEnvironmentType getBuildEnvironmentType() {
    return BuildEnvironmentType.UNKNOWN;
  }

  @Override
  void loadBuildNumber(@NotNull Map<String, String> env, @NotNull Properties properties) {
  }

  @Override
  public String getBuildBranch(@NotNull Map<String, String> env, @NotNull LoggerBridge log) {
    return "";
  }
}
