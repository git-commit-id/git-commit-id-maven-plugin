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
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;
import pl.project13.maven.git.AvailableGitTestRepo;
import pl.project13.maven.git.FileSystemMavenSandbox;
import pl.project13.maven.git.GitIntegrationTest;

import static org.fest.assertions.Assertions.assertThat;

public class DescribeResultTest extends GitIntegrationTest {

  final static String PROJECT_NAME = "my-jar-project";

  final static String VERSION = "v2.5";
  final static String DEFAULT_ABBREV_COMMIT_ID = "b6a73ed";
  final static String FULL_HEAD_COMMIT_ID = "b6a73ed747dd8dc98642d731ddbf09824efb9d48";
  public static final ObjectId HEAD_OBJECT_ID = ObjectId.fromString(FULL_HEAD_COMMIT_ID);
  final static String G_DEFAULT_ABBREV_COMMIT_ID = "g" + DEFAULT_ABBREV_COMMIT_ID;
  final static String DIRTY_MARKER = "-DEV";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);
  }

  @Override
  protected Optional<String> projectDir() {
    return Optional.of(PROJECT_NAME);
  }

  @Test
  public void shouldToStringForTag() throws Exception {
    // given
    git().reset().setMode(ResetCommand.ResetType.HARD).call();

    DescribeResult res = new DescribeResult(VERSION);

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(VERSION);
  }

  @Test
  public void shouldToStringForDirtyTag() throws Exception {
    // given
    Repository repo = git().getRepository();
    git().reset().setMode(ResetCommand.ResetType.HARD).call();

    DescribeResult res = new DescribeResult(repo.newObjectReader(), VERSION, 2, HEAD_OBJECT_ID, true, DIRTY_MARKER);

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(VERSION + "-" + 2 + "-" + G_DEFAULT_ABBREV_COMMIT_ID + DIRTY_MARKER);
  }

  @Test
  public void shouldToStringForDirtyTagAnd10Abbrev() throws Exception {
    // given
    Repository repo = git().getRepository();
    git().reset().setMode(ResetCommand.ResetType.HARD).call();

    DescribeResult res = new DescribeResult(repo.newObjectReader(), VERSION, 2, HEAD_OBJECT_ID, true, DIRTY_MARKER)
        .withCommitIdAbbrev(10);

    String expectedHash = "gb6a73ed747";

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(VERSION + "-" + 2 + "-" + expectedHash + DIRTY_MARKER);
  }

  @Test
  public void shouldToStringFor2CommitsAwayFromTag() throws Exception {
    // given
    Repository repo = git().getRepository();
    git().reset().setMode(ResetCommand.ResetType.HARD).call();

    DescribeResult res = new DescribeResult(repo.newObjectReader(), VERSION, 2, HEAD_OBJECT_ID);

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(VERSION + "-" + 2 + "-" + G_DEFAULT_ABBREV_COMMIT_ID);
  }

  @Test
  public void shouldToStringForNoTagJustACommit() throws Exception {
    // given
    Repository repo = git().getRepository();
    git().reset().setMode(ResetCommand.ResetType.HARD).call();

    DescribeResult res = new DescribeResult(repo.newObjectReader(), HEAD_OBJECT_ID);


    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(DEFAULT_ABBREV_COMMIT_ID);
  }
}
