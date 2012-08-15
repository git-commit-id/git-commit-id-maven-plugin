package pl.project13.maven.git;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;

import java.io.File;

/**
 * Encapsulates logic to locate a valid .git directory
 * @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 */
public class GitDirLocator {

  public File lookupGitDirectory(MavenProject project, File manualyConfiguredDir) throws MojoExecutionException {

    if (isExistingDirectory(manualyConfiguredDir)) {
      return manualyConfiguredDir;
    }

    MavenProject mavenProject = project;

    File dir;
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