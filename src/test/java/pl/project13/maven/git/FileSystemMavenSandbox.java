package pl.project13.maven.git;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

/**
 * Quick and dirty maven projects tree structure to create on disk during integration tests
 * Can have both parent and child projects set up
 * Copies sample git repository from prototype location to newly created project
 * Has ability to set target project for storing git repository
 *
 * @author mostr
 */
public class FileSystemMavenSandbox {

  private static final String FILE_SEPARATOR = System.getProperty("file.separator");

  /**
   * Sample git repository location to use as source in integration tests
   * Test should copy content of this folder to ".git" in correct destination
   */
  private static final String PROROTYPE_GIT_REPO_PATH = "src/test/resources/_git";

  private MavenProject parentProject;
  private MavenProject childProject;
  private String rootSandboxPath;
  private File gitRepoTargetDir;

  public FileSystemMavenSandbox(String rootSandboxPath) {
    this.rootSandboxPath = rootSandboxPath;
  }

  public FileSystemMavenSandbox withParentProject(String parentProjectDirName, String packaging) {
    parentProject = createProject(new File(rootSandboxPath + FILE_SEPARATOR + parentProjectDirName), packaging);
    return this;
  }

  public FileSystemMavenSandbox withNoChildProject() {
    // no-op: marker for better tests readability
    return this;
  }

  public FileSystemMavenSandbox withChildProject(String childProjectDirName, String packaging) {
    childProject = createProject(new File(parentProject.getBasedir(), childProjectDirName), packaging);
    childProject.setParent(parentProject);
    return this;
  }

  public FileSystemMavenSandbox withGitRepoInParent() {
    gitRepoTargetDir = parentProject.getBasedir();
    return this;
  }

  public FileSystemMavenSandbox withGitRepoInChild() {
    gitRepoTargetDir = childProject.getBasedir();
    return this;
  }

  public FileSystemMavenSandbox withGitRepoAboveParent() {
    gitRepoTargetDir = new File(rootSandboxPath);
    return this;
  }

  public FileSystemMavenSandbox withNoGitRepoAvailable() {
    gitRepoTargetDir = null;
    return this;
  }

  public FileSystemMavenSandbox create(CleanUp cleanupMode) throws IOException {
    cleanupIfRequired(cleanupMode);
    createParentDir();
    createChildDirIfRequired();
    createGitRepoIfRequired();
    return this;
  }

  private void createGitRepoIfRequired() throws IOException {
    if (gitRepoTargetDir != null) {
      FileUtils.copyDirectory(new File(PROROTYPE_GIT_REPO_PATH), new File(gitRepoTargetDir, ".git"));
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

  private void cleanupIfRequired(CleanUp cleanupMode) throws IOException {
    if (CleanUp.CLEANUP_FIRST == cleanupMode) {
      cleanup();
    }
  }

  public void cleanup() throws IOException {
    FileUtils.deleteDirectory(new File(rootSandboxPath));
  }

  public MavenProject getParentProject() {
    return parentProject;
  }

  public MavenProject getChildProject() {
    return childProject;
  }

  private MavenProject createProject(File basedir, String packaging) {
    MavenProject project = new MavenProject();
    project.setBasedir(basedir);
    project.setPackaging(packaging);
    return project;
  }

  enum CleanUp {
    CLEANUP_FIRST,
    NO_CLEANUP
  }


}