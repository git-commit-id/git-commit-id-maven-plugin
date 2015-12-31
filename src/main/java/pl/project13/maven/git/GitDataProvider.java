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

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.util.PropertyManager;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class GitDataProvider {

  @NotNull
  protected final Mojo mojo;

  protected boolean verbose;

  protected String prefixDot;

  protected int abbrevLength;

  protected String dateFormat;

  protected String dateFormatTimeZone;

  protected GitDescribeConfig gitDescribe = new GitDescribeConfig();

  public GitDataProvider(@NotNull Mojo mojo) {
    this.mojo = mojo;
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

  public GitDataProvider setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
    return this;
  }

  public GitDataProvider setDateFormatTimeZone(String dateFormatTimeZone){
    this.dateFormatTimeZone = dateFormatTimeZone;
    return this;
  }

  protected abstract void init() throws MojoExecutionException;
  protected abstract String getBuildAuthorName();
  protected abstract String getBuildAuthorEmail();
  protected abstract void prepareGitToExtractMoreDetailedRepoInformation() throws MojoExecutionException;
  protected abstract String getBranchName() throws IOException;
  protected abstract String getGitDescribe() throws MojoExecutionException;
  protected abstract String getCommitId();
  protected abstract String getAbbrevCommitId() throws MojoExecutionException;
  protected abstract boolean isDirty() throws MojoExecutionException;
  protected abstract String getCommitAuthorName();
  protected abstract String getCommitAuthorEmail();
  protected abstract String getCommitMessageFull();
  protected abstract String getCommitMessageShort();
  protected abstract String getCommitTime();
  protected abstract String getRemoteOriginUrl() throws MojoExecutionException;
  protected abstract String getTags() throws MojoExecutionException;
  protected abstract String getClosestTagName() throws MojoExecutionException;
  protected abstract String getClosestTagCommitCount() throws MojoExecutionException;
  protected abstract void finalCleanUp();

  public void loadGitData(@NotNull Properties properties) throws IOException, MojoExecutionException{
    init();
    // git.user.name
    put(properties, GitCommitIdMojo.BUILD_AUTHOR_NAME, getBuildAuthorName());
    // git.user.email
    put(properties, GitCommitIdMojo.BUILD_AUTHOR_EMAIL, getBuildAuthorEmail());

    try {
      prepareGitToExtractMoreDetailedRepoInformation();
      validateAbbrevLength(abbrevLength);

      // git.branch
      put(properties, GitCommitIdMojo.BRANCH, determineBranchName(System.getenv()));
      // git.commit.id.describe
      maybePutGitDescribe(properties);
      // git.commit.id
      put(properties, GitCommitIdMojo.COMMIT_ID, getCommitId());
      // git.commit.id.abbrev      
      put(properties, GitCommitIdMojo.COMMIT_ID_ABBREV, getAbbrevCommitId());
      // git.dirty
      put(properties, GitCommitIdMojo.DIRTY, Boolean.toString(isDirty()));
      // git.commit.author.name
      put(properties, GitCommitIdMojo.COMMIT_AUTHOR_NAME, getCommitAuthorName());
      // git.commit.author.email
      put(properties, GitCommitIdMojo.COMMIT_AUTHOR_EMAIL, getCommitAuthorEmail());
      // git.commit.message.full
      put(properties, GitCommitIdMojo.COMMIT_MESSAGE_FULL, getCommitMessageFull());
      // git.commit.message.short
      put(properties, GitCommitIdMojo.COMMIT_MESSAGE_SHORT, getCommitMessageShort());
      // git.commit.time
      put(properties, GitCommitIdMojo.COMMIT_TIME, getCommitTime());
      // git remote.origin.url
      put(properties, GitCommitIdMojo.REMOTE_ORIGIN_URL, getRemoteOriginUrl());

      //
      put(properties, GitCommitIdMojo.TAGS, getTags());
      
      put(properties,GitCommitIdMojo.CLOSEST_TAG_NAME, getClosestTagName());
      put(properties,GitCommitIdMojo.CLOSEST_TAG_COMMIT_COUNT, getClosestTagCommitCount());
    } finally {
      finalCleanUp();
    }
  }

  private void maybePutGitDescribe(@NotNull Properties properties) throws MojoExecutionException{
    boolean isGitDescribeOptOutByDefault = (gitDescribe == null);
    boolean isGitDescribeOptOutByConfiguration = (gitDescribe != null && !gitDescribe.isSkip());

    if (isGitDescribeOptOutByDefault || isGitDescribeOptOutByConfiguration) {
      put(properties, GitCommitIdMojo.COMMIT_DESCRIBE, getGitDescribe());
    }
  }

  void validateAbbrevLength(int abbrevLength) throws MojoExecutionException {
    if (abbrevLength < 2 || abbrevLength > 40) {
      throw new MojoExecutionException(String.format("Abbreviated commit id length must be between 2 and 40, inclusive! Was [%s]. ", abbrevLength) +
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
  protected String determineBranchName(Map<String, String> env) throws IOException {
    if (runningOnBuildServer(env)) {
      return determineBranchNameOnBuildServer(env);
    } else {
      return getBranchName();
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
   * Is "Jenkins aware", and prefers {@code GIT_BRANCH} to getting the branch via git if that environment variable is set.
   * The {@code GIT_BRANCH} variable is set by Jenkins/Hudson when put in detached HEAD state, but it still knows which branch was cloned.
   */
  protected String determineBranchNameOnBuildServer(Map<String, String> env) throws IOException {
    String environmentBasedBranch = env.get("GIT_BRANCH");
    if (isNullOrEmpty(environmentBasedBranch)) {
      mojo.getLog().info("Detected that running on CI environment, but using repository branch, no GIT_BRANCH detected.");
      return getBranchName();
    } else {
      mojo.getLog().info("Using environment variable based branch name. " + "GIT_BRANCH = " + environmentBasedBranch);
      return environmentBasedBranch;
    }
  }

  protected SimpleDateFormat getSimpleDateFormatWithTimeZone(){
    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
    if (dateFormatTimeZone != null){
      smf.setTimeZone(TimeZone.getTimeZone(dateFormatTimeZone));
    }
    return smf;
  }

  protected void put(@NotNull Properties properties, String key, String value) {
    String keyWithPrefix = prefixDot + key;
    mojo.getLog().info(keyWithPrefix + " " + value);
    PropertyManager.putWithoutPrefix(properties, keyWithPrefix, value);
  }
}
