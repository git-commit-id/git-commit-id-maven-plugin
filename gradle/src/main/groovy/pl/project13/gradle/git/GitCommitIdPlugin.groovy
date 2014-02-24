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
