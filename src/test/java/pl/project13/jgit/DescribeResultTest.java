/*
 * This file is part of git-commit-id-plugin by Konrad Malawski <konrad.malawski@java.pl>
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

package pl.project13.jgit;

import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class DescribeResultTest {

  String VERSION = "v2.5";
  String ZEROES_COMMIT_ID = "0000000000000000000000000000000000000000";
  String DIRTY_MARKER = "DEV";

  @Test
  public void shouldToStringForTag() throws Exception {
    // given
    DescribeResult res = new DescribeResult(VERSION);

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(VERSION);
  }


  @Test
  public void shouldToStringForDirtyTag() throws Exception {
    // given
    DescribeResult res = new DescribeResult(VERSION, 2, ObjectId.zeroId(), true, DIRTY_MARKER);

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(VERSION + "-" + 2 + "-" + ZEROES_COMMIT_ID + "-" + DIRTY_MARKER);
  }

  @Test
  public void shouldToStringFor2CommitsAwayFromTag() throws Exception {
    // given
    DescribeResult res = new DescribeResult(VERSION, 2, ObjectId.zeroId());

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(VERSION + "-" + 2 + "-" + ZEROES_COMMIT_ID);
  }

  @Test
  public void shouldToStringForNoTagJustACommit() throws Exception {
    // given
    DescribeResult res = new DescribeResult(ObjectId.zeroId());

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(ZEROES_COMMIT_ID);
  }
}
