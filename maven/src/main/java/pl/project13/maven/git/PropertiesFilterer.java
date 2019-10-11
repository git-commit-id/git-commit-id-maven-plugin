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

package pl.project13.maven.git;

import java.util.List;
import java.util.Properties;

import pl.project13.core.log.LoggerBridge;

import javax.annotation.Nullable;

public class PropertiesFilterer {

  private LoggerBridge log;

  public PropertiesFilterer(LoggerBridge log) {
    this.log = log;
  }

  public void filterNot(Properties properties, @Nullable List<String> exclusions, String prefixDot) {
    if (exclusions == null || exclusions.isEmpty()) {
      return;
    }

    properties.stringPropertyNames()
            .stream()
            .filter(key -> isOurProperty(key, prefixDot))
            .forEach(key -> {
              if (exclusions.stream()
                      .anyMatch(key::matches)) {
                log.debug("shouldExclude.apply({})", key);
                properties.remove(key);
              }
            });
  }

  public void filter(Properties properties, @Nullable List<String> inclusions, String prefixDot) {
    if (inclusions == null || inclusions.isEmpty()) {
      return;
    }

    properties.stringPropertyNames()
            .stream()
            .filter(key -> isOurProperty(key, prefixDot))
            .forEach(key -> {
              if (inclusions.stream()
                      .noneMatch(key::matches)) {
                log.debug("!shouldInclude.apply({})", key);
                properties.remove(key);
              }
            });
  }

  public static boolean isIncluded(String keyWithPrefix, @Nullable List<String> inclusions, @Nullable List<String> exclusions) {
    boolean included = true;
    if (inclusions != null && !inclusions.isEmpty()) {
      included &= inclusions.stream()
              .anyMatch(keyWithPrefix::matches);
    }
    if (exclusions != null && !exclusions.isEmpty()) {
      included &= exclusions.stream()
              .noneMatch(keyWithPrefix::matches);
    }

    return included;
  }

  private boolean isOurProperty(String key, String prefixDot) {
    return key.startsWith(prefixDot);
  }
}
