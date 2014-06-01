package pl.project13.maven.git.util;

import java.util.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PropertyManager {  
  public static void putWithoutPrefix(@NotNull Properties properties, String key, String value) {
    properties.put(key, value);
  }
}
