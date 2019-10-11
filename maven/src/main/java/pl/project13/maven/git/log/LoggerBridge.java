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
 * Interface for logging in git-commit-id plugin. Isolates logging to enable future possible code reuse in other tools.
 * Adds verbosity flag. Given SLF4J popularity and recent internal Maven adoption, follows SLF4J interfaces.
 */
public interface LoggerBridge {

  /**
   * Returns true if plugin does verbose logging.
   *
   * @return true if plugin does verbose logging
   */
  boolean isVerbose();

  /**
   * Sets plugin to verbose mode.
   *
   * @param verbose plugin verbosity flag
   */
  void setVerbose(boolean verbose);

  /*
      The following borrowed from SLF4J Logger.java
      Which is MIT licensed and thus compatible with this project's license.
  */

  /**
   * Copyright (c) 2004-2011 QOS.ch
   * All rights reserved.
   *
   * Permission is hereby granted, free  of charge, to any person obtaining
   * a  copy  of this  software  and  associated  documentation files  (the
   * "Software"), to  deal in  the Software without  restriction, including
   * without limitation  the rights to  use, copy, modify,  merge, publish,
   * distribute,  sublicense, and/or sell  copies of  the Software,  and to
   * permit persons to whom the Software  is furnished to do so, subject to
   * the following conditions:
   *
   * The  above  copyright  notice  and  this permission  notice  shall  be
   * included in all copies or substantial portions of the Software.
   *
   * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
   * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
   * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
   * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
   * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
   * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
   * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
   *
   */

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return True if this Logger is enabled for the DEBUG level,
   *         false otherwise.
   */
  boolean isDebugEnabled();

  /**
   * Log a message at the DEBUG level.
   *
   * @param msg the message string to be logged
   */
  void debug(String msg);

  /**
   * Log a message at the DEBUG level according to the specified format
   * and argument.
   *
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the DEBUG level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   */
  void debug(String format, Object arg);

  /**
   * Log a message at the DEBUG level according to the specified format
   * and arguments.
   *
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the DEBUG level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  void debug(String format, Object arg1, Object arg2);

  /**
   * Log a message at the DEBUG level according to the specified format
   * and arguments.
   *
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the DEBUG level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for DEBUG. The variants taking
   * {@link #debug(String, Object) one} and {@link #debug(String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  void debug(String format, Object... arguments);

  /**
   * Log an exception (throwable) at the DEBUG level with an
   * accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  void debug(String msg, Throwable t);

  /**
   * Is the logger instance enabled for the INFO level?
   *
   * @return True if this Logger is enabled for the INFO level,
   *         false otherwise.
   */
  boolean isInfoEnabled();

  /**
   * Log a message at the INFO level.
   *
   * @param msg the message string to be logged
   */
  void info(String msg);

  /**
   * Log a message at the INFO level according to the specified format
   * and argument.
   *
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the INFO level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   */
  void info(String format, Object arg);

  /**
   * Log a message at the INFO level according to the specified format
   * and arguments.
   *
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the INFO level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  void info(String format, Object arg1, Object arg2);

  /**
   * Log a message at the INFO level according to the specified format
   * and arguments.
   *
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the INFO level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for INFO. The variants taking
   * {@link #info(String, Object) one} and {@link #info(String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  void info(String format, Object... arguments);

  /**
   * Log an exception (throwable) at the INFO level with an
   * accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  void info(String msg, Throwable t);

  /**
   * Is the logger instance enabled for the WARN level?
   *
   * @return True if this Logger is enabled for the WARN level,
   *         false otherwise.
   */
  boolean isWarnEnabled();

  /**
   * Log a message at the WARN level.
   *
   * @param msg the message string to be logged
   */
  void warn(String msg);

  /**
   * Log a message at the WARN level according to the specified format
   * and argument.
   *
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the WARN level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   */
  void warn(String format, Object arg);

  /**
   * Log a message at the WARN level according to the specified format
   * and arguments.
   *
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the WARN level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  void warn(String format, Object arg1, Object arg2);

  /**
   * Log a message at the WARN level according to the specified format
   * and arguments.
   *
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the WARN level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for WARN. The variants taking
   * {@link #warn(String, Object) one} and {@link #warn(String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  void warn(String format, Object... arguments);

  /**
   * Log an exception (throwable) at the WARN level with an
   * accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  void warn(String msg, Throwable t);

  /**
   * Is the logger instance enabled for the ERROR level?
   *
   * @return True if this Logger is enabled for the ERROR level,
   *         false otherwise.
   */
  boolean isErrorEnabled();

  /**
   * Log a message at the ERROR level.
   *
   * @param msg the message string to be logged
   */
  void error(String msg);

  /**
   * Log a message at the ERROR level according to the specified format
   * and argument.
   *
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the ERROR level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   */
  void error(String format, Object arg);

  /**
   * Log a message at the ERROR level according to the specified format
   * and arguments.
   *
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the ERROR level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  void error(String format, Object arg1, Object arg2);

  /**
   * Log a message at the ERROR level according to the specified format
   * and arguments.
   *
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the ERROR level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for ERROR. The variants taking
   * {@link #error(String, Object) one} and {@link #error(String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  void error(String format, Object... arguments);

  /**
   * Log an exception (throwable) at the ERROR level with an
   * accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  void error(String msg, Throwable t);
}
