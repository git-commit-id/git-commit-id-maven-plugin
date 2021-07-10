/*
 * This file is part of git-commit-id-maven-plugin by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
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

import pl.project13.core.PropertiesFilterer;
import pl.project13.core.log.LogInterface;

@RunWith(MockitoJUnitRunner.class)
public class PropertiesFiltererTest {

  private static final String PREFIX_DOT = "prefix.";

  @InjectMocks
  private PropertiesFilterer propertiesFilterer;

  @Mock
  private LogInterface log;

  @Mock
  private Properties properties;

  @Test
  public void filterNotWithoutExclusions() {
    List<String> exclusions = null;

    propertiesFilterer.filterNot(properties, exclusions, PREFIX_DOT);

    Mockito.verifyNoInteractions(properties);
  }

  @Test
  public void filterNotWithEmptyExclusions() {
    List<String> exclusions = Collections.emptyList();

    propertiesFilterer.filterNot(properties, exclusions, PREFIX_DOT);

    Mockito.verifyNoInteractions(properties);
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

    Mockito.verifyNoInteractions(properties);
  }

  @Test
  public void filterWithEmptyInclusions() {
    List<String> inclusions = Collections.emptyList();

    propertiesFilterer.filter(properties, inclusions, PREFIX_DOT);

    Mockito.verifyNoInteractions(properties);
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