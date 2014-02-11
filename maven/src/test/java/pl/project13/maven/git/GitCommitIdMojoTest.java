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

import static org.fest.assertions.Assertions.assertThat;

import static org.mockito.Matchers.any;

import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import java.util.Map;
import java.util.Properties;

import org.apache.maven.project.MavenProject;

import org.eclipse.jgit.lib.Repository;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * I'm not a big fan of this test - let's move to integration test from now on.
 *
 * @author  <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 */
@Ignore
public class GitCommitIdMojoTest {

    GitCommitIdMojo mojo;

    @Before
    public void setUp() throws Exception {
        mojo = new GitCommitIdMojo();
        mojo.setDotGitDirectory(new File(".git/"));
        mojo.setPrefix("git");
        mojo.setAbbrevLength(7);
        mojo.setDateFormat("dd.MM.yyyy '@' HH:mm:ss z");
        mojo.setVerbose(true);

        mojo.runningTests = true;
        mojo.project = mock(MavenProject.class, RETURNS_MOCKS);
        when(mojo.project.getPackaging()).thenReturn("jar");

        mojo = spy(mojo);
//        doNothing().when(mojo).putGitDescribe(any(Properties.class), any(Repository.class));
    }

    @Test
    public void shouldIncludeExpectedProperties() throws Exception {
        mojo.execute();

        Properties properties = mojo.getProperties();
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.email"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.remote.origin.url"));

//        verify(mojo).maybePutGitDescribe(any(Properties.class), any(Repository.class));
//        verify(mojo).putGitDescribe(any(Properties.class), any(Repository.class));
    }

    @Test
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
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
//        assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));

//        verify(mojo).maybePutGitDescribe(any(Properties.class), any(Repository.class));
//        verify(mojo).putGitDescribe(any(Properties.class), any(Repository.class));
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
//        verify(mojo).maybePutGitDescribe(any(Properties.class), any(Repository.class));
//        verify(mojo, never()).putGitDescribe(any(Properties.class), any(Repository.class));
    }

    @Test
    public void shouldUseJenkinsBranchInfoWhenAvailable() throws IOException {

        // given
        Repository git = mock(Repository.class);
        Map<String, String> env = Maps.newHashMap();

        String detachedHeadSHA1 = "16bb801934e652f5e291a003db05e364d83fba25";
        String ciUrl = "http://myciserver.com";

        when(git.getBranch()).thenReturn(detachedHeadSHA1);

        // when
        // in a detached head state, getBranch() will return the SHA1...standard behavior
//        assertThat(detachedHeadSHA1).isEqualTo(mojo.determineBranchName(git, env));

        // again, SHA1 will be returned if we're in jenkins, but GIT_BRANCH is not set
        env.put("JENKINS_URL", "http://myjenkinsserver.com");
//        assertThat(detachedHeadSHA1).isEqualTo(mojo.determineBranchName(git, env));

        // now set GIT_BRANCH too and see that the branch name from env var is returned
        env.clear();
        env.put("JENKINS_URL", ciUrl);
        env.put("GIT_BRANCH", "mybranch");
//        assertThat("mybranch").isEqualTo(mojo.determineBranchName(git, env));

        // same, but for hudson
        env.clear();
        env.put("GIT_BRANCH", "mybranch");
        env.put("HUDSON_URL", ciUrl);
//        assertThat("mybranch").isEqualTo(mojo.determineBranchName(git, env));

        // GIT_BRANCH but no HUDSON_URL or JENKINS_URL
        env.clear();
        env.put("GIT_BRANCH", "mybranch");
//        assertThat(detachedHeadSHA1).isEqualTo(mojo.determineBranchName(git, env));
    }
}
