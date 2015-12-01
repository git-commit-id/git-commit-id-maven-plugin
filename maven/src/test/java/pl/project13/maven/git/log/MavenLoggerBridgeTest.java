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

package pl.project13.maven.git.log;

import org.junit.Test;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class MavenLoggerBridgeTest {
  Logger logger = mock(Logger.class);
  MavenLoggerBridge bridge = new MavenLoggerBridge(null, true);

  @Test
  public void shouldNotFailWhenMessageContainsPercentSigns() throws Exception {
    // given
    String start = "the output was: [";
    String content = "100% coverage!!!";
    String end = "]";
    String expectedExplicit = "the output was: [ 100% coverage!!! ]";

    // when
    bridge.setLogger(logger);
    bridge.log(start, content, end);

    // then
    verify(logger).info(expectedExplicit);
  }

}
