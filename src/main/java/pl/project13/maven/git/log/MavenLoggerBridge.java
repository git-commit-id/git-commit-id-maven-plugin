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

import java.util.Properties;

import com.google.common.base.Joiner;

import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.impl.SimpleLogger;
import org.slf4j.impl.SimpleLoggerFactory;

public class MavenLoggerBridge implements LoggerBridge {

  private Logger logger;
  private boolean verbose;

  public MavenLoggerBridge(Log log, boolean verbose) {
    setSimpleLoggerPorperties();
    this.logger = new SimpleLoggerFactory().getLogger(getClass().getName());
    this.verbose = verbose;
  }

  private void setSimpleLoggerPorperties() {
    Properties sysProperties = System.getProperties();
    if(!sysProperties.containsKey(SimpleLogger.SHOW_THREAD_NAME_KEY)){
      System.setProperty(SimpleLogger.SHOW_THREAD_NAME_KEY, String.valueOf(false));
    }
    if(!sysProperties.containsKey(SimpleLogger.LEVEL_IN_BRACKETS_KEY)){
      System.setProperty(SimpleLogger.LEVEL_IN_BRACKETS_KEY, String.valueOf(true));
    }
  }

  @Override
  public void log(Object... parts) {
    if (verbose) {
      logger.info(Joiner.on(" ").useForNull("null").join(parts));
    }
  }

  @Override
  public void error(Object... parts) {
    if (verbose) {
      logger.error(Joiner.on(" ").useForNull("null").join(parts));
    }
  }

  @Override
  public void debug(Object... parts) {
    if (verbose) {
      logger.debug(Joiner.on(" ").useForNull("null").join(parts));
    }
  }

  @Override
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  protected void setLogger(Logger logger){
    this.logger = logger;
  }

}
