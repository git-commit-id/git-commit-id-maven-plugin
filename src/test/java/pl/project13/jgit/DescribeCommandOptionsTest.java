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

import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import org.mockito.Matchers;
import pl.project13.maven.git.GitDescribeConfig;
import pl.project13.test.utils.AssertException;

import static org.mockito.Mockito.*;

public class DescribeCommandOptionsTest {


  @Test
  public void abbrev_shouldVerifyLengthContract_failOn41() throws Exception {
    // given
    final Repository repo = mock(Repository.class);
    final int length = 41;

    // when
    AssertException.CodeBlock block = new AssertException.CodeBlock() {
      @Override
      public void run() throws Exception {
        DescribeCommand.on(repo).abbrev(length);
      }
    };

    // then
    AssertException.thrown(IllegalArgumentException.class, block);
  }

  @Test
  public void abbrev_shouldVerifyLengthContract_failOnMinus12() throws Exception {
    // given
    final Repository repo = mock(Repository.class);
    final int length = -12;

    // when
    AssertException.CodeBlock block = new AssertException.CodeBlock() {
      @Override
      public void run() {
        DescribeCommand.on(repo).abbrev(length);
      }
    };

    // then
    AssertException.thrown(IllegalArgumentException.class, block);
  }

  @Test
  public void apply_shouldDelegateToAllOptions() throws Exception {
    // given
      final String DEVEL = "DEVEL";
      final String MATCH = "*";
    final int ABBREV = 12;

    GitDescribeConfig config = new GitDescribeConfig(true, DEVEL, MATCH, ABBREV, true, true);

    Repository repo = mock(Repository.class);
    DescribeCommand command = DescribeCommand.on(repo);
    DescribeCommand spiedCommand = spy(command);

    // when
    spiedCommand.apply(config);

    // then
    verify(spiedCommand).always(Matchers.eq(true));
    verify(spiedCommand).abbrev(Matchers.eq(ABBREV));
    verify(spiedCommand).dirty(Matchers.eq(DEVEL));
    verify(spiedCommand).tags(Matchers.eq(true));
    verify(spiedCommand).forceLongFormat(Matchers.eq(true));
  }
}
