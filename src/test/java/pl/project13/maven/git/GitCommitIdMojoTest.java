/*
 * This file is part of git-commit-id-maven-plugin by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
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

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import pl.project13.core.PropertiesFileGenerator;

import java.io.File;
import java.io.IOException;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitCommitIdMojoTest {
  @Test
  public void testCraftPropertiesOutputFileWithRelativePath() throws IOException {
    File baseDir = new File(".");
    String targetDir = baseDir.getCanonicalPath() + File.separator;
    String generateGitPropertiesFilePath = "target" + File.separator + "classes" + File.separator + "git.properties";
    File generateGitPropertiesFile = new File(generateGitPropertiesFilePath);
    
    File result = PropertiesFileGenerator.craftPropertiesOutputFile(baseDir, generateGitPropertiesFile);
    assertThat(result.getCanonicalPath()).isEqualTo(
            new File(targetDir).toPath().resolve(generateGitPropertiesFilePath).toFile().getCanonicalPath());
  }

  @Test
  public void testCraftPropertiesOutputFileWithFullPath() throws IOException {
    File baseDir = new File(".");
    String targetDir = baseDir.getCanonicalPath() + File.separator;
    String generateGitPropertiesFilePath = targetDir + "target" + File.separator + "classes" + File.separator + "git.properties";
    File generateGitPropertiesFile = new File(generateGitPropertiesFilePath);

    File result = PropertiesFileGenerator.craftPropertiesOutputFile(baseDir, generateGitPropertiesFile);
    assertThat(result.getCanonicalPath()).isEqualTo(
            new File(targetDir).toPath().resolve(generateGitPropertiesFilePath).toFile().getCanonicalPath());
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
