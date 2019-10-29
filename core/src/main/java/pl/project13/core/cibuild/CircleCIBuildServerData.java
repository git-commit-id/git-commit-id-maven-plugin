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

import pl.project13.core.GitCommitPropertyConstant;
import pl.project13.core.log.LoggerBridge;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Properties;

public class CircleCIBuildServerData extends BuildServerDataProvider {

  CircleCIBuildServerData(LoggerBridge log, @Nonnull Map<String, String> env) {
    super(log,env);
  }

  /**
   * @see <a href="https://circleci.com/docs/2.0/env-vars/#built-in-environment-variables">CircleCIBuiltInVariables</a>
   */
  public static boolean isActiveServer(Map<String, String> env) {
    return env.containsKey("CIRCLECI");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    String buildNumber = env.get("CIRCLE_BUILD_NUM");
    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber == null ? "" : buildNumber);
  }

  @Override
  public String getBuildBranch() {
    String environmentBasedBranch = env.get("CIRCLE_BRANCH");
    log.info("Using environment variable based branch name. CIRCLE_BRANCH = {}", environmentBasedBranch);
    return environmentBasedBranch;
  }
}
