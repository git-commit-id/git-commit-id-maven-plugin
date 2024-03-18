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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

/**
 * Test cases to verify the {@link ValidationMojo} works properly.
 */
public class ValidationMojoTest {
  @Test
  public void validationNotMatchingAndValidationShouldFailIfNoMatch()
      throws MojoExecutionException {
    assertThrows(MojoExecutionException.class, () -> {
      List<ValidationProperty> validationProperties = getNonMatchingValidationProperties();
      executeMojo(validationProperties, true);
    });
  }

  @Test
  public void validationNotMatchingAndValidationShouldNotFailIfNoMatch()
      throws MojoExecutionException {
    List<ValidationProperty> validationProperties = getNonMatchingValidationProperties();
    executeMojo(validationProperties, false);
  }

  private List<ValidationProperty> getNonMatchingValidationProperties() {
    return getListValidationProperty("name", "value", "thisIsNotMatchingToValue");
  }

  @Test
  public void validationMatchingAndValidationShouldFailIfNoMatch() throws MojoExecutionException {
    List<ValidationProperty> validationProperties = getMatchingValidationProperties();
    executeMojo(validationProperties, true);
  }

  private List<ValidationProperty> getMatchingValidationProperties() {
    return getListValidationProperty("name", "value", "value");
  }

  @Test
  public void nullTests() throws MojoExecutionException {
    boolean validationShouldFailIfNoMatch = true;
    executeMojo(null, validationShouldFailIfNoMatch);
    executeMojo(getListValidationProperty(null, null, null), validationShouldFailIfNoMatch);
    executeMojo(getListValidationProperty("", null, null), validationShouldFailIfNoMatch);
    executeMojo(getListValidationProperty(null, "", null), validationShouldFailIfNoMatch);
    executeMojo(getListValidationProperty(null, null, ""), validationShouldFailIfNoMatch);
  }

  private void executeMojo(
      List<ValidationProperty> validationProperties, boolean validationShouldFailIfNoMatch)
      throws MojoExecutionException {
    ValidationMojo cut = new ValidationMojo();
    cut.setValidationProperties(validationProperties);
    cut.setValidationShouldFailIfNoMatch(validationShouldFailIfNoMatch);
    cut.execute();
  }

  private List<ValidationProperty> getListValidationProperty(
      String name, String value, String shouldMatchTo) {
    List<ValidationProperty> list = new ArrayList<>();
    list.add(new ValidationProperty(name, value, shouldMatchTo));
    return list;
  }
}
