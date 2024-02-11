/*
 * This file is part of git-commit-id-maven-plugin
 * Originally invented by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
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

package pl.project13.log;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pl.project13.core.log.LogInterface;

/**
 * A class to test any logging interaction.
 */
public class DummyTestLoggerBridge implements LogInterface {
  private List<String> debugs = new ArrayList<>();
  private ArrayList<String> infos = new ArrayList<>();
  private ArrayList<String> warns = new ArrayList<>();
  private ArrayList<String> errors = new ArrayList<>();
  private ArrayList<Map.Entry<String, Throwable>> errorsWithThrowables = new ArrayList<>();

  @Override
  public void debug(String msg) {
    debugs.add(msg);
  }

  public List<String> getDebugs() {
    return debugs;
  }

  @Override
  public void info(String msg) {
    infos.add(msg);
  }

  public ArrayList<String> getInfos() {
    return infos;
  }

  @Override
  public void warn(String msg) {
    warns.add(msg);
  }

  public ArrayList<String> getWarns() {
    return warns;
  }

  @Override
  public void error(String msg) {
    errors.add(msg);
  }

  public ArrayList<String> getErrors() {
    return errors;
  }

  @Override
  public void error(String msg, Throwable t) {
    errorsWithThrowables.add(new AbstractMap.SimpleEntry<>(msg, t));
  }

  public ArrayList<Map.Entry<String, Throwable>> getErrorsWithThrowables() {
    return errorsWithThrowables;
  }
}
