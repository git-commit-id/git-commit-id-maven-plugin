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

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.Assert;

@RunWith(JUnitParamsRunner.class)
public class PropertyReplacementTest {
  public static Collection useRegexReplacement() {
    return asList(true, false);
  }
	
  @Test
  public void testPerformReplacementWithNullValues() throws IOException {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    Properties properties = null;
    List<ReplacementProperty> replacementProperties = null;
    commitIdMojo.performReplacement(properties, replacementProperties);
  }

  @Test
  @Parameters(method = "useRegexReplacement")
  public void testPerformReplacementWithInvalidReplacement(boolean regex) throws IOException {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    Properties actualProperties = build("key1", "value1", "key2", "value2");

    List<ReplacementProperty> replacementProperties = new ArrayList<>();
    replacementProperties.add(new ReplacementProperty("key1", null, null, regex));

    commitIdMojo.performReplacement(actualProperties, replacementProperties);
  }

  @Test
  @Parameters(method = "useRegexReplacement")
  public void testPerformReplacementWithSingleProperty(boolean regex) throws IOException {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    Properties actualProperties = build("key1", "value1", "key2", "value2");

    List<ReplacementProperty> replacementProperties = new ArrayList<>();
    replacementProperties.add(new ReplacementProperty("key1", "value", "another", regex));

    commitIdMojo.performReplacement(actualProperties, replacementProperties);

    Properties exptecedProperties = build("key1", "another1", "key2", "value2");
    assertEquals(exptecedProperties, actualProperties);
  }

  @Test
  @Parameters(method = "useRegexReplacement")
  public void testPerformReplacementWithMultipleProperties(boolean regex) throws IOException {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    Properties actualProperties = build("key1", "value1", "key2", "value2");

    List<ReplacementProperty> replacementProperties = new ArrayList<>();
    replacementProperties.add(new ReplacementProperty(null, "value", "another", regex));

    commitIdMojo.performReplacement(actualProperties, replacementProperties);

    Properties exptecedProperties = build("key1", "another1", "key2", "another2");
    assertEquals(exptecedProperties, actualProperties);
  }

  @Test
  public void testPerformReplacementWithPatternGroupAndMatching() throws IOException {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    Properties actualProperties = build("git.branch", "feature/feature_name", "git.commit.author", "author/name");

    List<ReplacementProperty> replacementProperties = new ArrayList<>();
    replacementProperties.add(new ReplacementProperty("git.branch", "^([^\\/]*)\\/([^\\/]*)$", "$1-$2", true));

    commitIdMojo.performReplacement(actualProperties, replacementProperties);

    Properties exptecedProperties = build("git.branch", "feature-feature_name", "git.commit.author", "author/name");
    assertEquals(exptecedProperties, actualProperties);
  }

  @Test
  public void testPerformReplacementWithPatternGroupAndNoMatch() throws IOException {
    GitCommitIdMojo commitIdMojo = new GitCommitIdMojo();
    Properties actualProperties = build("git.branch", "feature#feature_name", "git.commit.author", "author#");

    List<ReplacementProperty> replacementProperties = new ArrayList<>();
    replacementProperties.add(new ReplacementProperty("git.branch", "^([^\\/]*)\\/([^\\/]*)$", "$1-$2", true));

    commitIdMojo.performReplacement(actualProperties, replacementProperties);

    Properties exptecedProperties = build("git.branch", "feature#feature_name", "git.commit.author", "author#");
    assertEquals(exptecedProperties, actualProperties);
  }

  private Properties build(String key1, String value1, String key2, String value2) {
    Properties properties = new Properties();
    properties.put(key1, value1);
    properties.put(key2, value2);
    return properties;
  }

  private void assertEquals(Properties expected, Properties actual) {
    if(expected == null) {
      Assert.assertNull(actual);
    } else if(actual == null) {
      Assert.assertNull(expected);
    } else {
      Assert.assertEquals(expected.size(), actual.size());
      for(Map.Entry<Object, Object> expectedElementEntry : expected.entrySet()) {
        String expectedKey = (String)expectedElementEntry.getKey();
        String expectedValue = (String)expectedElementEntry.getValue();
        Assert.assertTrue(actual.containsKey(expectedKey));
        String actualValue = actual.getProperty(expectedKey);
        Assert.assertEquals(expectedValue, actualValue);
      }
    }
  }
}
