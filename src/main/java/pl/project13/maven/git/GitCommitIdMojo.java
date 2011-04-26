/*
 * This file is part of git-commit-id-plugin by Konrad Malawski <konrad.malawski@java.pl>
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Goal which touches a timestamp file.
 *
 * @author <a href="mailto:konrad.malawski@project13.pl">Konrad 'ktoso' Malawski</a>
 * @goal revision
 * @phase initialize
 * @requiresProject
 * @since 1.0
 */
@SuppressWarnings({"JavaDoc"})
public class GitCommitIdMojo extends AbstractMojo {

  private static final int DEFAULT_COMMIT_ABBREV_LENGTH = 7;
  
  // these properties will be exposed to maven
  public final String BRANCH               = "branch";
  public final String COMMIT_ID            = "commit.id";
  public final String COMMIT_ID_ABBREV     = "commit.id.abbrev";
  public final String BUILD_AUTHOR_NAME    = "build.user.name";
  public final String BUILD_AUTHOR_EMAIL   = "build.user.email";
  public final String COMMIT_AUTHOR_NAME   = "commit.user.name";
  public final String COMMIT_AUTHOR_EMAIL  = "commit.user.email";
  public final String COMMIT_MESSAGE_FULL  = "commit.message.full";
  public final String COMMIT_MESSAGE_SHORT = "commit.message.short";
  public final String COMMIT_TIME          = "commit.time";

  /**
   * The maven project.
   *
   * @parameter expression="${project}"
   * @readonly
   */
  private MavenProject project;

  /**
   * Specifies whether the goal runs in verbose mode.
   * To be more specific, this means more info being printed out while scanning for paths and also
   * it will make git-commit-id "eat it's own dog food" :-)
   *
   * @parameter default-value="false"
   */
  private boolean verbose;

  /**
   * The root directory of the repository we want to check
   *
   * @parameter default-value="${project.basedir}/.git"
   */
  private File dotGitDirectory;

  /**
   * The prefix to expose the properties on, for example 'git' would allow you to access '${git.branch}'
   *
   * @parameter default-value="git"
   */
  private String prefix;
  private String prefixDot;

  /**
   * The date format to be used for any dates exported by this plugin.
   * It should be a valid SimpleDateFormat string.
   *
   * @parameter default-value="dd.MM.yyyy '@' HH:mm:ss z"
   */
  private String dateFormat;

  /**
   * The properties we store our data in and then expose them
   */
  private Properties properties;

  public final String logPrefix = "[GitCommitIdMojo] ";

  public void execute() throws MojoExecutionException {
    dotGitDirectory = lookupGitDirectory();

    log("Running on '" + dotGitDirectory.getAbsolutePath() + "' repository...");

    try {
      initProperties();
      prefixDot = prefix + ".";

      loadGitData(properties);
      loadBuildTimeData(properties);

      logProperties(properties);
    } catch (IOException e) {
      throw new MojoExecutionException("Could not complete Mojo execution...", e);
    }
    log("Finished running.");
  }

  /**
   * Find the git directory of the currently used project.
   * If it's not already specified, this method will try to find it.
   *
   * @return the File representation of the .git directory
   */
  private File lookupGitDirectory() throws MojoExecutionException {
    if (dotGitDirectory == null || !dotGitDirectory.exists()) { // given dotGitDirectory is not valid

      if (project == null) { // we're running from an unit test
        dotGitDirectory = new File(Constants.DOT_GIT);
        if (dotGitDirectory.exists() && dotGitDirectory.isDirectory()) {
          return dotGitDirectory;
        }
      }

      //Walk up the project parent hierarchy seeking the .git directory
      MavenProject mavenProject = project;
      while(mavenProject != null) {
        dotGitDirectory = new File(mavenProject.getBasedir(), Constants.DOT_GIT);
        if (dotGitDirectory.exists() && dotGitDirectory.isDirectory()) {
          return dotGitDirectory;
        }
        // If we've reached the top-level parent and not found the .git directory, look one level further up
        if (mavenProject.getParent() == null) {
            dotGitDirectory = new File(mavenProject.getBasedir().getParentFile(), Constants.DOT_GIT);
            if (dotGitDirectory.exists() && dotGitDirectory.isDirectory()) {
              return dotGitDirectory;
            }
        }
        mavenProject = mavenProject.getParent();
      }

      throw new MojoExecutionException("Could not find .git directory. Please specify a valid dotGitDirectory in your pom.xml");
    }

    return dotGitDirectory;
  }

