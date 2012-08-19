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
import org.eclipse.jgit.lib.ObjectId;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class DescribeResult {

  private Optional<String> tagName = Optional.absent();

  private Optional<ObjectId> commitId = Optional.absent();
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

  public DescribeResult(String tagName, int commitsAwayFromTag, ObjectId commitId) {
    this(tagName, commitsAwayFromTag, commitId, false, null);
  }

  public DescribeResult(String tagName, int commitsAwayFromTag, ObjectId commitId, boolean dirty, String dirtyMarker) {
    this.tagName = Optional.of(tagName);
    this.commitsAwayFromTag = commitsAwayFromTag;
    this.commitId = Optional.fromNullable(commitId);
    this.dirty = dirty;
    this.dirtyMarker = dirtyMarker;
  }


  @Override
  public String toString() {
    List<String> parts = newArrayList(tag(), commitsAwayFromTag(), commitId(), dirtyMarker());
    return Joiner.on("-").skipNulls().join(parts);
  }

  public String commitsAwayFromTag() {
    return commitsAwayFromTag == 0 ? null : String.valueOf(commitsAwayFromTag);
  }

  public String dirtyMarker() {
    return dirty ? dirtyMarker : null;
  }

  public String commitId() {
    if (commitId.isPresent()) {
      return String.valueOf(commitId.get().getName());
    } else {
      return null;
    }
  }

  public String tag() {
    return tagName.orNull();
  }
}
