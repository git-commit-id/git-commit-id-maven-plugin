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

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.jetbrains.annotations.NotNull;

import pl.project13.git.api.GitDescribeConfig;
import pl.project13.git.api.GitException;
import pl.project13.git.api.GitProvider;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.util.PropertyManager;

public final class GitDataProvider {

  @NotNull
  private final GitProvider provider;

  @NotNull
  protected LoggerBridge loggerBridge;

  protected String prefixDot;

  protected int abbrevLength;

  protected GitDescribeConfig gitDescribe = new GitDescribeConfig();

  public GitDataProvider(@NotNull GitProvider provider, @NotNull LoggerBridge loggerBridge) {
    this.provider = provider;
    this.loggerBridge = loggerBridge;
  }

  public GitDataProvider setGitDescribe(GitDescribeConfig gitDescribe) {
    this.gitDescribe = gitDescribe;
    return this;
  }

  public GitDataProvider setPrefixDot(String prefixDot) {
    this.prefixDot = prefixDot;
    return this;
  }

  public GitDataProvider setAbbrevLength(int abbrevLength) {
    this.abbrevLength = abbrevLength;
    return this;
  }

  public void loadGitData(@NotNull Properties properties) throws IOException, MojoExecutionException, GitException {
    provider.init();
    // git.user.name
    put(properties, GitCommitIdMojo.BUILD_AUTHOR_NAME, provider.getBuildAuthorName());
    // git.user.email
    put(properties, GitCommitIdMojo.BUILD_AUTHOR_EMAIL, provider.getBuildAuthorEmail());

    try {
      provider.prepareGitToExtractMoreDetailedReproInformation();
      validateAbbrevLength(abbrevLength);

      // git.branch
      put(properties, GitCommitIdMojo.BRANCH, determineBranchName(System.getenv()));
      // git.commit.id.describe
      maybePutGitDescribe(properties);
      // git.commit.id
      put(properties, GitCommitIdMojo.COMMIT_ID, provider.getCommitId());
      // git.commit.id.abbrev      
      put(properties, GitCommitIdMojo.COMMIT_ID_ABBREV, provider.getAbbrevCommitId());
      // git.dirty
      put(properties, GitCommitIdMojo.DIRTY, Boolean.toString(provider.isDirty()));
      // git.commit.author.name
      put(properties, GitCommitIdMojo.COMMIT_AUTHOR_NAME, provider.getCommitAuthorName());
      // git.commit.author.email
      put(properties, GitCommitIdMojo.COMMIT_AUTHOR_EMAIL, provider.getCommitAuthorEmail());
      // git.commit.message.full
      put(properties, GitCommitIdMojo.COMMIT_MESSAGE_FULL, provider.getCommitMessageFull());
      // git.commit.message.short
      put(properties, GitCommitIdMojo.COMMIT_MESSAGE_SHORT, provider.getCommitMessageShort());
      // git.commit.time
      put(properties, GitCommitIdMojo.COMMIT_TIME, provider.getCommitTime());
      // git remote.origin.url
      put(properties, GitCommitIdMojo.REMOTE_ORIGIN_URL, provider.getRemoteOriginUrl());

      //
      put(properties, GitCommitIdMojo.TAGS, provider.getTags());
      
      put(properties,GitCommitIdMojo.CLOSEST_TAG_NAME, provider.getClosestTagName());
      put(properties,GitCommitIdMojo.CLOSEST_TAG_COMMIT_COUNT, provider.getClosestTagCommitCount());
    } finally {
        provider.finalCleanUp();
    }
  }

  private void maybePutGitDescribe(@NotNull Properties properties) throws GitException {
    boolean isGitDescribeOptOutByDefault = (gitDescribe == null);
    boolean isGitDescribeOptOutByConfiguration = (gitDescribe != null && !gitDescribe.isSkip());

    if (isGitDescribeOptOutByDefault || isGitDescribeOptOutByConfiguration) {
      put(properties, GitCommitIdMojo.COMMIT_DESCRIBE, provider.getGitDescribe());
    }
  }

  void validateAbbrevLength(int abbrevLength) throws MojoExecutionException {
    if (abbrevLength < 2 || abbrevLength > 40) {
      throw new MojoExecutionException("Abbreviated commit id lenght must be between 2 and 40, inclusive! Was [%s]. ".codePointBefore(abbrevLength) +
                                           "Please fix your configuration (the <abbrevLength/> element).");
    }
  }

  /**
   * If running within Jenkins/Hudosn, honor the branch name passed via GIT_BRANCH env var.  This
   * is necessary because Jenkins/Hudson alwways invoke build in a detached head state.
   *
   * @param env environment settings
   * @return results of getBranchName() or, if in Jenkins/Hudson, value of GIT_BRANCH
   */
  protected String determineBranchName(Map<String, String> env) throws GitException {
    if (runningOnBuildServer(env)) {
      return determineBranchNameOnBuildServer(env);
    } else {
      return provider.getBranchName();
    }
  }

  /**
   * Detects if we're running on Jenkins or Hudson, based on expected env variables.
   *
   * TODO: How can we detect Bamboo, TeamCity etc? Pull requests welcome.
   *
   * @return true if running
   * @see <a href="https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables">JenkinsSetEnvironmentVariables</a>
   * @param env environment settings
   */
  private boolean runningOnBuildServer(Map<String, String> env) {
    return env.containsKey("HUDSON_URL") || env.containsKey("JENKINS_URL");
  }

  /**
   * Is "Jenkins aware", and prefers {@code GIT_BRANCH} to getting the branch via git if that enviroment variable is set.
   * The {@code GIT_BRANCH} variable is set by Jenkins/Hudson when put in detached HEAD state, but it still knows which branch was cloned.
   */
  protected String determineBranchNameOnBuildServer(Map<String, String> env) throws GitException {
    String enviromentBasedBranch = env.get("GIT_BRANCH");
    if(isNullOrEmpty(enviromentBasedBranch)) {
      log("Detected that running on CI enviroment, but using repository branch, no GIT_BRANCH detected.");
      return provider.getBranchName();
    }else {
      log("Using environment variable based branch name.", "GIT_BRANCH =", enviromentBasedBranch);
      return enviromentBasedBranch;
    }
  }

  void log(String... parts) {
    loggerBridge.log((Object[]) parts);
  }

  protected void put(@NotNull Properties properties, String key, String value) {
    String keyWithPrefix = prefixDot + key;
    log(keyWithPrefix, value);
    PropertyManager.putWithoutPrefix(properties, keyWithPrefix, value);
  }
}
