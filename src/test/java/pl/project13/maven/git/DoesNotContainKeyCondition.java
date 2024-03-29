/*
 * This file is part of git-commit-id-maven-plugin
 * Originally invented by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-maven-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-maven-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.maven.git;

import java.util.Map;
import javax.annotation.Nonnull;
import org.assertj.core.api.Condition;

class DoesNotContainKeyCondition extends Condition<Map<?, ?>> {

  private String key;

  public DoesNotContainKeyCondition(String key) {
    this.key = key;
  }

  @Override
  public boolean matches(@Nonnull Map<?, ?> map) {
    boolean containsKey = map.containsKey(key);
    if (containsKey) {
      System.out.println(String.format("Map contained [%s] key! Map is: %s", key, map));
      throw new RuntimeException(String.format("Map contained [%s] key! Map is: %s", key, map));
    }
    return true;
  }
}
