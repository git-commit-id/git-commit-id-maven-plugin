package pl.project13.maven.git;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import pl.project13.maven.git.log.MavenLoggerBridge;

@RunWith(MockitoJUnitRunner.class)
public class PropertiesFiltererTest {

  private static final String PREFIX_DOT = "prefix.";

  @InjectMocks
  private PropertiesFilterer propertiesFilterer;

  @Mock
  private MavenLoggerBridge log;

  @Mock
  private Properties properties;

  @Test
  public void filterNotWithoutExclusions() {
    List<String> exclusions = null;

    propertiesFilterer.filterNot(properties, exclusions, PREFIX_DOT);

    Mockito.verifyZeroInteractions(properties);
  }

  @Test
  public void filterNotWithEmptyExclusions() {
    List<String> exclusions = Collections.emptyList();

    propertiesFilterer.filterNot(properties, exclusions, PREFIX_DOT);

    Mockito.verifyZeroInteractions(properties);
  }

  @Test
  public void filterNotRemovesOwnPropertyInExclusionAndSkipsOtherOnes() {
    List<String> inclusions = Arrays.asList("^prefix\\.exclude1.*$", "^prefix\\.exclude2.*$");
    when(properties.stringPropertyNames()).thenReturn(new HashSet<>(Arrays.asList("prefix.include", "prefix.exclude1", "prefix.exclude2", "global")));

    propertiesFilterer.filterNot(properties, inclusions, PREFIX_DOT);

    verify(properties).stringPropertyNames();
    verify(properties).remove("prefix.exclude1");
    verify(properties).remove("prefix.exclude2");
    verifyNoMoreInteractions(properties);
  }

  @Test
  public void filterWithoutInclusions() {
    List<String> inclusions = null;

    propertiesFilterer.filter(properties, inclusions, PREFIX_DOT);

    Mockito.verifyZeroInteractions(properties);
  }

  @Test
  public void filterWithEmptyInclusions() {
    List<String> inclusions = Collections.emptyList();

    propertiesFilterer.filter(properties, inclusions, PREFIX_DOT);

    Mockito.verifyZeroInteractions(properties);
  }

  @Test
  public void filterRemovesOwnPropertyNotInInclusionAndSkipsOtherOnes() {
    List<String> inclusions = Arrays.asList("^prefix\\.include1.*$", "^prefix\\.include2.*$");
    when(properties.stringPropertyNames()).thenReturn(new HashSet<>(Arrays.asList("prefix.include1", "prefix.include2", "prefix.exclude", "global")));

    propertiesFilterer.filter(properties, inclusions, PREFIX_DOT);

    verify(properties).stringPropertyNames();
    verify(properties).remove("prefix.exclude");
    verifyNoMoreInteractions(properties);
  }
}