package pl.project13.maven.git;

import static org.junit.Assert.assertThat;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.StoredConfig;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

    protected abstract T gitProvider();

    protected void extraSetup() {
	// Override in subclass to perform extra stuff in setup
    }

    @Test
    public void shouldNotBeAheadOrBehind() throws Exception {

	AheadBehind aheadBehind = gitProvider.getAheadBehind();
	assertThat(aheadBehind.ahead(), CoreMatchers.is("0"));
	assertThat(aheadBehind.behind(), CoreMatchers.is("0"));
    }

    @Test
    public void shouldBe1Ahead() throws Exception {

	createLocalCommit();

	AheadBehind aheadBehind = gitProvider.getAheadBehind();
	assertThat(aheadBehind.ahead(), CoreMatchers.is("1"));
	assertThat(aheadBehind.behind(), CoreMatchers.is("0"));
    }

    @Test
    public void shouldBe1Behind() throws Exception {

	createCommitInSecondRepoAndPush();

	AheadBehind aheadBehind = gitProvider.getAheadBehind();
	assertThat(aheadBehind.ahead(), CoreMatchers.is("0"));
	assertThat(aheadBehind.behind(), CoreMatchers.is("1"));
    }

    @Test
    public void shouldBe1AheadAnd1Behind() throws Exception {

	createLocalCommit();
	createCommitInSecondRepoAndPush();

	AheadBehind aheadBehind = gitProvider.getAheadBehind();
	assertThat(aheadBehind.ahead(), CoreMatchers.is("1"));
	assertThat(aheadBehind.behind(), CoreMatchers.is("1"));
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
