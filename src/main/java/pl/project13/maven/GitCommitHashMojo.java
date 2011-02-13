package pl.project13.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
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
 * @goal revision
 * @phase initialize
 * @requiresProject
 */
@SuppressWarnings({"JavaDoc"})
public class GitCommitHashMojo extends AbstractMojo {

  /**
   * The maven project.
   *
   * @paremeter expression="${project}"
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
   */
  private File basedir;

  /**
   * The prefix to expose the properties on, for example 'git' would allow you to access '${git.branch}'
   *
   * @parameter default-value="git"
   */
  private String prefix;

  /**
   * The properties we store our data in and then expose them
   */
  private Properties properties = new Properties();

  public void execute() throws MojoExecutionException {
    try {
      loadGitData(properties);

      exposeProperties(project, properties);
    } catch (IOException e) {
      throw new MojoExecutionException("Could not complete Mojo execution...", e);
    }
  }

  private void loadGitData(Properties properties) throws IOException, MojoExecutionException {
    Repository git = getGitRepository();
    String prefixDot = prefix + ".";

    // git.user.name
    String userName = git.getConfig().getString("user", null, "name");
    properties.put(prefixDot + "build.author.name", userName);

    // git.user.email
    String userEmail = git.getConfig().getString("user", null, "email");
    properties.put(prefixDot + "build.author.email", userEmail);

    // more details parsed out bellow
//    ObjectId objectId = git.resolve(Constants.HEAD);
    Ref HEAD = git.getRef("refs/heads/master");
    ObjectId objectId = HEAD.getObjectId();
    String objectHash = objectId.getName();

    RevWalk revWalk = new RevWalk(git);
    RevCommit headCommit = revWalk.parseCommit(objectId);
    revWalk.markStart(headCommit);
    revWalk.markUninteresting(headCommit.getParent(1));

    // git.branch
    try {
      String branch = git.getBranch();
      properties.put(prefixDot + "branch", branch);

      // git.commit.hash
      properties.put(prefixDot + "commit.hash", objectHash);

      // git.commit.author.name
      String commitAuthor = headCommit.getAuthorIdent().getName();
      properties.put(prefixDot + "commit.author.name", commitAuthor);

      // git.commit.author.email
      String commitEmail = headCommit.getAuthorIdent().getEmailAddress();
      properties.put(prefixDot + "commit.author.email", commitEmail);

      // git commit.message.full
      String fullMessage = headCommit.getFullMessage();
      properties.put(prefixDot + "commit.message.full", fullMessage);

      // git commit.message.short
      String shortMessage = headCommit.getShortMessage();
      properties.put(prefixDot + "commit.message.short", shortMessage);

      int timeSinceEpoch = headCommit.getCommitTime();
      Date commitDate = new Date(timeSinceEpoch);
      SimpleDateFormat smf = new SimpleDateFormat("dd.MM.yyyy '@' HH:mm:ss z"); // todo extract as plugin property
      properties.put(prefixDot + "commit.time", smf.format(commitDate));
    } finally {
      revWalk.dispose();
    }
  }

  private Repository getGitRepository() throws MojoExecutionException {
    Repository repository = null;

    FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
    if (basedir == null) {
      basedir = project.getBasedir();
    }

    try {
      repository = repositoryBuilder
          .setGitDir(basedir)
          .readEnvironment() // scan environment GIT_* variables
              // user.email etc. can be overridden by the GIT_AUTHOR_EMAIL, GIT_COMMITTER_EMAIL, and EMAIL environment variables
          .findGitDir() // scan up the file system tree
          .build();
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (repository == null) {
      throw new MojoExecutionException("Could not create git repository. Are you sure '" + basedir + "' is the valid Git root for your project?");
    }

    return repository;
  }

  private void exposeProperties(MavenProject mavenProject, Properties properties) {
    mavenProject.getProperties().putAll(properties);
  }

  // TEST SETTERS ----------------------------------------------------


  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public void setBasedir(File basedir) {
    this.basedir = basedir;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }
}
