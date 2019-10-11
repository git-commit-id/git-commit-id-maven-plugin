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

import pl.project13.maven.git.GitCommitPropertyConstant;
import pl.project13.core.log.LoggerBridge;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;

public class HudsonJenkinsBuildServerData extends BuildServerDataProvider {

  HudsonJenkinsBuildServerData(@Nonnull LoggerBridge log, @Nonnull Map<String, String> env) {
    super(log, env);
  }

  /**
   * @see <a href="https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables">JenkinsSetEnvironmentVariables</a>
   */
  public static boolean isActiveServer(@Nonnull Map<String, String> env) {
    return env.containsKey("JENKINS_URL") || env.containsKey("JENKINS_HOME") ||
        env.containsKey("HUDSON_URL") || env.containsKey("HUDSON_HOME");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    String buildNumber = env.get("BUILD_NUMBER");

    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber == null ? "" : buildNumber);
  }

  @Override
  public String getBuildBranch() {
    String environmentBasedLocalBranch = env.get("GIT_LOCAL_BRANCH");
    if (!isNullOrEmpty(environmentBasedLocalBranch)) {
      log.info("Using environment variable based branch name. GIT_LOCAL_BRANCH = {}",
          environmentBasedLocalBranch);
      return environmentBasedLocalBranch;
    }
    String environmentBasedBranch = env.get("GIT_BRANCH");
    log.info("Using environment variable based branch name. GIT_BRANCH = {}",
        environmentBasedBranch);
    return environmentBasedBranch;
  }
}
