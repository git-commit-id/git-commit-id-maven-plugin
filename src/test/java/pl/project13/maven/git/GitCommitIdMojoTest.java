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

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.util.Properties;
import java.util.regex.Pattern;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitCommitIdMojoTest {

  @Test
  public void loadShortDescribe() {
    assertShortDescribe("1.0.2-12-g19471", "1.0.2-12");
    assertShortDescribe("v1.0.0-0-gde4db35917", "v1.0.0-0");
    assertShortDescribe("1.0.2-12-g19471-DEV", "1.0.2-12-DEV");
    assertShortDescribe("V-1.0.2-12-g19471-DEV", "V-1.0.2-12-DEV");

    assertShortDescribe(null, null);
    assertShortDescribe("12.4.0-1432", "12.4.0-1432");
    assertShortDescribe("12.6.0", "12.6.0");
    assertShortDescribe("", "");
  }

  private void assertShortDescribe(String commitDescribe, String expectedShortDescribe) {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    Properties prop = new Properties();
    if (commitDescribe != null) {
      prop.put(GitCommitPropertyConstant.COMMIT_DESCRIBE, commitDescribe);
    }
    commitIdMojo.loadShortDescribe(prop);
    assertThat(prop.getProperty(GitCommitPropertyConstant.COMMIT_SHORT_DESCRIBE)).isEqualTo(expectedShortDescribe);
  }

  @Test
  public void testCraftPropertiesOutputFileWithRelativePath() throws IOException {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    File baseDir = new File(".");
    String targetDir = baseDir.getCanonicalPath() + File.separator;
    String generateGitPropertiesFilename = "target" + File.separator + "classes" + File.separator + "git.properties";
    
    File result = commitIdMojo.craftPropertiesOutputFile(baseDir, generateGitPropertiesFilename);
    assertThat(result.getCanonicalPath()).isEqualTo(targetDir + generateGitPropertiesFilename);
  }

  @Test
  public void testCraftPropertiesOutputFileWithFullPath() throws IOException {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    File baseDir = new File(".");
    String targetDir = baseDir.getCanonicalPath() + File.separator;
    String generateGitPropertiesFilename = targetDir + "target" + File.separator + "classes" + File.separator + "git.properties";

    File result = commitIdMojo.craftPropertiesOutputFile(baseDir, generateGitPropertiesFilename);
    assertThat(result.getCanonicalPath()).isEqualTo(generateGitPropertiesFilename);
  }

  @Test
  public void verifyAllowedCharactersForEvaluateOnCommit() throws MojoExecutionException {
    Pattern p = GitCommitIdMojo.allowedCharactersForEvaluateOnCommit;
    assertTrue(p.matcher("5957e419d").matches());
    assertTrue(p.matcher("my_tag").matches());
    assertTrue(p.matcher("my-tag").matches());
    assertTrue(p.matcher("my.tag").matches());
    assertTrue(p.matcher("HEAD^1").matches());
    assertTrue(p.matcher("feature/branch").matches());

    assertFalse(p.matcher("; CODE INJECTION").matches());
    assertFalse(p.matcher("|exit").matches());
    assertFalse(p.matcher("&&cat /etc/passwd").matches());
  }
}
