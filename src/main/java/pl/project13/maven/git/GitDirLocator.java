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

import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.List;

/**
 * Encapsulates logic to locate a valid .git directory.
 *
 */
public class GitDirLocator {
  final MavenProject mavenProject;
  final List<MavenProject> reactorProjects;

  public GitDirLocator(MavenProject mavenProject, List<MavenProject> reactorProjects) {
    this.mavenProject = mavenProject;
    this.reactorProjects = reactorProjects;
  }

  @Nullable
  public File lookupGitDirectory(@NotNull File manuallyConfiguredDir) {
    if (manuallyConfiguredDir.exists()) {

      // If manuallyConfiguredDir is a directory then we can use it as the git path.
      if (manuallyConfiguredDir.isDirectory()) {
        return manuallyConfiguredDir;
      }

      // If the path exists but is not a directory it might be a git submodule "gitdir" link.
      File gitDirLinkPath = processGitDirFile(manuallyConfiguredDir);

      // If the linkPath was found from the file and it exists then use it.
      if (isExistingDirectory(gitDirLinkPath)) {
        return gitDirLinkPath;
      }

      /**
       * FIXME: I think we should fail here because a manual path was set and it was not found
       * but I'm leaving it falling back to searching for the git path because that is the current
       * behaviour - Unluckypixie.
       */
    }

    return findProjectGitDirectory();
  }

  /**
   * Search up all the maven parent project hierarchy until a .git directory is found.
   *
   * @return File which represents the location of the .git directory or NULL if none found.
   */
  @Nullable
  private File findProjectGitDirectory() {
    if (this.mavenProject == null) {
      return null;
    }

    File basedir = mavenProject.getBasedir();
    while (basedir != null) {
      File gitdir = new File(basedir, Constants.DOT_GIT);
      if (gitdir.exists()) {
        if (gitdir.isDirectory()) {
          return gitdir;
        } else if (gitdir.isFile()) {
          return processGitDirFile(gitdir);
        } else {
          return null;
        }
      }
      basedir = basedir.getParentFile();
    }
    return null;
  }

  /**
   * Load a ".git" git submodule file and read the gitdir path from it.
   *
   * @return File object with path loaded or null
   */
  private File processGitDirFile(@NotNull File file) {
    try {
      BufferedReader reader = null;

      try {
        reader = new BufferedReader(new FileReader(file));

        // There should be just one line in the file, e.g.
        // "gitdir: /usr/local/src/parentproject/.git/modules/submodule"
        String line = reader.readLine();
        if (line == null) {
          return null;
        }
        // Separate the key and the value in the string.
        String[] parts = line.split(": ");

        // If we don't have 2 parts or if the key is not gitdir then give up.
        if (parts.length != 2 || !parts[0].equals("gitdir")) {
          return null;
        }

        // All seems ok so return the "gitdir" value read from the file.
        File gitDir = new File(parts[1]);
        if (gitDir.isAbsolute()) {
          // gitdir value is an absolute path. Return as-is
          return gitDir;
        } else {
          // gitdir value is relative.
          return new File(file.getParentFile(), parts[1]);
        }
      } catch (FileNotFoundException e) {
        return null;
      } finally {
        if (reader != null) {
          reader.close();
        }
      }
    } catch (IOException e) {
      return null;
    }
  }

  private static boolean isExistingDirectory(@Nullable File fileLocation) {
    return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory();
  }
}
