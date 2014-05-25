package pl.project13.maven.git;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;

/**
*
* @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
*/
public abstract class GitDataProvider {
  @NotNull
  protected LoggerBridge loggerBridge;

  protected boolean verbose;

  protected String prefixDot;

  protected int abbrevLength;

  protected String dateFormat;

  protected GitDescribeConfig gitDescribe;

  public abstract void loadGitData(@NotNull Properties properties) throws IOException, MojoExecutionException;

  void log(String... parts) {
    if(loggerBridge!=null){
      loggerBridge.log((Object[]) parts);
    }
  }

  // TODO SL: clean this up
  protected void put(@NotNull Properties properties, String key, String value) {
    putWithoutPrefix(properties, prefixDot + key, value);
  }

  private void putWithoutPrefix(@NotNull Properties properties, String key, String value) {
    if (!isNotEmpty(value)) {
      value = "Unknown";
    }

    log(key, value);
    properties.put(key, value);
  }

  private boolean isNotEmpty(@Nullable String value) {
    return null != value && !" ".equals(value.trim().replaceAll(" ", ""));
  }
}
