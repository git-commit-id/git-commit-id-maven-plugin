package pl.project13.maven.git;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jetbrains.annotations.NotNull;
import pl.project13.jgit.DescribeCommand;
import pl.project13.jgit.DescribeResult;
import pl.project13.maven.git.log.LoggerBridge;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 */
public class JGitProvider extends GitDataProvider {

  private File dotGitDirectory;
  private Repository git;
  private ObjectReader objectReader;
  private RevWalk revWalk;
  private RevCommit headCommit;

  @NotNull
  public static JGitProvider on(@NotNull File dotGitDirectory) {
    return new JGitProvider(dotGitDirectory);
  }

  JGitProvider(@NotNull File dotGitDirectory) {
    this.dotGitDirectory = dotGitDirectory;
  }

  @NotNull
  public JGitProvider withLoggerBridge(LoggerBridge bridge) {
    super.loggerBridge = bridge;
    return this;
  }

  @NotNull
  public JGitProvider setVerbose(boolean verbose) {
    super.verbose = verbose;
    super.loggerBridge.setVerbose(verbose);
    return this;
  }

  public JGitProvider setPrefixDot(String prefixDot) {
    super.prefixDot = prefixDot;
    return this;
  }

  public JGitProvider setAbbrevLength(int abbrevLength) {
    super.abbrevLength = abbrevLength;
    return this;
  }

  public JGitProvider setDateFormat(String dateFormat) {
    super.dateFormat = dateFormat;
    return this;
  }

  public JGitProvider setGitDescribe(GitDescribeConfig gitDescribe) {
    super.gitDescribe = gitDescribe;
    return this;
  }

  @Override
  protected void init() throws MojoExecutionException {
    git = getGitRepository();
    objectReader = git.newObjectReader();
  }

  @Override
  protected String getBuildAuthorName() {
    String userName = git.getConfig().getString("user", null, "name");
    return userName;
  }

  @Override
  protected String getBuildAuthorEmail() {
    String userEmail = git.getConfig().getString("user", null, "email");
    return userEmail;
  }

  @Override
  protected void prepareGitToExtractMoreDetailedReproInformation() throws MojoExecutionException {
    try {
      // more details parsed out bellow
      Ref HEAD = git.getRef(Constants.HEAD);
      if (HEAD == null) {
        throw new MojoExecutionException("Could not get HEAD Ref, are you sure you've set the dotGitDirectory property of this plugin to a valid path?");
      }
      revWalk = new RevWalk(git);
      headCommit = revWalk.parseCommit(HEAD.getObjectId());
      revWalk.markStart(headCommit);
    } catch (Exception e) {
      throw new MojoExecutionException("Error", e);
    }
  }

  @Override
  protected String getBranchName() throws IOException {
    String branch = git.getBranch();
    return branch;
  }

  @Override
  protected String getGitDescribe() throws MojoExecutionException {
    String gitDescribe = getGitDescribe(git);
    return gitDescribe;
  }

  @Override
  protected String getCommitId() {
    String commitId = headCommit.getName();
    return commitId;
  }

  @Override
  protected String getAbbrevCommitId() throws MojoExecutionException {
    String abbrevCommitId = getAbbrevCommitId(objectReader, headCommit, abbrevLength);
    return abbrevCommitId;
  }

  @Override
  protected String getCommitAuthorName() {
    String commitAuthor = headCommit.getAuthorIdent().getName();
    return commitAuthor;
  }

  @Override
  protected String getCommitAuthorEmail() {
    String commitEmail = headCommit.getAuthorIdent().getEmailAddress();
    return commitEmail;
  }

  @Override
  protected String getCommitMessageFull() {
    String fullMessage = headCommit.getFullMessage();
    return fullMessage;
  }

  @Override
  protected String getCommitMessageShort() {
    String shortMessage = headCommit.getShortMessage();
    return shortMessage;
  }

  @Override
  protected String getCommitTime() {
    long timeSinceEpoch = headCommit.getCommitTime();
    Date commitDate = new Date(timeSinceEpoch * 1000); // git is "by sec" and java is "by ms"
    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
    return smf.format(commitDate);
  }

  @Override
  protected String getRemoteOriginUrl() throws MojoExecutionException {
    String remoteOriginUrl = git.getConfig().getString("remote", "origin", "url");
    return remoteOriginUrl;
  }

  @Override
  protected String getTags() throws MojoExecutionException {
    RevWalk walk = null;
    try {
      Repository repo = getGitRepository();
      Git git = Git.wrap(repo);
      walk = new RevWalk(repo);
      List<Ref> tagRefs = git.tagList().call();

      final ObjectId headId = headCommit.toObjectId();
      final RevWalk finalWalk = walk;
      Collection<Ref> tagsForHeadCommit = Collections2.filter(tagRefs, new Predicate<Ref>() {
        @Override public boolean apply(Ref tagRef) {
          boolean lightweightTag = tagRef.getObjectId().equals(headId);

          try {
            // TODO make this configurable (most users shouldn't really care too much what kind of tag it is though)
            return lightweightTag || finalWalk.parseTag(tagRef.getObjectId()).getObject().getId().equals(headId); // or normal tag
          } catch (IOException e) {
            return false;
          }
        }
      });

      Collection<String> tags = Collections2.transform(tagsForHeadCommit, new Function<Ref, String>() {
        @Override public String apply(Ref input) {
          return input.getName().replaceAll("refs/tags/", "");
        }
      });

      return Joiner.on(",").join(tags);
    } catch (GitAPIException e) {
      loggerBridge.error("Unable to extract tags from commit: " + headCommit.getName() + " (" + e.getClass().getName() + ")");
      return "";
    } finally {
      if (walk != null) {
        walk.dispose();
      }
    }
  }

  @Override
  protected void finalCleanUp() {
    if (revWalk != null) {
      revWalk.dispose();
    }
  }


  @VisibleForTesting String getGitDescribe(@NotNull Repository repository) throws MojoExecutionException {
    try {
      DescribeResult describeResult = DescribeCommand
        .on(repository)
        .withLoggerBridge(super.loggerBridge)
        .setVerbose(super.verbose)
        .apply(super.gitDescribe)
        .call();

      return describeResult.toString();
    } catch (GitAPIException ex) {
      ex.printStackTrace();
      throw new MojoExecutionException("Unable to obtain git.commit.id.describe information", ex);
    }
  }

  private String getAbbrevCommitId(ObjectReader objectReader, RevCommit headCommit, int abbrevLength) throws MojoExecutionException {
    try {
      AbbreviatedObjectId abbreviatedObjectId = objectReader.abbreviate(headCommit, abbrevLength);
      return abbreviatedObjectId.name();
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to abbreviate commit id! " +
                                         "You may want to investigate the <abbrevLength/> element in your configuration.", e);
    }
  }


  @NotNull
  private Repository getGitRepository() throws MojoExecutionException {
    Repository repository;

    FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
    try {
      repository = repositoryBuilder
        .setGitDir(dotGitDirectory)
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir() // scan up the file system tree
        .build();
    } catch (IOException e) {
      throw new MojoExecutionException("Could not initialize repository...", e);
    }

    if (repository == null) {
      throw new MojoExecutionException("Could not create git repository. Are you sure '" + dotGitDirectory + "' is the valid Git root for your project?");
    }

    return repository;
  }

  // SETTERS FOR TESTS ----------------------------------------------------

  @VisibleForTesting
  public void setRepository(Repository git) {
    this.git = git;
  }
}
