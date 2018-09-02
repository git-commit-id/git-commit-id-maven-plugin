package pl.project13.maven.git.build;

import pl.project13.maven.git.log.LoggerBridge;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Properties;

public class UnknownBuildServerData extends BuildServerDataProvider {
  public UnknownBuildServerData(@Nonnull LoggerBridge log, @Nonnull Map<String, String> env) {
    super(log, env);
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
  }

  @Override
  public String getBuildBranch() {
    return "";
  }
}
