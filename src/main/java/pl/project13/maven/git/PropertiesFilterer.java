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

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import pl.project13.maven.git.log.LoggerBridge;

public class PropertiesFilterer {

  private LoggerBridge log;

  public PropertiesFilterer(LoggerBridge log) {
    this.log = log;
  }

  public void filterNot(Properties properties, @Nullable List<String> exclusions, String prefixDot) {
    if (exclusions == null || exclusions.isEmpty()) {
      return;
    }

    List<Predicate<CharSequence>> excludePredicates = Lists.transform(exclusions, 
        new Function<String, Predicate<CharSequence>>() {
            @Override
            public Predicate<CharSequence> apply(String exclude) {
              return Predicates.containsPattern(exclude);
            }
        });

    Predicate<CharSequence> shouldExclude = Predicates.alwaysFalse();
    for (Predicate<CharSequence> predicate : excludePredicates) {
      shouldExclude = Predicates.or(shouldExclude, predicate);
    }

    for (String key : properties.stringPropertyNames()) {
      if (isOurProperty(key, prefixDot) && shouldExclude.apply(key)) {
        log.debug("shouldExclude.apply({}) = {}", key, shouldExclude.apply(key));
        properties.remove(key);
      }
    }
  }

  public void filter(Properties properties, @Nullable List<String> inclusions, String prefixDot) {
    if (inclusions == null || inclusions.isEmpty()) {
      return;
    }

    List<Predicate<CharSequence>> includePredicates = Lists.transform(inclusions,
        new Function<String, Predicate<CharSequence>>() {
            @Override
            public Predicate<CharSequence> apply(String exclude) {
              return Predicates.containsPattern(exclude);
            }
        });

    Predicate<CharSequence> shouldInclude = Predicates.alwaysFalse();
    for (Predicate<CharSequence> predicate : includePredicates) {
      shouldInclude = Predicates.or(shouldInclude, predicate);
    }

    for (String key : properties.stringPropertyNames()) {
      if (isOurProperty(key, prefixDot) && !shouldInclude.apply(key)) {
        log.debug("!shouldInclude.apply({}) = {}", key, shouldInclude.apply(key));
        properties.remove(key);
      }
    }
  }

  private boolean isOurProperty(String key, String prefixDot) {
    return key.startsWith(prefixDot);
  }
}
