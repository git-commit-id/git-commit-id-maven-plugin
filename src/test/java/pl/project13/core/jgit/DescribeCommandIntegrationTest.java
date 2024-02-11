/*
 * This file is part of git-commit-id-maven-plugin
 * Originally invented by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-maven-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-maven-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.core.jgit;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import pl.project13.log.DummyTestLoggerBridge;
import pl.project13.maven.git.AvailableGitTestRepo;
import pl.project13.maven.git.GitIntegrationTest;

/**
 * Testcases to verify that the {@link DescribeResult} works properly.
 */
public class DescribeCommandIntegrationTest extends GitIntegrationTest {

  public static final int DEFAULT_ABBREV_LEN = 7;
  public static final String DIRTY_SUFFIX = "-dirty";
  static final String PROJECT_NAME = "my-jar-project";

  @Override
  protected Optional<String> projectDir() {
    return Optional.of(PROJECT_NAME);
  }

  @Test
  public void shouldGiveTheCommitIdAndDirtyMarkerWhenNothingElseCanBeFound() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT_DIRTY)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()).call();

      // then
      assertThat(res).isNotNull();

      RevCommit head = git.log().call().iterator().next();
      assertThat(res.toString()).isEqualTo(abbrev(head.getName()));
    }
  }

  @Test
  public void shouldGiveTheCommitIdWhenNothingElseCanBeFound() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      // when
      DescribeCommand command =
          spy(DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()));
      doReturn(false).when(command).findDirtyState(any(Repository.class));

      DescribeResult res = command.call();

      // then
      assertThat(res).isNotNull();

      RevCommit head = git.log().call().iterator().next();
      assertThat(res.toString()).isEqualTo(abbrev(head.getName()));
    }
  }

  @Test
  public void shouldGiveTheCommitIdWhenTagIsOnOtherBranch() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      // when
      DescribeCommand command =
          spy(DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()));
      doReturn(false).when(command).findDirtyState(any(Repository.class));

      DescribeResult res = command.call();

      // then
      assertThat(res).isNotNull();

      RevCommit head = git.log().call().iterator().next();
      assertThat(res.toString()).isEqualTo(abbrev(head.getName()));
    }
  }

  @Test
  public void shouldGiveTheCommitIdWhenNothingElseCanBeFoundAndUseAbbrevVersionOfIt()
      throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();

    int abbrevLength = 10;
    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      // when
      DescribeCommand command =
          spy(DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()));
      doReturn(false).when(command).findDirtyState(any(Repository.class));

      command.abbrev(abbrevLength);
      DescribeResult res = command.call();

      // then
      assertThat(res).isNotNull();

      RevCommit head = git.log().call().iterator().next();
      assertThat(res.toString()).isEqualTo(abbrev(head.getName(), abbrevLength));
    }
  }

  @Test
  public void shouldGiveTagWithDistanceToCurrentCommitAndItsIdAndDirtyMarker() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.GIT_COMMIT_ID)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      // when
      DescribeCommand command =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge());
      command.dirty(DIRTY_SUFFIX);
      DescribeResult res = command.call();

      // then
      assertThat(res).isNotNull();
      RevCommit head = git.log().call().iterator().next();
      assertThat(res.toString()).isEqualTo("v2.0.4-25-g" + abbrev(head.getName()) + DIRTY_SUFFIX);
    }
  }

  @Test
  public void shouldGiveTagWithDistanceToCurrentCommitAndItsIdAndCustomDirtyMarker()
      throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.GIT_COMMIT_ID)
        .create();

    String customDirtySuffix = "-DEV";

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      // when
      DescribeCommand command =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge())
              .dirty(customDirtySuffix);
      DescribeResult res = command.call();

      // then
      assertThat(res).isNotNull();
      RevCommit head = git.log().call().iterator().next();
      assertThat(res.toString())
          .isEqualTo("v2.0.4-25-g" + abbrev(head.getName()) + customDirtySuffix);
    }
  }

  @Test
  public void shouldGiveTagWithDistanceToCurrentCommitAndItsId() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.GIT_COMMIT_ID)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      try (final Git wrap = Git.wrap(repo)) {
        wrap.reset().setMode(ResetCommand.ResetType.HARD).call();
      }

      // when
      DescribeCommand command =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge());
      DescribeResult res = command.call();

      // then
      assertThat(res).isNotNull();
      RevCommit head = git.log().call().iterator().next();
      assertThat(res.toString()).isEqualTo("v2.0.4-25-g" + abbrev(head.getName()));
    }
  }

  @Test
  public void shouldGiveTag() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()).tags().call();

      // then
      assertThat(res).isNotNull();

      assertThat(res.toString()).isEqualTo("v1.0.0");
    }
  }

  @Test
  public void shouldNotGiveDirtyMarkerWhenOnATagAndDirtyButNoDirtyOptionConfigured()
      throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.checkout().setName("v1.0.0").call();

      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()).tags().call();

      // then
      assertThat(res).isNotNull();

      assertThat(res.toString()).isEqualTo("v1.0.0");
    }
  }

  @Test
  public void shouldGiveTagWithCustomDirtyMarker() throws Exception {
    // given
    String customDirtySuffix = "-banana";

    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.checkout().setName("v1.0.0").call();

      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge())
              .tags()
              .dirty(customDirtySuffix)
              .call();

      // then
      assertThat(res).isNotNull();

      assertThat(res.toString()).isEqualTo("v1.0.0" + customDirtySuffix);
    }
  }

  @Test
  public void shouldNotGiveDirtyTagByDefault() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()).tags().call();

      // then
      assertThat(res.toString()).isEqualTo("v1.0.0");
    }
  }

  @Test
  public void shouldGiveAnnotatedTagWithDirtyMarker() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge())
              .dirty(DIRTY_SUFFIX)
              .abbrev(0)
              .call();

      // then
      assertThat(res).isNotNull();

      assertThat(res.toString()).isEqualTo("annotated-tag" + DIRTY_SUFFIX);
    }
  }

  @Test
  public void shouldGiveLightweightTagWithDirtyMarker() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG_DIRTY)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      try (final Git wrap = Git.wrap(repo)) {
        wrap.reset().setMode(ResetCommand.ResetType.HARD).call();
      }

      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()).tags().call();

      // then
      assertThat(res).isNotNull();

      assertThat(res.toString()).isEqualTo("v1.0.0");
    }
  }

  @Test
  public void isATag_shouldProperlyDetectIfACommitIsATag() throws Exception {
    // given
    String tagName = "v1";
    String commitHash = "de4db35917b268089c81c9ab1b52541bb778f5a0";

    ObjectId oid = ObjectId.fromString(commitHash);

    // when
    boolean isATag =
        DescribeCommand.hasTags(oid, Collections.singletonMap(oid, singletonList(tagName)));

    // then
    assertThat(isATag).isTrue();
  }

  @Test
  public void isATag_shouldProperlyDetectIfACommitIsANotTag() throws Exception {
    // given
    String tagName = "v1";
    String tagHash = "de4db35917b268089c81c9ab1b52541bb778f5a0";
    ObjectId tagOid = ObjectId.fromString(tagHash);

    String commitHash = "de4db35917b268089c81c9ab1b52541bb778f5a0";
    ObjectId oid = ObjectId.fromString(commitHash);

    // when
    boolean isATag =
        DescribeCommand.hasTags(oid, Collections.singletonMap(tagOid, singletonList(tagName)));

    // then
    assertThat(isATag).isTrue();
  }

  @Test
  public void shouldReturnJustTheNearestTagWhenAbbrevIsZero() throws Exception {
    // given
    int zeroAbbrev = 0;
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create();

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      try (final Git wrap = Git.wrap(repo)) {
        wrap.reset().setMode(ResetCommand.ResetType.HARD).call();
      }

      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge())
              .abbrev(zeroAbbrev)
              .call();

      // then
      assertThat(res.toString()).isEqualTo("annotated-tag");

      ObjectId objectId = res.commitObjectId();
      assert objectId != null;
      assertThat(objectId.getName()).isNotEmpty();
    }
  }

  String abbrev(@Nonnull String id) {
    return abbrev(id, DEFAULT_ABBREV_LEN);
  }

  String abbrev(@Nonnull String id, int n) {
    return id.substring(0, n);
  }
}
