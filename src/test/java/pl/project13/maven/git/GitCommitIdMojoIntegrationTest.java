/*
 * This file is part of git-commit-id-maven-plugin
 * Originally invented by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.project13.core.CommitIdPropertiesOutputFormat;
import pl.project13.core.git.GitDescribeConfig;
import pl.project13.core.util.GenericFileManager;

@RunWith(JUnitParamsRunner.class)
public class GitCommitIdMojoIntegrationTest extends GitIntegrationTest {
  @Test
  @Parameters(method = "useNativeGit")
  public void shouldIncludeExpectedProperties(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertGitPropertiesPresentInProject(properties);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldExcludeAsConfiguredProperties(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.useNativeGit = useNativeGit;
    mojo.excludeProperties = Arrays.asList("git.remote.origin.url", ".*.user.*");

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();

    // explicitly excluded
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.remote.origin.url"));

    // glob excluded
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.build.user.name"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.build.user.email"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.user.name"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.user.email"));

    // these stay
    assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.full"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldIncludeOnlyAsConfiguredProperties(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.useNativeGit = useNativeGit;
    mojo.includeOnlyProperties =
        Arrays.asList("git.remote.origin.url", ".*.user.*", "^git.commit.id.full$");

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();

    // explicitly included
    assertThat(properties).satisfies(new ContainsKeyCondition("git.remote.origin.url"));

    // glob included
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.email"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.full"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));

    // these excluded
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.branch"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.id.abbrev"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.message.full"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.message.short"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.time"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldExcludeAndIncludeAsConfiguredProperties(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.useNativeGit = useNativeGit;
    mojo.includeOnlyProperties = Arrays.asList("git.remote.origin.url", ".*.user.*");
    mojo.excludeProperties = Collections.singletonList("git.build.user.email");

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();

    // explicitly included
    assertThat(properties).satisfies(new ContainsKeyCondition("git.remote.origin.url"));

    // explicitly excluded -> overrules include only properties
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.build.user.email"));

    // glob included
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));

    // these excluded
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.branch"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.id.full"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.id.abbrev"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.message.full"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.message.short"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.time"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldHaveNoPrefixWhenConfiguredPrefixIsEmptyStringAsConfiguredProperties(
      boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.useNativeGit = useNativeGit;
    mojo.prefix = "";

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();

    // explicitly excluded
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.remote.origin.url"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition(".remote.origin.url"));
    assertThat(properties).satisfies(new ContainsKeyCondition("remote.origin.url"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldSkipDescribeWhenConfiguredToDoSo(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig config = new GitDescribeConfig();
    config.setSkip(true);

    mojo.useNativeGit = useNativeGit;
    mojo.gitDescribe = config;

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties())
        .satisfies(new DoesNotContainKeyCondition("git.commit.id.describe"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldNotUseBuildEnvironmentBranchInfoWhenParameterSet(boolean useNativeGit)
      throws Exception {
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    mojo.useBranchNameFromBuildEnvironment = false;
    mojo.useNativeGit = useNativeGit;

    Map<String, String> env = new HashMap<>();

    env.put("JENKINS_URL", "http://myciserver.com");
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_LOCAL_BRANCH", "localbranch");
    when(mojo.getCustomSystemEnv()).thenReturn(env);

    // reset repo and force detached HEAD
    try (final Git git = git("my-jar-project")) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("b6a73ed").call();
      git.checkout().setCreateBranch(true).setName("test_branch").setForceRefUpdate(true).call();
    }

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.branch", "test_branch");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldUseJenkinsBranchInfoWhenAvailable(boolean useNativeGit) throws Exception {
    // given
    Map<String, String> env = new HashMap<>();

    String detachedHeadSha1 = "b6a73ed747dd8dc98642d731ddbf09824efb9d48";
    String ciUrl = "http://myciserver.com";

    // when
    // in a detached head state, getBranch() will return the SHA1...standard behavior
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(
        useNativeGit, env, detachedHeadSha1);

    // again, SHA1 will be returned if we're in jenkins, but GIT_BRANCH is not set
    env.put("JENKINS_URL", ciUrl);
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(
        useNativeGit, env, detachedHeadSha1);

    // now set GIT_BRANCH too and see that the branch name from env var is returned
    env.clear();
    env.put("JENKINS_URL", ciUrl);
    env.put("GIT_BRANCH", "mybranch");
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, "mybranch");

    // same, but for hudson
    env.clear();
    env.put("HUDSON_URL", ciUrl);
    env.put("GIT_BRANCH", "mybranch");
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, "mybranch");

    // now set GIT_LOCAL_BRANCH too and see that the branch name from env var is returned
    env.clear();
    env.put("JENKINS_URL", ciUrl);
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(
        useNativeGit, env, "mylocalbranch");

    // same, but for hudson
    env.clear();
    env.put("HUDSON_URL", ciUrl);
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(
        useNativeGit, env, "mylocalbranch");

    // GIT_BRANCH but no HUDSON_URL or JENKINS_URL
    env.clear();
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
    shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(
        useNativeGit, env, detachedHeadSha1);
  }

  private void shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(
      boolean useNativeGit, Map<String, String> env, String expectedBranchName) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    mojo.useNativeGit = useNativeGit;
    mojo.useBranchNameFromBuildEnvironment = true;

    when(mojo.getCustomSystemEnv()).thenReturn(env);

    // reset repo and force detached HEAD
    try (final Git git = git("my-jar-project")) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("b6a73ed").call();
      git.checkout().setName("b6a73ed").setForceRefUpdate(true).call();
    }

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.branch", expectedBranchName);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldResolvePropertiesOnDefaultSettingsForNonPomProject(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldNotRunWhenSkipIsSet(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-skip-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.skip = true;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).isEmpty();
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldNotRunWhenPackagingPomAndDefaultSettingsApply(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).isEmpty();
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldRunWhenPackagingPomAndSkipPomsFalse(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.skipPoms = false;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).isNotEmpty();
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldUseParentProjectRepoWhenInvokedFromChild(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.skipPoms = false;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldUseChildProjectRepoIfInvokedFromChild(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.skipPoms = false;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test(expected = MojoExecutionException.class)
  @Parameters(method = "useNativeGit")
  public void shouldFailWithExceptionWhenNoGitRepoFound(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withNoGitRepoAvailable()
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.skipPoms = false;
    mojo.useNativeGit = useNativeGit;

    mojo.execute();
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCustomPropertiesFileProperties(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    String targetFilePath = "target/classes/custom-git.properties";
    File expectedFile = new File(targetProject.getBasedir(), targetFilePath);

    setProjectToExecuteMojoIn(targetProject);
    mojo.generateGitPropertiesFile = true;
    mojo.generateGitPropertiesFilename = targetFilePath;
    mojo.useNativeGit = useNativeGit;

    // when
    try {
      mojo.execute();

      // then
      assertThat(expectedFile).exists();
    } finally {
      FileUtils.forceDelete(expectedFile);
    }
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCustomPropertiesFileJson(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    String targetFilePath = "target/classes/custom-git.properties";
    File expectedFile = new File(targetProject.getBasedir(), targetFilePath);

    setProjectToExecuteMojoIn(targetProject);
    mojo.generateGitPropertiesFile = true;
    mojo.generateGitPropertiesFilename = targetFilePath;
    mojo.format = "json";
    mojo.useNativeGit = useNativeGit;
    // when
    try {
      mojo.execute();

      // then
      assertThat(expectedFile).exists();
      Properties p =
          GenericFileManager.readPropertiesAsUtf8(
              CommitIdPropertiesOutputFormat.JSON, expectedFile);
      assertThat(p.size() > 10);
    } finally {
      FileUtils.forceDelete(expectedFile);
    }
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldSkipWithoutFailOnNoGitDirectoryWhenNoGitRepoFound(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withNoGitRepoAvailable()
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.failOnNoGitDirectory = false;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).isEmpty();
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldNotSkipWithoutFailOnNoGitDirectoryWhenNoGitRepoIsPresent(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    mojo.failOnNoGitDirectory = false;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateDescribeWithTagOnlyWhenForceLongFormatIsFalse(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
    gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected

    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe", "v1.0.0-dirty");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void
      shouldGenerateDescribeWithTagOnlyWhenForceLongFormatIsFalseAndAbbrevLengthIsNonDefault(
          boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 10);
    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe", "v1.0.0");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe-short", "v1.0.0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateDescribeWithTagAndZeroAndCommitIdWhenForceLongFormatIsTrue(
      boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe", "v1.0.0-0-gde4db35");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void
      shouldGenerateDescribeWithTagAndZeroAndCommitIdWhenForceLongFormatIsTrueAndAbbrevLengthIsNonDefault(
          boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 10);
    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe", "v1.0.0-0-gde4db35917");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe-short", "v1.0.0-0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCommitIdAbbrevWithDefaultLength(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    mojo.abbrevLength = 7;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "de4db35");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCommitIdAbbrevWithNonDefaultLength(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    mojo.abbrevLength = 10;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).contains(entry("git.commit.id.abbrev", "de4db35917"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldFormatDate(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    String dateFormat = "MM/dd/yyyy";
    mojo.dateFormat = dateFormat;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());

    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
    String expectedDate = smf.format(new Date());
    assertThat(targetProject.getProperties()).contains(entry("git.build.time", expectedDate));
    assertThat(targetProject.getProperties()).contains(entry("git.commit.time", "08/19/2012"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldSkipGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    gitDescribeConfig.setSkip(true);
    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties())
        .satisfies(new DoesNotContainKeyCondition("git.commit.id.describe"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldMarkGitDescribeAsDirty(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);
    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties())
        .contains(entry("git.commit.id.describe", "v1.0.0-0-gde4db35" + dirtySuffix));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldAlwaysPrintGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    gitDescribeConfig.setAlways(true);
    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe", "0b0181b");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldWorkWithEmptyGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldWorkWithNullGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    mojo.gitDescribe = null;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldExtractTagsOnGivenCommit(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
        .create();

    try (final Git git = git("my-jar-project")) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("d37a598").call();
    }

    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    mojo.gitDescribe = null;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertGitPropertiesPresentInProject(properties);

    assertThat(properties).satisfies(new ContainsKeyCondition("git.tags"));
    assertThat(properties.get("git.tags").toString()).doesNotContain("refs/tags/");

    assertThat(Arrays.asList(properties.get("git.tags").toString().split(",")))
        .containsOnly("lightweight-tag", "newest-tag");
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "2");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldExtractTagsOnGivenCommitWithOldestCommit(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
        .create();

    try (final Git git = git("my-jar-project")) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("9597545").call();
    }

    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    mojo.gitDescribe = null;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertGitPropertiesPresentInProject(properties);

    assertThat(properties).satisfies(new ContainsKeyCondition("git.tags"));
    assertThat(properties.get("git.tags").toString()).doesNotContain("refs/tags/");

    assertThat(Arrays.asList(properties.get("git.tags").toString().split(",")))
        .containsOnly("annotated-tag", "lightweight-tag", "newest-tag");
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "1");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldExtractTagsOnHead(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG)
        .create();

    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    mojo.gitDescribe = null;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertGitPropertiesPresentInProject(properties);

    assertThat(properties).satisfies(new ContainsKeyCondition("git.tags"));
    assertThat(properties.get("git.tags").toString()).doesNotContain("refs/tags/");

    assertThat(Arrays.asList(properties.get("git.tags").toString().split(",")))
        .containsOnly("v1.0.0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void runGitDescribeWithMatchOption(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-plugin-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(
            AvailableGitTestRepo.WITH_THREE_COMMITS_AND_TWO_TAGS_CURRENTLY_ON_COMMIT_WITHOUT_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();

    setProjectToExecuteMojoIn(targetProject);

    String headCommitId = "b0c6d28b3b83bf7b905321bae67d9ca4c75a203f";
    Map<String, String> gitTagMap = new HashMap<>();
    gitTagMap.put("v1.0", "f830b5f85cad3d33ba50d04c3d1454e1ae469057");
    gitTagMap.put("v2.0", "0e3495783c56589213ee5f2ae8900e2dc1b776c4");

    for (Map.Entry<String, String> entry : gitTagMap.entrySet()) {
      String gitDescribeMatchNeedle = entry.getKey();
      String commitIdOfMatchNeedle = entry.getValue();

      GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
      gitDescribeConfig.setMatch(gitDescribeMatchNeedle);
      gitDescribeConfig.setAlways(false);

      mojo.gitDescribe = gitDescribeConfig;
      mojo.useNativeGit = useNativeGit;

      // when
      mojo.execute();

      // then
      assertThat(targetProject.getProperties().stringPropertyNames())
          .contains("git.commit.id.describe");
      assertThat(targetProject.getProperties().getProperty("git.commit.id.describe"))
          .startsWith(gitDescribeMatchNeedle);

      assertThat(targetProject.getProperties().stringPropertyNames())
          .contains("git.commit.id.full");
      assertThat(targetProject.getProperties().get("git.commit.id.full"))
          .isNotEqualTo(commitIdOfMatchNeedle);
      assertThat(targetProject.getProperties().get("git.commit.id.full")).isEqualTo(headCommitId);
      assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "3");
    }
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWhenOnATag(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
    gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected

    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.name", "v1.0.0");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.commit.count", "0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWhenOnATagAndDirty(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);
    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.name", "v1.0.0");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.commit.count", "0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWhenCommitHasTwoTags(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
        .create();

    try (final Git git = git("my-jar-project")) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("d37a598").call();
    }

    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    mojo.gitDescribe = null;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    // AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS ==> Where the newest-tag was created
    // latest
    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.name", "newest-tag");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.commit.count", "0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldUseDateFormatTimeZone(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    // RFC 822 time zone: Sign TwoDigitHours Minutes
    // we want only the timezone (formated in RFC 822) out of the dateformat (easier for asserts)
    String dateFormat = "Z";
    String expectedTimeZoneOffset = "+0200";
    String executionTimeZoneOffset = "-0800";
    TimeZone expectedTimeZone = TimeZone.getTimeZone("GMT" + expectedTimeZoneOffset);
    TimeZone executionTimeZone = TimeZone.getTimeZone("GMT" + executionTimeZoneOffset);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;
    mojo.dateFormat = dateFormat;
    mojo.dateFormatTimeZone = expectedTimeZone.getID();

    // override the default timezone for execution and testing
    TimeZone currentDefaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(executionTimeZone);

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertPropertyPresentAndEqual(properties, "git.commit.time", expectedTimeZoneOffset);

    assertPropertyPresentAndEqual(properties, "git.build.time", expectedTimeZoneOffset);

    // set the timezone back
    TimeZone.setDefault(currentDefaultTimeZone);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCommitIdOldFashioned(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    mojo.useNativeGit = useNativeGit;
    mojo.commitIdGenerationMode = "flat";

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertThat(properties.stringPropertyNames()).contains("git.commit.id");
    assertThat(properties.stringPropertyNames()).doesNotContain("git.commit.id.full");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void testDetectCleanWorkingDirectory(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.GIT_WITH_NO_CHANGES)
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);
    mojo.gitDescribe = gitDescribeConfig;

    mojo.useNativeGit = useNativeGit;
    mojo.commitIdGenerationMode = "flat";

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertThat(properties.get("git.dirty")).isEqualTo("false");
    assertThat(properties)
        .contains(entry("git.commit.id.describe", "85c2888")); // assert no dirtySuffix at the end!
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void testDetectDirtyWorkingDirectory(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT) // GIT_WITH_CHANGES
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);
    mojo.gitDescribe = gitDescribeConfig;

    mojo.useNativeGit = useNativeGit;
    mojo.commitIdGenerationMode = "flat";

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertThat(properties.get("git.dirty")).isEqualTo("true");
    assertThat(properties)
        .contains(
            entry(
                "git.commit.id.describe",
                "0b0181b" + dirtySuffix)); // assert dirtySuffix at the end!
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWithExcludeLightweightTagsForClosestTag(
      boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
    gitDescribe.setDirty("-customDirtyMark");
    gitDescribe.setTags(false); // exclude lightweight tags

    mojo.gitDescribe = gitDescribe;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "b6a73ed");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(),
        "git.commit.id.describe",
        "annotated-tag-2-gb6a73ed74-customDirtyMark");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.name", "annotated-tag");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.commit.count", "2");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "3");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWithIncludeLightweightTagsForClosestTag(
      boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
    gitDescribe.setDirty("-customDirtyMark");
    gitDescribe.setTags(true); // include lightweight tags

    mojo.gitDescribe = gitDescribe;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "b6a73ed");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(),
        "git.commit.id.describe",
        "lightweight-tag-1-gb6a73ed74-customDirtyMark");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.name", "lightweight-tag");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.commit.count", "1");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void
      shouldGenerateClosestTagInformationWithIncludeLightweightTagsForClosestTagAndPreferAnnotatedTags(
          boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
    gitDescribe.setDirty("-customDirtyMark");
    gitDescribe.setTags(true); // include lightweight tags

    mojo.gitDescribe = gitDescribe;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "b6a73ed");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(),
        "git.commit.id.describe",
        "newest-tag-1-gb6a73ed74-customDirtyMark");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.name", "newest-tag");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.commit.count", "1");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWithIncludeLightweightTagsForClosestTagAndFilter(
      boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
    gitDescribe.setDirty("-customDirtyMark");
    gitDescribe.setTags(true); // include lightweight tags
    gitDescribe.setMatch("light*");

    mojo.gitDescribe = gitDescribe;
    mojo.useNativeGit = useNativeGit;

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "b6a73ed");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(),
        "git.commit.id.describe",
        "lightweight-tag-1-gb6a73ed74-customDirtyMark");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.name", "lightweight-tag");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.closest.tag.commit.count", "1");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyEvalOnDifferentCommitWithParentOfHead(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
    gitDescribe.setDirty(null);

    mojo.gitDescribe = gitDescribe;
    mojo.useNativeGit = useNativeGit;
    mojo.evaluateOnCommit = "HEAD^1";

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "e3d159d");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe", "e3d159dd7");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.tags", "test_tag");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.dirty", "true");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyEvalOnDifferentCommitWithBranchName(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
    gitDescribe.setDirty(null);

    mojo.gitDescribe = gitDescribe;
    mojo.useNativeGit = useNativeGit;
    mojo.evaluateOnCommit = "test";

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "9cb810e");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe", "test_tag-0-g9cb810e57");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.branch", "test");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.tags", "test_tag");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.dirty", "true");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "2");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyEvalOnDifferentCommitWithTagName(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
    gitDescribe.setDirty(null);

    mojo.gitDescribe = gitDescribe;
    mojo.useNativeGit = useNativeGit;
    mojo.evaluateOnCommit = "test_tag";

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "9cb810e");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe", "test_tag-0-g9cb810e57");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.branch", "9cb810e57e2994f38c7ec6a698a31de66fdd9e24");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.tags", "test_tag");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.dirty", "true");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "2");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyEvalOnDifferentCommitWithCommitHash(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
    gitDescribe.setDirty(null);

    mojo.gitDescribe = gitDescribe;
    mojo.useNativeGit = useNativeGit;
    mojo.evaluateOnCommit = "9cb810e";

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "9cb810e");

    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.commit.id.describe", "test_tag-0-g9cb810e57");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.branch", "test");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.tags", "test_tag");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.dirty", "true");

    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "2");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyEvalOnCommitWithTwoBranches(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    // create a new branch on the current HEAD commit:
    //    2343428 - Moved master - Fri, 29 Nov 2013 10:38:34 +0100 (HEAD, branch: master)
    try (final Git git = git("my-jar-project")) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("2343428").call();
      git.checkout().setCreateBranch(true).setName("another_branch").setForceRefUpdate(true).call();
      git.checkout().setCreateBranch(true).setName("z_branch").setForceRefUpdate(true).call();
    }

    mojo.useNativeGit = useNativeGit;
    mojo.evaluateOnCommit = "2343428";

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "2343428");
    assertPropertyPresentAndEqual(
        targetProject.getProperties(), "git.branch", "another_branch,master,z_branch");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void verifyDetachedHeadIsNotReportedAsBranch(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-jar-project", "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
        .create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);

    // detach head
    try (final Git git = git("my-jar-project")) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef("2343428").call();
    }

    mojo.useNativeGit = useNativeGit;
    // mojo.evaluateOnCommit = "HEAD"; // do not change this

    // when
    mojo.execute();

    // then
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "2343428");
    assertPropertyPresentAndEqual(targetProject.getProperties(), "git.branch", "master");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGeneratePropertiesWithMultiplePrefixesAndReactorProject(boolean useNativeGit)
      throws Exception {
    // given
    mavenSandbox
        .withParentProject("my-pom-project", "pom")
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG)
        .withChildProject("my-child-module", "jar")
        .create();
    MavenProject targetProject = mavenSandbox.getChildProject(); // "my-child-two-module"

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
    gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected

    mojo.gitDescribe = gitDescribeConfig;
    mojo.useNativeGit = useNativeGit;
    mojo.injectAllReactorProjects = true;

    List<String> prefixes = Arrays.asList("prefix-one", "prefix-two");
    // when
    // simulate plugin execution with multiple prefixes
    // see
    // https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/137#issuecomment-418144756
    for (String prefix : prefixes) {
      mojo.prefix = prefix;

      mojo.execute();
    }

    // then
    // since we inject into all reactors both projects should have both properties
    Properties properties = targetProject.getProperties();
    for (String prefix : prefixes) {
      assertPropertyPresentAndEqual(properties, prefix + ".commit.id.abbrev", "de4db35");

      assertPropertyPresentAndEqual(properties, prefix + ".closest.tag.name", "v1.0.0");

      assertPropertyPresentAndEqual(properties, prefix + ".closest.tag.commit.count", "0");
    }
  }

  private GitDescribeConfig createGitDescribeConfig(boolean forceLongFormat, int abbrev) {
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setTags(true);
    gitDescribeConfig.setForceLongFormat(forceLongFormat);
    gitDescribeConfig.setAbbrev(abbrev);
    gitDescribeConfig.setDirty("");
    return gitDescribeConfig;
  }
}
