package pl.project13.maven.git;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;

/**
 * Quick and dirty maven projects tree structure to create on disk during integration tests
 * Can have both parent and child projects set up
 * Copies sample git repository from prototype location to newly created project
 * Has ability to set target project for storing git repository 
 * @author mostr
 *
 */
class TemporaryMavenProject {
	
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	/**
	 * Sample git repository location to use as source in integration tests
	 * Test should copy content of this folder to ".git" in correct destination 
	 */
	private static final String PROROTYPE_GIT_REPO_PATH = "src/test/resources/_prototype_git_repo";
	
	private MavenProject parentProject;
	private MavenProject childProject;
	private String parentBasedir;
	private File gitRepoTargetDir;
	
	public TemporaryMavenProject withParentProject(String basedir, String packaging) {
		parentProject = createProject(basedir, packaging);
		parentBasedir = basedir;
		return this;
	}

	public TemporaryMavenProject withNoChildProject() {
		// no-op: marker for better tests readability 
		return this;
	}

	public TemporaryMavenProject withChildProject(String relativeDirname, String packaging) {
		childProject = createProject(parentBasedir + FILE_SEPARATOR + relativeDirname, packaging);
		childProject.setParent(parentProject);
		return this;
	}
	
	public TemporaryMavenProject withGitRepoInParent() {
		gitRepoTargetDir = parentProject.getBasedir();
		return this;
	}
	
	public TemporaryMavenProject withGitRepoInChild() {
		gitRepoTargetDir = childProject.getBasedir();
		return this;
	}
	
	public TemporaryMavenProject withGitRepoAboveParent() {
		gitRepoTargetDir = parentProject.getBasedir().getParentFile();
		return this;
	}

	public TemporaryMavenProject create(CleanUp cleanupMode) throws IOException {
		cleanupIfRequired(cleanupMode);
		createParentDir();
		createChildDirIfRequired();
		createGitRepoInDesiredLocation();
		return this;
	}

	private void createGitRepoInDesiredLocation() throws IOException {
		FileUtils.copyDirectory(new File(PROROTYPE_GIT_REPO_PATH), new File(gitRepoTargetDir, ".git"));
	}

	private void createParentDir() throws IOException {
		FileUtils.forceMkdir(parentProject.getBasedir());
	}

	private void createChildDirIfRequired() throws IOException {
		if(childProject != null) {
			FileUtils.forceMkdir(childProject.getBasedir());
		}
	}

	private void cleanupIfRequired(CleanUp cleanupMode) throws IOException {
		if(CleanUp.CLEANUP_FIRST == cleanupMode) {
			cleanUp();
		}
	}
	
	public void cleanUp() throws IOException {
		FileUtils.deleteDirectory(parentProject.getBasedir());
	}
	
	public MavenProject getParentProject() {
		return parentProject;
	}

	public MavenProject getChildProject() {
		return childProject;
	}

	private MavenProject createProject(String basedir, String packaging) {
		MavenProject project = new MavenProject();
		project.setBasedir(new File(basedir));
		project.setPackaging(packaging);
		return project;
	}
	
	enum CleanUp {
		CLEANUP_FIRST,
		NO_CLEANUP
	}
	
}