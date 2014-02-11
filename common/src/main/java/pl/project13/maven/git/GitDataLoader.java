package pl.project13.maven.git;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import pl.project13.jgit.DescribeCommand;
import pl.project13.jgit.DescribeResult;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.StdOutLoggerBridge;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

/**
 * Extracted from the Mojo to reuse code.
 * 
 * @author jbellmann
 *
 */
public class GitDataLoader {
	
	// these properties will be exposed to maven
	public static final String BRANCH = "branch";
	public static final String COMMIT_ID = "commit.id";
	public static final String COMMIT_ID_ABBREV = "commit.id.abbrev";
	public static final String COMMIT_DESCRIBE = "commit.id.describe";
	public static final String BUILD_AUTHOR_NAME = "build.user.name";
	public static final String BUILD_AUTHOR_EMAIL = "build.user.email";
	public static final String BUILD_TIME = "build.time";
	public static final String COMMIT_AUTHOR_NAME = "commit.user.name";
	public static final String COMMIT_AUTHOR_EMAIL = "commit.user.email";
	public static final String COMMIT_MESSAGE_FULL = "commit.message.full";
	public static final String COMMIT_MESSAGE_SHORT = "commit.message.short";
	public static final String COMMIT_TIME = "commit.time";
	public static final String REMOTE_ORIGIN_URL = "remote.origin.url";
	
	private File rootDirectory;
	private int abbrevLength = 7;
	private String prefixDot = "git";
	private String dateFormat = "dd.MM.yyyy '@' HH:mm:ss z";
	private GitDescribeConfig gitDescribe;
	private LoggerBridge loggerBridge;
	private boolean verbose = false;
	
	private GitDataLoader(GitDataLoaderBuilder builder){
		this.rootDirectory = builder.workingTreeDirectory;
		this.abbrevLength = builder.abbrevLength;
		this.prefixDot = builder.prefixDot;
		this.dateFormat = builder.dateFormat;
		
		this.gitDescribe = builder.gitDescribe;
		this.loggerBridge = builder.loggerBridge;
		this.verbose = builder.verbose;
	}

	public void loadGitData(Properties properties) throws IOException, GitDataLoaderException {
		Repository git = getGitRepository();
		ObjectReader objectReader = git.newObjectReader();
	
		// git.user.name
		String userName = git.getConfig().getString("user", null, "name");
		put(properties, BUILD_AUTHOR_NAME, userName);
	
		// git.user.email
		String userEmail = git.getConfig().getString("user", null, "email");
		put(properties, BUILD_AUTHOR_EMAIL, userEmail);
	
		// more details parsed out bellow
		Ref HEAD = git.getRef(Constants.HEAD);
		if (HEAD == null) {
		  throw new GitDataLoaderException("Could not get HEAD Ref, are you sure you've set the dotGitDirectory property of this plugin to a valid path?");
		}
		RevWalk revWalk = new RevWalk(git);
		RevCommit headCommit = revWalk.parseCommit(HEAD.getObjectId());
		revWalk.markStart(headCommit);
	
		try {
		  // git.branch
		  String branch = determineBranchName(git, System.getenv());
		  put(properties, BRANCH, branch);
	
		  // git.commit.id.describe
//		  maybePutGitDescribe(properties, git);
	
		  // git.commit.id
		  put(properties, COMMIT_ID, headCommit.getName());
	
		  // git.commit.id.abbrev
		  putAbbrevCommitId(objectReader, properties, headCommit, abbrevLength);
	
		  // git.commit.author.name
		  String commitAuthor = headCommit.getAuthorIdent().getName();
		  put(properties, COMMIT_AUTHOR_NAME, commitAuthor);
	
		  // git.commit.author.email
		  String commitEmail = headCommit.getAuthorIdent().getEmailAddress();
		  put(properties, COMMIT_AUTHOR_EMAIL, commitEmail);
	
		  // git commit.message.full
		  String fullMessage = headCommit.getFullMessage();
		  put(properties, COMMIT_MESSAGE_FULL, fullMessage);
	
		  // git commit.message.short
		  String shortMessage = headCommit.getShortMessage();
		  put(properties, COMMIT_MESSAGE_SHORT, shortMessage);
	
		  long timeSinceEpoch = headCommit.getCommitTime();
		  Date commitDate = new Date(timeSinceEpoch * 1000); // git is "by sec" and java is "by ms"
		  SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
		  put(properties, COMMIT_TIME, smf.format(commitDate));
	
		  // git remote.origin.url
		  String remoteOriginUrl = git.getConfig().getString("remote", "origin", "url");
		  put(properties, REMOTE_ORIGIN_URL, remoteOriginUrl);
		  
		  //
		  loadBuildTimeData(properties);
		} finally {
		  revWalk.dispose();
		}
	  }
	
	@Nonnull
	private Repository getGitRepository() throws GitDataLoaderException {
	  Repository repository;
  
	  FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
	  try {
		repository = repositoryBuilder.setWorkTree(rootDirectory).setMustExist(true)
//			.setGitDir(rootDirectory)
			.readEnvironment() // scan environment GIT_* variables
			.findGitDir() // scan up the file system tree
			.build();
	  } catch (IOException e) {
		throw new GitDataLoaderException("Could not initialize repository...", e);
	  }
  
	  if (repository == null) {
		throw new GitDataLoaderException("Could not create git repository. Are you sure '" + rootDirectory.getAbsolutePath() + "' is the valid Git root for your project?");
	  }
  
	  return repository;
	}

