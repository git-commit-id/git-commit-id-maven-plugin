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

public class AwsCodeBuildBuildServerData extends BuildServerDataProvider {

  AwsCodeBuildBuildServerData(LoggerBridge log, @Nonnull Map<String, String> env) {
    super(log,env);
  }

  /**
   * @see <a href="https://docs.aws.amazon.com/codebuild/latest/userguide/build-env-ref-env-vars.html">Environment variables in build environments</a>
   */
  public static boolean isActiveServer(Map<String, String> env) {
    return env.containsKey("CODEBUILD_BUILD_ARN");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    String buildNumber = env.getOrDefault("CODEBUILD_BUILD_NUMBER", "");
    maybePut(properties, GitCommitPropertyConstant.BUILD_NUMBER, () -> buildNumber);

    String buildArn = env.get("CODEBUILD_BUILD_ID");
    maybePut(properties, GitCommitPropertyConstant.BUILD_NUMBER_UNIQUE, () -> buildArn);
  }

  @Override
  public String getBuildBranch() {
    String environmentBasedBranch = env.getOrDefault("CODEBUILD_SOURCE_VERSION", "");
    log.info("Using environment variable based branch name. CODEBUILD_SOURCE_VERSION = {}", environmentBasedBranch);
    return environmentBasedBranch;
  }
}
