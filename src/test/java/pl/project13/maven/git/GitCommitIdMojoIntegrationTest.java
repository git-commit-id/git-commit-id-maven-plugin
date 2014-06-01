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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.util.Collection;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import pl.project13.maven.git.FileSystemMavenSandbox.CleanUp;
import pl.project13.test.utils.AssertException;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@RunWith(JUnitParamsRunner.class)
public class GitCommitIdMojoIntegrationTest extends GitIntegrationTest {

  public static Collection defaultParameter() {
    return Arrays.asList(new Object[] {
      Boolean.FALSE,
      Boolean.TRUE
    });
  }

  public static Collection performanceParameter() {
    return Arrays.asList(new Object[][]{
              {Boolean.FALSE, 1},
              {Boolean.TRUE, 1},
              {Boolean.FALSE, 10},
              {Boolean.TRUE, 10},
              {Boolean.FALSE, 100},
              {Boolean.TRUE, 100},
              {Boolean.FALSE, 1000},              
              {Boolean.TRUE, 1000}              
    });
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldResolvePropertiesOnDefaultSettingsForNonPomProject(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-jar-project", "jar").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldNotRunWhenSkipIsSet(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-skip-project", "jar").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("skip", Boolean.TRUE);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).isEmpty();
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldNotRunWhenPackagingPomAndDefaultSettingsApply(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).isEmpty();
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldRunWhenPackagingPomAndSkipPomsFalse(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("skipPoms", false);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).isNotEmpty();
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldUseParentProjectRepoWhenInvokedFromChild(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("skipPoms", false);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldUseChildProjectRepoIfInvokedFromChild(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT).create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("skipPoms", false);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldFailWithExceptionWhenNoGitRepoFound(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withNoGitRepoAvailable()
                .create(CleanUp.CLEANUP_FIRST);

    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("skipPoms", false);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    AssertException.CodeBlock block = new AssertException.CodeBlock() {
      @Override
      public void run() throws Exception {
        mojo.execute();
      }
    };

    // then
    AssertException.thrown(MojoExecutionException.class, block);
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldGenerateCustomPropertiesFileProperties(boolean useNativeGit) throws Exception {
      // given
      mavenSandbox.withParentProject("my-pom-project", "pom")
          .withChildProject("my-jar-module", "jar")
          .withGitRepoInChild(AvailableGitTestRepo.GIT_COMMIT_ID)
          .create(CleanUp.CLEANUP_FIRST);

      MavenProject targetProject = mavenSandbox.getChildProject();

      String targetFilePath = "target/classes/custom-git.properties";
      File expectedFile = new File(targetProject.getBasedir(), targetFilePath);

      setProjectToExecuteMojoIn(targetProject);
      alterMojoSettings("generateGitPropertiesFile", true);
      alterMojoSettings("generateGitPropertiesFilename", targetFilePath);
      alterMojoSettings("useNativeGit", useNativeGit);

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
  @Parameters(method = "defaultParameter")
  public void shouldGenerateCustomPropertiesFileJson(boolean useNativeGit) throws Exception {
      // given
      mavenSandbox.withParentProject("my-pom-project", "pom")
          .withChildProject("my-jar-module", "jar")
          .withGitRepoInChild(AvailableGitTestRepo.GIT_COMMIT_ID)
          .create(CleanUp.CLEANUP_FIRST);

      MavenProject targetProject = mavenSandbox.getChildProject();

      String targetFilePath = "target/classes/custom-git.properties";
      File expectedFile = new File(targetProject.getBasedir(), targetFilePath);

      setProjectToExecuteMojoIn(targetProject);
      alterMojoSettings("generateGitPropertiesFile", true);
      alterMojoSettings("generateGitPropertiesFilename", targetFilePath);
      alterMojoSettings("format", "json");
      alterMojoSettings("useNativeGit", useNativeGit);

      // when
      try {
        mojo.execute();

        // then
        assertThat(expectedFile).exists();
        String json = Files.toString(expectedFile, Charset.defaultCharset());
        ObjectMapper om = new ObjectMapper();
        Map<String, String> map = new HashMap<String, String>();
        map = om.readValue(expectedFile, map.getClass());
        assertThat(map.size() > 10);
      } finally {
        FileUtils.forceDelete(expectedFile);
      }
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldSkipWithoutFailOnNoGitDirectoryWhenNoGitRepoFound(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-jar-project", "jar")
                .withNoChildProject()
                .withNoGitRepoAvailable()
                .create(CleanUp.CLEANUP_FIRST);

    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("failOnNoGitDirectory", false);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).isEmpty();
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldNotSkipWithoutFailOnNoGitDirectoryWhenNoGitRepoIsPresent(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-jar-project", "jar")
                .withNoChildProject()
                .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
                .create(CleanUp.CLEANUP_FIRST);

    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("failOnNoGitDirectory", false);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldGenerateDescribeWithTagOnlyWhenForceLongFormatIsFalse(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create(CleanUp.CLEANUP_FIRST);

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false,7);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).includes(entry("git.commit.id.describe", "v1.0.0"));
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldGenerateDescribeWithTagOnlyWhenForceLongFormatIsFalseAndAbbrevLengthIsNonDefault(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create(CleanUp.CLEANUP_FIRST);

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false,10);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).includes(entry("git.commit.id.describe", "v1.0.0"));
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldGenerateDescribeWithTagAndZeroAndCommitIdWhenForceLongFormatIsTrue(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create(CleanUp.CLEANUP_FIRST);

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true,7);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).includes(entry("git.commit.id.describe", "v1.0.0-0-gde4db35"));
    assertThat(targetProject.getProperties()).includes(entry("git.commit.id.describe-short", "v1.0.0-0"));
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldGenerateDescribeWithTagAndZeroAndCommitIdWhenForceLongFormatIsTrueAndAbbrevLengthIsNonDefault(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create(CleanUp.CLEANUP_FIRST);

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true,10);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).includes(entry("git.commit.id.describe", "v1.0.0-0-gde4db35917"));
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldGenerateCommitIdAbbrevWithDefaultLength(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create(CleanUp.CLEANUP_FIRST);

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("abbrevLength", 7);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).includes(entry("git.commit.id.abbrev", "de4db35"));
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldGenerateCommitIdAbbrevWithNonDefaultLength(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create(CleanUp.CLEANUP_FIRST);

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("abbrevLength", 10);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).includes(entry("git.commit.id.abbrev", "de4db35917"));
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldFormatDate(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    String dateFormat = "MM/dd/yyyy";
    alterMojoSettings("dateFormat", dateFormat);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());

    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
    String expectedDate = smf.format(new Date());
    assertThat(targetProject.getProperties()).includes(entry("git.build.time", expectedDate));
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldSkipGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
        .create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true,7);
    gitDescribeConfig.setSkip(true);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).satisfies(new DoesNotContainKeyCondition("git.commit.id.describe"));
  }
  
  @Test
  @Parameters(method = "defaultParameter")
  public void shouldMarkGitDescribeAsDirty(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
        .create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true,7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).includes(entry("git.commit.id.describe", "v1.0.0-0-gde4db35"+dirtySuffix));
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldAlwaysPrintGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true,7);
    gitDescribeConfig.setAlways(true);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).includes(entry("git.commit.id.describe", "0b0181b"));
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldWorkWithEmptyGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldWorkWithNullGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    alterMojoSettings("gitDescribe", null);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "defaultParameter")
  public void shouldGenerateOriginRemoteUrl(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.MAVEN_GIT_COMMIT_ID_PLUGIN)
        .create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    alterMojoSettings("gitDescribe", null);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertGitPropertiesPresentInProject(properties);

    String remoteUrl = (String) properties.get("git.remote.origin.url");
    assertThat(remoteUrl.isEmpty()).isFalse();
  }

  @Test
  @Parameters(method = "performanceParameter")
  @Ignore("performance Test only")
  public void performance(boolean useNativeGit, int iterations) throws Exception {
    // given
   mavenSandbox.withParentProject("my-pom-project", "pom")
        .withChildProject("my-jar-module", "jar")
        .withGitRepoInChild(AvailableGitTestRepo.MAVEN_GIT_COMMIT_ID_PLUGIN)
        .create(CleanUp.CLEANUP_FIRST);
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true,7);
    gitDescribeConfig.setAlways(true);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    long startTime = System.currentTimeMillis();
    for(int i=0;i<iterations;i++){
      mojo.execute();
    }
    long estimatedTime = System.currentTimeMillis() - startTime;
    System.out.println("[***] Iterations: " +iterations+ " Time: " + estimatedTime + " ms for useNativeGit=" +useNativeGit);

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }
  
  
  private GitDescribeConfig createGitDescribeConfig(boolean forceLongFormat, int abbrev){
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setTags(true);
    gitDescribeConfig.setForceLongFormat(forceLongFormat);
    gitDescribeConfig.setAbbrev(abbrev);
    return gitDescribeConfig;
  }

  private void alterMojoSettings(String parameterName, Object parameterValue) {
    setInternalState(mojo, parameterName, parameterValue);
  }

  private void assertGitPropertiesPresentInProject(Properties properties) {
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.time"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.describe"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.email"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.remote.origin.url"));
  }
}
