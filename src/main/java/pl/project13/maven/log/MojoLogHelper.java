package pl.project13.maven.log;

import org.apache.maven.plugin.logging.Log;
import pl.project13.core.log.LogInterface;

import javax.annotation.Nonnull;
import java.util.Objects;


public final class MojoLogHelper implements LogInterface {

  private final Log log;
  private final boolean isVerbose;

  public MojoLogHelper(@Nonnull Log log, boolean isVerbose) {
    this.log = Objects.requireNonNull(log);
    this.isVerbose = isVerbose;
  }

  @Override
  public void debug(String msg) {
    if (isVerbose) {
      log.debug(msg);
    }
  }

  @Override
  public void info(String msg) {
    if (isVerbose) {
      log.info(msg);
    }
  }

  @Override
  public void warn(String msg) {
    log.warn(msg);
  }

  @Override
  public void error(String msg, Throwable t) {
    log.error(msg, t);
  }
}
