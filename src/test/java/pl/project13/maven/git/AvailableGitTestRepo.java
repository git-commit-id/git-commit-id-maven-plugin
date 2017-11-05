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

package pl.project13.maven.git;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public enum AvailableGitTestRepo {
  WITH_ONE_COMMIT("src/test/resources/_git_one_commit"),
  WITH_ONE_COMMIT_DIRTY("src/test/resources/_git_one_commit_dirty"),
  GIT_COMMIT_ID("src/test/resources/_git_of_git_commit_id"),
  GIT_WITH_NO_CHANGES("src/test/resources/_git_with_no_changes"),
  ON_A_TAG("src/test/resources/_git_on_a_tag"),
  /**
   * <pre>
   * $ lg
   *   * b6a73ed - (HEAD, master) third addition (32 hours ago) <p>Konrad Malawski</p>
   *   * d37a598 - (newest-tag, lightweight-tag) second line (32 hours ago) <p>Konrad Malawski</p>
   *   * 9597545 - (annotated-tag) initial commit (32 hours ago) <p>Konrad Malawski</p>
   * </pre>
   *
   * Where the <b>newest-tag</b> was created latest:
   * <pre>
   * $ tag -v newest-tag
   * object d37a598a7a98531ad1375966642c6b1263129436
   * tagger Konrad Malawski <p>konrad.malawski@project13.pl</p> 1346017608 +0200
   *
   * $ tag -v annotated-tag
   * object 95975455ef2b1af048f2926b9ba7fb804e22171b
   * tagger Konrad Malawski <p>konrad.malawski@project13.pl</p> 1345901561 +0200
   * </pre>
   */
  WITH_COMMIT_THAT_HAS_TWO_TAGS("src/test/resources/_git_with_commit_that_has_two_tags"),
  ON_A_TAG_DIRTY("src/test/resources/_git_on_a_tag_dirty"),
  WITH_SUBMODULES("src/test/resources/_git_with_submodules"),
  /**
   * <pre>
   * b6a73ed - (HEAD, master) third addition (4 minutes ago) <p>Konrad Malawski</p>
   * d37a598 - (lightweight-tag) second line (6 minutes ago) <p>Konrad Malawski</p>
   * 9597545 - (annotated-tag) initial commit (6 minutes ago) <p>Konrad Malawski</p>
   * </pre>
   */
  WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG("src/test/resources/_git_lightweight_tag_before_annotated_tag"),
  /**
   * <pre>
   * * 9cb810e - Change in tag - Fri, 29 Nov 2013 10:39:31 +0100 (tag: test_tag, branch: test)
   * | * 2343428 - Moved master - Fri, 29 Nov 2013 10:38:34 +0100 (HEAD, branch: master)
   * |/
   * * e3d159d - Added readme - Fri, 29 Nov 2013 10:38:02 +0100
   * </pre>
   */
  WITH_TAG_ON_DIFFERENT_BRANCH("src/test/resources/_git_with_tag_on_different_branch"),
  WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS("src/test/resources/_git_one_commit_with_umlaut"),
  /**
    * <pre>
    * b0c6d28b3b83bf7b905321bae67d9ca4c75a203f 2015-06-04 00:50:18 +0200  (HEAD, master)
    * 0e3495783c56589213ee5f2ae8900e2dc1b776c4 2015-06-03 23:59:10 +0200  (tag: v2.0)
    * f830b5f85cad3d33ba50d04c3d1454e1ae469057 2015-06-03 23:57:53 +0200  (tag: v1.0)
    * </pre>
    */
  WITH_THREE_COMMITS_AND_TWO_TAGS_CURRENTLY_ON_COMMIT_WITHOUT_TAG("src/test/resources/_git_three_commits_and_two_tags_currently_on_commit_without_tag"),
  // TODO: Why do the tests get stuck when we use .git??
  MAVEN_GIT_COMMIT_ID_PLUGIN("src/test/resources/_git_one_commit_with_umlaut")
  ;

  private String dir;

  AvailableGitTestRepo(String dir) {
    this.dir = dir;
  }

  @NotNull
  public File getDir() {
    return new File(dir);
  }
}
