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

import org.jetbrains.annotations.NotNull;

import pl.project13.git.api.GitDescribeConfig;
import pl.project13.git.api.GitException;
import pl.project13.git.api.GitProvider;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.util.PropertyManager;

import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import static com.google.common.base.Strings.isNullOrEmpty;

public final class GitDataProvider {
  @NotNull
  private final GitProvider provider;

  @NotNull
  protected final LoggerBridge log;

  protected String prefixDot;

  protected int abbrevLength;

  protected CommitIdGenerationMode commitIdGenerationMode;

  protected GitDescribeConfig gitDescribe = new GitDescribeConfig();

  public GitDataProvider(@NotNull GitProvider provider, @NotNull LoggerBridge log) {
    this.provider = provider;
    this.log = log;
  }

  public GitDataProvider setPrefixDot(String prefixDot) {
    this.prefixDot = prefixDot;
    return this;
  }

  public GitDataProvider setAbbrevLength(int abbrevLength) {
    this.abbrevLength = abbrevLength;
    provider.setAbbrevLength(abbrevLength);
    return this;
  }

  public GitDataProvider setGitDescribe(GitDescribeConfig gitDescribe) {
    this.gitDescribe = gitDescribe;
    provider.setGitDescribe(gitDescribe);
    return this;
  }

  public GitDataProvider setDateFormat(String dateFormat) {
    provider.setDateFormat(dateFormat);
    return this;
  }

  public GitDataProvider setDateFormatTimeZone(String dateFormatTimeZone) {
    provider.setDateFormatTimeZone(dateFormatTimeZone);
    return this;
  }

  public GitDataProvider setCommitIdGenerationMode(CommitIdGenerationMode commitIdGenerationMode) {
    this.commitIdGenerationMode = commitIdGenerationMode;
    return this;
  }

  public void loadGitData(@NotNull Properties properties) throws GitException {
    provider.init();
    provider.prepareGitToExtractMoreDetailedReproInformation();
    // git.user.name
    put(properties, GitCommitPropertyConstant.BUILD_AUTHOR_NAME, provider.getBuildAuthorName());
    // git.user.email
    put(properties, GitCommitPropertyConstant.BUILD_AUTHOR_EMAIL, provider.getBuildAuthorEmail());

    try {
      provider.prepareGitToExtractMoreDetailedReproInformation();
      validateAbbrevLength(abbrevLength);

      // git.branch
      put(properties, GitCommitPropertyConstant.BRANCH, determineBranchName(System.getenv()));
      // git.commit.id.describe
      maybePutGitDescribe(properties);
      // git.commit.id
      switch (commitIdGenerationMode) {
        case FULL: {
          put(properties, GitCommitPropertyConstant.COMMIT_ID_FULL, provider.getCommitId());
          break;
        }
        case FLAT: {
          put(properties, GitCommitPropertyConstant.COMMIT_ID_FLAT, provider.getCommitId());
          break;
        }
        default: {
          throw new GitException("Unsupported commitIdGenerationMode: " + commitIdGenerationMode);
        }
      }
      // git.commit.id.abbrev
      put(properties, GitCommitPropertyConstant.COMMIT_ID_ABBREV, provider.getAbbrevCommitId());
      // git.dirty
      put(properties, GitCommitPropertyConstant.DIRTY, Boolean.toString(provider.isDirty()));
      // git.commit.author.name
      put(properties, GitCommitPropertyConstant.COMMIT_AUTHOR_NAME, provider.getCommitAuthorName());
      // git.commit.author.email
      put(properties, GitCommitPropertyConstant.COMMIT_AUTHOR_EMAIL, provider.getCommitAuthorEmail());
      // git.commit.message.full
      put(properties, GitCommitPropertyConstant.COMMIT_MESSAGE_FULL, provider.getCommitMessageFull());
      // git.commit.message.short
      put(properties, GitCommitPropertyConstant.COMMIT_MESSAGE_SHORT, provider.getCommitMessageShort());
      // git.commit.time
      put(properties, GitCommitPropertyConstant.COMMIT_TIME, provider.getCommitTime());
      // git remote.origin.url
      put(properties, GitCommitPropertyConstant.REMOTE_ORIGIN_URL, provider.getRemoteOriginUrl());

      //
      put(properties, GitCommitPropertyConstant.TAGS, provider.getTags());

      put(properties, GitCommitPropertyConstant.CLOSEST_TAG_NAME, provider.getClosestTagName());
      put(properties, GitCommitPropertyConstant.CLOSEST_TAG_COMMIT_COUNT, provider.getClosestTagCommitCount());
    } finally {
      provider.finalCleanUp();
    }
  }

  private void maybePutGitDescribe(@NotNull Properties properties) throws GitException {
    boolean isGitDescribeOptOutByDefault = (gitDescribe == null);
    boolean isGitDescribeOptOutByConfiguration = (gitDescribe != null && !gitDescribe.isSkip());

    if (isGitDescribeOptOutByDefault || isGitDescribeOptOutByConfiguration) {
      put(properties, GitCommitPropertyConstant.COMMIT_DESCRIBE, provider.getGitDescribe());
    }
  }

  void validateAbbrevLength(int abbrevLength) throws GitException {
    if (abbrevLength < 2 || abbrevLength > 40) {
      throw new GitException(String.format("Abbreviated commit id length must be between 2 and 40, inclusive! Was [%s]. ", abbrevLength) +
                                           "Please fix your configuration (the <abbrevLength/> element).");
    }
  }

  /**
   * If running within Jenkins/Hudson, honor the branch name passed via GIT_BRANCH env var.
   * This is necessary because Jenkins/Hudson always invoke build in a detached head state.
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
   * @param env environment settings
   * @return true if running
   * @see <a href="https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables">JenkinsSetEnvironmentVariables</a>
   */
  private boolean runningOnBuildServer(Map<String, String> env) {
    return env.containsKey("HUDSON_URL") || env.containsKey("JENKINS_URL") ||
           env.containsKey("HUDSON_HOME") || env.containsKey("JENKINS_HOME");
  }

  /**
   * Is "Jenkins aware", and prefers {@code GIT_LOCAL_BRANCH} over {@code GIT_BRANCH} to getting the branch via git if that environment variables are set.
   * The {@code GIT_LOCAL_BRANCH} and {@code GIT_BRANCH} variables are set by Jenkins/Hudson when put in detached HEAD state, but it still knows which branch was cloned.
   */
  protected String determineBranchNameOnBuildServer(Map<String, String> env) throws GitException {
    String environmentBasedLocalBranch = env.get("GIT_LOCAL_BRANCH");
    if (!isNullOrEmpty(environmentBasedLocalBranch)) {
      log.info("Using environment variable based branch name. GIT_LOCAL_BRANCH = {}", environmentBasedLocalBranch);
      return environmentBasedLocalBranch;
    }

    String environmentBasedBranch = env.get("GIT_BRANCH");
    if (!isNullOrEmpty(environmentBasedBranch)) {
      log.info("Using environment variable based branch name. GIT_BRANCH = {}", environmentBasedBranch);
      return environmentBasedBranch;
    }

    log.info("Detected that running on CI environment, but using repository branch, no GIT_BRANCH detected.");
    return provider.getBranchName();
  }

  protected void put(@NotNull Properties properties, String key, String value) {
    String keyWithPrefix = prefixDot + key;
    log.info("{} {}", keyWithPrefix, value);
    PropertyManager.putWithoutPrefix(properties, keyWithPrefix, value);
  }
}