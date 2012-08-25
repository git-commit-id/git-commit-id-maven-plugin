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

/**
 * Represents options passed in via maven configuration,
 * corresponds to options of git-describe.
 */
@SuppressWarnings("JavaDoc")
public class GitDescribeConfig {

  /**
   * If you don't use describe, you can always disable it and make the build a bit faster.
   *
   * Although it's highly recommended to use <pre>git-describe</pre> to identify your build state,
   * so think twice before disabeling it.
   *
   * @parameter default-value=false
   */
  private boolean skip;

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
  private int abbrev;

  /**
   * <pre>--long</pre>
   * <p/>
   * Always output the long format (the tag, the number of commits and the abbreviated commit name)
   * even when it matches a tag. This is useful when you want to see parts of the commit object name
   * in "describe" output, even when the commit in question happens to be a tagged version. Instead
   * of just emitting the tag name, it will describe such a commit as v1.2-0-gdeadbee (0th commit
   * since tag v1.2 that points at object deadbee....).
   * <p/>
   *
   * <pre>false</pre> by default.
   */
  private Boolean forceLongFormat;

  public GitDescribeConfig() {
  }

  public GitDescribeConfig(boolean always, String dirty, Integer abbrev, boolean forceLongFormat) {
    this.always = always;
    this.dirty = dirty;
    this.abbrev = abbrev;
    this.forceLongFormat = forceLongFormat;
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

  public int getAbbrev() {
    return abbrev;
  }

  public void setAbbrev(int abbrev) {
    this.abbrev = abbrev;
  }

  public boolean isSkip() {
    return skip;
  }

  public void setSkip(boolean skip) {
    this.skip = skip;
  }

  public Boolean getForceLongFormat() {
    return forceLongFormat;
  }

  public void setForceLongFormat(Boolean forceLongFormat) {
    this.forceLongFormat = forceLongFormat;
  }
}
