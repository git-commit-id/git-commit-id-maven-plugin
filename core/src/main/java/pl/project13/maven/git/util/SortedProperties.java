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

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

public class SortedProperties extends Properties {
  private static final long serialVersionUID = -7401401887311920388L;
  @Override
  public Enumeration keys() {
     Enumeration keysEnum = super.keys();
     Vector<String> keyList = new Vector<String>();
     while(keysEnum.hasMoreElements()){
       keyList.add((String)keysEnum.nextElement());
     }
     Collections.sort(keyList);
     return keyList.elements();
  }
  
  @Override
  public Set<Object> keySet() {
    Set<Object> keySet = super.keySet();
    if(keySet==null) return keySet;
    return new TreeSet<>(keySet);
  }
  
  @Override
  public Set<Map.Entry<Object, Object>> entrySet() {
    Set<Map.Entry<Object, Object>> entrySet = super.entrySet();
    if (entrySet==null) return entrySet;

    Set<Map.Entry<Object, Object>> sortedSet = new TreeSet<>(new EntryComparator());
    sortedSet.addAll(entrySet);
    return sortedSet;
  }
  
  /**
   * Comparator for sorting Map.Entry by key
   */
  class EntryComparator implements Comparator<Map.Entry<Object, Object>> {
    @Override
    public int compare(Map.Entry<Object, Object> entry1, Map.Entry<Object, Object> entry2) {
      if((entry1 == null) && (entry2 == null)) {
        return 0;
      } else if((entry1 == null) && (entry2 != null)) {
        return 1;
      } else if((entry1 != null) && (entry2 == null)) {
        return -1;
      } else if((entry1 != null) && (entry2 != null)) {
        Object key1 = entry1.getKey();
        Object key2 = entry2.getKey();
        if((key1 != null) && (key2 != null)) {
          return key1.toString().compareTo(key2.toString());
        }
      }
      
      return 0;
    }
  }
}
