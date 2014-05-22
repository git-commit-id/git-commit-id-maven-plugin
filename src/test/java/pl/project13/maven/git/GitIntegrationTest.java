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

import com.google.common.base.Optional;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

public abstract class GitIntegrationTest {

  final String SANDBOX_DIR = "target/sandbox";

  protected GitCommitIdMojo mojo;
  protected FileSystemMavenSandbox mavenSandbox;

  @Before
  public void setUp() throws Exception {
    mavenSandbox = new FileSystemMavenSandbox(SANDBOX_DIR);
    mojo = new GitCommitIdMojo();
    initializeMojoWithDefaults(mojo);
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
      return new File(SANDBOX_DIR + File.separator + projectDir.get() + File.separator + ".git");
    } else {
      return new File(SANDBOX_DIR + File.separator + ".git");
    }
  }

  public static void initializeMojoWithDefaults(GitCommitIdMojo mojo) {
    Map<String, Object> mojoDefaults = new HashMap<String, Object>();
    mojoDefaults.put("verbose", false);
    mojoDefaults.put("skipPoms", true);
    mojoDefaults.put("abbrevLength", 7);
    mojoDefaults.put("generateGitPropertiesFile", false);
    mojoDefaults.put("generateGitPropertiesFilename", "src/main/resources/git.properties");
    mojoDefaults.put("prefix", "git");
    mojoDefaults.put("dateFormat", "dd.MM.yyyy '@' HH:mm:ss z");
    mojoDefaults.put("failOnNoGitDirectory", true);
    mojoDefaults.put("useNativeGit", false);
    for (Map.Entry<String, Object> entry : mojoDefaults.entrySet()) {
      setInternalState(mojo, entry.getKey(), entry.getValue());
    }
  }

  public void setProjectToExecuteMojoIn(@NotNull MavenProject project) {
    setInternalState(mojo, "project", project);
    setInternalState(mojo, "dotGitDirectory", new File(project.getBasedir(), ".git"));
  }

}
