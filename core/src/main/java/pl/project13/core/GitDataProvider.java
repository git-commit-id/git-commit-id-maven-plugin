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

package pl.project13.core;

import pl.project13.core.git.GitDescribeConfig;
import pl.project13.core.cibuild.BuildServerDataProvider;
import pl.project13.core.cibuild.UnknownBuildServerData;
import pl.project13.core.log.LoggerBridge;
import pl.project13.core.util.PropertyManager;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * An abstract Interface that dictates what functionality a {@code GitProvider} needs to fullfill.
 * As of now this project implements two variants of a {@code GitProvider}.
 * Namely {@link NativeGitProvider} which uses the native git binaries and
 * {@link JGitProvider} which uses a java git implementation ({@code jgit}).
 */
public abstract class GitDataProvider implements GitProvider {

  /**
   * Logging provider which will be used to log events.
   */
  @Nonnull
  protected final LoggerBridge log;

  /**
   * The {@code prefix} used for all generated properties.
   */
  protected String prefixDot;

  /**
   * The {@code commit abbreviation length}
   */
  protected int abbrevLength;

  /**
   * The {@code date format} for all generated properties.
   */
  protected String dateFormat;

  /**
   * The {@code date format time zone} for all generated properties.
   */
  protected String dateFormatTimeZone;

  /**
   * The {@link GitDescribeConfig}.
   */
  protected GitDescribeConfig gitDescribe = new GitDescribeConfig();

  /**
   * The {@code git commit id generation mode}.
   */
  protected CommitIdGenerationMode commitIdGenerationMode;

  /**
   * Allow to tell the plugin what commit should be used as reference to generate the
   * properties from. Defaults to {@code HEAD}.
   */
  protected String evaluateOnCommit;

  /**
   * When set to {@code true} this plugin will try to use the branch name from build environment.
   * Set to {@code false} to use JGit/GIT to get current branch name which can be useful
   * when using the JGitflow maven plugin.
   * See https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/24
   */
  protected boolean useBranchNameFromBuildEnvironment;

  /**
   * Can be used to exclude certain properties from being emitted (e.g. filter out properties
   * that you *don't* want to expose). May be useful when you want to hide
   * {@code git.remote.origin.url} (maybe because it contains your repo password?),
   * or the email of the committer etc.
   */
  protected List<String> excludeProperties;

  /**
   * Can be used to include only certain properties into the resulting file (e.g. include only
   * properties that you *want* to expose). This feature was implemented to avoid big exclude
   * properties tag when we only want very few specific properties.
   * The inclusion rules, will be overruled by the exclude rules (e.g. you can write an
   * inclusion rule that applies for multiple properties and then exclude a subset of them).
   */
  protected List<String> includeOnlyProperties;

  /**
   * When set to {@code true}, the plugin will not try to contact any remote repositories.
   * Any operations will only use the local state of the repo.
   * If set to {@code false}, it will execute {@code git fetch} operations e.g. to determine the
   * {@code ahead} and {@code behind} branch information.
   */
  protected boolean offline;

  /**
   * Constructor to encapsulates all references required to dertermine all git-data.
   * @param log logging provider which will be used to log events
   */
  public GitDataProvider(@Nonnull LoggerBridge log) {
    this.log = log;
  }

  /**
   * Sets the {@link GitDescribeConfig} for the {@code GitProvider}
   * @param gitDescribe The {@link GitDescribeConfig}
   * @return The {@code GitProvider} with the corresponding {@link GitDescribeConfig}.
   */
  public GitDataProvider setGitDescribe(GitDescribeConfig gitDescribe) {
    this.gitDescribe = gitDescribe;
    return this;
  }

  /**
   * Sets the {@code prefix} used for all generated properties for the {@code GitProvider}
   * @param prefixDot The {@code prefix} used for all generated properties
   * @return The {@code GitProvider} with the corresponding {@code prefix}.
   */
  public GitDataProvider setPrefixDot(String prefixDot) {
    this.prefixDot = prefixDot;
    return this;
  }

