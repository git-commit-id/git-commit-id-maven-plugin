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

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Quick and dirty maven projects tree structure to create on disk during integration tests.
 * Can have both parent and child projects set up.
 * Copies sample git repository from prototype location to newly created project.
 * Has ability to set target project for storing git repository.
 */
public class FileSystemMavenSandbox {

  private MavenProject childProject;
  private String rootSandboxPath;
  private MavenProject parentProject;
  private boolean keepSandboxWhenFinishedTest = false;

  /**
   * Sample git repository location to use as source in integration tests
   * Test should copy content of this folder to ".git" in correct destination
   */
  private File gitRepoSourceDir;

  @Nullable
  private File gitRepoTargetDir;

  public FileSystemMavenSandbox(String rootSandboxPath) {
    this.rootSandboxPath = rootSandboxPath;
  }

  @Nonnull
  public FileSystemMavenSandbox withParentProject(String parentProjectDirName, String packaging) {
    parentProject = createProject(new File(rootSandboxPath + File.separator + parentProjectDirName), packaging);
    return this;
  }

  @Nonnull
  public FileSystemMavenSandbox withNoChildProject() {
    // no-op: marker for better tests readability
    return this;
  }

  @Nonnull
  public FileSystemMavenSandbox withChildProject(String childProjectDirName, String packaging) {
    childProject = createProject(new File(parentProject.getBasedir(), childProjectDirName), packaging);
    childProject.setParent(parentProject);
    return this;
  }

  @Nonnull
  public FileSystemMavenSandbox withGitRepoInParent(@Nonnull AvailableGitTestRepo repo) {
    System.out.println("TEST: Will prepare sandbox repository based on: [" + repo.getDir() + "]");

    gitRepoSourceDir = repo.getDir();
    gitRepoTargetDir = parentProject.getBasedir();
    return this;
  }

  @Nonnull
  public FileSystemMavenSandbox withGitRepoInChild(@Nonnull AvailableGitTestRepo repo) {
    gitRepoSourceDir = repo.getDir();
    gitRepoTargetDir = childProject.getBasedir();
    return this;
  }

  @Nonnull
  public FileSystemMavenSandbox withGitRepoAboveParent(@Nonnull AvailableGitTestRepo repo) {
    gitRepoSourceDir = repo.getDir();
    gitRepoTargetDir = new File(rootSandboxPath);
    return this;
  }

  @Nonnull
  public FileSystemMavenSandbox withNoGitRepoAvailable() {
    gitRepoTargetDir = null;
    return this;
  }

  @Nonnull
  public FileSystemMavenSandbox create() throws RuntimeException {
    try {
      createParentDir();
      createChildDirIfRequired();
      createGitRepoIfRequired();

      return this;
    } catch (Exception ex) {
      throw new RuntimeException(String.format("Failed creating %s...", getClass().getSimpleName()), ex);
    }
  }

  private void createGitRepoIfRequired() throws IOException {
    if (gitRepoTargetDir != null) {
      File gitFolder = new File(gitRepoTargetDir, ".git");
      FileUtils.copyDirectory(gitRepoSourceDir, gitFolder);
      // As the WITH_NO_CHANGES and WITH_CHANGES git trees contain empty
      // folders whose existence is crucial for the native git to run (jgit does not mind)
      // *and* because empty folders are silently omitted from git checkins, ensure that
      // these folders exist
      Files.createDirectories(new File(gitFolder, "refs/heads").toPath());
      Files.createDirectories(new File(gitFolder, "refs/tags").toPath());
    }
  }

  private void createParentDir() throws IOException {
    FileUtils.forceMkdir(parentProject.getBasedir());
  }

  private void createChildDirIfRequired() throws IOException {
    if (childProject != null) {
      FileUtils.forceMkdir(childProject.getBasedir());
    }
  }

  public MavenProject getParentProject() {
    return parentProject;
  }

  public MavenProject getChildProject() {
    return childProject;
  }

  @Nonnull
  private MavenProject createProject(File basedir, String packaging) {
    MavenProject project = new MavenProject();
    project.setFile(new File(basedir + File.separator + "pom.xml"));
    project.setPackaging(packaging);
    return project;
  }

  @Override
  public String toString() {
    return "FileSystemMavenSandbox{" +
        "gitRepoTargetDir=" + gitRepoTargetDir +
        ", gitRepoSourceDir=" + gitRepoSourceDir +
        ", rootSandboxPath='" + rootSandboxPath + '\'' +
        '}';
  }

  @Nonnull
  public FileSystemMavenSandbox withKeepSandboxWhenFinishedTest(boolean keepSandboxWhenFinishedTest) {
    // if we want to keep the generated sandbox for overwiew the content of it
    this.keepSandboxWhenFinishedTest = keepSandboxWhenFinishedTest;
    return this;
  }

  public boolean isKeepSandboxWhenFinishedTest() {
    return keepSandboxWhenFinishedTest;
  }
}
