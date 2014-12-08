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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Adam Batkin
 */
public class GitCommitIdMojoDirtyFilesTest {

  @Test
  public void testDetectCleanWorkingDirectory() throws Exception {
    File dotGitDirectory = AvailableGitTestRepo.GIT_WITH_NO_CHANGES.getDir();
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setSkip(false);

    String prefix = "git";
    int abbrevLength = 7;
    String dateFormat = "dd.MM.yyyy '@' HH:mm:ss z";
    boolean verbose = true;

    GitCommitIdMojo mojo = new GitCommitIdMojo();
    mojo.setDotGitDirectory(dotGitDirectory);
    mojo.setPrefix(prefix);
    mojo.setAbbrevLength(abbrevLength);
    mojo.setDateFormat(dateFormat);
    mojo.setVerbose(verbose);
    mojo.useNativeGit(false);
    mojo.setGitDescribe(gitDescribeConfig);


    mojo.runningTests = true;
    mojo.project = mock(MavenProject.class, RETURNS_MOCKS);
    when(mojo.project.getPackaging()).thenReturn("jar");

    mojo.execute();

    Properties properties = mojo.getProperties();

    assertThat(properties.get("git.dirty")).isEqualTo("false");
  }

  @Test
  public void testDetectDirtyWorkingDirectory() throws Exception {
    File dotGitDirectory = AvailableGitTestRepo.GIT_WITH_CHANGES.getDir();
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setSkip(false);

    String prefix = "git";
    int abbrevLength = 7;
    String dateFormat = "dd.MM.yyyy '@' HH:mm:ss z";
    boolean verbose = true;

    GitCommitIdMojo mojo = new GitCommitIdMojo();
    mojo.setDotGitDirectory(dotGitDirectory);
    mojo.setPrefix(prefix);
    mojo.setAbbrevLength(abbrevLength);
    mojo.setDateFormat(dateFormat);
    mojo.setVerbose(verbose);
    mojo.useNativeGit(false);
    mojo.setGitDescribe(gitDescribeConfig);


    mojo.runningTests = true;
    mojo.project = mock(MavenProject.class, RETURNS_MOCKS);
    when(mojo.project.getPackaging()).thenReturn("jar");

    mojo.execute();

    Properties properties = mojo.getProperties();

    assertThat(properties.get("git.dirty")).isEqualTo("true");
  }
}
