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
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.runner.RunWith;
import pl.project13.maven.git.util.PropertyManager;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class GitPropertiesFileTest extends GitIntegrationTest {

  static final boolean USE_JGIT = false;
  static final boolean USE_NATIVE_GIT = true;

  public static Collection<?> useNativeGit() {
    return asList(USE_JGIT, USE_NATIVE_GIT);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldConformPropertiesFileWhenSpecialCharactersInValueString(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS)
                .withKeepSandboxWhenFinishedTest(false) // set true if you want to overview the result in the generated sandbox
                .create();

    MavenProject targetProject = mavenSandbox.getChildProject();

    String targetFilePath = "target/classes/custom-git.properties";
    File expectedFile = new File(targetProject.getBasedir(), targetFilePath);

    setProjectToExecuteMojoIn(targetProject);
    mojo.setGenerateGitPropertiesFile(true);
    mojo.setGenerateGitPropertiesFilename(targetFilePath);
    mojo.setUseNativeGit(useNativeGit);

    // when
    try {
      mojo.execute();

      // then
      assertThat(expectedFile).exists();
      
      // the git.properties should exist also among the mojo.project properties
      Properties propertiesInProject = mojo.project.getProperties();
      assertGitPropertiesPresentInProject(propertiesInProject);

      // when the properties file is conform
      // it does not matter if we read as UTF8 or ISO-8859-1
      {
        Properties propertiesFromFile = PropertyManager.readProperties(expectedFile, "UTF-8");
        assertGitPropertiesPresentInProject(propertiesFromFile);
        assertThat(propertiesFromFile.get("git.commit.message.full"))
                .isEqualTo(propertiesInProject.get("git.commit.message.full"));
      }
      {
        Properties propertiesFromFile = PropertyManager.readProperties(expectedFile, "ISO-8859-1");
        assertGitPropertiesPresentInProject(propertiesFromFile);
        assertThat(propertiesFromFile.get("git.commit.message.full"))
                .isEqualTo(propertiesInProject.get("git.commit.message.full"));
      }

    } finally {
      if (!mavenSandbox.isKeepSandboxWhenFinishedTest()) {
        FileUtils.forceDelete(expectedFile);
      }
    }
  }

  private void assertGitPropertiesPresentInProject(Properties properties) {
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
    assertThat(properties).satisfies(new ContainsKeyCondition("git.closest.tag.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.closest.tag.commit.count"));
  }
}
