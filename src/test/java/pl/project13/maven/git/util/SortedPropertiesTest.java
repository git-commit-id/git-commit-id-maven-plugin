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

import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Stack;

import org.junit.Test;

public class SortedPropertiesTest {
  @Test
  public void testSorting() throws IOException {
    List<String> testList = Arrays.asList("a", "b", "c.a", "c.b");
    testInternal(testList);
  }
  
  @Test
  public void testSortingWithGitProperties() throws IOException {
    List<String> testList = new ArrayList<>();
    testList.add("git.branch");
    testList.add("git.build.host");
    testList.add("git.build.time");
    testList.add("git.build.user.email");
    testList.add("git.build.user.name");
    testList.add("git.build.version");
    testList.add("git.closest.tag.commit.count");
    testList.add("git.closest.tag.name");
    testList.add("git.commit.id");
    testList.add("git.commit.id.abbrev");
    testList.add("git.commit.id.describe");
    testList.add("git.commit.id.describe-short");
    testList.add("git.commit.message.full");
    testList.add("git.commit.message.short");
    testList.add("git.commit.time");
    testList.add("git.commit.user.email");
    testList.add("git.commit.user.name");
    testList.add("git.dirty");
    testList.add("git.remote.origin.url");
    testList.add("git.tags");
    testInternal(testList);
  }
  
  private void testInternal(List<String> testList) throws IOException {
    Collections.reverse(testList);
    Properties properties = new Properties();
    Stack<String> expected = new Stack<>();
    int listSize = testList.size();
    for(int i=0; i<listSize; i++){
      String key = testList.get(i);
      String val = String.valueOf(listSize-i);
      properties.put(key, val);
      expected.push(key + "=" + val);
    }
    SortedProperties sp = new SortedProperties();
    sp.putAll(properties);
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    sp.store(out, "test");
    
    Assert.assertNotNull(out);
    String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
    Assert.assertNotNull(result);
    
    for(String line: result.split("\\r?\\n")){
      Assert.assertNotNull(line);
      if(line.startsWith("#")){
        continue;
      }
      Assert.assertEquals(expected.pop(), line);
    }
    Assert.assertTrue(expected.isEmpty());
  }
}
