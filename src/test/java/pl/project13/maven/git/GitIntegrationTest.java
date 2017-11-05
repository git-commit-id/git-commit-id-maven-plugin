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

import com.google.common.base.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public abstract class GitIntegrationTest {

  private static final String SANDBOX_DIR = "target" + File.separator + "sandbox" + File.separator;
  protected static final String evaluateOnCommit = "HEAD";

  /**
   * Sandbox directory with unique name for current test.
   */
  private String currSandbox;

  protected GitCommitIdMojo mojo;
  protected FileSystemMavenSandbox mavenSandbox;

  @Before
  public void setUp() throws Exception {
    // generate unique sandbox for this test
    File sandbox;
    do {
      currSandbox = SANDBOX_DIR + "sandbox" + Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
      sandbox = new File(currSandbox);
    } while (sandbox.exists());

    mavenSandbox = new FileSystemMavenSandbox(currSandbox);
    mojo = new GitCommitIdMojo();
    initializeMojoWithDefaults(mojo);
  }

  @After
  public void tearDown() throws Exception {
    final boolean keep = mavenSandbox != null && mavenSandbox.isKeepSandboxWhenFinishedTest();

    mojo = null;
    mavenSandbox = null;

    final File sandbox = new File(currSandbox);
    try {
      if (sandbox.exists() && !keep) {
        FileUtils.deleteDirectory(sandbox);
      }
    } catch (IOException e) {
      System.out.println("Unable to delete sandbox. Scheduling deleteOnExit: " + currSandbox);
      sandbox.deleteOnExit();
    }
  }

  protected Git git(String dir) throws IOException, InterruptedException {
    return Git.open(dotGitDir(Optional.of(dir)));
  }

  protected Git git() throws IOException, InterruptedException {
    return Git.open(dotGitDir(projectDir()));
  }

  protected Optional<String> projectDir() {
    return Optional.absent();
  }

  @NotNull
  protected File dotGitDir(@NotNull Optional<String> projectDir) {
    if (projectDir.isPresent()) {
      return new File(currSandbox + File.separator + projectDir.get() + File.separator + ".git");
    } else {
      return new File(currSandbox + File.separator + ".git");
    }
  }

  public static void initializeMojoWithDefaults(GitCommitIdMojo mojo) {
    mojo.setVerbose(false);
    mojo.setSkipPoms(true);
    mojo.setAbbrevLength(7);
    mojo.setGenerateGitPropertiesFile(false);
    mojo.setGenerateGitPropertiesFilename("src/main/resources/git.properties");
    mojo.setPrefix("git");
    mojo.setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    mojo.setFailOnNoGitDirectory(true);
    mojo.setUseNativeGit(false);
    mojo.setCommitIdGenerationMode("full");
    mojo.setEvaluateOnCommit(evaluateOnCommit);
  }

  public void setProjectToExecuteMojoIn(@NotNull MavenProject project) {
    mojo.setProject(project);
    mojo.setDotGitDirectory(new File(project.getBasedir(), ".git"));
  }

}
