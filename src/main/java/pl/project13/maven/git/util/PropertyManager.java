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

package pl.project13.maven.git.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PropertyManager {  
  public static void putWithoutPrefix(@NotNull Properties properties, String key, String value) {
    if (!isNotEmpty(value)) {
      value = "Unknown";
    }
    properties.put(key, value);
  }

  private static boolean isNotEmpty(@Nullable String value) {
    return null != value && !" ".equals(value.trim().replaceAll(" ", ""));
  }

  public static Properties readProperties(@NotNull File propertiesFile, @NotNull String sourceCharset) throws Exception {
    try (FileInputStream fis = new FileInputStream(propertiesFile);
         InputStreamReader reader = new InputStreamReader(fis, sourceCharset)) {
      final Properties retVal = new Properties();
      retVal.load(reader);
      return retVal;
    }
  }
}
