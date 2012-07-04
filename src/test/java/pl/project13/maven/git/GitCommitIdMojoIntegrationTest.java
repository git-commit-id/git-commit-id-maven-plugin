package pl.project13.maven.git;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pl.project13.maven.git.TemporaryMavenProject.CleanUp;

public class GitCommitIdMojoIntegrationTest {

	private GitCommitIdMojo mojo;
	private TemporaryMavenProject temporaryProject;

	@Before
	public void setUp() {
		temporaryProject = new TemporaryMavenProject();
		mojo = new GitCommitIdMojo();
		initializeWithDefaults(mojo);
	}
	
	@After
	public void tearDown() throws Exception {
		temporaryProject.cleanUp();
	}
	
	@Test
	public void shouldResolveGitRelatedPropertiesOnDefaultSettingsForJarProject() throws Exception {
		temporaryProject.withParentProject("target/sandbox/my-jar-project", "jar")
			.withNoChildProject()
			.withGitRepoInParent()
			.create(CleanUp.CLEANUP_FIRST);
		MavenProject targetProject = temporaryProject.getParentProject();
		setProjectToExecuteIn(targetProject);
		
		// when
		mojo.execute();
		
		// then
		// TODO: convert to matcher or custom assertion
		Properties properties = targetProject.getProperties();
	    assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
	    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id"));
	    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
	    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
	    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.email"));
	    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
	    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));
	    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
	    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
	    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));
	}

	private void alterSettings(String parameterName, Object parameterValue) {
		setInternalState(mojo, parameterName, parameterValue);
	}
	
	private void setProjectToExecuteIn(MavenProject project) {
		setInternalState(mojo, "project", project);
		setInternalState(mojo, "dotGitDirectory", new File(project.getBasedir(), ".git"));		
	}

	private void initializeWithDefaults(GitCommitIdMojo mojo) {
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
