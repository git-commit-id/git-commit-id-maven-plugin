/*
 * This file is part of git-commit-id-maven-plugin
 * Originally invented by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-maven-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-maven-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.maven.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;

/**
 * This class encapsulates logic to locate a valid .git directory of the currently used project. If
 * it's not already specified, this logic will try to find it.
 */
public class GitDirLocator {
  final MavenProject mavenProject;
  final List<MavenProject> reactorProjects;

  /**
   * Constructor to encapsulates all references required to locate a valid .git directory
   *
   * @param mavenProject The currently used (maven) project.
   * @param reactorProjects The list of reactor projects (sub-projects) of the current (maven)
   *     project.
   */
  public GitDirLocator(MavenProject mavenProject, List<MavenProject> reactorProjects) {
    this.mavenProject = mavenProject;
    this.reactorProjects = reactorProjects;
  }

  /**
   * Attempts to lookup a valid .git directory of the currently used project.
   *
   * @param manuallyConfiguredDir A user has the ability to configure a git-directory with the
   *     {@code dotGitDirectory} configuration setting. By default it should be simply {@code
   *     ${project.basedir}/.git}
   * @return A valid .git directory, or {@code null} if none could be found under the user specified
   *     location or within the project or it's reactor projects.
   */
  @Nullable
  public File lookupGitDirectory(@Nonnull File manuallyConfiguredDir) {
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

      /*
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
  private File processGitDirFile(@Nonnull File file) {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
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
      String extractFromConfig = parts[1];
      File gitDir = resolveWorktree(new File(extractFromConfig));
      if (gitDir.isAbsolute()) {
        // gitdir value is an absolute path. Return as-is
        return gitDir;
      } else {
        // gitdir value is relative.
        return new File(file.getParentFile(), extractFromConfig);
      }
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Attempts to resolve the actual location of the .git folder for a given
   * worktree.
   * For example for a worktree like {@code a/.git/worktrees/X} structure would return {@code a/.git}.
   *
   * If the conditions for a git worktree like file structure are met simply return the provided
   * argument as is.
   */
  static File resolveWorktree(File fileLocation) {
    Path parent = fileLocation.toPath().getParent();
    if (parent == null) {
      return fileLocation;
    }
    if (parent.endsWith(Path.of(".git", "worktrees"))) {
      return parent.getParent().toFile();
    }
    return fileLocation;
  }

  /**
   * Helper method to validate that the specified {@code File} is an existing directory.
   *
   * @param fileLocation The {@code File} that should be checked if it's actually an existing
   *     directory.
   * @return {@code true} if the specified {@code File} is an existing directory, {@false}
   *     otherwise.
   */
  private static boolean isExistingDirectory(@Nullable File fileLocation) {
    return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory();
  }
}
