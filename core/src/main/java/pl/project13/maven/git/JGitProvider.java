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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jetbrains.annotations.NotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;

import pl.project13.git.api.GitException;
import pl.project13.git.impl.AbstractBaseGitProvider;
import pl.project13.jgit.DescribeCommand;
import pl.project13.jgit.DescribeResult;
import pl.project13.jgit.JGitCommon;
import pl.project13.maven.git.log.LoggerBridge;

public class JGitProvider extends AbstractBaseGitProvider<JGitProvider> {

  private File dotGitDirectory;
  private Repository git;
  private ObjectReader objectReader;
  private RevWalk revWalk;
  private RevCommit headCommit;
  private JGitCommon jGitCommon;

  @NotNull
  public static JGitProvider on(@NotNull File dotGitDirectory, @NotNull LoggerBridge loggerBridge) {
    return new JGitProvider(dotGitDirectory, loggerBridge);
  }

  JGitProvider(@NotNull File dotGitDirectory, @NotNull LoggerBridge loggerBridge) {
    super(loggerBridge);
    this.dotGitDirectory = dotGitDirectory;
    this.jGitCommon = new JGitCommon();
  }

  @NotNull
  public JGitProvider setVerbose(boolean verbose) {
    super.loggerBridge.setVerbose(verbose);
    return this;
  }

  @Override
  public void init() throws GitException {
    git = getGitRepository();
    objectReader = git.newObjectReader();
  }

  @Override
  public String getBuildAuthorName() {
    String userName = git.getConfig().getString("user", null, "name");
    return MoreObjects.firstNonNull(userName, "");
  }

  @Override
  public String getBuildAuthorEmail() {
    String userEmail = git.getConfig().getString("user", null, "email");
    return MoreObjects.firstNonNull(userEmail, "");
  }

  @Override
  public void prepareGitToExtractMoreDetailedReproInformation() throws GitException {
    try {
      // more details parsed out bellow
      Ref head = git.getRef(Constants.HEAD);
      if (head == null) {
        throw new GitException("Could not get HEAD Ref, are you sure you have set the dotGitDirectory property of this plugin to a valid path?");
      }
      revWalk = new RevWalk(git);
      ObjectId headObjectId = head.getObjectId();
      if(headObjectId == null){
        throw new GitException("Could not get HEAD Ref, are you sure you have some commits in the dotGitDirectory?");
      }
      headCommit = revWalk.parseCommit(headObjectId);
      revWalk.markStart(headCommit);
    } catch (GitException e) {
      throw e;
    } catch (Exception e) {
      throw new GitException("Error", e);
    }
  }

  @Override
  public String getBranchName() throws GitException {
    try {
      return git.getBranch();
    } catch (IOException e) {
      throw new GitException(e);
    }
  }

  @Override
  public String getGitDescribe() throws GitException {
    String gitDescribe = getGitDescribe(git);
    return gitDescribe;
  }

  @Override
  public String getCommitId() {
    String commitId = headCommit.getName();
    return commitId;
  }

  @Override
  public String getAbbrevCommitId() throws GitException {
    String abbrevCommitId = getAbbrevCommitId(objectReader, headCommit, abbrevLength);
    return abbrevCommitId;
  }

  @Override
  public boolean isDirty() throws GitException {
    Git gitObject = Git.wrap(git);
    try {
      return !gitObject.status().call().isClean();
    } catch (GitAPIException e) {
      throw new GitException("Failed to get git status: " + e.getMessage(), e);
    }
  }

  @Override
  public String getCommitAuthorName() {
    String commitAuthor = headCommit.getAuthorIdent().getName();
    return commitAuthor;
  }

  @Override
  public String getCommitAuthorEmail() {
    String commitEmail = headCommit.getAuthorIdent().getEmailAddress();
    return commitEmail;
  }

  @Override
  public String getCommitMessageFull() {
    String fullMessage = headCommit.getFullMessage();
    return fullMessage.trim();
  }

  @Override
  public String getCommitMessageShort() {
    String shortMessage = headCommit.getShortMessage();
    return shortMessage.trim();
  }

  @Override
  public String getCommitTime() {
    long timeSinceEpoch = headCommit.getCommitTime();
    Date commitDate = new Date(timeSinceEpoch * 1000); // git is "by sec" and java is "by ms"
    SimpleDateFormat smf = getSimpleDateFormatWithTimeZone();
    return smf.format(commitDate);
  }

  @Override
  public String getRemoteOriginUrl() throws GitException {
    String remoteOriginUrl = git.getConfig().getString("remote", "origin", "url");
    return remoteOriginUrl;
  }

  @Override
  public String getTags() throws GitException {
    try {
      Repository repo = getGitRepository();
      ObjectId headId = headCommit.toObjectId();
      Collection<String> tags = jGitCommon.getTags(repo,headId);
      return Joiner.on(",").join(tags);
    } catch (GitAPIException e) {
      loggerBridge.error("Unable to extract tags from commit: " + headCommit.getName() + " (" + e.getClass().getName() + ")");
      return "";
    }
  }

  @Override
  public String getClosestTagName() throws GitException {
    Repository repo = getGitRepository();
    try {
      return jGitCommon.getClosestTagName(loggerBridge,repo);
    } catch (Throwable t) {
      // could not find any tags to describe
    }
    return "";
  }

  @Override
  public String getClosestTagCommitCount() throws GitException {
    Repository repo = getGitRepository();
    try {
      return jGitCommon.getClosestTagCommitCount(loggerBridge,repo,headCommit);
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
  }


  @VisibleForTesting String getGitDescribe(@NotNull Repository repository) throws GitException {
    try {
      DescribeResult describeResult = DescribeCommand
        .on(repository)
        .withLoggerBridge(super.loggerBridge)
        .apply(super.gitDescribe)
        .call();

      return describeResult.toString();
    } catch (GitAPIException ex) {
      ex.printStackTrace();
      throw new GitException("Unable to obtain git.commit.id.describe information", ex);
    }
  }

  private String getAbbrevCommitId(ObjectReader objectReader, RevCommit headCommit, int abbrevLength) throws GitException {
    try {
      AbbreviatedObjectId abbreviatedObjectId = objectReader.abbreviate(headCommit, abbrevLength);
      return abbreviatedObjectId.name();
    } catch (IOException e) {
      throw new GitException("Unable to abbreviate commit id! " +
                                         "You may want to investigate the <abbrevLength/> element in your configuration.", e);
    }
  }


  @NotNull
  private Repository getGitRepository() throws GitException {
    Repository repository;

    FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
    try {
      repository = repositoryBuilder
        .setGitDir(dotGitDirectory)
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir() // scan up the file system tree
        .build();
    } catch (IOException e) {
      throw new GitException("Could not initialize repository...", e);
    }

    if (repository == null) {
      throw new GitException("Could not create git repository. Are you sure '" + dotGitDirectory + "' is the valid Git root for your project?");
    }

    return repository;
  }

  // SETTERS FOR TESTS ----------------------------------------------------

  @VisibleForTesting
  public void setRepository(Repository git) {
    this.git = git;
  }
}
