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

package pl.project13.maven.git;

import java.util.Arrays;
import java.util.Collection;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pl.project13.core.git.GitDescribeConfig;

/**
 * Testcases to verify that the git-commit-id works properly.
 */
public class NaivePerformanceTest extends GitIntegrationTest {

  static final boolean UseJGit = false;
  static final boolean UseNativeGit = true;

  public static Collection<?> performanceParameter() {
    return Arrays.asList(
        new Object[][] {
          {UseJGit, 10},
          {UseNativeGit, 10},
          {UseJGit, 100},
          {UseNativeGit, 100}
        });
  }

  @ParameterizedTest
  @MethodSource("performanceParameter")
  @Disabled("Naive performance test - run this locally")
  public void performance(boolean useNativeGit, int iterations) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.MAVEN_GIT_COMMIT_ID_PLUGIN)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    gitDescribeConfig.setAlways(true);
    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      mojo.execute();
    }
    long estimatedTime = System.currentTimeMillis() - startTime;
    System.out.println(
        "[***] Iterations: "
            + iterations
            + " Avg. time: "
            + ((double) estimatedTime)
            + " ms for useNativeGit="
            + useNativeGit);

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
}
