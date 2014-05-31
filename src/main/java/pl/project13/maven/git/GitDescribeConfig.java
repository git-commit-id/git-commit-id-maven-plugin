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
   * <p/>
   * Although it's highly recommended to use <pre>git-describe</pre> to identify your build state,
   * so think twice before disabeling it.
   *
   * @parameter default-value=false
   */
  private boolean skip;

  /**
   * <pre>--always</pre>
   * <p>Show uniquely abbreviated commit object as fallback.</p>
   *
   * <b>true</b> by default. (Doesn't really make much sense to disable this option).
   *
   * @parameter default-value=true
   */
  private boolean always;

  /**
   * <pre>--dirty[=mark]</pre>
   * Describe the working tree. It means describe HEAD and appends mark (<pre>-dirty</pre> by default) if the
   * working tree is dirty.
   *
   * <b>-devel</b> by default, following git's behaviour.
   *
   * @parameter default-value="devel"
   */
  private String dirty;

  /**
   *<pre>--match glob-pattern</pre>
   * Only consider tags matching the given pattern (can be used to avoid leaking private tags made from the repository).
   *
   * <b>*</b> by default, following git's behaviour.
   *
   * @parameter default-value="*"
   */
  private String match;

  /**
   * <pre>--abbrev=N</pre>
   * <p>
   * Instead of using the default <em>7 hexadecimal digits</em> as the abbreviated object name,
   * use <b>N</b> digits, or as many digits as needed to form a unique object name.
   * Valid values range form 2 to 40 (inclusive). With special treatment for 0 (see "Special values").
   * </p>
   *
   * <p>
   *   <strong>Special values:</strong>
   *   <ul>
   *     <li>
   *       0 - will suppress long format, only showing the closest tag. (Won't show anything about the commit's id).
   *     </li>
   *     <li>
   *       <strong>1 - is invalid</strong>. Git's minimal abbrev lenght is 2 chars.
   *       This will be silently ignored and you'll get a full commit id.
   *     </li>
   *   </ul>
   * </p>
   *
   * Examples:
   *
   * <pre>
   * > git describe
   *    some-tag-2-gb6a7335 # the default 7 kicked in
   *
   * > git describe --abbrev=0
   *   some-tag
   * > git describe --abbrev=3
   *   some-tag-2-gb6a73 # you specified 3, but git determined that it wouldn't be unique, and returned 5 chars instead!
   *
   * > git describe --abbrev=40
   *   some-tag-2-gb6a73ed747dd8dc98642d731ddbf09824efb9d48
   * </pre>
   *
   * @parameter default-value=7
   */
  private int abbrev;

  /**
   * <pre>--tags</pre>
   * <p>
   * Instead of using only the annotated tags, use any tag found in .git/refs/tags.
   * This option enables matching a lightweight (non-annotated) tag.
   * </p>
   *
   * <p>Searching for lightweight tags is <b>false</b> by default.</p>
   * <p/>
   *
   * Example:
   * <pre>
   *    b6a73ed - (HEAD, master)
   *    d37a598 - (v1.0-fixed-stuff) - a lightweight tag (with no message)
   *    9597545 - (v1.0) - an annotated tag
   *
   *  > git describe
   *    annotated-tag-2-gb6a73ed     # the nearest "annotated" tag is found
   *
   *  > git describe --tags
   *    lightweight-tag-1-gb6a73ed   # the nearest tag (including lightweights) is found
   * </pre>
   *
   * <p>
   * Using only annotated tags to mark builds may be useful if you're using tags to help yourself with annotating
   * things like "i'll get back to that" etc - you don't need such tags to be exposed. But if you want lightweight
   * tags to be included in the search, enable this option.
   * </p>
   *
   * @parameter default-value=false
   */
  private boolean tags;

  /**
   * <pre>--long</pre>
   * <p/>
   * Always output the long format (the tag, the number of commits and the abbreviated commit name)
   * even when it matches a tag. This is useful when you want to see parts of the commit object name
   * in "describe" output, even when the commit in question happens to be a tagged version. Instead
   * of just emitting the tag name, it will describe such a commit as v1.2-0-gdeadbee (0th commit
   * since tag v1.2 that points at object deadbee....).
   * <p/>
   * <pre>false</pre> by default.
   */
  private boolean forceLongFormat;

  public GitDescribeConfig() {
  }

  public GitDescribeConfig(boolean always, String dirty, String match, Integer abbrev, boolean forceLongFormat, boolean tags) {
    this.always = always;
    this.dirty = dirty;
    this.match = match;
    this.abbrev = abbrev;
    this.forceLongFormat = forceLongFormat;
    this.tags = tags;
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

  public String getMatch() {
    return match;
  }

  public void setMatch(String match) {
    this.match = match;
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

  public boolean getForceLongFormat() {
    return forceLongFormat;
  }

  public void setForceLongFormat(boolean forceLongFormat) {
    this.forceLongFormat = forceLongFormat;
  }

  public boolean getTags() {
    return tags;
  }

  public void setTags(boolean tags) {
    this.tags = tags;
  }

  @Override
  public String toString() {
    return "GitDescribeConfig{" +
        "skip=" + skip +
        ", always=" + always +
        ", dirty='" + dirty + '\'' +
        ", match='" + match + '\'' +
        ", abbrev=" + abbrev +
        ", tags=" + tags +
        ", forceLongFormat=" + forceLongFormat +
        '}';
  }
}
