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

import org.jetbrains.annotations.NotNull;

import java.io.File;

public enum AvailableGitTestRepo {
  WITH_ONE_COMMIT("src/test/resources/_git_one_commit"),
  WITH_ONE_COMMIT_DIRTY("src/test/resources/_git_one_commit_dirty"),
  GIT_COMMIT_ID("src/test/resources/_git_of_git_commit_id"),
  ON_A_TAG("src/test/resources/_git_on_a_tag"),
  /**
   * <pre>
   * > lg
   *   * b6a73ed - (HEAD, master) third addition (32 hours ago) <Konrad Malawski>
   *   * d37a598 - (newest-tag, lightweight-tag) second line (32 hours ago) <Konrad Malawski>
   *   * 9597545 - (annotated-tag) initial commit (32 hours ago) <Konrad Malawski>
   * </pre>
   *
   * Where the <b>newest-tag</b> was created latest:
   * <pre>
   * > tag -v newest-tag
   * object d37a598a7a98531ad1375966642c6b1263129436
   * tagger Konrad Malawski <konrad.malawski@project13.pl> 1346017608 +0200
   *
   * > tag -v annotated-tag
   * object 95975455ef2b1af048f2926b9ba7fb804e22171b
   * tagger Konrad Malawski <konrad.malawski@project13.pl> 1345901561 +0200
   * </pre>
   */
  WITH_COMMIT_THAT_HAS_TWO_TAGS("src/test/resources/_git_with_commit_that_has_two_tags"),
  ON_A_TAG_DIRTY("src/test/resources/_git_on_a_tag_dirty"),
  WITH_SUBMODULES("src/test/resources/_git_with_submodules"),
  /**
   * <pre>
   * b6a73ed - (HEAD, master) third addition (4 minutes ago) <Konrad Malawski>
   * d37a598 - (lightweight-tag) second line (6 minutes ago) <Konrad Malawski>
   * 9597545 - (annotated-tag) initial commit (6 minutes ago) <Konrad Malawski>
   * </pre>
   */
  WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG("src/test/resources/_git_lightweight_tag_before_annotated_tag"),
  WITH_TAG_ON_DIFFERENT_BRANCH("src/test/resources/_git_with_tag_on_different_branch"),

  MAVEN_GIT_COMMIT_ID_PLUGIN(".git");

  private String dir;

  AvailableGitTestRepo(String dir) {
    this.dir = dir;
  }

  @NotNull
  public File getDir() {
    return new File(dir);
  }
}
