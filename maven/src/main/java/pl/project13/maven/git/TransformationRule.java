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

import org.apache.maven.plugins.annotations.Parameter;

public class TransformationRule extends pl.project13.git.api.TransformationRule {
  /**
   * Determines when the transformation should be taken place.
   * Currently supported is
   *   - BEFORE_REGEX
   *   - AFTER_REGEX
   */
  @Parameter(required = true)
  private String apply;

  /**
   * Determines the action that should be performed as transformation.
   * Currently supported is
   *   - LOWER_CASE
   *   - UPPER_CASE
   */
  @Parameter(required = true)
  private String action;

  public TransformationRule(){}

  public TransformationRule(String apply, String action) {
    super(apply, action);
    this.apply = apply;
    this.action = action;
  }

  public String getApply() {
    return apply;
  }

  public void setApply(String apply) {
    super.setApplyRule(apply);
    this.apply = apply;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    super.setActionRule(action);
    this.action = action;
  }
}
