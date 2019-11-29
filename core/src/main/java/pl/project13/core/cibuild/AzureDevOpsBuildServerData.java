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

package pl.project13.core.cibuild;

import pl.project13.core.log.LoggerBridge;
import pl.project13.core.GitCommitPropertyConstant;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Properties;

public class AzureDevOpsBuildServerData extends BuildServerDataProvider {

  AzureDevOpsBuildServerData(@Nonnull LoggerBridge log, @Nonnull Map<String, String> env) {
    super(log, env);
  }

  /**
   * @see <a href="https://docs.microsoft.com/en-us/azure/devops/pipelines/build/variables?view=azure-devops&tabs=yaml#build-variables">Azure DevOps - Build variables</a>
   */
  public static boolean isActiveServer(@Nonnull Map<String, String> env) {
    return env.containsKey("AZURE_HTTP_USER_AGENT");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    String buildNumber = env.getOrDefault("BUILD_BUILDNUMBER", "");

    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber);
  }

  @Override
  public String getBuildBranch() {
    String environmentBasedBuildSourceBranchName = env.get("BUILD_SOURCEBRANCHNAME");
    log.info("Using environment variable based branch name. BUILD_SOURCEBRANCHNAME = {}", environmentBasedBuildSourceBranchName);
    return environmentBasedBuildSourceBranchName;
  }
}
