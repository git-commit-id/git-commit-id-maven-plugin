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

package pl.project13.maven.git;

@SuppressWarnings("JavaDoc")
public class GitDescribeConfig {

  /**
   * @parameter default-value=true
   */
  private boolean always;

  /**
   * @parameter default-value="DEV"
   */
  private String dirty;

  /**
   * @parameter default-value=7
   */
  private Integer abbrev;

  public GitDescribeConfig() {
  }

  public GitDescribeConfig(boolean always, String dirty, Integer abbrev) {
    this.always = always;
    this.dirty = dirty;
    this.abbrev = abbrev;
  }

  public boolean isAlways() {
    return always;
  }

  public void setAlways(boolean always) {
    this.always = always;
  }

  public String getDirty() {
    return dirty;
  }

  public void setDirty(String dirty) {
    this.dirty = dirty;
  }

  public Integer getAbbrev() {
    return abbrev;
  }

  public void setAbbrev(Integer abbrev) {
    this.abbrev = abbrev;
  }
}
