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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Encapsulates logic to locate a valid .git directory
 * @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 */
public class GitDirLocator {
  final MavenProject mavenProject; 
  final List<MavenProject> reactorProjects;
  
  public GitDirLocator(MavenProject mavenProject, List<MavenProject> reactorProjects){
	  this.mavenProject = mavenProject;
	  this.reactorProjects = reactorProjects;
  }
	
  @Nullable
  public File lookupGitDirectory(File manuallyConfiguredDir) {

    if (manuallyConfiguredDir != null && manuallyConfiguredDir.exists()) {
    	
      // If manuallyConfiguredDir is a directory then we can use it as the git path. 
      if (manuallyConfiguredDir.isDirectory()) {
          return manuallyConfiguredDir;
      }
      
      // If the path exists but is not a directory it might be a git submodule "gitdir" link.
      File gitDirLinkPath = processGitDirFile(manuallyConfiguredDir);
      
      // If the linkPath was found from the file and it exists then use it.
      if (isExistingDirectory(gitDirLinkPath))
      {
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
   * Search up all the maven parent project heirarchy until a .git
   * directory is found.
   * 
   * @return File the location of the .git directory or NULL if none found.
   */
  private File findProjectGitDirectory()
  {
    MavenProject project = this.mavenProject;
    
    while (project != null) {
      File dir = getProjectGitDir(project);
      
      if (isExistingDirectory(dir)) {
        return dir;
      }

      /**
       * project.getParent always returns NULL for me, but if getParentArtifact returns
       * not null then there is actually a parent - seems like a bug in maven to me.
       */
      if (project.getParent() == null && project.getParentArtifact() != null) {
    	  project = getReactorParentProject(project);
      } else {
    	  // Get the parent, or NULL if no parent AND no parentArtifact.
    	  project = project.getParent();
      }
    }

    return null;
  }
  
  /**
   * Find a project in the reactor by its artifact, I'm new to maven coding
   * so there may be a better way to do this, it would not be necessary
   * if project.getParent() actually worked.
   * 
   * @param MavenProject project
   * @return MavenProject parent project or NULL if no parent available
   */
  private MavenProject getReactorParentProject(MavenProject project) {
    Artifact parentArtifact = project.getParentArtifact();
    
    if (parentArtifact != null) {
  	  for (MavenProject reactorProject : this.reactorProjects) {
  		  if (reactorProject.getArtifactId().equals(parentArtifact.getArtifactId())){
  			  return reactorProject;
  		  }
  	  }
    }
    
    return null;
  }
  
  /**
   * Load a ".git" git submodule file and read the gitdir path from it.
   * 
   * @param file
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

        // Separate the key and the value in the string.
        String[] parts = line.split(": ");
        
        // If we don't have 2 parts or if the key is not gitdir then give up.
        if (parts.length != 2 ||  !parts[0].equals("gitdir")) {
          return null;
        }

        // All seems ok so return the "gitdir" value read from the file.
        return new File(parts[1]);
      }
      catch (FileNotFoundException e)
      {
        return null;
      }
  	  finally
  	  {
  	    if (reader != null)
  	    {
  	      reader.close();
  	    }
  	  }
    }
  	catch (IOException e)
  	{
      return null;
  	}
  }
  
  @NotNull
  private File getProjectGitDir(@NotNull MavenProject mavenProject) {
    // FIXME Shouldn't this look at the dotGitDirectory property (if set) for the given project?
    return new File(mavenProject.getBasedir(), Constants.DOT_GIT);
  }

  public boolean isExistingDirectory(@Nullable File fileLocation) {
    return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory();
  }
}
