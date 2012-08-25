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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Encapsulates logic to locate a valid .git directory
 * @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 */
public class GitDirLocator {

  @Nullable
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

  @NotNull
  private File currentProjectGitDir(@NotNull MavenProject mavenProject) {
    return new File(mavenProject.getBasedir(), Constants.DOT_GIT);
  }

  public static boolean isExistingDirectory(@Nullable File fileLocation) {
    return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory();
  }


}