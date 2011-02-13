package pl.project13.maven;

import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Date: 2/13/11
 *
 * @author Konrad Malawski
 */
public class GitCommitHashMojoTest extends PlexusTestCase {

  GitCommitHashMojo mojo;

  public void setUp() throws Exception {
    mojo = new GitCommitHashMojo();
    mojo.setBasedir(new File("/home/ktoso/coding/maven-plugins/git-commit-hash-plugin/.git/"));
    mojo.setPrefix("git");
    mojo.setDateFormat("dd.MM.yyyy '@' HH:mm:ss z");
    mojo.setVerbose(true);

    super.setUp();
  }

  public void testExecute() throws Exception {
    mojo.execute();

    Properties properties = mojo.getProperties();
    assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.email"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
    assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));
  }

}
