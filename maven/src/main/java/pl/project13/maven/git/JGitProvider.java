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

import com.google.common.annotations.VisibleForTesting;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import pl.project13.core.AheadBehind;
import pl.project13.core.GitCommitIdExecutionException;
import pl.project13.core.jgit.DescribeResult;
import pl.project13.core.jgit.JGitCommon;
import pl.project13.core.jgit.DescribeCommand;
import pl.project13.core.log.LoggerBridge;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.jgit.storage.file.WindowCacheConfig;

import javax.annotation.Nonnull;

public class JGitProvider extends GitDataProvider {

  private File dotGitDirectory;
  private Repository git;
  private ObjectReader objectReader;
  private RevWalk revWalk;
  private RevCommit evalCommit;
  private JGitCommon jGitCommon;

  @Nonnull
  public static JGitProvider on(@Nonnull File dotGitDirectory, @Nonnull LoggerBridge log) {
    return new JGitProvider(dotGitDirectory, log);
  }

  JGitProvider(@Nonnull File dotGitDirectory, @Nonnull LoggerBridge log) {
    super(log);
    this.dotGitDirectory = dotGitDirectory;
    this.jGitCommon = new JGitCommon(log);
  }

  @Override
  public void init() throws GitCommitIdExecutionException {
    git = getGitRepository();
    objectReader = git.newObjectReader();
  }

  @Override
  public String getBuildAuthorName() throws GitCommitIdExecutionException {
    String userName = git.getConfig().getString("user", null, "name");
    return Optional.ofNullable(userName).orElse("");
  }

  @Override
  public String getBuildAuthorEmail() throws GitCommitIdExecutionException {
    String userEmail = git.getConfig().getString("user", null, "email");
    return Optional.ofNullable(userEmail).orElse("");
  }

  @Override
  public void prepareGitToExtractMoreDetailedRepoInformation() throws GitCommitIdExecutionException {
    try {
      // more details parsed out bellow
      Ref evaluateOnCommitReference = git.findRef(evaluateOnCommit);
      ObjectId evaluateOnCommitResolvedObjectId = git.resolve(evaluateOnCommit);

      if ((evaluateOnCommitReference == null) && (evaluateOnCommitResolvedObjectId == null)) {
        throw new GitCommitIdExecutionException("Could not get " + evaluateOnCommit + " Ref, are you sure you have set the dotGitDirectory property of this plugin to a valid path?");
      }
      revWalk = new RevWalk(git);
      ObjectId headObjectId;
      if (evaluateOnCommitReference != null) {
        headObjectId = evaluateOnCommitReference.getObjectId();
      } else {
        headObjectId = evaluateOnCommitResolvedObjectId;
      }

      if (headObjectId == null) {
        throw new GitCommitIdExecutionException("Could not get " + evaluateOnCommit + " Ref, are you sure you have some commits in the dotGitDirectory?");
      }
      evalCommit = revWalk.parseCommit(headObjectId);
      revWalk.markStart(evalCommit);
    } catch (Exception e) {
      throw new GitCommitIdExecutionException("Error", e);
    }
  }

  @Override
  public String getBranchName() throws GitCommitIdExecutionException {
    try {
      if (evalCommitIsNotHead()) {
        return getBranchForCommitish();
      } else {
        return getBranchForHead();
      }
    } catch (IOException e) {
      throw new GitCommitIdExecutionException(e);
    }
  }

  private String getBranchForHead() throws IOException {
    return git.getBranch();
  }

  private String getBranchForCommitish() throws GitCommitIdExecutionException {
    try {
      String commitId = getCommitId();

      boolean evaluateOnCommitPointsToTag = git.getRefDatabase().getRefsByPrefix(Constants.R_TAGS)
              .stream()
              .anyMatch(ref -> Repository.shortenRefName(ref.getName()).equalsIgnoreCase(evaluateOnCommit));

      if (evaluateOnCommitPointsToTag) {
        // 'git branch --points-at' only works for <sha-objects> and <branch> names
        // if the provided evaluateOnCommit points to a tag 'git branch --points-at' returns the commit-id instead
        return commitId;
      }

      List<String> branchesForCommit = git.getRefDatabase().getRefsByPrefix(Constants.R_HEADS)
              .stream()
              .filter(ref -> commitId.equals(ref.getObjectId().name()))
              .map(ref -> Repository.shortenRefName(ref.getName()))
              .distinct()
              .sorted()
              .collect(Collectors.toList());

      String branch = branchesForCommit.stream()
              .collect(Collectors.joining(","));

      if (branch != null && !branch.isEmpty()) {
        return branch;
      } else {
        return commitId;
      }
    } catch (IOException e) {
      throw new GitCommitIdExecutionException(e);
    }
  }

  private boolean evalCommitIsNotHead() {
    return (evaluateOnCommit != null) && !evaluateOnCommit.equals("HEAD");
  }

  @Override
  public String getGitDescribe() throws GitCommitIdExecutionException {
    return getGitDescribe(git);
  }

  @Override
  public String getCommitId() throws GitCommitIdExecutionException {
    return evalCommit.getName();
  }

  @Override
  public String getAbbrevCommitId() throws GitCommitIdExecutionException {
    return getAbbrevCommitId(objectReader, evalCommit, abbrevLength);
  }

  @Override
  public boolean isDirty() throws GitCommitIdExecutionException {
    try {
      return JGitCommon.isRepositoryInDirtyState(git);
    } catch (GitAPIException e) {
      throw new GitCommitIdExecutionException("Failed to get git status: " + e.getMessage(), e);
    }
  }

