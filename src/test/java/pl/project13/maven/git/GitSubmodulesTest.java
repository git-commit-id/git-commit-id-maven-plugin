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

import java.nio.file.Files;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testcases to verify that the git-commit-id-plugin works properly.
 */
@RunWith(JUnitParamsRunner.class)
public class GitSubmodulesTest extends GitIntegrationTest {

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldResolvePropertiesOnDefaultSettingsForNonPomProject(boolean useNativeGit) throws Exception {
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withGitRepoInParent(AvailableGitTestRepo.WITH_SUBMODULES)
        .withChildProject("example-child", "jar")
        .create();

    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGeneratePropertiesWithSubmodules(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
      .withParentProject("my-pom-project", "pom")
      .withGitRepoInParent(AvailableGitTestRepo.WITH_REMOTE_SUBMODULES)
      .withChildProject("remote-module", "jar")
      .create();
    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);

    // create a relative pointer to trigger the relative path logic in GitDirLocator#lookupGitDirectory
    // makes the dotGitDirectory look like "my-pom-project/.git/modules/remote-module"
    Files.write(
      mavenSandbox.getChildProject().getBasedir().toPath().resolve(".git"),
      "gitdir: ../.git/modules/remote-module".getBytes()
    );

    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "945bfe6");
  }
}
