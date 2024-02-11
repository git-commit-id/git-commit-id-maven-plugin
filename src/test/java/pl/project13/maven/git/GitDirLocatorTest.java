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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GitDirLocatorTest {

  @Mock MavenProject project;

  List<MavenProject> reactorProjects = Collections.emptyList();

  @Test
  public void shouldUseTheManuallySpecifiedDirectory() throws Exception {
    // given
    File dotGitDir = Files.createTempDirectory("temp").toFile();
    try {

      // when
      GitDirLocator locator = new GitDirLocator(project, reactorProjects);
      File foundDirectory = locator.lookupGitDirectory(dotGitDir);

      // then
      assert foundDirectory != null;
      assertThat(foundDirectory.getAbsolutePath()).isEqualTo(dotGitDir.getAbsolutePath());
    } finally {
      if (!dotGitDir.delete()) {
        dotGitDir.deleteOnExit();
      }
    }
  }

  @Test
  public void testWorktreeResolution() {
    String[] noopCases = {"", "a", "a/b", ".git/worktrees", ".git/worktrees/", "a.git/worktrees/b"};
    for (String path : noopCases) {
      assertThat(GitDirLocator.resolveWorktree(new File(path))).isEqualTo(new File(path));
    }
    assertThat(GitDirLocator.resolveWorktree(new File("a/.git/worktrees/b")))
        .isEqualTo(new File("a/.git"));
    assertThat(GitDirLocator.resolveWorktree(new File("/a/.git/worktrees/b")))
        .isEqualTo(new File("/a/.git"));
  }
}
