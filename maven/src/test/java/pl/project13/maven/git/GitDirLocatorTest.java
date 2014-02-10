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

import com.google.common.io.Files;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class GitDirLocatorTest {

  @Mock
  MavenProject project;

  List<MavenProject> reactorProjects = Collections.emptyList();

  @Test
  public void shouldUseTheManuallySpecifiedDirectory() throws Exception {
    // given
    File dotGitDir = Files.createTempDir();

    // when
    GitDirLocator locator = new GitDirLocator(project, reactorProjects);
    File foundDirectory = locator.lookupGitDirectory(dotGitDir);

    // then
    assert foundDirectory != null;
    assertThat(foundDirectory.getAbsolutePath()).isEqualTo(dotGitDir.getAbsolutePath());
  }

}
