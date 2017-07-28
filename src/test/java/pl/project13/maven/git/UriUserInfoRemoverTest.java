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

package pl.project13.maven.git;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import java.util.Arrays;
import java.util.Collection;

@RunWith(JUnitParamsRunner.class)
public class UriUserInfoRemoverTest {

  public static Collection<Object[]> parameters() {
        Object[][] data = new Object[][] {
                { "https://example.com", "https://example.com" },
                { "https://example.com:8888", "https://example.com:8888" },
                { "https://user@example.com", "https://user@example.com" },
                { "https://user@example.com:8888", "https://user@example.com:8888" },
                { "https://user:password@example.com", "https://user@example.com" },
                { "https://user:password@example.com:8888", "https://user@example.com:8888" },
                { "git@github.com", "git@github.com" },
                { "git@github.com:8888", "git@github.com:8888" },
                { "user@host.xz:~user/path/to/repo.git", "user@host.xz:~user/path/to/repo.git" },
                { "[user@mygithost:10022]:my-group/my-sample-project.git", "[user@mygithost:10022]:my-group/my-sample-project.git" },
                { "ssh://git@github.com/", "ssh://git@github.com/" },
                { "/path/to/repo.git/", "/path/to/repo.git/" },
                { "file:///path/to/repo.git/", "file:///path/to/repo.git/"},
                };
        return Arrays.asList(data);
  }

  @Test
  @Parameters(method = "parameters")
  public void testStripCrecentialsFromOriginUrl(String input, String expected) throws GitCommitIdExecutionException {
    String result = GitDataProvider.stripCredentialsFromOriginUrl(input);
    assertEquals(expected, result);
  }

}
