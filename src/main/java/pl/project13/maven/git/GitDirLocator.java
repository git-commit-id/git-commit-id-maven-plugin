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

import com.google.common.base.Optional;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.project13.maven.git.log.LoggerBridge;

import java.io.*;
import java.util.List;

/**
 * Encapsulates logic to locate a valid .git directory.
 *
 * @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 */
public class GitDirLocator {
  @NotNull
  final LoggerBridge loggerBridge;
  final MavenProject mavenProject;
  final List<MavenProject> reactorProjects;
  final boolean stopAtFileSystemBoundary;

  public GitDirLocator(MavenProject mavenProject, List<MavenProject> reactorProjects, LoggerBridge loggerBridge,
                       boolean stopAtFileSystemBoundary) {
    this.mavenProject = mavenProject;
    this.reactorProjects = reactorProjects;
    this.loggerBridge = loggerBridge;
    this.stopAtFileSystemBoundary = stopAtFileSystemBoundary;
  }

  @Nullable
  public File lookupGitDirectory(@NotNull File manuallyConfiguredDir) {
    if (manuallyConfiguredDir.exists()) {
      return findProjectGitDirectory(manuallyConfiguredDir);
    } else {
      return findProjectGitDirectory();
    }
  }

  private File findProjectGitDirectory(File gitFileOrFolder) {
    if (gitFileOrFolder.exists()) {
      if (gitFileOrFolder.isDirectory()) {
        return gitFileOrFolder;
      } else {
        return processGitDirFile(gitFileOrFolder);
      }
    }
    return null;
  }

  /**
   * Search up all the maven parent project heirarchy until a .git
   * directory is found.
   *
   * @return File which represents the location of the .git directory or NULL if none found.
   */
  @Nullable
  private File findProjectGitDirectory() {
    MavenProject currentProject = this.mavenProject;

    while (currentProject != null) {
      File projectDir = currentProject.getBasedir();
      log("Searching for .git folder in %s", projectDir.getAbsolutePath());
      File dir = findProjectGitDirectory(appendDotGit(projectDir));

      if (isExistingDirectory(dir)) {
        return dir;
      } else {
        /**
         * project.getParent always returns NULL for me, but if getParentArtifact returns
         * not null then there is actually a parent - seems like a bug in maven to me.
         */
        if (currentProject.getParent() == null && currentProject.getParentArtifact() != null) {
          Optional<MavenProject> maybeFoundParentProject = getReactorParentProject(currentProject);

          if (maybeFoundParentProject.isPresent())
            currentProject = maybeFoundParentProject.get();

        } else {
          // Get the parent, or NULL if no parent AND no parentArtifact.
          currentProject = currentProject.getParent();
        }
      }
    }
    if (stopAtFileSystemBoundary) {
      log("Searching until filesytem boundary is hit");
      File dir = mavenProject.getBasedir();
      while(dir.getParentFile() != null) {
        log("Searching for .git folder in %s", dir.getAbsolutePath());
        File gitDir = findProjectGitDirectory(appendDotGit(dir));
        if (gitDir != null){
          return gitDir;
        }
        dir = dir.getParentFile();
      }
    }

    return null;
  }



  /**
   * Find a project in the reactor by its artifact, I'm new to maven coding
   * so there may be a better way to do this, it would not be necessary
   * if project.getParent() actually worked.
   *
   * @return MavenProject parent project or NULL if no parent available
   */
  private Optional<MavenProject> getReactorParentProject(@NotNull MavenProject project) {
    Artifact parentArtifact = project.getParentArtifact();

    if (parentArtifact != null) {
      for (MavenProject reactorProject : this.reactorProjects) {
        if (reactorProject.getArtifactId().equals(parentArtifact.getArtifactId())) {
          return Optional.of(reactorProject);
        }
      }
    }

    return Optional.absent();
  }

  /**
   * Load a ".git" git submodule file and read the gitdir path from it.
   *
   * @return File object with path loaded or null
   */
  private File processGitDirFile(@NotNull File file) {
    log("Processing submodule %s", file.getAbsolutePath());
    try {
      BufferedReader reader = null;

      try {
        reader = new BufferedReader(new FileReader(file));

        // There should be just one line in the file, e.g.
        // "gitdir: /usr/local/src/parentproject/.git/modules/submodule"
        String line = reader.readLine();

        // Separate the key and the value in the string.
        String[] parts = line.split(": ");

        // If we don't have 2 parts or if the key is not gitdir then give up.
        if (parts.length != 2 || !parts[0].equals("gitdir")) {
          return null;
        }

        // All seems ok so return the "gitdir" value read from the file.
        File found = new File(file.getParentFile(), parts[1]);
        log("Found git root from .git file: %s", found.getAbsolutePath());
        if (isExistingDirectory(found)) {
          return found;
        } else {
          log("Folder does not exist");
        }
      } catch (FileNotFoundException e) {

      } finally {
        if (reader != null) {
          reader.close();
        }
      }
    } catch (IOException e) {

    }
    return null;
  }

  @NotNull
  private static File appendDotGit(@NotNull File directory) {
    // FIXME Shouldn't this look at the dotGitDirectory property (if set) for the given project?
    return new File(directory, Constants.DOT_GIT);
  }

  private static boolean isExistingDirectory(@Nullable File fileLocation) {
    return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory();
  }

  void log(String message, String... interpolations) {
    loggerBridge.log(GitCommitIdMojo.logPrefix + message, (Object[]) interpolations);
  }
}
