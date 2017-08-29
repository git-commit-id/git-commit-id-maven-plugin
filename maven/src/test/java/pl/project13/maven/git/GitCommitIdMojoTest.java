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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;
import pl.project13.git.log.StdOutLoggerBridge;
import pl.project13.git.JGitProvider;
import pl.project13.git.GitCommitIdExecutionException;
import pl.project13.git.GitCommitPropertyConstant;
import pl.project13.git.PropertiesReplacer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * I'm not a big fan of this test - let's move to integration test from now on.
 *
 */
// todo remove this test in favor of complete integration tests
public class GitCommitIdMojoTest {

  GitCommitIdMojo mojo;
  JGitProvider jGitProvider;

  @Before
  public void setUp() throws Exception {
    File dotGitDirectory = AvailableGitTestRepo.GIT_COMMIT_ID.getDir();
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setSkip(false);

    String prefix = "git";
    int abbrevLength = 7;
    String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";

    mojo = new GitCommitIdMojo();
    mojo.setDotGitDirectory(dotGitDirectory);
    mojo.setPrefix(prefix);
    mojo.setAbbrevLength(abbrevLength);
    mojo.setDateFormat(dateFormat);
    mojo.setVerbose(true);
    mojo.useNativeGit(false);
    mojo.setGitDescribe(gitDescribeConfig);
    mojo.setCommitIdGenerationMode("full");


    mojo.project = mock(MavenProject.class, RETURNS_MOCKS);
    Properties props = new Properties();
    when(mojo.project.getProperties()).thenReturn(props);
    when(mojo.project.getPackaging()).thenReturn("jar");
    when(mojo.project.getVersion()).thenReturn("3.3-SNAPSHOT");

    jGitProvider = JGitProvider.on(mojo.lookupGitDirectory(), new StdOutLoggerBridge(true));
  }

