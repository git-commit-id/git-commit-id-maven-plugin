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

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import pl.project13.core.AheadBehind;
import pl.project13.core.GitProvider;

import static org.junit.Assert.assertEquals;

public abstract class AheadBehindTest<T extends GitProvider> {

  @Rule
  public TemporaryFolder remoteRepository = new TemporaryFolder();

  @Rule
  public TemporaryFolder localRepository = new TemporaryFolder();

  @Rule
  public TemporaryFolder secondLocalRepository = new TemporaryFolder();

  protected Git localRepositoryGit;

  protected Git secondLocalRepositoryGit;

  protected T gitProvider;

  @Before
  public void setup() throws Exception {

    createRemoteRepository();

    setupLocalRepository();

    createAndPushInitialCommit();

    setupSecondLocalRepository();

    gitProvider = gitProvider();

    extraSetup();
  }

  @After
  public void tearDown() throws Exception {
    if (localRepositoryGit != null) {
      localRepositoryGit.close();
    }

    if (secondLocalRepositoryGit != null) {
      secondLocalRepositoryGit.close();
    }
  }

  protected abstract T gitProvider();

  protected void extraSetup() {
    // Override in subclass to perform extra stuff in setup
  }

  @Test
  public void shouldNotBeAheadOrBehind() throws Exception {

    AheadBehind aheadBehind = gitProvider.getAheadBehind();
    assertEquals(aheadBehind.ahead(), "0");
    assertEquals(aheadBehind.behind(), "0");
  }

  @Test
  public void shouldBe1Ahead() throws Exception {

    createLocalCommit();

    AheadBehind aheadBehind = gitProvider.getAheadBehind();
    assertEquals(aheadBehind.ahead(),"1");
    assertEquals(aheadBehind.behind(),"0");
  }

  @Test
  public void shouldBe1Behind() throws Exception {

    createCommitInSecondRepoAndPush();

    AheadBehind aheadBehind = gitProvider.getAheadBehind();
    assertEquals(aheadBehind.ahead(),"0");
    assertEquals(aheadBehind.behind(),"1");
  }

  @Test
  public void shouldBe1AheadAnd1Behind() throws Exception {

    createLocalCommit();
    createCommitInSecondRepoAndPush();

    AheadBehind aheadBehind = gitProvider.getAheadBehind();
    assertEquals(aheadBehind.ahead(),"1");
    assertEquals(aheadBehind.behind(),"1");
  }

  protected void createLocalCommit() throws Exception {
    File newFile = localRepository.newFile();
    localRepositoryGit.add().addFilepattern(newFile.getName()).call();
    localRepositoryGit.commit().setMessage("ahead").call();
  }

  protected void createCommitInSecondRepoAndPush() throws Exception {
    secondLocalRepositoryGit.pull().call();

    File newFile = secondLocalRepository.newFile();
    secondLocalRepositoryGit.add().addFilepattern(newFile.getName()).call();
    secondLocalRepositoryGit.commit().setMessage("behind").call();

    secondLocalRepositoryGit.push().call();
  }

  protected void createRemoteRepository() throws Exception {
    Git.init().setBare(true).setDirectory(remoteRepository.getRoot()).call();
  }

  protected void setupLocalRepository() throws Exception {
    localRepositoryGit = Git.cloneRepository().setURI(remoteRepository.getRoot().toURI().toString())
        .setDirectory(localRepository.getRoot()).setBranch("master").call();

    StoredConfig config = localRepositoryGit.getRepository().getConfig();
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, "master", "remote", "origin");
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, "master", "merge", "refs/heads/master");
    config.save();
  }

  protected void setupSecondLocalRepository() throws Exception {
    secondLocalRepositoryGit = Git.cloneRepository().setURI(remoteRepository.getRoot().toURI().toString())
        .setDirectory(secondLocalRepository.getRoot()).setBranch("master").call();
  }

  protected void createAndPushInitialCommit() throws Exception {
    File newFile = localRepository.newFile();
    localRepositoryGit.add().addFilepattern(newFile.getName()).call();
    localRepositoryGit.commit().setMessage("initial").call();

    localRepositoryGit.push().call();
  }

}
