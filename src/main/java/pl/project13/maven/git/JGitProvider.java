package pl.project13.maven.git;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.project13.jgit.DescribeCommand;
import pl.project13.jgit.DescribeResult;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
*
* @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
*/
public class JGitProvider extends GitDataProvider {

  private File dotGitDirectory;

  @NotNull
  public static JGitProvider on(@NotNull File dotGitDirectory) {
    return new JGitProvider(dotGitDirectory);
  }

  JGitProvider(@NotNull File dotGitDirectory){
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

  public JGitProvider setGitDescribe(GitDescribeConfig gitDescribe){
    super.gitDescribe = gitDescribe;
    return this;
  }

  @Override
  public void loadGitData(@NotNull Properties properties) throws IOException, MojoExecutionException{    
    Repository git = getGitRepository();
    ObjectReader objectReader = git.newObjectReader();

    // git.user.name
    String userName = git.getConfig().getString("user", null, "name");
    put(properties, GitCommitIdMojo.BUILD_AUTHOR_NAME, userName);

    // git.user.email
    String userEmail = git.getConfig().getString("user", null, "email");
    put(properties, GitCommitIdMojo.BUILD_AUTHOR_EMAIL, userEmail);

    // more details parsed out bellow
    Ref HEAD = git.getRef(Constants.HEAD);
    if (HEAD == null) {
      throw new MojoExecutionException("Could not get HEAD Ref, are you sure you've set the dotGitDirectory property of this plugin to a valid path?");
    }
    RevWalk revWalk = new RevWalk(git);
    RevCommit headCommit = revWalk.parseCommit(HEAD.getObjectId());
    revWalk.markStart(headCommit);

    try {
      // git.branch
      String branch = determineBranchName(git, System.getenv());
      put(properties, GitCommitIdMojo.BRANCH, branch);

      // git.commit.id.describe
      maybePutGitDescribe(properties, git);

      // git.commit.id
      put(properties, GitCommitIdMojo.COMMIT_ID, headCommit.getName());

      // git.commit.id.abbrev
      putAbbrevCommitId(objectReader, properties, headCommit, abbrevLength);

      // git.commit.author.name
      String commitAuthor = headCommit.getAuthorIdent().getName();
      put(properties, GitCommitIdMojo.COMMIT_AUTHOR_NAME, commitAuthor);

      // git.commit.author.email
      String commitEmail = headCommit.getAuthorIdent().getEmailAddress();
      put(properties, GitCommitIdMojo.COMMIT_AUTHOR_EMAIL, commitEmail);

      // git commit.message.full
      String fullMessage = headCommit.getFullMessage();
      put(properties, GitCommitIdMojo.COMMIT_MESSAGE_FULL, fullMessage);

      // git commit.message.short
      String shortMessage = headCommit.getShortMessage();
      put(properties, GitCommitIdMojo.COMMIT_MESSAGE_SHORT, shortMessage);

      long timeSinceEpoch = headCommit.getCommitTime();
      Date commitDate = new Date(timeSinceEpoch * 1000); // git is "by sec" and java is "by ms"
      SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
      put(properties, GitCommitIdMojo.COMMIT_TIME, smf.format(commitDate));

      // git remote.origin.url
      String remoteOriginUrl = git.getConfig().getString("remote", "origin", "url");
      put(properties, GitCommitIdMojo.REMOTE_ORIGIN_URL, remoteOriginUrl);
    } finally {
      revWalk.dispose();
    }
  }

  void maybePutGitDescribe(@NotNull Properties properties, @NotNull Repository repository) throws MojoExecutionException {
    boolean isGitDescribeOptOutByDefault = (super.gitDescribe == null);
    boolean isGitDescribeOptOutByConfiguration = (super.gitDescribe != null && !super.gitDescribe.isSkip());
    
    if (isGitDescribeOptOutByDefault || isGitDescribeOptOutByConfiguration) {
      putGitDescribe(properties, repository);
    }
  }

  @VisibleForTesting
  void putGitDescribe(@NotNull Properties properties, @NotNull Repository repository) throws MojoExecutionException {
    try {
      DescribeResult describeResult = DescribeCommand
          .on(repository)
          .withLoggerBridge(super.loggerBridge)
          .setVerbose(super.verbose)
          .apply(super.gitDescribe)
          .call();

      put(properties, GitCommitIdMojo.COMMIT_DESCRIBE, describeResult.toString());
    } catch (GitAPIException ex) {
      ex.printStackTrace();
      throw new MojoExecutionException("Unable to obtain git.commit.id.describe information", ex);
    }
  }

  private void putAbbrevCommitId(ObjectReader objectReader, Properties properties, RevCommit headCommit, int abbrevLength) throws MojoExecutionException {
    if (abbrevLength < 2 || abbrevLength > 40) {
      throw new MojoExecutionException("Abbreviated commit id lenght must be between 2 and 40, inclusive! Was [%s]. ".codePointBefore(abbrevLength) +
                                           "Please fix your configuration (the <abbrevLength/> element).");
    }

    try {
      AbbreviatedObjectId abbreviatedObjectId = objectReader.abbreviate(headCommit, abbrevLength);
      put(properties, GitCommitIdMojo.COMMIT_ID_ABBREV, abbreviatedObjectId.name());
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to abbreviate commit id! " +
                                           "You may want to investigate the <abbrevLength/> element in your configuration.", e);
    }
  }

  /**
   * If running within Jenkins/Hudosn, honor the branch name passed via GIT_BRANCH env var.  This
   * is necessary because Jenkins/Hudson alwways invoke build in a detached head state.
   *
   * @param git
   * @param env
   * @return results of git.getBranch() or, if in Jenkins/Hudson, value of GIT_BRANCH
   */
  protected String determineBranchName(Repository git, Map<String, String> env) throws IOException {
    if (runningOnBuildServer(env)) {
      return determineBranchNameOnBuildServer(git, env);
    } else {
      return git.getBranch();
    }
  }

  /**
   * Detects if we're running on Jenkins or Hudson, based on expected env variables.
   * <p/>
   * TODO: How can we detect Bamboo, TeamCity etc? Pull requests welcome.
   *
   * @return true if running
   * @see <a href="https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables">JenkinsSetEnvironmentVariables</a>
   * @param env
   */
  private boolean runningOnBuildServer(Map<String, String> env) {
    return env.containsKey("HUDSON_URL") || env.containsKey("JENKINS_URL");
  }

  /**
   * Is "Jenkins aware", and prefers {@code GIT_BRANCH} to getting the branch via git if that enviroment variable is set.
   * The {@GIT_BRANCH} variable is set by Jenkins/Hudson when put in detached HEAD state, but it still knows which branch was cloned.
   */
  protected String determineBranchNameOnBuildServer(Repository git, Map<String, String> env) throws IOException {
    String enviromentBasedBranch = env.get("GIT_BRANCH");
    if(isNullOrEmpty(enviromentBasedBranch)) {
      log("Detected that running on CI enviroment, but using repository branch, no GIT_BRANCH detected.");
      return git.getBranch();
    }else {
      log("Using environment variable based branch name.", "GIT_BRANCH =", enviromentBasedBranch);
      return enviromentBasedBranch;
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
}