  @Test
  @SuppressWarnings("")
  public void shouldIncludeExpectedProperties() throws Exception {
    mojo.execute();

    Properties properties = mojo.getProperties();

    assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.dirty"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.full"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.email"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.remote.origin.url"));
  }

  @Test
  @SuppressWarnings("")
  public void shouldExcludeAsConfiguredProperties() throws Exception {
    // given
    mojo.setExcludeProperties(ImmutableList.of("git.remote.origin.url", ".*.user.*"));

    // when
    mojo.execute();

    // then
    Properties properties = mojo.getProperties();

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
  public void shouldIncludeOnlyAsConfiguredProperties() throws Exception {
    // given
    mojo.setIncludeOnlyProperties(ImmutableList.of("git.remote.origin.url", ".*.user.*", "^git.commit.id.full$"));

    // when
    mojo.execute();

    // then
    Properties properties = mojo.getProperties();

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
  public void shouldExcludeAndIncludeAsConfiguredProperties() throws Exception {
    // given
    mojo.setIncludeOnlyProperties(ImmutableList.of("git.remote.origin.url", ".*.user.*"));
    mojo.setExcludeProperties(ImmutableList.of("git.build.user.email"));

    // when
    mojo.execute();

    // then
    Properties properties = mojo.getProperties();

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
  @SuppressWarnings("")
  public void shouldHaveNoPrefixWhenConfiguredPrefixIsEmptyStringAsConfiguredProperties() throws Exception {
    // given
    mojo.setPrefix("");

    // when
    mojo.execute();

    // then
    Properties properties = mojo.getProperties();

    // explicitly excluded
    assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.remote.origin.url"));
    assertThat(properties).satisfies(new DoesNotContainKeyCondition(".remote.origin.url"));
    assertThat(properties).satisfies(new ContainsKeyCondition("remote.origin.url"));
  }

  @Test
  public void shouldSkipDescribeWhenConfiguredToDoSo() throws Exception {
    // given
    GitDescribeConfig config = new GitDescribeConfig();
    config.setSkip(true);

    // when
    mojo.setGitDescribe(config);
    mojo.execute();

    // then
    assertThat(mojo.getProperties()).satisfies(new DoesNotContainKeyCondition("git.commit.id.describe"));
  }

  @Test
  public void shouldUseJenkinsBranchInfoWhenAvailable() throws GitCommitIdExecutionException, IOException {
    // given
    Repository git = mock(Repository.class);
    Map<String, String> env = Maps.newHashMap();

    String detachedHeadSHA1 = "16bb801934e652f5e291a003db05e364d83fba25";
    String ciUrl = "http://myciserver.com";

    when(git.getBranch()).thenReturn(detachedHeadSHA1);
    jGitProvider.setRepository(git);
    // when
    // in a detached head state, getBranch() will return the SHA1...standard behavior
    assertThat(detachedHeadSHA1).isEqualTo(jGitProvider.determineBranchName(env));

    // again, SHA1 will be returned if we're in jenkins, but GIT_BRANCH is not set
    env.put("JENKINS_URL", "http://myjenkinsserver.com");
    assertThat(detachedHeadSHA1).isEqualTo(jGitProvider.determineBranchName(env));

    // now set GIT_BRANCH too and see that the branch name from env var is returned
    env.clear();
    env.put("JENKINS_URL", ciUrl);
    env.put("GIT_BRANCH", "mybranch");
    assertThat("mybranch").isEqualTo(jGitProvider.determineBranchName(env));
  
    // same, but for hudson
    env.clear();
    env.put("GIT_BRANCH", "mybranch");
    env.put("HUDSON_URL", ciUrl);
    assertThat("mybranch").isEqualTo(jGitProvider.determineBranchName(env));

    // now set GIT_LOCAL_BRANCH too and see that the branch name from env var is returned
    env.clear();
    env.put("JENKINS_URL", ciUrl);
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
    assertThat("mylocalbranch").isEqualTo(jGitProvider.determineBranchName(env));

    // same, but for hudson
    env.clear();
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
    env.put("HUDSON_URL", ciUrl);
    assertThat("mylocalbranch").isEqualTo(jGitProvider.determineBranchName(env));

    // GIT_BRANCH but no HUDSON_URL or JENKINS_URL
    env.clear();
    env.put("GIT_BRANCH", "mybranch");
    env.put("GIT_BRANCH", "mylocalbranch");
    assertThat(detachedHeadSHA1).isEqualTo(jGitProvider.determineBranchName(env));
  }
  
  @Test
  public void loadShortDescribe() {
    assertShortDescribe("1.0.2-12-g19471", "1.0.2-12");
    assertShortDescribe("v1.0.0-0-gde4db35917", "v1.0.0-0");
    assertShortDescribe("1.0.2-12-g19471-DEV", "1.0.2-12-DEV");
    assertShortDescribe("V-1.0.2-12-g19471-DEV", "V-1.0.2-12-DEV");

    assertShortDescribe(null, null);
    assertShortDescribe("12.4.0-1432", "12.4.0-1432");
    assertShortDescribe("12.6.0", "12.6.0");
    assertShortDescribe("", "");
  }

  private void assertShortDescribe(String commitDescribe, String expectedShortDescribe) {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    Properties prop = new Properties();
    if (commitDescribe != null) {
      prop.put(GitCommitPropertyConstant.COMMIT_DESCRIBE, commitDescribe);
    }
    commitIdMojo.loadShortDescribe(prop);
    assertThat(prop.getProperty(GitCommitPropertyConstant.COMMIT_SHORT_DESCRIBE)).isEqualTo(expectedShortDescribe);
  }

  @Test
  public void testCraftPropertiesOutputFileWithRelativePath() throws IOException {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    File baseDir = new File(".");
    String targetDir = baseDir.getCanonicalPath() + File.separator;
    String generateGitPropertiesFilename = "target" + File.separator + "classes" + File.separator + "git.properties";
    
    File result = commitIdMojo.craftPropertiesOutputFile(baseDir, generateGitPropertiesFilename);
    assertThat(result.getCanonicalPath()).isEqualTo(targetDir + generateGitPropertiesFilename);
  }

  @Test
  public void testCraftPropertiesOutputFileWithFullPath() throws IOException {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    File baseDir = new File(".");
    String targetDir = baseDir.getCanonicalPath() + File.separator;
    String generateGitPropertiesFilename = targetDir + "target" + File.separator + "classes" + File.separator + "git.properties";

    File result = commitIdMojo.craftPropertiesOutputFile(baseDir, generateGitPropertiesFilename);
    assertThat(result.getCanonicalPath()).isEqualTo(generateGitPropertiesFilename);
  }

  @Test
  public void shouldPerformReplacements() throws MojoExecutionException
  {
    mojo.propertiesReplacer = mock(PropertiesReplacer.class);
    mojo.replacementProperties = mock(List.class);

    mojo.execute();

    verify(mojo.propertiesReplacer).performReplacement(any(Properties.class), eq(mojo.replacementProperties));
  }

}
