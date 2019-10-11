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

/**
 * Logs everything to System.out.
 */
public class StdOutLoggerBridge implements LoggerBridge {

  private boolean verbose;

  public StdOutLoggerBridge(boolean verbose) {
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
    return true;
  }

  @Override
  public void debug(String msg) {
    if (verbose) {
      System.out.println("[DEBUG] " + msg);
    }
  }

  @Override
  public void debug(String format, Object arg) {
    if (verbose) {
      System.out.println("[DEBUG] " + MessageFormatter.format(format, arg).getMessage());
    }
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    if (verbose) {
      System.out.println("[DEBUG] " + MessageFormatter.format(format, arg1, arg2).getMessage());
    }
  }

  @Override
  public void debug(String format, Object... arguments) {
    if (verbose) {
      System.out.println("[DEBUG] " + MessageFormatter.arrayFormat(format, arguments).getMessage());
    }
  }

  @Override
  public void debug(String msg, Throwable t) {
    if (verbose) {
      System.out.println("[DEBUG] " + msg);
      t.printStackTrace(System.out);
    }
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public void info(String msg) {
    if (verbose) {
      System.out.println("[INFO] " + msg);
    }
  }

  @Override
  public void info(String format, Object arg) {
    if (verbose) {
      System.out.println("[INFO] " + MessageFormatter.format(format, arg).getMessage());
    }
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    if (verbose) {
      System.out.println("[INFO] " + MessageFormatter.format(format, arg1, arg2).getMessage());
    }
  }

  @Override
  public void info(String format, Object... arguments) {
    if (verbose) {
      System.out.println("[INFO] " + MessageFormatter.arrayFormat(format, arguments).getMessage());
    }
  }

  @Override
  public void info(String msg, Throwable t) {
    if (verbose) {
      System.out.println("[INFO] " + msg);
      t.printStackTrace(System.out);
    }
  }

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public void warn(String msg) {
    if (verbose) {
      System.out.println("[WARN] " + msg);
    }
  }

  @Override
  public void warn(String format, Object arg) {
    if (verbose) {
      System.out.println("[WARN] " + MessageFormatter.format(format, arg).getMessage());
    }
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    if (verbose) {
      System.out.println("[WARN] " + MessageFormatter.format(format, arg1, arg2).getMessage());
    }
  }

  @Override
  public void warn(String format, Object... arguments) {
    if (verbose) {
      System.out.println("[WARN] " + MessageFormatter.arrayFormat(format, arguments).getMessage());
    }
  }

  @Override
  public void warn(String msg, Throwable t) {
    if (verbose) {
      System.out.println("[WARN] " + msg);
      t.printStackTrace(System.out);
    }
  }

  @Override
  public boolean isErrorEnabled() {
    return true;
  }

  @Override
  public void error(String msg) {
    if (verbose) {
      System.out.println("[ERROR] " + msg);
    }
  }

  @Override
  public void error(String format, Object arg) {
    if (verbose) {
      System.out.println("[ERROR] " + MessageFormatter.format(format, arg).getMessage());
    }
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    if (verbose) {
      System.out.println("[ERROR] " + MessageFormatter.format(format, arg1, arg2).getMessage());
    }

  }

  @Override
  public void error(String format, Object... arguments) {
    if (verbose) {
      System.out.println("[ERROR] " + MessageFormatter.arrayFormat(format, arguments).getMessage());
    }
  }

  @Override
  public void error(String msg, Throwable t) {
    if (verbose) {
      System.out.println("[ERROR] " + msg);
      t.printStackTrace(System.out);
    }
  }
}