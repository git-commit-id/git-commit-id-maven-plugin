package pl.project13.maven.git;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pl.project13.maven.git.FileSystemMavenSandbox.CleanUp;

public class GitCommitIdMojoIntegrationTest {

	private GitCommitIdMojo mojo;
	private FileSystemMavenSandbox mavenSandbox;

	@Before
	public void setUp() {
		mavenSandbox = new FileSystemMavenSandbox("target/sandbox");
		mojo = new GitCommitIdMojo();
		initializeWithDefaults(mojo);
	}

	@After
	public void cleanUp() throws IOException {
		mavenSandbox.cleanup();
	}

	@Test
	public void shouldResolvePropertiesOnDefaultSettingsForNonPomProject() throws Exception {
		mavenSandbox.withParentProject("my-jar-project", "jar").withNoChildProject().withGitRepoInParent().create(CleanUp.CLEANUP_FIRST);
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);

		// when
		mojo.execute();

		// then
		assertGitPropertiesPresentInProject(targetProject.getProperties());
	}

	@Test
	public void shouldNotRunWhenPackagingPomAndDefaultSettingsApply() throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom").withNoChildProject().withGitRepoInParent().create(CleanUp.CLEANUP_FIRST);
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);

		// when
		mojo.execute();

		// then
		assertThat(targetProject.getProperties()).isEmpty();
	}

	@Test
	public void shouldRunWhenPackagingPomAndSkipPomsFalse() throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom").withNoChildProject().withGitRepoInParent().create(CleanUp.CLEANUP_FIRST);
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		alterMojoSettings("skipPoms", false);

		// when
		mojo.execute();

		// then
		assertThat(targetProject.getProperties()).isNotEmpty();
	}

	@Test
	public void shouldUseParentProjectRepoWhenInvokedFromChild() throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withGitRepoInParent().create(CleanUp.CLEANUP_FIRST);
		MavenProject targetProject = mavenSandbox.getChildProject();
		setProjectToExecuteMojoIn(targetProject);
		alterMojoSettings("skipPoms", false);

		// when
		mojo.execute();

		// then
		assertGitPropertiesPresentInProject(targetProject.getProperties());
	}

	@Test
	public void shouldUseChildProjectRepoIfInvokedFromChild() throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withGitRepoInChild().create(CleanUp.CLEANUP_FIRST);
		MavenProject targetProject = mavenSandbox.getChildProject();
		setProjectToExecuteMojoIn(targetProject);
		alterMojoSettings("skipPoms", false);

		// when
		mojo.execute();

		// then
		assertGitPropertiesPresentInProject(targetProject.getProperties());
	}

	@Test(expected = MojoExecutionException.class)
	public void shouldFailWithExceptionWhenNoGitRepoFound() throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withNoGitRepoAvailable().create(CleanUp.CLEANUP_FIRST);
		MavenProject targetProject = mavenSandbox.getChildProject();
		setProjectToExecuteMojoIn(targetProject);
		alterMojoSettings("skipPoms", false);

		// when
		mojo.execute();
	}

	private void alterMojoSettings(String parameterName, Object parameterValue) {
		setInternalState(mojo, parameterName, parameterValue);
	}

	private void setProjectToExecuteMojoIn(MavenProject project) {
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

	private void assertGitPropertiesPresentInProject(Properties properties) {
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

}
