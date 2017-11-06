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

package pl.project13.jgit;

import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import pl.project13.maven.git.GitDescribeConfig;
import pl.project13.maven.git.log.StdOutLoggerBridge;

import static org.mockito.Mockito.*;

public class DescribeCommandOptionsTest {
  private static final String evaluateOnCommit = "HEAD";

  @Test(expected = IllegalArgumentException.class)
  public void abbrev_shouldVerifyLengthContract_failOn41() throws Exception {
    // given
    final Repository repo = mock(Repository.class);
    final int length = 41;

    DescribeCommand.on(evaluateOnCommit, repo, new StdOutLoggerBridge(true)).abbrev(length);
  }

  @Test(expected = IllegalArgumentException.class)
  public void abbrev_shouldVerifyLengthContract_failOnMinus12() throws Exception {
    // given
    final Repository repo = mock(Repository.class);
    final int length = -12;

    DescribeCommand.on(evaluateOnCommit, repo, new StdOutLoggerBridge(true)).abbrev(length);
  }

  @Test
  public void apply_shouldDelegateToAllOptions() throws Exception {
    // given
    final String devel = "DEVEL";
    final String match = "*";
    final int abbrev = 12;

    GitDescribeConfig config = new GitDescribeConfig(true, devel, match, abbrev, true, true);

    Repository repo = mock(Repository.class);
    DescribeCommand command = DescribeCommand.on(evaluateOnCommit, repo, new StdOutLoggerBridge(true));
    DescribeCommand spiedCommand = spy(command);

    // when
    spiedCommand.apply(config);

    // then
    verify(spiedCommand).always(ArgumentMatchers.eq(true));
    verify(spiedCommand).abbrev(ArgumentMatchers.eq(abbrev));
    verify(spiedCommand).dirty(ArgumentMatchers.eq(devel));
    verify(spiedCommand).tags(ArgumentMatchers.eq(true));
    verify(spiedCommand).forceLongFormat(ArgumentMatchers.eq(true));
  }
}