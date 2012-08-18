package pl.project13.maven.git;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import pl.project13.maven.git.FileSystemMavenSandbox.CleanUp;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

public class GitCommitIdMojoIntegrationTest extends GitIntegrationTest {

  @Test
  public void shouldResolvePropertiesOnDefaultSettingsForNonPomProject() throws Exception {
    mavenSandbox.withParentProject("my-jar-project", "jar").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
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
    mavenSandbox.withParentProject("my-pom-project", "pom").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
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
    mavenSandbox.withParentProject("my-pom-project", "pom").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
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
    mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
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
    mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
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