  @Override
  public String getCommitAuthorName() throws GitCommitIdExecutionException {
    return evalCommit.getAuthorIdent().getName();
  }

  @Override
  public String getCommitAuthorEmail() throws GitCommitIdExecutionException {
    return evalCommit.getAuthorIdent().getEmailAddress();
  }

  @Override
  public String getCommitMessageFull() throws GitCommitIdExecutionException {
    return evalCommit.getFullMessage().trim();
  }

  @Override
  public String getCommitMessageShort() throws GitCommitIdExecutionException {
    return evalCommit.getShortMessage().trim();
  }

  @Override
  public String getCommitTime() throws GitCommitIdExecutionException {
    long timeSinceEpoch = evalCommit.getCommitTime();
    Date commitDate = new Date(timeSinceEpoch * 1000); // git is "by sec" and java is "by ms"
    SimpleDateFormat smf = getSimpleDateFormatWithTimeZone();
    return smf.format(commitDate);
  }

  @Override
  public String getRemoteOriginUrl() throws GitCommitIdExecutionException {
    String url = git.getConfig().getString("remote", "origin", "url");
    return stripCredentialsFromOriginUrl(url);
  }

  @Override
  public String getTags() throws GitCommitIdExecutionException {
    try {
      Repository repo = getGitRepository();
      ObjectId headId = evalCommit.toObjectId();
      Collection<String> tags = jGitCommon.getTags(repo, headId);
      return String.join(",", tags);
    } catch (GitAPIException e) {
      log.error("Unable to extract tags from commit: {} ({})", evalCommit.getName(), e.getClass().getName());
      return "";
    }
  }

  @Override
  public String getClosestTagName() throws GitCommitIdExecutionException {
    Repository repo = getGitRepository();
    try {
      return jGitCommon.getClosestTagName(evaluateOnCommit, repo, gitDescribe);
    } catch (Throwable t) {
      // could not find any tags to describe
    }
    return "";
  }

  @Override
  public String getClosestTagCommitCount() throws GitCommitIdExecutionException {
    Repository repo = getGitRepository();
    try {
      return jGitCommon.getClosestTagCommitCount(evaluateOnCommit, repo, gitDescribe);
    } catch (Throwable t) {
      // could not find any tags to describe
    }
    return "";
  }

  @Override
  public String getTotalCommitCount() throws GitCommitIdExecutionException {
    try {
      return String.valueOf(RevWalkUtils.count(revWalk, evalCommit, null));
    } catch (Throwable t) {
      // could not find any tags to describe
    }
    return "";
  }

  @Override
  public void finalCleanUp() {
    if (revWalk != null) {
      revWalk.dispose();
    }
    // http://www.programcreek.com/java-api-examples/index.php?api=org.eclipse.jgit.storage.file.WindowCacheConfig
    // Example 3
    if (git != null) {
      git.close();
      // git.close() is not enough with jGit on Windows
      // remove the references from packFile by initializing cache used in the repository
      // fixing lock issues on Windows when repository has pack files
      WindowCacheConfig config = new WindowCacheConfig();
      config.install();
    }
  }

  @VisibleForTesting String getGitDescribe(@Nonnull Repository repository) throws GitCommitIdExecutionException {
    try {
      DescribeResult describeResult = DescribeCommand
          .on(evaluateOnCommit, repository, log)
          .apply(super.gitDescribe)
          .call();

      return describeResult.toString();
    } catch (GitAPIException ex) {
      ex.printStackTrace();
      throw new GitCommitIdExecutionException("Unable to obtain git.commit.id.describe information", ex);
    }
  }

  private String getAbbrevCommitId(ObjectReader objectReader, RevCommit headCommit, int abbrevLength) throws GitCommitIdExecutionException {
    try {
      AbbreviatedObjectId abbreviatedObjectId = objectReader.abbreviate(headCommit, abbrevLength);
      return abbreviatedObjectId.name();
    } catch (IOException e) {
      throw new GitCommitIdExecutionException("Unable to abbreviate commit id! " +
                                         "You may want to investigate the <abbrevLength/> element in your configuration.", e);
    }
  }

  @Nonnull
  private Repository getGitRepository() throws GitCommitIdExecutionException {
    Repository repository;

    FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
    try {
      repository = repositoryBuilder
        .setGitDir(dotGitDirectory)
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir() // scan up the file system tree
        .build();
    } catch (IOException e) {
      throw new GitCommitIdExecutionException("Could not initialize repository...", e);
    }

    if (repository == null) {
      throw new GitCommitIdExecutionException("Could not create git repository. Are you sure '" + dotGitDirectory + "' is the valid Git root for your project?");
    }

    return repository;
  }
  
  @Override
  public AheadBehind getAheadBehind() throws GitCommitIdExecutionException {
    try {
      if (!offline) {
        fetch();
      }
      Optional<BranchTrackingStatus> branchTrackingStatus = Optional.ofNullable(BranchTrackingStatus.of(git, getBranchName()));
      return branchTrackingStatus.map(bts -> AheadBehind.of(bts.getAheadCount(), bts.getBehindCount()))
                                 .orElse(AheadBehind.NO_REMOTE);
    } catch (Exception e) {
      throw new GitCommitIdExecutionException("Failed to read ahead behind count: " + e.getMessage(), e);
    }
  }

  private void fetch() {
    FetchCommand fetchCommand = Git.wrap(git).fetch();
    try {
      fetchCommand.setThin(true).call();
    } catch (Exception e) {
      log.error("Failed to perform fetch", e);
    }
  }

  // SETTERS FOR TESTS ----------------------------------------------------

  @VisibleForTesting
  public void setRepository(Repository git) {
    this.git = git;
  }
}
