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

import java.io.File;

public enum AvailableGitTestRepo {
  WITH_ONE_COMMIT("src/test/resources/_git_one_commit"),
  GIT_COMMIT_ID("src/test/resources/_git_of_git_commit_id"),
  ON_A_TAG("src/test/resources/_git_on_a_tag"),
  ON_A_TAG_DIRTY("src/test/resources/_git_on_a_tag_dirty"),
  /**
   * <pre>
   * de4db35 - (HEAD, v1.0.0, master) isecond line (38 minutes ago) <Konrad Malawski>
   * 245e5b2 - initial commit (38 minutes ago) <Konrad Malawski>
   * </pre>
   */
  ON_A_ANNOT_TAG_DIRTY("src/test/resources/_git_on_a_annotated_tag_dirty");

  String dir;

  AvailableGitTestRepo(String dir) {
    this.dir = dir;
  }

  public File getDir() {
    return new File(dir);
  }
}
