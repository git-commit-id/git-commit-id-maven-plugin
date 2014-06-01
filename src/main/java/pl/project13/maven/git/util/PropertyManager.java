package pl.project13.maven.git.util;

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
}
