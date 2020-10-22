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
import pl.project13.core.CannotReadFileException;

import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class JsonManager {
  public static void dumpJson(OutputStream outputStream, OrderedProperties sortedLocalProperties, Charset sourceCharset) throws IOException {
    JsonGeneratorFactory jgf = Json.createGeneratorFactory(
            Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

    try (Writer outputWriter = new OutputStreamWriter(outputStream, sourceCharset)) {
      JsonGenerator jg = jgf.createGenerator(outputWriter);
      jg.writeStartObject();

      for (Map.Entry e : sortedLocalProperties.entrySet()) {
        jg.write((String) e.getKey(), (String) e.getValue());
      }
      jg.writeEnd();
      jg.close();
    }

  }

  public static Properties readJsonProperties(@Nonnull File jsonFile, Charset sourceCharset) throws CannotReadFileException {
    Properties retVal = new Properties();
    try (FileInputStream fis = new FileInputStream(jsonFile)) {
      try (InputStreamReader reader = new InputStreamReader(fis, sourceCharset)) {
        try (JsonReader jsonReader = Json.createReader(reader)) {
          jsonReader.readObject().forEach((key, val) -> {
            if (val instanceof JsonString) {
              retVal.setProperty(key, ((JsonString) val).getString());
            } else {
              // TODO warning?
            }
          });
        }
      }
    } catch (IOException e) {
      throw new CannotReadFileException(e);
    }
    return retVal;
  }
}
