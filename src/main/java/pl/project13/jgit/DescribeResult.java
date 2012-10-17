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
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Represents the result of a <code>git describe</code> command.
 * <p/>
 * See {@link pl.project13.jgit.DescribeResult#toString()} for a detailed information how this result looks like.
 */
public class DescribeResult {

  private Optional<String> tagName = Optional.absent();

  private Optional<ObjectId> commitId = Optional.absent();
  private Optional<AbbreviatedObjectId> abbreviatedObjectId = Optional.absent();

  private int abbrev = 7;
  private int commitsAwayFromTag;

  private boolean dirty;
  private String dirtyMarker;

  private ObjectReader objectReader;

  public static final DescribeResult EMPTY = new DescribeResult("");

  public DescribeResult(@NotNull String tagName) {
    this(tagName, false, Optional.<String>absent());
  }

  public DescribeResult(@NotNull ObjectReader objectReader, String tagName, int commitsAwayFromTag, @Nullable ObjectId commitId) {
    this(objectReader, tagName, commitsAwayFromTag, commitId, false, Optional.<String>absent());
  }

  public DescribeResult(@NotNull ObjectReader objectReader, @NotNull ObjectId commitId) {
    this.objectReader = objectReader;

    this.commitId = Optional.of(commitId);
    this.abbreviatedObjectId = createAbbreviatedCommitId(objectReader, commitId, this.abbrev);
  }

  public DescribeResult(@NotNull ObjectReader objectReader, String tagName, int commitsAwayFromTag, ObjectId commitId, boolean dirty, String dirtyMarker) {
    this(objectReader, tagName, commitsAwayFromTag, commitId, dirty, Optional.of(dirtyMarker));
  }

  public DescribeResult(@NotNull ObjectReader objectReader, String tagName, int commitsAwayFromTag, ObjectId commitId, boolean dirty, Optional<String> dirtyMarker) {
    this(objectReader, commitId, dirty, dirtyMarker);
    this.tagName = Optional.of(tagName);
    this.commitsAwayFromTag = commitsAwayFromTag;
  }

  public DescribeResult(@NotNull ObjectReader objectReader, @NotNull ObjectId commitId, boolean dirty, @NotNull Optional<String> dirtyMarker) {
    this.objectReader = objectReader;

    this.commitId = Optional.of(commitId);
    this.abbreviatedObjectId = createAbbreviatedCommitId(objectReader, commitId, this.abbrev);

    this.dirty = dirty;
    this.dirtyMarker = dirtyMarker.or("");
  }

  public DescribeResult(@NotNull String tagName, boolean dirty, @NotNull Optional<String> dirtyMarker) {
    this.tagName = Optional.of(tagName);
    this.dirty = dirty;
    this.dirtyMarker = dirtyMarker.or("");
  }

  @NotNull
  public DescribeResult withCommitIdAbbrev(int n) {
    Preconditions.checkArgument(n >= 0, String.format("The --abbrev parameter must be >= 0, but it was: [%s]", n));
    this.abbrev = n;
    this.abbreviatedObjectId = createAbbreviatedCommitId(this.objectReader, this.commitId.get(), this.abbrev);
    return this;
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
   * <p/>
   * Other outputs may look like:
   * <pre>
   * v1.0.4 -- if the repository is "on a tag"
   * v1.0.4-DEV -- if the repository is "on a tag", but in "dirty" state
   * 2414721 -- a plain commit id hash if not tags were defined (of determined "near" this commit).
   *            It does NOT include the "g" prefix, that is used in the "full" describe output format!
   * </pre>
   * <p/>
   * For more details (on when what output will be returned etc), see <code>man git-describe</code>.
   * In general, you can assume it's a "best effort" approach, to give you as much info about the repo state as possible.
   *
   * @return the String representation of this Describe command
   */
  @Override
  public String toString() {
    List<String> parts;

    if (abbrevZeroHidesCommitsPartOfDescribe()) {
      parts = newArrayList(tag());
    } else {
      parts = newArrayList(tag(), commitsAwayFromTag(), prefixedCommitId());
    }

    return Joiner.on("-").skipNulls().join(parts) + dirtyMarker(); // like in the describe spec the entire "-dirty" is configurable (incl. "-")
  }

  private boolean abbrevZeroHidesCommitsPartOfDescribe() {
    return abbrev == 0;
  }

  @Nullable
  public String commitsAwayFromTag() {
    return commitsAwayFromTag == 0 ? null : String.valueOf(commitsAwayFromTag);
  }

  @Nullable
  public String dirtyMarker() {
    return dirty ? dirtyMarker : "";
  }

  /**
   * <p>The (possibly) "g" prefixed <strong>abbriverated</strong> object id of a commit.</p>
   * <p>
   * The "g" prefix is prepended to be compatible with git's describe output, please refer to
   * <b>man git-describe</b> to check why it's included.
   * </p>
   * <p>
   * The "g" prefix is used when a tag is defined on this result. If it's not, this method yields a plain commit id hash.
   * This is following git's behaviour - so any git tooling should be happy with this output.
   * </p>
   * <p>
   * Notes about the abbriverated object id:<br/>
   * Git will try to use your given abbrev lenght, but when it's to short to guarantee uniqueness -
   * a longer one will be used (which WILL guarantee uniqueness).
   * If you need the full commit id, it's always available via {@link pl.project13.jgit.DescribeResult#commitObjectId()}.
   * </p>
   */
  @Nullable
  public String prefixedCommitId() {
    if (abbreviatedObjectId.isPresent()) {
      String name = abbreviatedObjectId.get().name();
      return gPrefixedCommitId(name);

    } else if (commitId.isPresent()) {
      String name = commitId.get().name();
      return gPrefixedCommitId(name);

    } else {
      return null;
    }
  }

  private String gPrefixedCommitId(String name) {
    if (tagName.isPresent()) {
      return "g" + name;
    } else {
      return name;
    }
  }

  /**
   * JGit won't ever use 1 char as abbreviated ID, that's why only values of:
   * <ul>
   *   <li>0 (special meaning - don't show commit id at all),</li>
   *   <li>the range from 2 to 40 (inclusive) are valid</li>
   * </ul>
   *
   * @return the abbreviated commit id, possibly longer than the requested len (if it wouldn't be unique)
   */
  private static Optional<AbbreviatedObjectId> createAbbreviatedCommitId(@NotNull ObjectReader objectReader, ObjectId commitId, int requestedLenght) {
    if(requestedLenght < 2) {
      // 0 means we don't want to print commit id's at all
      return Optional.absent();
    }

    try {
      AbbreviatedObjectId abbreviatedObjectId = objectReader.abbreviate(commitId, requestedLenght);
      return Optional.of(abbreviatedObjectId);
    } catch (IOException e) {
      return Optional.absent();
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
