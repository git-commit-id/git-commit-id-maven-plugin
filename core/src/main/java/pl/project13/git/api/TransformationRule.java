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

package pl.project13.git.api;

public class TransformationRule {
	private ApplyEnum applyRule;

  public enum ApplyEnum {
    BEFORE,
    AFTER,
    ;
  };

  private ActionEnum actionRule;

  public enum ActionEnum {
    LOWER_CASE {
        @Override
        public String perform(String input) {
          if(input != null) {
            return input.toLowerCase();
          }
          return input;
        }
      },
    UPPER_CASE {
      @Override
      public String perform(String input) {
        if(input != null) {
          return input.toUpperCase();
        }
        return null;
      }
    },
    ;
    public abstract String perform(String input);
  }

  protected TransformationRule(){}

  protected TransformationRule(String apply, String action) {
    this(ApplyEnum.valueOf(apply), ActionEnum.valueOf(action));
  }

  private TransformationRule(ApplyEnum applyRule, ActionEnum actionRule) {
    this.applyRule = applyRule;
    this.actionRule = actionRule;
  }

  protected void setApplyRule(String apply) {
    this.applyRule = ApplyEnum.valueOf(apply);
  }

  public ApplyEnum getApplyRule() {
    return applyRule;
  }

  protected void setActionRule(String action) {
    this.actionRule = ActionEnum.valueOf(action);
  }

  public ActionEnum getActionRule() {
    return actionRule;
  }
}
