package pl.project13.maven.git.build;

import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.log.LoggerBridge;

import java.util.Map;
import java.util.Properties;

public class UnknownBuildServerData extends BuildServerDataProvider {
  public UnknownBuildServerData(@NotNull LoggerBridge log, @NotNull Map<String, String> env) {
    super(log, env);
  }

  @Override
  void loadBuildNumber(@NotNull Properties properties) {
  }

  @Override
  public String getBuildBranch() {
    return "";
  }
}
