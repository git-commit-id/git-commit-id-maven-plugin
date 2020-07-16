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

package pl.project13.core.util;

import nu.studer.java.util.OrderedProperties;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import pl.project13.core.PropertiesFileGenerator;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class TestJsonManager {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testDumpWithFlatGitProperty() throws Exception {
    // given
    Charset sourceCharset = StandardCharsets.UTF_8;
    File jsonFile = tempFolder.newFile("git_properties.json");
    OrderedProperties sortedLocalProperties = PropertiesFileGenerator.createOrderedProperties();
    sortedLocalProperties.setProperty("git.commit.id", "beef4e92e9cabd043b105a14514289f331b40bf2");
    sortedLocalProperties.setProperty("git.commit.id.abbrev", "beef4e9");

    // when
    try (OutputStream outputStream = new FileOutputStream(jsonFile)) {
      JsonManager.dumpJson(outputStream, sortedLocalProperties, sourceCharset);
    }

    // then
    Assert.assertEquals(
            Arrays.asList(
                    "",
                    "{",
                    "    \"git.commit.id\": \"beef4e92e9cabd043b105a14514289f331b40bf2\",",
                    "    \"git.commit.id.abbrev\": \"beef4e9\"",
                    "}"
            ),
            Files.readAllLines(jsonFile.toPath()));
  }

  @Test
  public void testReadJsonPropertiesWithFlatGitProperty() throws Exception {
    // given
    Charset sourceCharset = StandardCharsets.UTF_8;
    File jsonFile = tempFolder.newFile("git_properties.json");
    try (OutputStream outputStream = new FileOutputStream(jsonFile)) {
      try (Writer myWriter = new OutputStreamWriter(outputStream, sourceCharset)) {
        myWriter.write("{");
        myWriter.write("  \"git.commit.id\" : \"beef4e92e9cabd043b105a14514289f331b40bf2\",");
        myWriter.write("  \"git.commit.id.abbrev\" : \"beef4e9\"");
        myWriter.write("}");
      }
    }

    // when
    Properties properties = JsonManager.readJsonProperties(jsonFile, sourceCharset);

    // then
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("beef4e92e9cabd043b105a14514289f331b40bf2", properties.getProperty("git.commit.id"));
    Assert.assertEquals("beef4e9", properties.getProperty("git.commit.id.abbrev"));
  }

  @Test
  public void testDumpWithFullGitProperty() throws Exception {
    // given
    Charset sourceCharset = StandardCharsets.UTF_8;
    File jsonFile = tempFolder.newFile("git_properties.json");
    OrderedProperties sortedLocalProperties = PropertiesFileGenerator.createOrderedProperties();
    sortedLocalProperties.setProperty("git.commit.id.full", "beef4e92e9cabd043b105a14514289f331b40bf2");
    sortedLocalProperties.setProperty("git.commit.id.abbrev", "beef4e9");

    // when
    try (OutputStream outputStream = new FileOutputStream(jsonFile)) {
      JsonManager.dumpJson(outputStream, sortedLocalProperties, sourceCharset);
    }

    // then
    Assert.assertEquals(
            Arrays.asList(
                    "",
                    "{",
                    "    \"git.commit.id.abbrev\": \"beef4e9\",",
                    "    \"git.commit.id.full\": \"beef4e92e9cabd043b105a14514289f331b40bf2\"",
                    "}"
            ),
            Files.readAllLines(jsonFile.toPath()));
  }

  @Test
  public void testReadJsonPropertiesWithFullGitProperty() throws Exception {
    // given
    Charset sourceCharset = StandardCharsets.UTF_8;
    File jsonFile = tempFolder.newFile("git_properties.json");
    try (OutputStream outputStream = new FileOutputStream(jsonFile)) {
      try (Writer myWriter = new OutputStreamWriter(outputStream, sourceCharset)) {
        myWriter.write("{");
        myWriter.write("  \"git.commit.id.full\" : \"beef4e92e9cabd043b105a14514289f331b40bf2\",");
        myWriter.write("  \"git.commit.id.abbrev\" : \"beef4e9\"");
        myWriter.write("}");
      }
    }

    // when
    Properties properties = JsonManager.readJsonProperties(jsonFile, sourceCharset);

    // then
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("beef4e92e9cabd043b105a14514289f331b40bf2", properties.getProperty("git.commit.id.full"));
    Assert.assertEquals("beef4e9", properties.getProperty("git.commit.id.abbrev"));
  }

  @Test
  public void testDumpWithUnicode() throws Exception {
    // given
    Charset sourceCharset = StandardCharsets.UTF_8;
    File jsonFile = tempFolder.newFile("git_properties.json");
    OrderedProperties sortedLocalProperties = PropertiesFileGenerator.createOrderedProperties();
    sortedLocalProperties.setProperty("git.commit.user.name", "Александр Eliáš");
    sortedLocalProperties.setProperty("git.commit.message.full", "initial commit on test project with some special characters äöüàñ.");

    // when
    try (OutputStream outputStream = new FileOutputStream(jsonFile)) {
      JsonManager.dumpJson(outputStream, sortedLocalProperties, sourceCharset);
    }

    // then
    Assert.assertEquals(
            Arrays.asList(
                    "",
                    "{",
                    "    \"git.commit.message.full\": \"initial commit on test project with some special characters äöüàñ.\",",
                    "    \"git.commit.user.name\": \"Александр Eliáš\"",
                    "}"
            ),
            Files.readAllLines(jsonFile.toPath()));
  }

  @Test
  public void testReadJsonPropertiesWithUnicode() throws Exception {
    // given
    Charset sourceCharset = StandardCharsets.UTF_8;
    File jsonFile = tempFolder.newFile("git_properties.json");
    try (OutputStream outputStream = new FileOutputStream(jsonFile)) {
      try (Writer myWriter = new OutputStreamWriter(outputStream, sourceCharset)) {
        myWriter.write("{");
        myWriter.write("  \"git.commit.user.name\": \"Александр Eliáš\",");
        myWriter.write("  \"git.commit.message.full\": \"initial commit on test project with some special characters äöüàñ.\"");
        myWriter.write("}");
      }
    }

    // when
    Properties properties = JsonManager.readJsonProperties(jsonFile, sourceCharset);

    // then
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("Александр Eliáš", properties.getProperty("git.commit.user.name"));
    Assert.assertEquals(
            "initial commit on test project with some special characters äöüàñ.",
            properties.getProperty("git.commit.message.full"));
  }
}
