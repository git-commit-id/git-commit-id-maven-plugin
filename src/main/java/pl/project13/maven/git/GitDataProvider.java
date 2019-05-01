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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.apache.http.client.utils.URIBuilder;
import pl.project13.maven.git.build.BuildServerDataProvider;
import pl.project13.maven.git.build.UnknownBuildServerData;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.util.PropertyManager;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class GitDataProvider implements GitProvider {

  @Nonnull
  protected final LoggerBridge log;

  protected String prefixDot;

  protected int abbrevLength;

  protected String dateFormat;

  protected String dateFormatTimeZone;

  protected GitDescribeConfig gitDescribe = new GitDescribeConfig();

  protected CommitIdGenerationMode commitIdGenerationMode;

  protected String evaluateOnCommit;
  
  protected boolean useBranchNameFromBuildEnvironment;

  protected List<String> excludeProperties;

  protected List<String> includeOnlyProperties;

  public GitDataProvider(@Nonnull LoggerBridge log) {
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

  public GitDataProvider setDateFormatTimeZone(String dateFormatTimeZone) {
    this.dateFormatTimeZone = dateFormatTimeZone;
    return this;
  }
  
  public GitDataProvider setUseBranchNameFromBuildEnvironment(boolean useBranchNameFromBuildEnvironment) {
    this.useBranchNameFromBuildEnvironment = useBranchNameFromBuildEnvironment;
    return this;
  }

  public GitDataProvider setExcludeProperties(List<String> excludeProperties) {
    this.excludeProperties = excludeProperties;
    return this;
  }

  public GitDataProvider setIncludeOnlyProperties(List<String> includeOnlyProperties) {
    this.includeOnlyProperties = includeOnlyProperties;
    return this;
  }

  public void loadGitData(@Nonnull String evaluateOnCommit, @Nonnull Properties properties) throws GitCommitIdExecutionException {
    this.evaluateOnCommit = evaluateOnCommit;
    init();
    // git.user.name
    maybePut(properties, GitCommitPropertyConstant.BUILD_AUTHOR_NAME, () -> getBuildAuthorName());
    // git.user.email
    maybePut(properties, GitCommitPropertyConstant.BUILD_AUTHOR_EMAIL, () -> getBuildAuthorEmail());

    try {
      prepareGitToExtractMoreDetailedRepoInformation();
      validateAbbrevLength(abbrevLength);

      // git.branch
      maybePut(properties, GitCommitPropertyConstant.BRANCH, () -> determineBranchName(System.getenv()));
      // git.commit.id.describe
      maybePutGitDescribe(properties);
      loadShortDescribe(properties);
      // git.commit.id
      switch (commitIdGenerationMode) {
        case FULL: {
          maybePut(properties, GitCommitPropertyConstant.COMMIT_ID_FULL, () -> getCommitId());
          break;
        }
        case FLAT: {
          maybePut(properties, GitCommitPropertyConstant.COMMIT_ID_FLAT, () -> getCommitId());
          break;
        }
        default: {
          throw new GitCommitIdExecutionException("Unsupported commitIdGenerationMode: " + commitIdGenerationMode);
        }
      }
      // git.commit.id.abbrev
      maybePut(properties, GitCommitPropertyConstant.COMMIT_ID_ABBREV, () -> getAbbrevCommitId());
      // git.dirty
      maybePut(properties, GitCommitPropertyConstant.DIRTY, () -> Boolean.toString(isDirty()));
      // git.commit.author.name
      maybePut(properties, GitCommitPropertyConstant.COMMIT_AUTHOR_NAME, () -> getCommitAuthorName());
      // git.commit.author.email
      maybePut(properties, GitCommitPropertyConstant.COMMIT_AUTHOR_EMAIL, () -> getCommitAuthorEmail());
      // git.commit.message.full
      maybePut(properties, GitCommitPropertyConstant.COMMIT_MESSAGE_FULL, () -> getCommitMessageFull());
      // git.commit.message.short
      maybePut(properties, GitCommitPropertyConstant.COMMIT_MESSAGE_SHORT, () -> getCommitMessageShort());
      // git.commit.time
      maybePut(properties, GitCommitPropertyConstant.COMMIT_TIME, () -> getCommitTime());
      // git remote.origin.url
      maybePut(properties, GitCommitPropertyConstant.REMOTE_ORIGIN_URL, () -> getRemoteOriginUrl());

      //
      maybePut(properties, GitCommitPropertyConstant.TAGS, () -> getTags());

      maybePut(properties,GitCommitPropertyConstant.CLOSEST_TAG_NAME, () -> getClosestTagName());
      maybePut(properties,GitCommitPropertyConstant.CLOSEST_TAG_COMMIT_COUNT, () -> getClosestTagCommitCount());

      maybePut(properties,GitCommitPropertyConstant.TOTAL_COMMIT_COUNT, () -> getTotalCommitCount());

      SupplierEx<AheadBehind> aheadBehindSupplier = memoize(() -> getAheadBehind());
      maybePut(properties, GitCommitPropertyConstant.LOCAL_BRANCH_AHEAD, () -> aheadBehindSupplier.get().ahead());
      maybePut(properties, GitCommitPropertyConstant.LOCAL_BRANCH_BEHIND, () -> aheadBehindSupplier.get().behind());
    } finally {
      finalCleanUp();
    }
  }

  private void maybePutGitDescribe(@Nonnull Properties properties) throws GitCommitIdExecutionException {
    boolean isGitDescribeOptOutByDefault = (gitDescribe == null);
    boolean isGitDescribeOptOutByConfiguration = (gitDescribe != null && !gitDescribe.isSkip());

    if (isGitDescribeOptOutByDefault || isGitDescribeOptOutByConfiguration) {
      maybePut(properties, GitCommitPropertyConstant.COMMIT_DESCRIBE, () -> getGitDescribe());
    }
  }

  protected void loadShortDescribe(@Nonnull Properties properties) throws GitCommitIdExecutionException {
    //removes git hash part from describe
    String commitDescribe = properties.getProperty(prefixDot + GitCommitPropertyConstant.COMMIT_DESCRIBE);

    if (commitDescribe != null) {
      int startPos = commitDescribe.indexOf("-g");
      if (startPos > 0) {
        String commitShortDescribe;
        int endPos = commitDescribe.indexOf('-', startPos + 1);
        if (endPos < 0) {
          commitShortDescribe = commitDescribe.substring(0, startPos);
        } else {
          commitShortDescribe = commitDescribe.substring(0, startPos) + commitDescribe.substring(endPos);
        }
        maybePut(properties, GitCommitPropertyConstant.COMMIT_SHORT_DESCRIBE, () -> commitShortDescribe);
      } else {
        maybePut(properties, GitCommitPropertyConstant.COMMIT_SHORT_DESCRIBE, () -> commitDescribe);
      }
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
  protected String determineBranchName(@Nonnull Map<String, String> env) throws GitCommitIdExecutionException {
    BuildServerDataProvider buildServerDataProvider = BuildServerDataProvider.getBuildServerProvider(env,log);
    if (useBranchNameFromBuildEnvironment && !(buildServerDataProvider instanceof UnknownBuildServerData)) {
      String branchName = buildServerDataProvider.getBuildBranch();
      if (isNullOrEmpty(branchName)) {
        log.info("Detected that running on CI environment, but using repository branch, no GIT_BRANCH detected.");
        return getBranchName();
      }
      return branchName;
    } else {
      return getBranchName();
    }
  }

  protected SimpleDateFormat getSimpleDateFormatWithTimeZone() {
    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
    if (dateFormatTimeZone != null) {
      smf.setTimeZone(TimeZone.getTimeZone(dateFormatTimeZone));
    }
    return smf;
  }

  protected void maybePut(@Nonnull Properties properties, String key, SupplierEx<String> value)
          throws GitCommitIdExecutionException {
    String keyWithPrefix = prefixDot + key;
    if (properties.contains(keyWithPrefix)) {
      String propertyValue = properties.getProperty(keyWithPrefix);
      log.info("Using cached {} with value {}", keyWithPrefix, propertyValue);
    } else if (PropertiesFilterer.isIncluded(keyWithPrefix, includeOnlyProperties, excludeProperties)) {
      String propertyValue = value.get();
      log.info("Collected {} with value {}", keyWithPrefix, propertyValue);
      PropertyManager.putWithoutPrefix(properties, keyWithPrefix, propertyValue);
    }
  }

  @FunctionalInterface
  public interface SupplierEx<T> {
    @CanIgnoreReturnValue
    T get() throws GitCommitIdExecutionException;
  }

  public static <T> SupplierEx<T> memoize(SupplierEx<T> delegate) {
    final AtomicBoolean retrived = new AtomicBoolean(false);
    final AtomicReference<T> value = new AtomicReference<>();
    return () -> {
      if (!retrived.get()) {
        value.set(delegate.get());
        retrived.set(true);
      }
      return value.get();
    };
  }

  /**
   * Regex to check for SCP-style SSH+GIT connection strings such as 'git@github.com'
   */
  static final Pattern GIT_SCP_FORMAT = Pattern.compile("^([a-zA-Z0-9_.+-])+@(.*)|^\\[([^\\]])+\\]:(.*)|^file:/{2,3}(.*)");
  /**
   * If the git remote value is a URI and contains a user info component, strip the password from it if it exists.
   *
   * @param gitRemoteString The value of the git remote
   * @return returns the gitRemoteUri with stripped password (might be used in http or https)
   * @throws GitCommitIdExecutionException Exception when URI is invalid
   */

  protected String stripCredentialsFromOriginUrl(String gitRemoteString) throws GitCommitIdExecutionException {

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
      log.error("Something went wrong to strip the credentials from git's remote url (please report this)!", e);
      return "";
    }
  }
}
