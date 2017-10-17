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

import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class GitSubmodulesTest extends GitIntegrationTest {

  @Test
  public void shouldResolvePropertiesOnDefaultSettingsForNonPomProject() throws Exception {
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withGitRepoInParent(AvailableGitTestRepo.WITH_SUBMODULES)
        .withChildProject("example-child", "jar")
        .create();

    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  public void setProjectToExecuteMojoIn(@NotNull MavenProject project) {
    mojo.setProject(project);
    mojo.setDotGitDirectory(new File(project.getBasedir(), ".git"));
  }

  private void assertGitPropertiesPresentInProject(Properties properties) {
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.time"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.host"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.full"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
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
