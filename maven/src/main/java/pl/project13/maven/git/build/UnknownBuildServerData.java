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

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Properties;

public class UnknownBuildServerData extends BuildServerDataProvider {
  public UnknownBuildServerData(@Nonnull LoggerBridge log, @Nonnull Map<String, String> env) {
    super(log, env);
  }

  @Override
  void loadBuildNumber(@Nonnull Properties properties) {
  }

  @Override
  public String getBuildBranch() {
    return "";
  }
}
