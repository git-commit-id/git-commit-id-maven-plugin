/*
 * This file is part of git-commit-id-plugin by Konrad Malawski <konrad.malawski@java.pl>
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

import com.google.common.base.Joiner;

public class StdOutLoggerBridge implements LoggerBridge {

  private boolean verbose;

  public StdOutLoggerBridge(boolean verbose) {
    this.verbose = verbose;
  }

  @Override
  public void log(Object... parts) {
    if (verbose) {
      System.out.println("LOG: " + Joiner.on(" ").join(parts));
    }
  }

  @Override
  public void error(Object... parts) {
    if(verbose) {
      System.out.println("ERR: " + Joiner.on(" ").join(parts));
    }
  }
  
  @Override
  public void debug(Object... parts) {
    if(verbose) {
      System.out.println("DBG: " + Joiner.on(" ").join(parts));
    }
  }

  @Override
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

}
