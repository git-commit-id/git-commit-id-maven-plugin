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

public class TransformationRule {
  /**
   * Determines when the transformation should be taken place.
   * Currently supported is
   *   - BEFORE_REGEX
   *   - AFTER_REGEX
   */
  @Parameter(required = true)
  private String apply;

  private ApplyEnum applyRule;

  protected enum ApplyEnum {
    BEFORE,
    AFTER,
    ;
  };

  /**
   * Determines the action that should be performed as transformation.
   * Currently supported is
   *   - LOWER_CASE
   *   - UPPER_CASE
   */
  @Parameter(required = true)
  private String action;

  private ActionEnum actionRule;

  protected enum ActionEnum {
    LOWER_CASE {
        @Override
        protected String perform(String input) {
          if(input != null) {
            return input.toLowerCase();
          }
          return input;
        }
      },
    UPPER_CASE {
      @Override
      protected String perform(String input) {
        if(input != null) {
          return input.toUpperCase();
        }
        return null;
      }
    },
    ;
    protected abstract String perform(String input);
  }

  public TransformationRule(){}

  public TransformationRule(String apply, String action) {
    this(ApplyEnum.valueOf(apply), ActionEnum.valueOf(action));
    this.apply = apply;
    this.action = action;
  }

  protected TransformationRule(ApplyEnum applyRule, ActionEnum actionRule) {
    this.applyRule = applyRule;
    this.actionRule = actionRule;
  }

  public String getApply() {
    return apply;
  }

  public void setApply(String apply) {
    this.applyRule = ApplyEnum.valueOf(apply);
    this.apply = apply;
  }

  public ApplyEnum getApplyRule() {
    if(applyRule == null) {
      throw new IllegalStateException("The parameter 'apply' for TransformationRule is missing or invalid");
    }
    return applyRule;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.actionRule = ActionEnum.valueOf(action);
    this.action = action;
  }

  public ActionEnum getActionRule() {
    if(actionRule == null) {
      throw new IllegalStateException("The parameter 'action' for TransformationRule is missing or invalid");
    }
    return actionRule;
  }
}
