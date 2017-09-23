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

import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.util.PropertyManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class GitDataProvider implements GitProvider {

  @NotNull
  protected final LoggerBridge log;

  protected String prefixDot;

  protected int abbrevLength;

  protected String dateFormat;

  protected String dateFormatTimeZone;

  protected GitDescribeConfig gitDescribe = new GitDescribeConfig();

  protected CommitIdGenerationMode commitIdGenerationMode;

  public GitDataProvider(@NotNull LoggerBridge log) {
    this.log = log;
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

  public GitDataProvider setCommitIdGenerationMode(CommitIdGenerationMode commitIdGenerationMode) {
    this.commitIdGenerationMode = commitIdGenerationMode;
    return this;
  }

  public GitDataProvider setDateFormatTimeZone(String dateFormatTimeZone){
    this.dateFormatTimeZone = dateFormatTimeZone;
    return this;
  }

  public void loadGitData(@NotNull Properties properties) throws GitCommitIdExecutionException {
    init();
    // git.user.name
    put(properties, GitCommitPropertyConstant.BUILD_AUTHOR_NAME, getBuildAuthorName());
    // git.user.email
    put(properties, GitCommitPropertyConstant.BUILD_AUTHOR_EMAIL, getBuildAuthorEmail());

    try {
      prepareGitToExtractMoreDetailedRepoInformation();
      validateAbbrevLength(abbrevLength);

      // git.branch
      put(properties, GitCommitPropertyConstant.BRANCH, determineBranchName(System.getenv()));
      // git.commit.id.describe
      maybePutGitDescribe(properties);
      // git.commit.id
      switch (commitIdGenerationMode) {
        case FULL: {
          put(properties, GitCommitPropertyConstant.COMMIT_ID_FULL, getCommitId());
          break;
        }
        case FLAT: {
          put(properties, GitCommitPropertyConstant.COMMIT_ID_FLAT, getCommitId());
          break;
        }
        default: {
          throw new GitCommitIdExecutionException("Unsupported commitIdGenerationMode: " + commitIdGenerationMode);
        }
      }
      // git.commit.id.abbrev
      put(properties, GitCommitPropertyConstant.COMMIT_ID_ABBREV, getAbbrevCommitId());
      // git.dirty
      put(properties, GitCommitPropertyConstant.DIRTY, Boolean.toString(isDirty()));
      // git.commit.author.name
      put(properties, GitCommitPropertyConstant.COMMIT_AUTHOR_NAME, getCommitAuthorName());
      // git.commit.author.email
      put(properties, GitCommitPropertyConstant.COMMIT_AUTHOR_EMAIL, getCommitAuthorEmail());
      // git.commit.message.full
      put(properties, GitCommitPropertyConstant.COMMIT_MESSAGE_FULL, getCommitMessageFull());
      // git.commit.message.short
      put(properties, GitCommitPropertyConstant.COMMIT_MESSAGE_SHORT, getCommitMessageShort());
      // git.commit.time
      put(properties, GitCommitPropertyConstant.COMMIT_TIME, getCommitTime());
      // git remote.origin.url
      put(properties, GitCommitPropertyConstant.REMOTE_ORIGIN_URL, getRemoteOriginUrl());

      //
      put(properties, GitCommitPropertyConstant.TAGS, getTags());
      
      put(properties,GitCommitPropertyConstant.CLOSEST_TAG_NAME, getClosestTagName());
      put(properties,GitCommitPropertyConstant.CLOSEST_TAG_COMMIT_COUNT, getClosestTagCommitCount());
    } finally {
      finalCleanUp();
    }
  }

  private void maybePutGitDescribe(@NotNull Properties properties) throws GitCommitIdExecutionException{
    boolean isGitDescribeOptOutByDefault = (gitDescribe == null);
    boolean isGitDescribeOptOutByConfiguration = (gitDescribe != null && !gitDescribe.isSkip());

    if (isGitDescribeOptOutByDefault || isGitDescribeOptOutByConfiguration) {
      put(properties, GitCommitPropertyConstant.COMMIT_DESCRIBE, getGitDescribe());
    }
  }

  void validateAbbrevLength(int abbrevLength) throws GitCommitIdExecutionException {
    if (abbrevLength < 2 || abbrevLength > 40) {
      throw new GitCommitIdExecutionException(String.format("Abbreviated commit id length must be between 2 and 40, inclusive! Was [%s]. ", abbrevLength) +
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
  protected String determineBranchName(Map<String, String> env) throws GitCommitIdExecutionException {
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
    return env.containsKey("HUDSON_URL") || env.containsKey("JENKINS_URL") ||
           env.containsKey("HUDSON_HOME") || env.containsKey("JENKINS_HOME");
  }

  /**
   * Is "Jenkins aware", and prefers {@code GIT_LOCAL_BRANCH} over {@code GIT_BRANCH} to getting the branch via git if that environment variables are set.
   * The {@code GIT_LOCAL_BRANCH} and {@code GIT_BRANCH} variables are set by Jenkins/Hudson when put in detached HEAD state, but it still knows which branch was cloned.
   */
  protected String determineBranchNameOnBuildServer(Map<String, String> env) throws GitCommitIdExecutionException {
    String environmentBasedLocalBranch = env.get("GIT_LOCAL_BRANCH");
    if(!isNullOrEmpty(environmentBasedLocalBranch)){
      log.info("Using environment variable based branch name. GIT_LOCAL_BRANCH = {}", environmentBasedLocalBranch);
      return environmentBasedLocalBranch;
    }

    String environmentBasedBranch = env.get("GIT_BRANCH");
    if (!isNullOrEmpty(environmentBasedBranch)) {
      log.info("Using environment variable based branch name. GIT_BRANCH = {}", environmentBasedBranch);
      return environmentBasedBranch;
    }

    log.info("Detected that running on CI environment, but using repository branch, no GIT_BRANCH detected.");
    return getBranchName();
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
    log.info("{} {}", keyWithPrefix, value);
    PropertyManager.putWithoutPrefix(properties, keyWithPrefix, value);
  }

  /**
   * Regex to check for SCP-style SSH+GIT connection strings such as 'git@github.com'
   */
  static final Pattern GIT_SCP_FORMAT = Pattern.compile("^([a-zA-Z0-9_.+-])+@(.*)|^\\[([^\\]])+\\]:(.*)|^file:///(.*)");
  /**
   * If the git remote value is a URI and contains a user info component, strip the password from it if it exists.
   *
   * @param gitRemoteString The value of the git remote
   * @return
   * @throws GitCommitIdExecutionException
     */
  protected static String stripCredentialsFromOriginUrl(String gitRemoteString) throws GitCommitIdExecutionException {

    // The URL might be null if the repo hasn't set a remote
    if (gitRemoteString == null) {
      return gitRemoteString;
    }

    // Remotes using ssh connection strings in the 'git@github' format aren't
    // proper URIs and won't parse . Plus since you should be using SSH keys,
    // credentials like are not in the URL.
    if (GIT_SCP_FORMAT.matcher(gitRemoteString).matches()) {
      return gitRemoteString;
    }
    // At this point, we should have a properly formatted URL
    try {
      URI original = new URI(gitRemoteString);
      String userInfoString = original.getUserInfo();
      if (null == userInfoString) {
        return gitRemoteString;
      }
      URIBuilder b = new URIBuilder(gitRemoteString);
      String[] userInfo = userInfoString.split(":");
      // Build a new URL from the original URL, but nulling out the password
      // component of the userinfo. We keep the username so that ssh uris such
      // ssh://git@github.com will retain 'git@'.
      b.setUserInfo(userInfo[0]);
      return b.build().toString();

    } catch (URISyntaxException e) {
      throw new GitCommitIdExecutionException(e);
    }
  }
}
