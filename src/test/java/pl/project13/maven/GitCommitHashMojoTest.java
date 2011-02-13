package pl.project13.maven;

import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * Date: 2/13/11
 *
 * @author Konrad Malawski
 */
public class GitCommitHashMojoTest extends PlexusTestCase {

  GitCommitHashMojo mojo;

  public void setUp() throws Exception {
    mojo = new GitCommitHashMojo();
    super.setUp();
  }

  public void testExecute() throws Exception {
    mojo.setBasedir(new File("/home/ktoso/coding/maven-plugins/git-commit-hash-plugin/"));
    mojo.setPrefix("git");

    mojo.execute();


    System.out.println(mojo);
//    mojo.get
  }
}
