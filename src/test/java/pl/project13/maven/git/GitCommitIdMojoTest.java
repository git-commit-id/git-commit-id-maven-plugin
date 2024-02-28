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
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.project13.core.PropertiesFileGenerator;

/**
 * Testcases to verify that the git-commit-id works properly.
 */
@RunWith(JUnitParamsRunner.class)
public class GitCommitIdMojoTest {
  @Test
  public void testCraftPropertiesOutputFileWithRelativePath() throws IOException {
    File baseDir = new File(".");
    String targetDir = baseDir.getCanonicalPath() + File.separator;
    String generateGitPropertiesFilePath =
        "target" + File.separator + "classes" + File.separator + "git.properties";
    File generateGitPropertiesFile = new File(generateGitPropertiesFilePath);

    File result =
        PropertiesFileGenerator.craftPropertiesOutputFile(baseDir, generateGitPropertiesFile);
    assertThat(result.getCanonicalPath())
        .isEqualTo(
            new File(targetDir)
                .toPath()
                .resolve(generateGitPropertiesFilePath)
                .toFile()
                .getCanonicalPath());
  }

  @Test
  public void testCraftPropertiesOutputFileWithFullPath() throws IOException {
    File baseDir = new File(".");
    String targetDir = baseDir.getCanonicalPath() + File.separator;
    String generateGitPropertiesFilePath =
        targetDir + "target" + File.separator + "classes" + File.separator + "git.properties";
    File generateGitPropertiesFile = new File(generateGitPropertiesFilePath);

    File result =
        PropertiesFileGenerator.craftPropertiesOutputFile(baseDir, generateGitPropertiesFile);
    assertThat(result.getCanonicalPath())
        .isEqualTo(
            new File(targetDir)
                .toPath()
                .resolve(generateGitPropertiesFilePath)
                .toFile()
                .getCanonicalPath());
  }

  /**
   * test cases for output timestamp parsing.
   * This timestamp is configured for Reproducible Builds' archive entries
   * (https://maven.apache.org/guides/mini/guide-reproducible-builds.html). The value from <code>
   * ${project.build.outputTimestamp}</code> is either formatted as ISO 8601 <code>
   * yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like <a
   * href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>.
   * When using ISO 8601 formatting please note that the entire expression must be entirely either
   * in the basic format (20240215T135459+0100) or in the
   * extended format (e.g. 2024-02-15T13:54:59+01:00).
   * The maven plugin only supports the extended format.
   */
  private Object[] parametersParseOutputTimestamp() {
    return new Object[] {
      // long since epoch
      new Object[] {
        "1644689403"
      },
      // Date and time with timezone:
      new Object[] {
        "2022-02-12T15:30+00:00"
      },
      new Object[] {
        "2022-02-12T15:30:45-05:00"
      },
      new Object[] {
        "2022-02-12T15:30:00+00:00"
      },
      new Object[] {
        "2023-11-30T09:17:06+05:30"
      },
      new Object[] {
        "2024-08-15T20:45:30-03:00"
      },
      new Object[] {
        "2022-02-12T15:30:00Z"
      },
      new Object[] {
        "2023-11-30T09:17:06+0100"
      },
      // Lowercase time designator
      new Object[] {
        "2019-03-26t14:00Z"
      },
      // Lowercase UTC designator
      new Object[] {
        "2019-03-26T14:00z"
      },
      // Hours-only offset
      new Object[] {
        "2019-03-26T10:00-04"
      },
    };
  }

  @Test
  @Parameters(method = "parametersParseOutputTimestamp")
  public void testParseOutputTimestamp(String input) {
    Date actual = GitCommitIdMojo.parseOutputTimestamp(input);
    assertThat(actual).isNotNull();
  }
}
