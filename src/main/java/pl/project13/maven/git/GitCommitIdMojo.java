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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
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
 * @since 1.0
 *
 * @goal revision
 * @phase initialize
 * @requiresProject
 */
@SuppressWarnings({"JavaDoc"})
public class GitCommitIdMojo extends AbstractMojo {

  // these properties will be exposed to maven
  public final String BRANCH               = "branch";
  public final String COMMIT_ID            = "commit.id";
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
   *
   * @parameter default-value="false"
   */
  private boolean verbose;

  /**
   * The root directory of the repository we want to check
   *
   * @parameter
   * @required
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
   * @parameter default-value="dd.MM.yyyy '@' HH:mm:ss z"
   */
  private String dateFormat;

  /**
   * The properties we store our data in and then expose them
   */
  private Properties properties;
  public final String logPrefix = "[GitCommitIdMojo] ";

  public void execute() throws MojoExecutionException {
    getLog().info(logPrefix + "Running on '" + dotGitDirectory.getAbsolutePath() + "' repository...");

    try {
      initProperties();
      prefixDot = prefix + ".";

      loadGitData(properties);
      loadBuildTimeData(properties);

      logPropertiesIfVerbose(properties);
    } catch (IOException e) {
      throw new MojoExecutionException("Could not complete Mojo execution...", e);
    }
    getLog().info(logPrefix + "Finished running.");
  }

  private void initProperties() throws MojoExecutionException {
    getLog().info(logPrefix + "initializing properties...");
    if (project != null) {
      getLog().info(logPrefix + "Using maven project properties...");
      properties = project.getProperties();
    } else {
      properties = new Properties();
//      throw new MojoExecutionException("Maven project WAS NULL! Created blank properties...");
    }
  }

  private void logPropertiesIfVerbose(Properties properties) {
    if (verbose) {
      Log log = getLog();
      log.info(logPrefix + "------------------git properties loaded------------------");

      for (Object key : properties.keySet()) {
        log.info(logPrefix + key + " = " + properties.getProperty((String) key));
      }
      log.info(logPrefix + "---------------------------------------------------------");
    }
  }

  private void loadBuildTimeData(Properties properties) {
      Date commitDate = new Date();
      SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
      put(properties, prefixDot + COMMIT_TIME, smf.format(commitDate));
  }

  private void loadGitData(Properties properties) throws IOException, MojoExecutionException {
    getLog().info(logPrefix +"Loading data from git repository...");
    Repository git = getGitRepository();

    // git.user.name
    String userName = git.getConfig().getString("user", null, "name");
    put(properties, prefixDot + BUILD_AUTHOR_NAME, userName);

    // git.user.email
    String userEmail = git.getConfig().getString("user", null, "email");
    put(properties, prefixDot + BUILD_AUTHOR_EMAIL, userEmail);

    // more details parsed out bellow
    Ref HEAD = git.getRef(Constants.HEAD);
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
    Repository repository = null;

    FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
    if (dotGitDirectory == null) {
      dotGitDirectory = project.getBasedir();
    }

    try {
      repository = repositoryBuilder
          .setGitDir(dotGitDirectory)
          .readEnvironment() // scan environment GIT_* variables
              // user.email etc. can be overridden by the GIT_AUTHOR_EMAIL, GIT_COMMITTER_EMAIL, and EMAIL environment variables
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

//  private void exposeProperties(MavenProject mavenProject, Properties properties) {
//    if (mavenProject != null) {
//      Properties propz = mavenProject.getProperties();
//      for (Object key : properties.keySet()) {
//        String value = properties.getProperty((String) key);
//        getLog().info(logPrefix + "Exposing " + key + "...");
//        propz.setProperty((String) key, value);
//      }
//
//    } else {
//      getLog().debug(logPrefix + "Could not inject properties into mavenProject as it was null.");
//    }
//  }

  private void put(Properties properties, String key, String value) {
    getLog().info(logPrefix + "Storing: " + key + " = " + value);
    properties.put(key, value);
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
