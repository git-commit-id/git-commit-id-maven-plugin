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

package pl.project13.maven.git.build;

import pl.project13.core.log.LoggerBridge;
import pl.project13.core.GitCommitPropertyConstant;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class TeamCityBuildServerData extends BuildServerDataProvider {

  private final Properties teamcitySystemProperties = new Properties();

  TeamCityBuildServerData(@Nonnull LoggerBridge log, @Nonnull Map<String, String> env) {
    super(log, env);
    if (isActiveServer(env)) {
      //https://confluence.jetbrains.com/display/TCD18/Predefined+Build+Parameters
      try {
        teamcitySystemProperties.load(new FileInputStream(env.get("TEAMCITY_BUILD_PROPERTIES_FILE")));
      } catch (IOException | NullPointerException e) {
        log.error("Failed to retrieve Teamcity properties file", e);
      }
    }
  }

  /**
   * @see <a href=https://confluence.jetbrains.com/display/TCD18/Predefined+Build+Parameters#PredefinedBuildParameters-ServerBuildProperties>TeamCity</a>
   */
  public static boolean isActiveServer(@Nonnull Map<String, String> env) {
    return env.containsKey("TEAMCITY_VERSION");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    String buildNumber = env.get("BUILD_NUMBER");
    String buildNumberUnique = teamcitySystemProperties.getProperty("teamcity.build.id");

    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber == null ? "" : buildNumber);
    put(properties,
        GitCommitPropertyConstant.BUILD_NUMBER_UNIQUE,
        buildNumberUnique == null ? "" : buildNumberUnique);
  }

  @Override
  public String getBuildBranch() {
    //there is no branch environment variable in TeamCity 10 or earlier
    String environmentBasedBranch = teamcitySystemProperties.getProperty("teamcity.build.branch");
    log.info("Using property file based branch name. teamcity.build.branch = {}",
        environmentBasedBranch);
    return environmentBasedBranch;
  }
}
