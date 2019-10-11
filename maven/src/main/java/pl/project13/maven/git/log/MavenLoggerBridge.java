/*
 * This file is part of git-commit-id-plugin by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.maven.git.log;

import org.apache.maven.plugin.Mojo;

/**
 * Bridges logging to standard Maven log adhering to verbosity level.
 */
public class MavenLoggerBridge implements LoggerBridge {

  private boolean verbose;
  private final Mojo mojo;

  public MavenLoggerBridge(Mojo mojo, boolean verbose) {
    this.mojo = mojo;
    this.verbose = verbose;
  }

  @Override
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  @Override
  public boolean isVerbose() {
    return verbose;
  }

  @Override
  public boolean isDebugEnabled() {
    return mojo.getLog().isDebugEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return mojo.getLog().isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return mojo.getLog().isWarnEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return mojo.getLog().isErrorEnabled();
  }

  @Override
  public void debug(String msg) {
    if (verbose) {
      mojo.getLog().debug(msg);
    }
  }

  @Override
  public void debug(String format, Object arg) {
    if (verbose) {
      debug(MessageFormatter.format(format, arg));
    }
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    if (verbose) {
      debug(MessageFormatter.format(format, arg1, arg2));
    }
  }

  @Override
  public void debug(String format, Object... arguments) {
    if (verbose) {
      debug(MessageFormatter.arrayFormat(format, arguments));
    }
  }

  @Override
  public void debug(String msg, Throwable t) {
    if (verbose) {
      mojo.getLog().debug(msg, t);
    }
  }

  @Override
  public void info(String msg) {
    if (verbose) {
      mojo.getLog().info(msg);
    }
  }

  @Override
  public void info(String format, Object arg) {
    if (verbose) {
      info(MessageFormatter.format(format, arg));
    }
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    if (verbose) {
      info(MessageFormatter.format(format, arg1, arg2));
    }
  }

  @Override
  public void info(String format, Object... arguments) {
    if (verbose) {
      info(MessageFormatter.arrayFormat(format, arguments));
    }
  }

  @Override
  public void info(String msg, Throwable t) {
    if (verbose) {
      mojo.getLog().info(msg, t);
    }
  }

  @Override
  public void warn(String msg) {
    if (verbose) {
      mojo.getLog().warn(msg);
    }
  }

  @Override
  public void warn(String format, Object arg) {
    if (verbose) {
      warn(MessageFormatter.format(format, arg));
    }
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    if (verbose) {
      warn(MessageFormatter.format(format, arg1, arg2));
    }
  }

  @Override
  public void warn(String format, Object... arguments) {
    if (verbose) {
      warn(MessageFormatter.arrayFormat(format, arguments));
    }
  }

  @Override
  public void warn(String msg, Throwable t) {
    if (verbose) {
      mojo.getLog().warn(msg, t);
    }
  }

  @Override
  public void error(String msg) {
    if (verbose) {
      mojo.getLog().error(msg);
    }
  }

  @Override
  public void error(String format, Object arg) {
    if (verbose) {
      error(MessageFormatter.format(format, arg));
    }
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    if (verbose) {
      error(MessageFormatter.format(format, arg1, arg2));
    }
  }

  @Override
  public void error(String format, Object... arguments) {
    if (verbose) {
      error(MessageFormatter.arrayFormat(format, arguments));
    }
  }

  @Override
  public void error(String msg, Throwable t) {
    if (verbose) {
      mojo.getLog().error(msg, t);
    }
  }

  private void debug(FormattingTuple tuple) {
    if (null == tuple.getThrowable()) {
      mojo.getLog().debug(tuple.getMessage());
    } else {
      mojo.getLog().debug(tuple.getMessage(), tuple.getThrowable());
    }
  }

  private void info(FormattingTuple tuple) {
    if (null == tuple.getThrowable()) {
      mojo.getLog().info(tuple.getMessage());
    } else {
      mojo.getLog().info(tuple.getMessage(), tuple.getThrowable());
    }
  }

  private void warn(FormattingTuple tuple) {
    if (null == tuple.getThrowable()) {
      mojo.getLog().warn(tuple.getMessage());
    } else {
      mojo.getLog().warn(tuple.getMessage(), tuple.getThrowable());
    }
  }

  private void error(FormattingTuple tuple) {
    if (null == tuple.getThrowable()) {
      mojo.getLog().error(tuple.getMessage());
    } else {
      mojo.getLog().error(tuple.getMessage(), tuple.getThrowable());
    }
  }
}