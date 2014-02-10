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

import com.google.common.base.Optional;
import org.eclipse.jgit.lib.Repository;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import pl.project13.maven.git.AvailableGitTestRepo;
import pl.project13.maven.git.FileSystemMavenSandbox;
import pl.project13.maven.git.GitIntegrationTest;

import static org.fest.assertions.Assertions.assertThat;

public class DescribeCommandAbbrevIntegrationTest extends GitIntegrationTest {

  final String PROJECT_NAME = "my-jar-project";

  @Override
  protected Optional<String> projectDir() {
    return Optional.of(PROJECT_NAME);
  }

  /**
   * Test for such situation:
   * <pre>
   * master!tag-test> lg
   *   b6a73ed - (HEAD, master) third addition (8 hours ago) <Konrad Malawski>
   *   d37a598 - (lightweight-tag) second line (8 hours ago) <Konrad Malawski>
   *   9597545 - (annotated-tag) initial commit (8 hours ago) <Konrad Malawski>
   *
   * master!tag-test> describe --abbrev=1
   *   annotated-tag-2-gb6a7
   *
   * master!tag-test> describe --abbrev=2
   *   annotated-tag-2-gb6a7
   * </pre>
   *
   * <p>
   *   Notice that git will not use less than 4 chars for the abbrev, and in large repositories,
   *  it will use the abbrev so long that it's guaranteed to be unique.
   * </p>
   */
  @Test
  public void shouldGiveTheCommitIdAndDirtyMarkerWhenNothingElseCanBeFound() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    Repository repo = git().getRepository();

    // when
    DescribeResult res = DescribeCommand
        .on(repo)
        .abbrev(2) // 2 is enough to be unique in this small repo
        .setVerbose(true)
        .call();

    // then
    // git will notice this, and fallback to use 4 chars
    String smallestAbbrevGitWillUse = abbrev("b6a73ed747dd8dc98642d731ddbf09824efb9d48", 2);

    assertThat(res.prefixedCommitId()).isEqualTo("g" + smallestAbbrevGitWillUse);
  }

  @Test
  public void onGitCommitIdsRepo_shouldNoticeThat2CharsIsTooLittleToBeUniqueAndUse4CharsInstead() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.GIT_COMMIT_ID)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    Repository repo = git().getRepository();

    // when
    DescribeResult res = DescribeCommand
        .on(repo)
        .abbrev(2) // way too small to be unique in git-commit-id's repo!
        .setVerbose(true)
        .call();

    // then
    // git will notice this, and fallback to use 4 chars
    String smallestAbbrevGitWillUse = abbrev("7181832b7d9afdeb86c32cf51214abfca63625be", 4);

    assertThat(res.prefixedCommitId()).isEqualTo("g" + smallestAbbrevGitWillUse);
  }

  String abbrev(@NotNull String id, int n) {
    return id.substring(0, n);
  }
}
