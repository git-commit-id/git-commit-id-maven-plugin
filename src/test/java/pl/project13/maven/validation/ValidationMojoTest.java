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

package pl.project13.maven.validation;

import org.apache.maven.plugin.MojoExecutionException;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ValidationMojoTest {
  @Test(expected = MojoExecutionException.class)
  public void validationNotMatchingAndValidationShouldFailIfNoMatch() throws MojoExecutionException {
    List<ValidationProperty> validationProperties = getNonMatchingValidationProperties();
    executeMojo(validationProperties, true);
  }

  @Test
  public void validationNotMatchingAndValidationShouldNotFailIfNoMatch() throws MojoExecutionException {
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

  private void executeMojo(List<ValidationProperty> validationProperties, boolean validationShouldFailIfNoMatch) throws MojoExecutionException {
    ValidationMojo cut = new ValidationMojo();
    cut.setValidationProperties(validationProperties);
    cut.setValidationShouldFailIfNoMatch(validationShouldFailIfNoMatch);
    cut.execute();
  }

  private List<ValidationProperty> getListValidationProperty(String name, String value, String shouldMatchTo) {
    List<ValidationProperty> list = new ArrayList<ValidationProperty>();
    list.add(new ValidationProperty(name, value, shouldMatchTo));
    return list;
  }
}