  /**
   * Sets the {@code commit abbreviation length} for the {@code GitProvider}
   * @param abbrevLength The {@code commit abbreviation length}
   * @return The {@code GitProvider} with the corresponding {@code commit abbreviation length}.
   */
  public GitDataProvider setAbbrevLength(int abbrevLength) {
    this.abbrevLength = abbrevLength;
    return this;
  }

  /**
   * Sets the {@code date format} for all generated properties for the {@code GitProvider}
   * @param dateFormat The {@code date format} for all generated properties.
   * @return The {@code GitProvider} with the corresponding {@code date format}.
   */
  public GitDataProvider setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
    return this;
  }

  /**
   * Sets the {@code git commit id generation mode} for the {@code GitProvider}
   * @param commitIdGenerationMode The {@code git commit id generation mode}
   * @return The {@code GitProvider} with the corresponding {@code git commit id generation mode}.
   */
  public GitDataProvider setCommitIdGenerationMode(CommitIdGenerationMode commitIdGenerationMode) {
    this.commitIdGenerationMode = commitIdGenerationMode;
    return this;
  }

  /**
   * Sets the {@code date format time zone} for all generated properties for the {@code GitProvider}
   * @param dateFormatTimeZone The {@code date format time zone} for all generated properties.
   * @return The {@code GitProvider} with the corresponding {@code date format time zone}.
   */
  public GitDataProvider setDateFormatTimeZone(String dateFormatTimeZone) {
    this.dateFormatTimeZone = dateFormatTimeZone;
    return this;
  }

  /**
   * Sets the Indicator if the branch name should be obtained from the build environment, or by the plugin.
   * @param useBranchNameFromBuildEnvironment When set to {@code true} this plugin will try to use the branch name
   *                                          from build environment. Set to {@code false} to use JGit/GIT to get
   *                                          current branch name which can be useful when using the JGitflow
   *                                          maven plugin.
   * See https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/24
   *
   * @return The {@code GitProvider} with the corresponding Indicator set.
   */
  public GitDataProvider setUseBranchNameFromBuildEnvironment(boolean useBranchNameFromBuildEnvironment) {
    this.useBranchNameFromBuildEnvironment = useBranchNameFromBuildEnvironment;
    return this;
  }

  /**
   * Can be used to exclude certain properties from being emitted (e.g. filter out properties
   * that you *don't* want to expose). May be useful when you want to hide
   * {@code git.remote.origin.url} (maybe because it contains your repo password?),
   * or the email of the committer etc.
   *
   * @param excludeProperties The properties that shall be excluded from generation.
   * @return The {@code GitProvider} with the corresponding {@code properties exclusion list}.
   */
  public GitDataProvider setExcludeProperties(List<String> excludeProperties) {
    this.excludeProperties = excludeProperties;
    return this;
  }

  /**
   * Can be used to include only certain properties into the resulting file (e.g. include only
   * properties that you *want* to expose). This feature was implemented to avoid big exclude
   * properties tag when we only want very few specific properties.
   * The inclusion rules, will be overruled by the exclude rules (e.g. you can write an
   * inclusion rule that applies for multiple properties and then exclude a subset of them).
   *
   * @param includeOnlyProperties The properties that shall only be included in the generation.
   * @return The {@code GitProvider} with the corresponding {@code properties inclusion only list}.
   */
  public GitDataProvider setIncludeOnlyProperties(List<String> includeOnlyProperties) {
    this.includeOnlyProperties = includeOnlyProperties;
    return this;
  }

  /**
   * When set to {@code true}, the plugin will not try to contact any remote repositories.
   * Any operations will only use the local state of the repo.
   * If set to {@code false}, it will execute {@code git fetch} operations e.g. to determine the
   * {@code ahead} and {@code behind} branch information.
   *
   * @param offline Indicator if the plugin should operate in {@code offline} or {@code online}-mode.
   * @return The {@code GitProvider} with the corresponding Indicator set.
   */
  public GitDataProvider setOffline(boolean offline) {
    this.offline = offline;
    return this;
  }

  /**
   * Main function that will attempt to load the desired properties from the git repository.
   *
   * @param evaluateOnCommit The commit that should be used as reference to generate the properties from.
   *                         Defaults to {@code HEAD}.
   * @param properties The Properties-Set that should be enriched by the generated one.
   * @throws GitCommitIdExecutionException In case any problem occurred during loading of the properties from the git repository.
   */
  public void loadGitData(@Nonnull String evaluateOnCommit, @Nonnull Properties properties) throws GitCommitIdExecutionException {
    this.evaluateOnCommit = evaluateOnCommit;
    init();
    // git.user.name
    maybePut(properties, GitCommitPropertyConstant.BUILD_AUTHOR_NAME, this::getBuildAuthorName);
    // git.user.email
    maybePut(properties, GitCommitPropertyConstant.BUILD_AUTHOR_EMAIL, this::getBuildAuthorEmail);

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
          maybePut(properties, GitCommitPropertyConstant.COMMIT_ID_FULL, this::getCommitId);
          break;
        }
        case FLAT: {
          maybePut(properties, GitCommitPropertyConstant.COMMIT_ID_FLAT, this::getCommitId);
          break;
        }
        default: {
          throw new GitCommitIdExecutionException("Unsupported commitIdGenerationMode: " + commitIdGenerationMode);
        }
      }
      // git.commit.id.abbrev
      maybePut(properties, GitCommitPropertyConstant.COMMIT_ID_ABBREV, this::getAbbrevCommitId);
      // git.dirty
      maybePut(properties, GitCommitPropertyConstant.DIRTY, () -> Boolean.toString(isDirty()));
      // git.commit.author.name
      maybePut(properties, GitCommitPropertyConstant.COMMIT_AUTHOR_NAME, this::getCommitAuthorName);
      // git.commit.author.email
      maybePut(properties, GitCommitPropertyConstant.COMMIT_AUTHOR_EMAIL, this::getCommitAuthorEmail);
      // git.commit.message.full
      maybePut(properties, GitCommitPropertyConstant.COMMIT_MESSAGE_FULL, this::getCommitMessageFull);
      // git.commit.message.short
      maybePut(properties, GitCommitPropertyConstant.COMMIT_MESSAGE_SHORT, this::getCommitMessageShort);
      // git.commit.time
      maybePut(properties, GitCommitPropertyConstant.COMMIT_TIME, this::getCommitTime);
      // commit.author.time
      maybePut(properties, GitCommitPropertyConstant.COMMIT_AUTHOR_TIME, this::getCommitAuthorTime);
      // commit.committer.time
      maybePut(properties, GitCommitPropertyConstant.COMMIT_COMMITTER_TIME, this::getCommitAuthorTime);
      // git remote.origin.url
      maybePut(properties, GitCommitPropertyConstant.REMOTE_ORIGIN_URL, this::getRemoteOriginUrl);

      //
      maybePut(properties, GitCommitPropertyConstant.TAGS, this::getTags);

      maybePut(properties,GitCommitPropertyConstant.CLOSEST_TAG_NAME, this::getClosestTagName);
      maybePut(properties,GitCommitPropertyConstant.CLOSEST_TAG_COMMIT_COUNT, this::getClosestTagCommitCount);

      maybePut(properties,GitCommitPropertyConstant.TOTAL_COMMIT_COUNT, this::getTotalCommitCount);

      SupplierEx<AheadBehind> aheadBehindSupplier = memoize(this::getAheadBehind);
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
      maybePut(properties, GitCommitPropertyConstant.COMMIT_DESCRIBE, this::getGitDescribe);
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
   * @throws GitCommitIdExecutionException the branch name could not be determined
   */
  protected String determineBranchName(@Nonnull Map<String, String> env) throws GitCommitIdExecutionException {
    BuildServerDataProvider buildServerDataProvider = BuildServerDataProvider.getBuildServerProvider(env,log);
    if (useBranchNameFromBuildEnvironment && !(buildServerDataProvider instanceof UnknownBuildServerData)) {
      String branchName = buildServerDataProvider.getBuildBranch();
      if (branchName == null || branchName.isEmpty()) {
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
    if (properties.stringPropertyNames().contains(keyWithPrefix)) {
      String propertyValue = properties.getProperty(keyWithPrefix);
      log.info("Using cached {} with value {}", keyWithPrefix, propertyValue);
    } else if (PropertiesFilterer.isIncluded(keyWithPrefix, includeOnlyProperties, excludeProperties)) {
      String propertyValue = value.get();
      log.info("Collected {} with value {}", keyWithPrefix, propertyValue);
      PropertyManager.putWithoutPrefix(properties, keyWithPrefix, propertyValue);
    }
  }

  /**
   * Represents a supplier of results that is allowed to throw a {@link GitCommitIdExecutionException}.
   * Similar concept as {@link java.util.function.Supplier}.
   *
   * @param <T> the type of results supplied by this supplier
   */
  @FunctionalInterface
  public interface SupplierEx<T> {
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
   * Note that this method will return an empty string if any failure occurred, while stripping the password from the
   * credentials. This merely serves as save-guard to avoid any potential password exposure inside generated properties.
   *
   * This method further operates on the assumption that a valid URL schema follows the rules outlined in
   * <a href=https://www.ietf.org/rfc/rfc2396.txt>RFC-2396</a> in section "3.2.2. Server-based Naming Authority"
   * which declares the following as valid URL schema:
   *  <pre>
   *  &lt;userinfo&gt;@&lt;host&gt;:&lt;port&gt;
   *  </pre>
   *  The "userinfo" part is declared in the same section allowing the following pattern:
   *  <pre>
   *    userinfo = *( unreserved | escaped | ";" | ":" | "&amp;" | "=" | "+" | "$" | "," )
   *  </pre>
   *  The "unreserved" part is declared in section "2.3. Unreserved Characters" as the following:
   *  <pre>
   *    unreserved  = alphanum | mark
   *    mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
   *
   *    alphanum = alpha | digit
   *    digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |  "8" | "9"
   *    alpha = lowalpha | upalpha
   *    lowalpha = "a" | "b" | "c" | ... | "x" | "y" | "z"
   *    upalpha = "A" | "B" | "C" | ... | "X" | "Y" | "Z"
   *  </pre>
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

      for (String s: Arrays.asList(
              // escape all 'delims' characters in a URI as per https://www.ietf.org/rfc/rfc2396.txt
              "<", ">", "#", "%", "\"",
              // escape all 'unwise' characters in a URI as per https://www.ietf.org/rfc/rfc2396.txt
              "{", "}", "|", "\\", "^", "[", "]", "`")) {
        gitRemoteString = gitRemoteString.replaceAll(
                Pattern.quote(s), URLEncoder.encode(s, StandardCharsets.UTF_8.toString()));
      }
      URI original = new URI(gitRemoteString);
      String userInfoString = original.getUserInfo();
      if (null == userInfoString) {
        return gitRemoteString;
      }

      String[] userInfo = userInfoString.split(":");
      // Build a new URL from the original URL, but nulling out the password
      // component of the userinfo. We keep the username so that ssh uris such
      // ssh://git@github.com will retain 'git@'.
      String extractedUserInfo = null;
      if (userInfo.length > 0) {
        extractedUserInfo = userInfo[0];
        if (extractedUserInfo.isEmpty()) {
          extractedUserInfo = null;
        }
      }
      return new URI(original.getScheme(),
              extractedUserInfo,
              original.getHost(),
              original.getPort(),
              original.getPath(),
              original.getQuery(),
              original.getFragment()).toString();

    } catch (Exception e) {
      log.error("Something went wrong to strip the credentials from git's remote url (please report this)!", e);
      return "";
    }
  }
}