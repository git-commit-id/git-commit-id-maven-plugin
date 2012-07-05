package pl.project13.maven.git;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;

public class GitDirLocator {

	public File lookupGitDirectory(MavenProject project, File dotGitDirectory) throws MojoExecutionException {
		if (isExistingDirectory(dotGitDirectory)) {
			return dotGitDirectory;
		}
		MavenProject mavenProject = project;
		File dir = null;
		while (mavenProject != null) {
			dir = currentProjectGitDir(mavenProject);
			if (isExistingDirectory(dir)) {
				return dir;
			}
			if (mavenProject.getBasedir() != null) {
				dir = new File(mavenProject.getBasedir().getParentFile(), Constants.DOT_GIT);
				if (isExistingDirectory(dir)) {
					return dir;
				}
			}
			mavenProject = mavenProject.getParent();
		}
		return null;
	}

	private File currentProjectGitDir(MavenProject mavenProject) {
		return new File(mavenProject.getBasedir(), Constants.DOT_GIT);
	}

	public static boolean isExistingDirectory(File fileLocation) {
		return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory();
	}


}
