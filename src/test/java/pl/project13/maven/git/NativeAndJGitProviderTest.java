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

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;

import pl.project13.maven.git.FileSystemMavenSandbox.CleanUp;

public class NativeAndJGitProviderTest extends GitIntegrationTest
{
  public static final String[] GIT_KEYS = new String[] {
    "git.build.time",
    "git.build.host",
    "git.branch",
    "git.commit.id",
    "git.commit.id.abbrev",
    "git.commit.id.describe",
    "git.build.user.name",
    "git.build.user.email",
    "git.commit.user.name",
    "git.commit.user.email",
    "git.commit.message.full",
    "git.commit.message.short",
    "git.commit.time",
    "git.remote.origin.url"
  };

  public static final String DEFAULT_FORMAT_STRING  = "dd.MM.yyyy '@' HH:mm:ss z";
  public static final String ISO8601_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssZZ";

  @Test
  public void testCompareBasic() throws Exception
  {
    // Test on all available basic repos to ensure that the output is identical.
    for (AvailableGitTestRepo testRepo : AvailableGitTestRepo.values()) {
      mavenSandbox.withParentProject("my-basic-project", "jar").withNoChildProject().withGitRepoInParent(testRepo).create(CleanUp.CLEANUP_FIRST);
      MavenProject targetProject = mavenSandbox.getParentProject();
      verifyNativeAndJGit(targetProject, DEFAULT_FORMAT_STRING);
    }
  }

  @Test
  public void testCompareSubrepoInRoot() throws Exception
  {
    for (AvailableGitTestRepo testRepo : AvailableGitTestRepo.values()) {
      if (testRepo == AvailableGitTestRepo.MAVEN_GIT_COMMIT_ID_PLUGIN) {
        continue; // Don't create a subrepo based off the plugin repo itself.
      }
      mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withGitRepoInParent(testRepo).create(CleanUp.CLEANUP_FIRST);
      MavenProject targetProject = mavenSandbox.getParentProject();
      verifyNativeAndJGit(targetProject, DEFAULT_FORMAT_STRING);
    }
  }

  @Test
  public void testCompareSubrepoInChild() throws Exception
  {
    for (AvailableGitTestRepo testRepo : AvailableGitTestRepo.values()) {
      if (testRepo == AvailableGitTestRepo.MAVEN_GIT_COMMIT_ID_PLUGIN) {
        continue; // Don't create a subrepo based off the plugin repo itself.
      }
      mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withGitRepoInParent(testRepo).create(CleanUp.CLEANUP_FIRST);
      MavenProject targetProject = mavenSandbox.getChildProject();
      verifyNativeAndJGit(targetProject, DEFAULT_FORMAT_STRING);
    }
  }

  @Test
  public void testCompareISO8601Time() throws Exception
  {
    // Test on all available basic repos to ensure that the output is identical.
    for (AvailableGitTestRepo testRepo : AvailableGitTestRepo.values()) {
      mavenSandbox.withParentProject("my-basic-project", "jar").withNoChildProject().withGitRepoInParent(testRepo).create(CleanUp.CLEANUP_FIRST);
      MavenProject targetProject = mavenSandbox.getParentProject();
      verifyNativeAndJGit(targetProject, ISO8601_FORMAT_STRING);
    }
  }

  private void verifyNativeAndJGit(MavenProject targetProject, String formatString) throws Exception
  {
    setProjectToExecuteMojoIn(targetProject);

    alterMojoSettings("skipPoms", false);
    alterMojoSettings("dateFormat", formatString);

    DateFormat format = new SimpleDateFormat(formatString);

    alterMojoSettings("useNativeGit", false);
    mojo.execute();
    Properties jgitProps = createCopy(targetProject.getProperties());

    alterMojoSettings("useNativeGit", true);
    mojo.execute();
    Properties nativeProps = createCopy(targetProject.getProperties());

    assertGitPropertiesPresentInProject(jgitProps);
    assertGitPropertiesPresentInProject(nativeProps);

    for (String key : GIT_KEYS) {
      if (!key.equals("git.build.time")) { // git.build.time is excused because the two runs happened at different times.
        assertEquals("Key difference for key: '" + key + "'", jgitProps.getProperty(key), nativeProps.getProperty(key));
      }
      else {
        // Ensure that the date formats are parseable and within reason. If running all the git commands on the
        // native provider takes more than 60 seconds, then something is seriously wrong.
        long jGitBuildTimeInMs = format.parse(jgitProps.getProperty(key)).getTime();
        long nativeBuildTimeInMs = format.parse(nativeProps.getProperty(key)).getTime();
        Assert.assertTrue("Time ran backwards, jgitTime after nativeTime!", jGitBuildTimeInMs <= nativeBuildTimeInMs);
        Assert.assertTrue("Build ran too slow.", (nativeBuildTimeInMs - jGitBuildTimeInMs) < 60000L); // If native takes more than 1 minute, something is wrong.
      }
    }

    // Check the commit time to be equal in ms, too.
    long jGitCommitTimeInMs = format.parse(jgitProps.getProperty("git.commit.time")).getTime();
    long nativeCommitTimeInMs = format.parse(nativeProps.getProperty("git.commit.time")).getTime();

    assertEquals("commit times parse to different time stamps", jGitCommitTimeInMs, nativeCommitTimeInMs);
  }

  private void alterMojoSettings(String parameterName, Object parameterValue)
  {
    setInternalState(mojo, parameterName, parameterValue);
  }

  private Properties createCopy(Properties orig)
  {
    Properties p = new Properties();
    for (String key : orig.stringPropertyNames()) {
      p.setProperty(key, orig.getProperty(key));
    }

    return p;
  }

  private void assertGitPropertiesPresentInProject(Properties properties)
  {
    for (String key : GIT_KEYS) {
      assertThat(properties).satisfies(new ContainsKeyCondition(key));
    }
  }
}
