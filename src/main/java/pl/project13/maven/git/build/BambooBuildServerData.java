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
import pl.project13.maven.git.log.LoggerBridge;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

public class BambooBuildServerData extends BuildServerDataProvider {

  BambooBuildServerData(LoggerBridge log, @Nonnull Map<String, String> env) {
    super(log, env);
  }

  public static boolean isActiveServer(Map<String, String> env) {
    return env.containsKey("bamboo_buildKey") ||
            env.containsKey("bamboo.buildKey") ||
            env.containsKey("BAMBOO_BUILDKEY");
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
    String buildNumber = Optional.ofNullable(
            env.get("bamboo.buildNumber")).orElseGet(() -> env.get("BAMBOO_BUILDNUMBER"));

    put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber == null ? "" : buildNumber);
  }

  @Override
  public String getBuildBranch() {
    String environmentBasedKey = null;
    String environmentBasedBranch = null;

    for (String envKey : Arrays.asList(
            "bamboo.planRepository.branchName",
            "bamboo.planRepository.<position>.branchName",
            "BAMBOO_PLANREPOSITORY_BRANCH")) {
      environmentBasedBranch = env.get(envKey);
      if (environmentBasedBranch != null) {
        environmentBasedKey = envKey;
        break;
      }
    }
    log.info("Using environment variable based branch name. {} = {}", environmentBasedKey, environmentBasedBranch);
    return environmentBasedBranch;
  }
}
