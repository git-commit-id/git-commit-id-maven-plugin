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

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import pl.project13.maven.git.FileSystemMavenSandbox.CleanUp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@RunWith(JUnitParamsRunner.class)
public class NaivePerformanceTest extends GitIntegrationTest {

  static final boolean UseJGit = false;
  static final boolean UseNativeGit = true;

  public static Collection performanceParameter() {

    return Arrays.asList(new Object[][]{
        {UseJGit, 10},
        {UseNativeGit, 10},
        {UseJGit, 100},
        {UseNativeGit, 100}
    });
  }

  @Test
  @Parameters(method = "performanceParameter")
  @Ignore("Naive performance test - run this locally")
  public void performance(boolean useNativeGit, int iterations) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.MAVEN_GIT_COMMIT_ID_PLUGIN)
                .create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    gitDescribeConfig.setAlways(true);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      mojo.execute();
    }
    long estimatedTime = System.currentTimeMillis() - startTime;
    System.out.println("[***] Iterations: " + iterations + " Avg. time: " + ((double) estimatedTime) + " ms for useNativeGit=" + useNativeGit);

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }


  private GitDescribeConfig createGitDescribeConfig(boolean forceLongFormat, int abbrev) {
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setTags(true);
    gitDescribeConfig.setForceLongFormat(forceLongFormat);
    gitDescribeConfig.setAbbrev(abbrev);
    return gitDescribeConfig;
  }

  private void alterMojoSettings(String parameterName, Object parameterValue) {
    setInternalState(mojo, parameterName, parameterValue);
  }

  private void assertGitPropertiesPresentInProject(Properties properties) {
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.time"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id"));
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
}
