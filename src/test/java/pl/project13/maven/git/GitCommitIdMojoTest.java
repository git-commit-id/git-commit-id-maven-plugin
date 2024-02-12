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

  private Object[] parametersParseOutputTimestamp() {
    return new Object[] {
      // long since epoch
      new Object[] {
        "1644689403",
        Instant.ofEpochMilli(1644689403000L)
      },
      // Date only:
      new Object[] {
        "2022-02",
        Instant.ofEpochMilli(1643670000000L)
      },
      new Object[] {
        "2022-02-12",
        Instant.ofEpochMilli(1644620400000L)
      },
      // Date and time:
      new Object[] {
        "2022-02-12T15:30",
        Instant.ofEpochMilli(1644676200000L)
      },
      new Object[] {
        "2022-02-12T15:30:45",
        Instant.ofEpochMilli(1644676245000L)
      },
      // Date and time with timezone:
      new Object[] {
        "2022-02-12T15:30+00:00",
        Instant.ofEpochMilli(1644679800000L)
      },
      new Object[] {
        "2022-02-12T15:30:45-05:00",
        Instant.ofEpochMilli(1644697845000L)
      },
      new Object[] {
        "2022-02-12T15:30:00+00:00",
        Instant.ofEpochMilli(1644679800000L)
      },
      new Object[] {
        "2023-11-30T09:17:06+05:30",
        Instant.ofEpochMilli(1701316026000L)
      },
      new Object[] {
        "2024-08-15T20:45:30-03:00",
        Instant.ofEpochMilli(1723765530000L)
      },
      new Object[] {
        "2022-02-12T15:30:00Z",
        Instant.ofEpochMilli(1644679800000L)
      },
      // Not valid according to the ISO 8601 standard. The issue is with the time zone
      // representation. ISO 8601 uses a specific format for time zones, either as "Z" for UTC or
      // in the format "+HH:MM" or "-HH:MM" for the offset from UTC.
      // The time zone "EST" or "PST" does not follow this format.
      // new Object[] { "2023-11-30T09:17:06PST", null },
      // new Object[] { "2024-08-15T20:45:30EST", null },
      new Object[] {
        "2023-11-30T09:17:06+0100",
        Instant.ofEpochMilli(1701332226000L)
      },
      // Week date:
      new Object[] {
        "2022-W06",
        Instant.ofEpochMilli(1644188400000L)
      },
      new Object[] {
        "2022-W06-5",
        Instant.ofEpochMilli(1644534000000L)
      },
      // Week date with time:
      new Object[] {
        "2022-W06-5T15:30",
        Instant.ofEpochMilli(1644589800000L)
      },
      new Object[] {
        "2022-W06-5T15:30:45",
        Instant.ofEpochMilli(1644589845000L)
      },
      // https://tc39.es/proposal-uniform-interchange-date-parsing/cases.html
      // positive leap second
      // not working: new Object[] { "1972-06-30T23:59:60Z", null },
      // Too few fractional second digits
      new Object[] {
        "2019-03-26T14:00:00.9Z",
        Instant.ofEpochMilli(1553608800900L)
      },
      // Too many fractional second digits
      new Object[] {
        "2019-03-26T14:00:00.4999Z",
        Instant.ofEpochMilli(1553608800499L)
      },
      // Too many fractional second digits (pre-epoch)
      new Object[] {
        "1969-03-26T14:00:00.4999Z",
        Instant.ofEpochMilli(-24227999501L)
      },
      // Too many fractional second digits (BCE)
      new Object[] {
        "-000043-03-15T14:00:00.4999Z",
        Instant.ofEpochMilli(-63517773599501L)
      },
      // Lowercase time designator
      new Object[] {
        "2019-03-26t14:00Z",
        Instant.ofEpochMilli(1553608800000L)
      },
      // Lowercase UTC designator
      new Object[] {
        "2019-03-26T14:00z",
        Instant.ofEpochMilli(1553608800000L)
      },
      // Hours-only offset
      new Object[] {
        "2019-03-26T10:00-04",
        Instant.ofEpochMilli(1553608800000L)
      },
      // Fractional minutes
      new Object[] {
        "2019-03-26T14:00.9Z",
        Instant.ofEpochMilli(1553608854000L)
      },
      // ISO basic format date and time
      // not working: new Object[] { "20190326T1400Z", null },
    };
  }

  @Test
  @Parameters(method = "parametersParseOutputTimestamp")
  public void testParseOutputTimestamp(String input, Instant expected) {
    Date actual = GitCommitIdMojo.parseOutputTimestamp(input);
    assertThat(actual.toInstant()).isEqualTo(expected);
  }
}
