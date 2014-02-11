package pl.project13.gradle.git

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger

import pl.project13.maven.git.GitDataLoader
import pl.project13.maven.git.GitDataLoaderException
import pl.project13.maven.git.GitDescribeConfig
import pl.project13.maven.git.GitDataLoader.GitDataLoaderBuilder


/**
 * Simple, but hopefully works.
 *
 */
class GitCommitIdPlugin implements Plugin<Project> {

	private File rootDirectory;
	private Logger logger;

	@Override
	public void apply(Project project) {
		rootDirectory = project.getRootDir();
		logger = project.getLogger();
		
		String prefix = "git"
		String prefixDot = prefix + ".";
		
		Properties properties = new Properties();
		
		  GitDataLoader gitDataLoader = new GitDataLoaderBuilder()
										  .withWorkingTreeDirectory(rootDirectory)
//										  .withAbbrevationLength(abbrevLength)
//										  .withDateFormat(dateFormat)
										  .withGitDescribe(new GitDescribeConfig())
										  .withPrefixDot(prefixDot)
//										  .withLoggerBridge(loggerBridge)
										  .build();
		  
		  try {
			gitDataLoader.loadGitData(properties);
		} catch (GitDataLoaderException e) {
			throw new GradleException(e.getMessage(), e);
		}

		System.getProperties().putAll(properties)
	}

}
