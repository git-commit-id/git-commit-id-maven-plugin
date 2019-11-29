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

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;

public class GitHubBuildServerData extends BuildServerDataProvider {

    private static final String BRANCH_REF_PREFIX = "refs/heads/";
    private static final String PULL_REQUEST_REF_PREFIX = "refs/pull/";

    GitHubBuildServerData(LoggerBridge log, @Nonnull Map<String, String> env) {
    super(log,env);
  }

  /**
   * @see <a href="https://help.github.com/en/actions/automating-your-workflow-with-github-actions/using-environment-variables">GitHubActionsUsingEnvironmentVariables</a>
   */
  public static boolean isActiveServer(Map<String, String> env) {
    return env.containsKey("GITHUB_ACTIONS");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    // This information is not reliably available on GitHub Actions
  }

  @Override
  public String getBuildBranch() {
    String environmentBasedBranchKey = "GITHUB_REF";
    String environmentBasedBranch = env.get(environmentBasedBranchKey);
    if (!isNullOrEmpty(environmentBasedBranch)) {
        if (environmentBasedBranch.startsWith(BRANCH_REF_PREFIX)) {
        environmentBasedBranch = environmentBasedBranch.substring(BRANCH_REF_PREFIX.length());
      } else if (environmentBasedBranch.startsWith(PULL_REQUEST_REF_PREFIX)) {
        environmentBasedBranchKey = "GITHUB_HEAD_REF";
        environmentBasedBranch = env.get(environmentBasedBranchKey);
      }
    }
    log.info("Using environment variable based branch name. {} = {}",
            environmentBasedBranchKey, environmentBasedBranch);
    return environmentBasedBranch;
  }
}
