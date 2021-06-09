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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * This class represents a specific property replacement the user wants to perform.
 * For a use-case refer to https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/317.
 * @since 2.2.3
 */
public class ReplacementProperty {
  /**
   * Defines if a replacement should only be applied to a single property.
   * If left empty the replacement will be performed on all generated properties.
   */
  @Parameter
  private String property;

  /**
   * @since 2.2.4
   * Defines an additional output property suffix.
   * Note: 
   * this will only be *appended* to the current property key
   * (e.g. when the property is set to 'sample' the property
   * 'git.branch' will be transformed to 'git.branch.sample')
   * 
   * Be advised that you might want to adjust your include
   * or exclude filters which be applied after the regex validation.
   */
  @Parameter
  private String propertyOutputSuffix;

  /**
   * Token to replace.
   * This may or may not be a regular expression.
   */
  @Parameter(required = true)
  private String token;

  /**
   * Value to replace token with.
   * The text to be written over any found tokens. 
   * You can also reference grouped regex matches made in the token here by $1, $2, etc.
   */
  @Parameter(defaultValue = "")
  private String value = "";

  /**
   * Indicates if the token should be located with regular expressions. 
   */
  @Parameter(defaultValue = "true")
  private boolean regex = true;

  /**
   * Forces the plugin to evaluate the value on *every* project.
   * Note that this essentially means that the plugin *must* run for every child-project of a reactor
   * build and thus might cause some overhead (the git properties should be cached).
   *
   * For a use-case refer to https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/457.
   */
  @Parameter(defaultValue = "false")
  private boolean forceValueEvaluation = false;

  /**
   * @since 2.2.4
   * Provides the ability to perform certain string transformations before regex evaluation or after.
   */
  @Parameter
  private List<TransformationRule> transformationRules = new ArrayList<>();

  /**
   * Empty constructor
   */
  public ReplacementProperty() {
  }

  /**
   * Constructs a specific property replacement the user wants to perform.
   * @param property The source (input) property on which the replacements should be performed (e.g. {@code git.branch})
   * @param propertyOutputSuffix The property output suffix where the replacement result should be stored in (e.g. {@code git.branch-no-slashes})
   * @param token The replacement token acts as {@code needle} that will be searched in the input property (e.g. {@code ^([^\/]*)\/([^\/]*)$})
   * @param value The value acts as the text to be written over any found tokens ("replacement").
   * @param regex If {@code true} the replacement will be performed with regular expressions or,
   *             if {@code false} performs a replacement with java's string.replace-function.
   * @param forceValueEvaluation If {@code true} forces the plugin to evaluate the given value on *every* project.
   *                             This might come handy if *every* project needs a unique value and a user wants to
   *                             project specific variables like {@code project.artifactId}.
   * @param transformationRules The list of transformation-rules that should be applied during replacement.
   */
  public ReplacementProperty(String property, String propertyOutputSuffix, String token, String value, boolean regex, boolean forceValueEvaluation, List<TransformationRule> transformationRules) {
    this.property = property;
    this.propertyOutputSuffix = propertyOutputSuffix;
    this.token = token;
    this.value = value;
    this.regex = regex;
    this.forceValueEvaluation = forceValueEvaluation;
    this.transformationRules = transformationRules;
  }

  /**
   * @return The source (input) property on which the replacements should be performed (e.g. {@code git.branch})
   */
  public String getProperty() {
    return property;
  }

  /**
   * Set the source (input) property on which the replacements should be performed (e.g. {@code git.branch})
   * @param property The source (input) property
   */
  public void setProperty(String property) {
    this.property = property;
  }

  /**
   * @return The property output suffix where the replacement result should be stored in (e.g. {@code git.branch-no-slashes})
   */
  public String getPropertyOutputSuffix() {
    return propertyOutputSuffix;
  }

  /**
   * Set the property output suffix where the replacement result should be stored in (e.g. {@code git.branch-no-slashes})
   * @param propertyOutputSuffix The property output suffix
   */
  public void setPropertyOutputSuffix(String propertyOutputSuffix) {
    this.propertyOutputSuffix = propertyOutputSuffix;
  }

  /**
   * @return The replacement token acts as {@code needle} that will be searched in the input property (e.g. {@code ^([^\/]*)\/([^\/]*)$})
   */
  public String getToken() {
    return token;
  }

  /**
   * Set the replacement token
   * @param token The replacement token
   */
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * @return The value that acts as the text to be written over any found tokens ("replacement").
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value that acts as the text to be written over any found tokens ("replacement").
   * @param value The replacment value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * @return Indicator if the replacement will be performed with regular expressions ({@code true}) or,
   * if a replacement with java's string.replace-function is performed ({@code false}).
   */
  public boolean isRegex() {
    return regex;
  }

  /**
   * Sets Indicator if the replacement will be performed with regular expressions ({@code true}) or,
   * if a replacement with java's string.replace-function is performed ({@code false}).
   * @param regex Indicator
   */
  public void setRegex(boolean regex) {
    this.regex = regex;
  }

  /**
   * @return Indicator if the plugin forces to evaluate the given value on *every* project ({@code true}).
   */
  public boolean isForceValueEvaluation() {
    return forceValueEvaluation;
  }

  /**
   * Sets Indicator if the plugin forces to evaluate the given value on *every* project ({@code true}).
   * @param forceValueEvaluation Indicator if the plugin forces to evaluate the given value on *every* project ({@code true}).
   */
  public void setForceValueEvaluation(boolean forceValueEvaluation) {
    this.forceValueEvaluation = forceValueEvaluation;
  }

  /**
   * @return The list of transformation-rules that should be applied during replacement.
   */
  public List<TransformationRule> getTransformationRules() {
    return transformationRules;
  }

  /**
   * Sets the list of transformation-rules that should be applied during replacement.
   * @param transformationRules The list of transformation-rules
   */
  public void setTransformationRules(List<TransformationRule> transformationRules) {
    this.transformationRules = transformationRules;
  }
}
