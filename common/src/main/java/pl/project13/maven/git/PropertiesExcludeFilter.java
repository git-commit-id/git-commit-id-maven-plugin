package pl.project13.maven.git;

import java.util.List;
import java.util.Properties;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/**
 * Exclude properties configured for exclusion.
 *
 */
public class PropertiesExcludeFilter {

  public static void filterNot(Properties properties, @Nullable List<String> exclusions) {
    if (exclusions == null)
      return;

    List<Predicate<CharSequence>> excludePredicates = Lists.transform(exclusions, new Function<String, Predicate<CharSequence>>() {
      @Override
      public Predicate<CharSequence> apply(String exclude) {
        return Predicates.containsPattern(exclude);
      }
    });

    Predicate<CharSequence> shouldExclude = Predicates.alwaysFalse();
    for (Predicate<CharSequence> predicate : excludePredicates) {
      shouldExclude = Predicates.or(shouldExclude, predicate);
    }

    for (String key : properties.stringPropertyNames()) {
      if (shouldExclude.apply(key)) {
        System.out.println("shouldExclude.apply(" + key +") = " + shouldExclude.apply(key));
        properties.remove(key);
      }
    }
  }
}
