/*
 * This file is part of git-commit-id-maven-plugin by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
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

package pl.project13.maven.git;

import junitparams.JUnitParamsRunner;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.project13.core.git.GitDescribeConfig;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Run this to simulate hanging native-git-process for repo-state with lots of changes.
 *
 * The test case will still finish successfully because all git-related errors are cught in.
 *
 * @author eternach
 *
 */
@RunWith(JUnitParamsRunner.class)
public class BigDiffTest extends GitIntegrationTest {

  @Test
  // @Ignore("Run this to simulate hanging native-git-process for repo-state with lots of changes")
  public void bigDiff() throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.MAVEN_GIT_COMMIT_ID_PLUGIN)
        .create();
    final MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    final GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    gitDescribeConfig.setAlways(true);
    // set timeout to one hour
    mojo.nativeGitTimeoutInMs = 60 * 60 * 1000;
    mojo.useNativeGit = true;
    mojo.gitDescribe = gitDescribeConfig;

    for (int i = 0; i < 100000; i++) {
      final Path path = Paths.get(mavenSandbox.getChildProject().getBasedir().toString(), "very-long-file-name-with-id-" + Integer.toString(i) + ".txt");
      final byte[] bytes = "for performance test\n".getBytes();
      Files.createFile(path);
      try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
        for (int ii = 0; ii < 100; ii++) {
          fos.write(bytes);
        }
      }
    }
    // when
    final long startTime = System.currentTimeMillis();

    mojo.execute();

    final long estimatedTime = System.currentTimeMillis() - startTime;
    System.out.println(
        "[***] time: " + (double) estimatedTime + " ms");

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  private void assertGitPropertiesPresentInProject(final Properties properties) {
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.time"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.host"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.full"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.describe"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.email"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.remote.origin.url"));
  }

  private GitDescribeConfig createGitDescribeConfig(final boolean forceLongFormat, final int abbrev) {
    final GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setTags(true);
    gitDescribeConfig.setForceLongFormat(forceLongFormat);
    gitDescribeConfig.setAbbrev(abbrev);
    return gitDescribeConfig;
  }
}
