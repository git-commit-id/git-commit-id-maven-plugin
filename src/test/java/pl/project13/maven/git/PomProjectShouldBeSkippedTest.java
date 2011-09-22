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

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

import static org.mockito.Mockito.*;

/**
 * Date: 2/13/11
 *
 * @author <a href="mailto:konrad.malawski@project13.pl">Konrad 'ktoso' Malawski</a>
 */
public class PomProjectShouldBeSkippedTest extends PlexusTestCase {

  GitCommitIdMojo mojo;

  public void setUp() throws Exception {
    mojo = new GitCommitIdMojo();
    mojo.setDotGitDirectory(new File(".git/"));
    mojo.setPrefix("git");
    mojo.setDateFormat("dd.MM.yyyy '@' HH:mm:ss z");
    mojo.setVerbose(true);

    mojo.runningTests = true;
    mojo.project = mock(MavenProject.class, RETURNS_MOCKS);
    when(mojo.project.getPackaging()).thenReturn("pom");
    
    super.setUp();
  }

  public void testExecute() throws Exception {
    // given
    mojo = spy(mojo);

    // when
    mojo.execute();

    // then
    verify(mojo).isPomProject(mojo.project);
    verify(mojo, times(2)).log(anyString());
  }

}
