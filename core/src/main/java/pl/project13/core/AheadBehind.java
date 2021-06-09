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

/**
 * A local git repository can either be "ahead", or "behind" in the number of commits
 * relative to the remote repository. This class tracks the amount of commits the local git repository
 * is "behind", or "ahead" relative to it's remote.
 */
public class AheadBehind {

  /**
   * Indication that we could not find a remote repository to calculate a "behind", or "ahead" relation.
   */
  public static final AheadBehind NO_REMOTE = AheadBehind.of("NO_REMOTE", "NO_REMOTE");

  private final String ahead;

  private final String behind;

  /**
   * Constructor for a "AheadBehind"-object.
   * @param ahead Number of commits the local repository is "ahead" in relation to it's remote.
   * @param behind Number of commits the local repository is "behind" in relation to it's remote.
   */
  private AheadBehind(String ahead, String behind) {
    this.ahead = ahead;
    this.behind = behind;
  }

  /**
   * Constructor for a "AheadBehind"-object.
   * @param ahead Number of commits the local repository is "ahead" in relation to it's remote.
   * @param behind Number of commits the local repository is "behind" in relation to it's remote.
   *
   * @return a "AheadBehind"-object.
   */
  public static AheadBehind of(int ahead, int behind) {
    return new AheadBehind(String.valueOf(ahead), String.valueOf(behind));
  }

  /**
   * Constructor for a "AheadBehind"-object.
   * @param ahead Number of commits the local repository is "ahead" in relation to it's remote.
   * @param behind Number of commits the local repository is "behind" in relation to it's remote.
   *
   * @return a "AheadBehind"-object.
   */
  public static AheadBehind of(String ahead, String behind) {
    return new AheadBehind(ahead, behind);
  }

  /**
   * @return Number of commits the local repository is "ahead" in relation to it's remote.
   */
  public String ahead() {
    return ahead;
  }

  /**
   * @return Number of commits the local repository is "behind" in relation to it's remote.
   */
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