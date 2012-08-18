package pl.project13.maven.git;

import com.google.common.base.Optional;
import org.eclipse.jgit.api.Git;
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
  public void setUp() {
    mavenSandbox = new FileSystemMavenSandbox(SANDBOX_DIR);
    mojo = new GitCommitIdMojo();
    initializeMojoWithDefaults(mojo);
  }

//  @After
//  public void cleanUp() throws IOException {
//    mavenSandbox.cleanup();
//  }

  protected Git git() throws IOException, InterruptedException {
    return Git.open(dotGitDir(projectDir()));
  }

  protected Optional<String> projectDir() {
    return Optional.absent();
  }

  protected File dotGitDir(Optional<String> projectDir) {
    if (projectDir.isPresent()) {
      return new File(SANDBOX_DIR + File.separator + projectDir.get() + File.separator + ".git");
    } else {
      return new File(SANDBOX_DIR + File.separator + ".git");
    }
  }

  void initializeMojoWithDefaults(GitCommitIdMojo mojo) {
    Map<String, Object> mojoDefaults = new HashMap<String, Object>();
    mojoDefaults.put("verbose", false);
    mojoDefaults.put("skipPoms", true);
    mojoDefaults.put("generateGitPropertiesFile", false);
    mojoDefaults.put("generateGitPropertiesFilename", "src/main/resources/git.properties");
    mojoDefaults.put("prefix", "git");
    mojoDefaults.put("dateFormat", "dd.MM.yyyy '@' HH:mm:ss z");
    mojoDefaults.put("failOnNoGitDirectory", true);
    for (Map.Entry<String, Object> entry : mojoDefaults.entrySet()) {
      setInternalState(mojo, entry.getKey(), entry.getValue());
    }
  }
}
