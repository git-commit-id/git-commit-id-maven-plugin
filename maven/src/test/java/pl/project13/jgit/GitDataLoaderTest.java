package pl.project13.jgit;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;

import pl.project13.maven.git.AvailableGitTestRepo;
import pl.project13.maven.git.FileSystemMavenSandbox;
import pl.project13.maven.git.GitDataLoader;
import pl.project13.maven.git.GitDataLoader.GitDataLoaderBuilder;
import pl.project13.maven.git.GitDataLoaderException;
import pl.project13.maven.git.GitIntegrationTest;

/**
 * Tests for {@link GitDataLoader} and {@link GitDataLoaderBuilder}.
 * 
 * @author jbellmann
 *
 */
public class GitDataLoaderTest extends GitIntegrationTest {

	final static String PROJECT_NAME = "my-jar-project";
	final int ABBREV_LENGTH = 8;
	
	@Override
	@Before
	public void setUp() throws Exception {
	  super.setUp();
	
	  mavenSandbox
	      .withParentProject(PROJECT_NAME, "jar")
	      .withNoChildProject()
	      .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
	      .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);
	}
	
	@Test
	public void test() throws IOException, GitDataLoaderException{
		System.out.println("WHERE IS THE REPOSITORY?");
		String path = mavenSandbox.getSandboxDir().getAbsolutePath();
		System.out.println(path);
		File workingTreeDirectory = mavenSandbox.getSandboxDir().getAbsoluteFile();
		
		GitDataLoaderBuilder dataLoaderBuilder = new GitDataLoaderBuilder();
		GitDataLoader gitDataLoader = dataLoaderBuilder.withWorkingTreeDirectory(workingTreeDirectory).withAbbrevationLength(ABBREV_LENGTH).build();
		Properties props = new Properties();
		gitDataLoader.loadGitData(props);
		System.out.println(props);
		String abbrev = props.getProperty("gitcommit.id.abbrev");
		Assertions.assertThat(abbrev.length()).isEqualTo(ABBREV_LENGTH);
	}

}
