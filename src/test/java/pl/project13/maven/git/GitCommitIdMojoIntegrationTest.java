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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Arrays.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@RunWith(JUnitParamsRunner.class)
public class GitCommitIdMojoIntegrationTest extends GitIntegrationTest {

  static final boolean UseJGit = false;
  static final boolean UseNativeGit = true;

  public static Collection<?> useNativeGit() {
    return asList(UseJGit, UseNativeGit);
  }

  public static Collection<?> useDirty() {
    return asList(true, false);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldResolvePropertiesOnDefaultSettingsForNonPomProject(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-jar-project", "jar").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldNotRunWhenSkipIsSet(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-skip-project", "jar").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("skip", true);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).isEmpty();
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldNotRunWhenPackagingPomAndDefaultSettingsApply(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create();
    MavenProject targetProject = mavenSandbox.getParentProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).isEmpty();
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldRunWhenPackagingPomAndSkipPomsFalse(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom").withNoChildProject().withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create();
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
  @Parameters(method = "useNativeGit")
  public void shouldUseParentProjectRepoWhenInvokedFromChild(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT).create();
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
  @Parameters(method = "useNativeGit")
  public void shouldUseChildProjectRepoIfInvokedFromChild(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom").withChildProject("my-jar-module", "jar").withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT).create();
    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("skipPoms", false);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertGitPropertiesPresentInProject(targetProject.getProperties());
  }

  @Test(expected = MojoExecutionException.class)
  @Parameters(method = "useNativeGit")
  public void shouldFailWithExceptionWhenNoGitRepoFound(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withNoGitRepoAvailable()
                .create();

    MavenProject targetProject = mavenSandbox.getChildProject();
    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("skipPoms", false);
    alterMojoSettings("useNativeGit", useNativeGit);

    mojo.execute();
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCustomPropertiesFileProperties(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS)
                .create();

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
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCustomPropertiesFileJson(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS)
                .create();

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
      String json = Files.toString(expectedFile, Charset.forName("UTF-8"));
      ObjectMapper om = new ObjectMapper();
      Map<?, ?> map = new HashMap<>();
      map = om.readValue(json, map.getClass());
      assertThat(map.size() > 10);
    } finally {
      FileUtils.forceDelete(expectedFile);
    }
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldSkipWithoutFailOnNoGitDirectoryWhenNoGitRepoFound(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-jar-project", "jar")
                .withNoChildProject()
                .withNoGitRepoAvailable()
                .create();

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
  @Parameters(method = "useNativeGit")
  public void shouldNotSkipWithoutFailOnNoGitDirectoryWhenNoGitRepoIsPresent(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-jar-project", "jar")
                .withNoChildProject()
                .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
                .create();

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
  @Parameters(method = "useNativeGit")
  public void shouldGenerateDescribeWithTagOnlyWhenForceLongFormatIsFalse(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
                .create();

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
    gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected

    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.commit.id.describe");
    assertThat(targetProject.getProperties().getProperty("git.commit.id.describe")).isEqualTo("v1.0.0-dirty");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateDescribeWithTagOnlyWhenForceLongFormatIsFalseAndAbbrevLengthIsNonDefault(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
                .create();

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 10);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    Set<String> propNames = targetProject.getProperties().stringPropertyNames();

    assertThat(propNames).contains("git.commit.id.describe");
    assertThat(targetProject.getProperties().getProperty("git.commit.id.describe")).isEqualTo("v1.0.0");

    assertThat(propNames).contains("git.commit.id.describe-short");
    assertThat(targetProject.getProperties().getProperty("git.commit.id.describe-short")).isEqualTo("v1.0.0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateDescribeWithTagAndZeroAndCommitIdWhenForceLongFormatIsTrue(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
                .create();

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.commit.id.describe");
    assertThat(targetProject.getProperties().getProperty("git.commit.id.describe")).isEqualTo("v1.0.0-0-gde4db35");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateDescribeWithTagAndZeroAndCommitIdWhenForceLongFormatIsTrueAndAbbrevLengthIsNonDefault(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
                .create();

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 10);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    Set<String> propNames = targetProject.getProperties().stringPropertyNames();

    assertThat(propNames).contains("git.commit.id.describe");
    assertThat(targetProject.getProperties().getProperty("git.commit.id.describe")).isEqualTo("v1.0.0-0-gde4db35917");

    assertThat(propNames).contains("git.commit.id.describe-short");
    assertThat(targetProject.getProperties().getProperty("git.commit.id.describe-short")).isEqualTo("v1.0.0-0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCommitIdAbbrevWithDefaultLength(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
                .create();

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    alterMojoSettings("abbrevLength", 7);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.commit.id.abbrev");
    assertThat(targetProject.getProperties().getProperty("git.commit.id.abbrev")).isEqualTo("de4db35");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCommitIdAbbrevWithNonDefaultLength(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
                .create();

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
  @Parameters(method = "useNativeGit")
  public void shouldFormatDate(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
                .create();
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
  @Parameters(method = "useNativeGit")
  public void shouldSkipGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
                .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    gitDescribeConfig.setSkip(true);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).satisfies(new DoesNotContainKeyCondition("git.commit.id.describe"));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldMarkGitDescribeAsDirty(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
                .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties()).includes(entry("git.commit.id.describe", "v1.0.0-0-gde4db35" + dirtySuffix));
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldAlwaysPrintGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
                .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    gitDescribeConfig.setAlways(true);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.commit.id.describe");
    assertThat(targetProject.getProperties().getProperty("git.commit.id.describe")).isEqualTo("0b0181b");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldWorkWithEmptyGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
                .create();
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
  @Parameters(method = "useNativeGit")
  public void shouldWorkWithNullGitDescribe(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
                .create();
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

    alterMojoSettings("gitDescribe", null);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertGitPropertiesPresentInProject(properties);

    assertThat(properties).satisfies(new ContainsKeyCondition("git.tags"));
    assertThat(properties.get("git.tags").toString()).doesNotContain("refs/tags/");

    assertThat(Splitter.on(",").split(properties.get("git.tags").toString()))
      .containsOnly("lightweight-tag", "newest-tag");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void runGitDescribeWithMatchOption(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-plugin-project", "jar")
                .withNoChildProject()
                .withGitRepoInParent(AvailableGitTestRepo.WITH_THREE_COMMITS_AND_TWO_TAGS_CURRENTLY_ON_COMMIT_WITHOUT_TAG)
                .create();
    MavenProject targetProject = mavenSandbox.getParentProject();

    setProjectToExecuteMojoIn(targetProject);

    String headCommitId = "b0c6d28b3b83bf7b905321bae67d9ca4c75a203f";
    Map<String,String> gitTagMap = new HashMap<>();
    gitTagMap.put("v1.0", "f830b5f85cad3d33ba50d04c3d1454e1ae469057");
    gitTagMap.put("v2.0", "0e3495783c56589213ee5f2ae8900e2dc1b776c4");

    for (Map.Entry<String,String> entry : gitTagMap.entrySet()) {
      String gitDescribeMatchNeedle = entry.getKey();
      String commitIdOfMatchNeedle = entry.getValue();

      GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
      gitDescribeConfig.setMatch(gitDescribeMatchNeedle);
      gitDescribeConfig.setAlways(false);

      alterMojoSettings("gitDescribe", gitDescribeConfig);
      alterMojoSettings("useNativeGit", useNativeGit);

      // when
      mojo.execute();

      // then
      assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.commit.id.describe");
      assertThat(targetProject.getProperties().getProperty("git.commit.id.describe")).startsWith(gitDescribeMatchNeedle);

      assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.commit.id.full");
      assertThat(targetProject.getProperties().get("git.commit.id.full")).isNotEqualTo(commitIdOfMatchNeedle);
      assertThat(targetProject.getProperties().get("git.commit.id.full")).isEqualTo(headCommitId);
    }
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWhenOnATag(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
                .create();

    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);
    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
    gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected

    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.closest.tag.name");
    assertThat(targetProject.getProperties().getProperty("git.closest.tag.name")).isEqualTo("v1.0.0");

    assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.closest.tag.commit.count");
    assertThat(targetProject.getProperties().getProperty("git.closest.tag.commit.count")).isEqualTo("0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWhenOnATagAndDirty(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
                .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.closest.tag.name");
    assertThat(targetProject.getProperties().getProperty("git.closest.tag.name")).isEqualTo("v1.0.0");

    assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.closest.tag.commit.count");
    assertThat(targetProject.getProperties().getProperty("git.closest.tag.commit.count")).isEqualTo("0");
  }  


  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateClosestTagInformationWhenCommitHasTwoTags(boolean useNativeGit) throws Exception {
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

    alterMojoSettings("gitDescribe", null);
    alterMojoSettings("useNativeGit", useNativeGit);

    // when
    mojo.execute();

    // then
    // AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS ==> Where the newest-tag was created latest
    assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.closest.tag.name");
    assertThat(targetProject.getProperties().getProperty("git.closest.tag.name")).isEqualTo("newest-tag");

    assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.closest.tag.commit.count");
    assertThat(targetProject.getProperties().getProperty("git.closest.tag.commit.count")).isEqualTo("0");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldUseDateFormatTimeZone(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
                .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    // RFC 822 time zone: Sign TwoDigitHours Minutes
    String dateFormat = "Z"; // we want only the timezone (formated in RFC 822) out of the dateformat (easier for asserts)
    String expectedTimeZoneOffset = "+0200";
    String executionTimeZoneOffset = "-0800";
    TimeZone expectedTimeZone = TimeZone.getTimeZone("GMT" + expectedTimeZoneOffset);
    TimeZone executionTimeZone = TimeZone.getTimeZone("GMT" + executionTimeZoneOffset);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    alterMojoSettings("gitDescribe", gitDescribeConfig);
    alterMojoSettings("useNativeGit", useNativeGit);
    alterMojoSettings("dateFormat", dateFormat);
    alterMojoSettings("dateFormatTimeZone", expectedTimeZone.getID());

    // override the default timezone for execution and testing
    TimeZone currentDefaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(executionTimeZone);

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertThat(properties.stringPropertyNames()).contains("git.commit.time");
    assertThat(properties.getProperty("git.commit.time")).isEqualTo(expectedTimeZoneOffset);

    assertThat(properties.stringPropertyNames()).contains("git.build.time");
    assertThat(properties.getProperty("git.build.time")).isEqualTo(expectedTimeZoneOffset);

    // set the timezone back
    TimeZone.setDefault(currentDefaultTimeZone);
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void shouldGenerateCommitIdOldFashioned(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
                .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    alterMojoSettings("useNativeGit", useNativeGit);
    alterMojoSettings("commitIdGenerationMode", "flat");

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertThat(properties.stringPropertyNames()).contains("git.commit.id");
    assertThat(properties.stringPropertyNames()).excludes("git.commit.id.full");
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void testDetectCleanWorkingDirectory(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.GIT_WITH_NO_CHANGES)
                .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);
    alterMojoSettings("gitDescribe", gitDescribeConfig);

    alterMojoSettings("useNativeGit", useNativeGit);
    alterMojoSettings("commitIdGenerationMode", "flat");

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertThat(properties.get("git.dirty")).isEqualTo("false");
    assertThat(properties).includes(entry("git.commit.id.describe", "85c2888")); // assert no dirtySuffix at the end!
  }

  @Test
  @Parameters(method = "useNativeGit")
  public void testDetectDirtyWorkingDirectory(boolean useNativeGit) throws Exception {
    // given
    mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT) // GIT_WITH_CHANGES
                .create();
    MavenProject targetProject = mavenSandbox.getChildProject();

    setProjectToExecuteMojoIn(targetProject);

    GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
    String dirtySuffix = "-dirtyTest";
    gitDescribeConfig.setDirty(dirtySuffix);
    alterMojoSettings("gitDescribe", gitDescribeConfig);

    alterMojoSettings("useNativeGit", useNativeGit);
    alterMojoSettings("commitIdGenerationMode", "flat");

    // when
    mojo.execute();

    // then
    Properties properties = targetProject.getProperties();
    assertThat(properties.get("git.dirty")).isEqualTo("true");
    assertThat(properties).includes(entry("git.commit.id.describe", "0b0181b" + dirtySuffix)); // assert dirtySuffix at the end!
  }

  private GitDescribeConfig createGitDescribeConfig(boolean forceLongFormat, int abbrev) {
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setTags(true);
    gitDescribeConfig.setForceLongFormat(forceLongFormat);
    gitDescribeConfig.setAbbrev(abbrev);
    gitDescribeConfig.setDirty("");
    return gitDescribeConfig;
  }

  private void alterMojoSettings(String parameterName, Object parameterValue) {
    setInternalState(mojo, parameterName, parameterValue);
  }

  private void assertGitPropertiesPresentInProject(Properties properties) {
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.time"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.host"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.full"));
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
    assertThat(properties).satisfies(new ContainsKeyCondition("git.closest.tag.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.closest.tag.commit.count"));
  }
}
