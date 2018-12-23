package pl.project13.maven.git;

import java.nio.file.Paths;

public class JgitProviderAheadBehindTest extends AheadBehindTest<JGitProvider> {

  @Override
  public void extraSetup() {
    gitProvider.setRepository(localRepositoryGit.getRepository());
  }

  @Override
  protected JGitProvider gitProvider() {
    return new JGitProvider(Paths.get(localRepository.getRoot().getAbsolutePath(), ".git").toFile(), null);
  }
}
