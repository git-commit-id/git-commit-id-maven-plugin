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

package pl.project13.jgit;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.eclipse.jgit.lib.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Represents the result of a <code>git describe</code> command.
 *
 * See {@link pl.project13.jgit.DescribeResult#toString()} for a detailed information how this result looks like.
 */
public class DescribeResult {

  public static final String DEFAULT_DIRTY_MARKER = "DEV";
  private Optional<String> tagName = Optional.absent();

  private Optional<ObjectId> commitId = Optional.absent();
  private int abbrev = 7;
  private int commitsAwayFromTag;

  private boolean dirty;
  private String dirtyMarker;

  public static final DescribeResult EMPTY = new DescribeResult("");

  public DescribeResult(String tagName) {
    this(tagName, 0, null);
  }

  public DescribeResult(ObjectId commitId) {
    this.commitId = Optional.of(commitId);
  }

  public DescribeResult(String tagName, int commitsAwayFromTag, @Nullable ObjectId commitId) {
    this(tagName, commitsAwayFromTag, commitId, false, Optional.<String>absent());
  }

  public DescribeResult(String tagName, int commitsAwayFromTag, ObjectId commitId, boolean dirty, String dirtyMarker) {
    this(tagName, commitsAwayFromTag, commitId, dirty, Optional.of(dirtyMarker));
  }

  @NotNull
  public DescribeResult withCommitIdAbbrev(int n) {
    Preconditions.checkArgument(n >= 0, String.format("The --abbrev parameter must be >= 0, but it was: [%s]", n));
    abbrev = n;
    return this;
  }

  public DescribeResult(String tagName, int commitsAwayFromTag, ObjectId commitId, boolean dirty, @NotNull Optional<String> dirtyMarker) {
    this.tagName = Optional.of(tagName);
    this.commitsAwayFromTag = commitsAwayFromTag;
    this.commitId = Optional.fromNullable(commitId);
    this.dirty = dirty;
    this.dirtyMarker = dirtyMarker.or(DEFAULT_DIRTY_MARKER);
  }

  public DescribeResult(ObjectId commitId, boolean dirty, @NotNull Optional<String> dirtyMarker) {
    this.commitId = Optional.of(commitId);
    this.dirty = dirty;
    this.dirtyMarker = dirtyMarker.or(DEFAULT_DIRTY_MARKER);
  }

  public DescribeResult(String tagName, boolean dirty) {
    this.tagName = Optional.of(tagName);
    this.dirty = dirty;
    this.dirtyMarker = DEFAULT_DIRTY_MARKER;
  }

  /**
   * The format of a describe result is defined as:
   * <pre>
   * v1.0.4-14-g2414721-DEV
   *   ^    ^    ^       ^
   *   |    |    |       |-- if a dirtyMarker was given, it will appear here if the repository is in "dirty" state
   *   |    |    |---------- the "g" prefixed commit id. The prefix is compatible with what git-describe would return - weird, but true.
   *   |    |--------------- the number of commits away from the found tag. So "2414721" is 14 commits ahead of "v1.0.4", in this example.
   *   |-------------------- the "nearest" tag, to the mentioned commit.
   * </pre>
   *
   * Other outputs may look like:
   * <pre>
   * v1.0.4 -- if the repository is "on a tag"
   * v1.0.4-DEV -- if the repository is "on a tag", but in "dirty" state
   * 2414721 -- a plain commit id hash if not tags were defined (of determined "near" this commit).
   *            It does NOT include the "g" prefix, that is used in the "full" describe output format!
   * </pre>
   *
   * For more details (on when what output will be returned etc), see <code>man git-describe</code>.
   * In general, you can assume it's a "best effort" approach, to give you as much info about the repo state as possible.
   *
   * @return the String representation of this Describe command
   */
  @Override
  public String toString() {
    List<String> parts = newArrayList(tag(), commitsAwayFromTag(), prefixedCommitId(), dirtyMarker());
    return Joiner.on("-").skipNulls().join(parts);
  }

  @Nullable
  public String commitsAwayFromTag() {
    return commitsAwayFromTag == 0 ? null : String.valueOf(commitsAwayFromTag);
  }

  @Nullable
  public String dirtyMarker() {
    return dirty ? dirtyMarker : null;
  }

  /**
   * The (possibly) "g" prefixed abbriverated commit id of a commit.
   *
   * The "g" prefix is prepended to be compatible with git's describe output, please refer to
   * <pre>man git-describe</pre> to check why it's included.
   *
   * The "g" prefix is used when a tag is defined on this result. If it's not, this method yields a plain commit id hash.
   * This is following git's behaviour - so any git tooling should be happy with this output.
   *
   * If you need the full commit id, it's always available via {@link pl.project13.jgit.DescribeResult#commitObjectId()}.
   */
  @Nullable
  public String prefixedCommitId() {
    if (commitId.isPresent()) {
      String fullHash = String.valueOf(commitId.get().getName());
      String abbrevHash = fullHash.substring(0, abbrev);

      if(tagName.isPresent()) {
        return "g" + abbrevHash;
      } else {
        return abbrevHash;
      }
    } else {
      return null;
    }
  }

  @Nullable
  public ObjectId commitObjectId() {
    if (commitId.isPresent()) {
      return commitId.get();
    } else {
      return null;
    }
  }

  @Nullable
  public String tag() {
    return tagName.orNull();
  }
}
