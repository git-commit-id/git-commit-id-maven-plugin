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
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.stream.Stream;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pl.project13.core.CommitIdPropertiesOutputFormat;
import pl.project13.core.util.GenericFileManager;

/**
 * Testcases to verify that the git-commit-id works properly.
 */
public class GitPropertiesFileTest extends GitIntegrationTest {

  static final boolean USE_JGIT = false;
  static final boolean USE_NATIVE_GIT = true;

  public static Stream<Arguments> useNativeGit() {
    return Stream.of(
      Arguments.of(USE_JGIT),
      Arguments.of(USE_NATIVE_GIT)
    );
  }

  @ParameterizedTest
  @MethodSource("useNativeGit")
  public void shouldConformPropertiesFileWhenSpecialCharactersInValueString(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS)
        .withKeepSandboxWhenFinishedTest(
            false) // set true if you want to overview the result in the generated sandbox
        .create();

    MavenProject targetProject = mavenSandbox.getChildProject();

    String targetFilePath = "target/classes/custom-git.properties";
    File expectedFile = new File(targetProject.getBasedir(), targetFilePath);

    setProjectToExecuteMojoIn(targetProject);
    mojo.generateGitPropertiesFile = true;
    mojo.generateGitPropertiesFilename = targetFilePath;
    mojo.useNativeGit = useNativeGit;

    // when
    try {
      mojo.execute();

      // then
      assertThat(expectedFile).exists();

      // the git.properties should exist also among the mojo.project properties
      Properties propertiesInProject = mojo.project.getProperties();
      assertGitPropertiesPresentInProject(propertiesInProject);

        // when the properties file is conform
        // it does not matter if we read as UTF-8 or ISO-8859-1
        {
          Properties propertiesFromFile =
              GenericFileManager.readProperties(
                  CommitIdPropertiesOutputFormat.PROPERTIES, expectedFile, StandardCharsets.UTF_8);
          assertGitPropertiesPresentInProject(propertiesFromFile);
          assertThat(propertiesFromFile.get("git.commit.message.full"))
              .isEqualTo(propertiesInProject.get("git.commit.message.full"));
        }
        {
          Properties propertiesFromFile =
              GenericFileManager.readProperties(
                  CommitIdPropertiesOutputFormat.PROPERTIES,
                  expectedFile,
                  StandardCharsets.ISO_8859_1);
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
}
