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

package pl.project13.maven.validation;

/**
 * Allows to configure a single validation action that shall be performed
 * when running the {@link ValidationMojo}.
 * A full configuration may like:
 * <pre>{@code
 * <validationProperties>
 *   <validationProperty>
 *     <name>validating project version</name>
 *     <value>${project.version}</value>
 *     <shouldMatchTo><![CDATA[^.*(?<!-SNAPSHOT)$]]></shouldMatchTo>
 *   </validationProperty>
 * </validationProperties>
 * }</pre>
 */
public class ValidationProperty {
  private String name;
  private String value;
  private String shouldMatchTo;

  public ValidationProperty() {}

  ValidationProperty(String name, String value, String shouldMatchTo) {
    this.name = name;
    this.value = value;
    this.shouldMatchTo = shouldMatchTo;
  }

  /**
   * Sets a descriptive name that will be used to be able to identify the validation that
   * does not match up (will be displayed in the error message).
   *
   * @param name The name that shall be used to identify the validation
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets the value that needs to be validated.
   *
   * @param value The value that needs to be validated.
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Sets the expectation what the given value should match to.
   *
   * @param shouldMatchTo The expectation what the given value should match to.
   */
  public void setShouldMatchTo(String shouldMatchTo) {
    this.shouldMatchTo = shouldMatchTo;
  }

  /**
   * @return A descriptive name that will be used to be able to identify the validation that
   * does not match up (will be displayed in the error message).
   */
  public String getName() {
    return name;
  }

  /**
   * @return The value that needs to be validated.
   */
  public String getValue() {
    return value;
  }

  /**
   * @return The expectation what the given value should match to.
   */
  public String getShouldMatchTo() {
    return shouldMatchTo;
  }
}
