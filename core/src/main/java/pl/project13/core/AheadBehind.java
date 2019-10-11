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

package pl.project13.core;

import java.util.Objects;

public class AheadBehind {

  public static final AheadBehind NO_REMOTE = AheadBehind.of("NO_REMOTE", "NO_REMOTE");

  private final String ahead;

  private final String behind;

  private AheadBehind(String ahead, String behind) {
    this.ahead = ahead;
    this.behind = behind;
  }

  public static AheadBehind of(int ahead, int behind) {
    return new AheadBehind(String.valueOf(ahead), String.valueOf(behind));
  }

  public static AheadBehind of(String ahead, String behind) {
    return new AheadBehind(ahead, behind);
  }

  public String ahead() {
    return ahead;
  }

  public String behind() {
    return behind;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ahead, behind);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AheadBehind other = (AheadBehind) obj;

    if (!Objects.equals(ahead, other.ahead)) {
      return false;
    }
    if (!Objects.equals(behind, other.behind)) {
      return false;
    }
    return true;
  }
}