	private void put(@Nonnull Properties properties, String key, String value) {
		putWithoutPrefix(properties, prefixDot + key, value);
	  }
	
	  private void putWithoutPrefix(@Nonnull Properties properties, String key, String value) {
		if (!isNotEmpty(value)) {
		  value = "Unknown";
		}
	
		log(key, value);
		properties.put(key, value);
	  }
	
	  private boolean isNotEmpty(@Nullable String value) {
		return null != value && !" ".equals(value.trim().replaceAll(" ", ""));
	  }
	  
	  private void putAbbrevCommitId(ObjectReader objectReader, Properties properties, RevCommit headCommit, int abbrevLength) throws GitDataLoaderException {
		  if (abbrevLength < 2 || abbrevLength > 40) {
			throw new GitDataLoaderException("Abbreviated commit id lenght must be between 2 and 40, inclusive! Was [%s]. ".codePointBefore(abbrevLength) +
												 "Please fix your configuration (the <abbrevLength/> element).");
		  }
	  
		  try {
			AbbreviatedObjectId abbreviatedObjectId = objectReader.abbreviate(headCommit, abbrevLength);
			put(properties, COMMIT_ID_ABBREV, abbreviatedObjectId.name());
		  } catch (IOException e) {
			throw new GitDataLoaderException("Unable to abbreviate commit id! " +
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
	   * Is "Jenkins aware", and prefers {@code GIT_BRANCH} to getting the branch via git if that enviroment variable is set.
	   * The {@GIT_BRANCH} variable is set by Jenkins/Hudson when put in detached HEAD state, but it still knows which branch was cloned.
	   */
	  protected String determineBranchNameOnBuildServer(Repository git, Map<String, String> env) throws IOException {
		String enviromentBasedBranch = env.get("GIT_BRANCH");
		if(Strings.isNullOrEmpty(enviromentBasedBranch)) {
		  log("Detected that running on CI enviroment, but using repository branch, no GIT_BRANCH detected.");
		  return git.getBranch();
		}else {
		  log("Using environment variable based branch name.", "GIT_BRANCH =", enviromentBasedBranch);
		  return enviromentBasedBranch;
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
	  
	void maybePutGitDescribe(@Nonnull Properties properties,
			@Nonnull Repository repository) throws GitDataLoaderException {
		if (gitDescribe == null || !gitDescribe.isSkip()) {
			putGitDescribe(properties, repository);
		}
	}
	  
	  @VisibleForTesting
	  void putGitDescribe(@Nonnull Properties properties, @Nonnull Repository repository) throws GitDataLoaderException {
	    try {
	      DescribeResult describeResult = DescribeCommand
	          .on(repository)
	          .withLoggerBridge(loggerBridge)
	          .setVerbose(verbose)
	          .apply(gitDescribe)
	          .call();

	      put(properties, COMMIT_DESCRIBE, describeResult.toString());
	    } catch (GitAPIException ex) {
	      throw new GitDataLoaderException("Unable to obtain git.commit.id.describe information", ex);
	    }
	  }

	  void loadBuildTimeData(@Nonnull Properties properties) {
	    Date commitDate = new Date();
	    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
	    put(properties, BUILD_TIME, smf.format(commitDate));
	  }	  

	  private void log(Object... parts){
		  loggerBridge.log(parts);
	  }
	  
	  public static class GitDataLoaderBuilder {
		  
		  private File workingTreeDirectory;
		  private int abbrevLength = 7;
		  private String prefixDot = "git";
		  private String dateFormat = "dd.MM.yyyy '@' HH:mm:ss z";
		  
		  private GitDescribeConfig gitDescribe;
		  private LoggerBridge loggerBridge = new StdOutLoggerBridge(false);
		  private boolean verbose = false;
		  
		  
		  public GitDataLoaderBuilder withWorkingTreeDirectory(File workingTreeDirectory){
			  // TODO, validate here
			  this.workingTreeDirectory = workingTreeDirectory;
			  return this;
		  }
		  
		  public GitDataLoaderBuilder withAbbrevationLength(int abbrevationLength){
			  this.abbrevLength = abbrevationLength;
			  return this;
		  }
		  
		  public GitDataLoaderBuilder withPrefixDot(String prefixDot){
			  this.prefixDot = prefixDot;
			  return this;
		  }
		  
		  public GitDataLoaderBuilder withDateFormat(String dateformat){
			  this.dateFormat = dateformat;
			  return this;
		  }
		  
		  public GitDataLoaderBuilder withGitDescribe(GitDescribeConfig gitDescribeConfig){
			  this.gitDescribe = gitDescribeConfig;
			  return this;
		  }
		  
		  public GitDataLoaderBuilder withLoggerBridge(LoggerBridge loggerBridge){
			  this.loggerBridge = loggerBridge;
			  return this;
		  }
		  
		  public GitDataLoaderBuilder verbose(boolean verbose){
			  this.verbose = verbose;
			  return this;
		  }
		  
		  public GitDataLoader build(){
			  return new GitDataLoader(this);
		  }
		  
	  }
}