  private void initProperties() throws MojoExecutionException {
    log("Initializing properties...");
    if (project != null) {
      log("Using maven project properties...");
      properties = project.getProperties();
    } else {
      properties = new Properties(); // that's ok for unit tests
    }
  }

  private void logProperties(Properties properties) {
    log("------------------git properties loaded------------------");

    for (Object key : properties.keySet()) {
      String keyString = key.toString();
      if (keyString.startsWith(this.prefix)) { // only print OUR properties ;-)
        log(key + " = " + properties.getProperty((String) key));
      }
    }
    log("---------------------------------------------------------");
  }

  private void loadBuildTimeData(Properties properties) {
    Date commitDate = new Date();
    SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
    put(properties, prefixDot + COMMIT_TIME, smf.format(commitDate));
  }

  private void loadGitData(Properties properties) throws IOException, MojoExecutionException {
    log("Loading data from git repository...");
    Repository git = getGitRepository();

    int abbrevLength = DEFAULT_COMMIT_ABBREV_LENGTH;
    
    StoredConfig config = git.getConfig();
        
    if (config != null) {
       abbrevLength = config.getInt("core", "abbrev", DEFAULT_COMMIT_ABBREV_LENGTH);
    }
    
    // git.user.name
    String userName = git.getConfig().getString("user", null, "name");
    put(properties, prefixDot + BUILD_AUTHOR_NAME, userName);

    // git.user.email
    String userEmail = git.getConfig().getString("user", null, "email");
    put(properties, prefixDot + BUILD_AUTHOR_EMAIL, userEmail);

    // more details parsed out bellow
    Ref HEAD = git.getRef(Constants.HEAD);
    if(HEAD == null){
      throw new MojoExecutionException("Could not get HEAD Ref, are you sure you've set the dotGitDirectory property of this plugin to a valid path?");
    }
    RevWalk revWalk = new RevWalk(git);
    RevCommit headCommit = revWalk.parseCommit(HEAD.getObjectId());
    revWalk.markStart(headCommit);
//    revWalk.markUninteresting(headCommit.getParent(1));

    // git.branch
    try {
      String branch = git.getBranch();
      put(properties, prefixDot + BRANCH, branch);

      // git.commit.id
      put(properties, prefixDot + COMMIT_ID, headCommit.getName());

      // git.commit.id.abbrev
      put(properties, prefixDot + COMMIT_ID_ABBREV, headCommit.getName().substring(0, abbrevLength));

     // git.commit.author.name
      String commitAuthor = headCommit.getAuthorIdent().getName();
      put(properties, prefixDot + COMMIT_AUTHOR_NAME, commitAuthor);

      // git.commit.author.email
      String commitEmail = headCommit.getAuthorIdent().getEmailAddress();
      put(properties, prefixDot + COMMIT_AUTHOR_EMAIL, commitEmail);

      // git commit.message.full
      String fullMessage = headCommit.getFullMessage();
      put(properties, prefixDot + COMMIT_MESSAGE_FULL, fullMessage);

      // git commit.message.short
      String shortMessage = headCommit.getShortMessage();
      put(properties, prefixDot + COMMIT_MESSAGE_SHORT, shortMessage);

      int timeSinceEpoch = headCommit.getCommitTime();
      Date commitDate = new Date(timeSinceEpoch * 1000); // git is "by sec" and java is "by ms"
      SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
      put(properties, prefixDot + COMMIT_TIME, smf.format(commitDate));
    } finally {
      revWalk.dispose();
    }
  }

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

  private void put(Properties properties, String key, String value) {
    if (verbose) {
      String s = "Storing: " + key + " = " + value;
      log(s);
    }

    if (isNotEmpty(value)) {
      properties.put(key, value);
    } else {
      properties.put(key, "Unknown"); //todo think if just omitting it would not be better
    }
  }

  private boolean isNotEmpty(String value) {
    return null != value && !" ".equals(value.trim().replace(" ", ""));
  }

  private void log(String message) {
    getLog().info(logPrefix + message);
  }

  // SETTERS FOR TESTS ----------------------------------------------------

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void setDotGitDirectory(File dotGitDirectory) {
    this.dotGitDirectory = dotGitDirectory;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public Properties getProperties() {
    return properties;
  }
}
