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

package pl.project13.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            { "https://:@git.server.com/pathToRepo.git", "https://git.server.com/pathToRepo.git" },
            { "https://:password@git.server.com/pathToRepo.git", "https://git.server.com/pathToRepo.git" },
            { "git@github.com", "git@github.com" },
            { "git@github.com:8888", "git@github.com:8888" },
            { "user@host.xz:~user/path/to/repo.git", "user@host.xz:~user/path/to/repo.git" },
            { "[user@mygithost:10022]:my-group/my-sample-project.git", "[user@mygithost:10022]:my-group/my-sample-project.git" },
            { "ssh://git@github.com/", "ssh://git@github.com/" },
            { "/path/to/repo.git/", "/path/to/repo.git/" },
            { "file:///path/to/repo.git/", "file:///path/to/repo.git/"},
            { "file:///C:\\Users\\test\\example", "file:///C:\\Users\\test\\example"},
            { "file://C:\\Users\\test\\example", "file://C:\\Users\\test\\example" },
            // ensure a percent encoded password is stripped too, that should be allowed
            { "https://user:passw%40rd@example.com:8888", "https://user@example.com:8888" },
            // Must Support: use of 'unreserved' characters as https://www.ietf.org/rfc/rfc2396.txt, Section "2.3. Unreserved Characters"
            { "https://user:A-_.!~*'()Z@example.com:8888", "https://user@example.com:8888" },
            // Optional Support: use of 'reserved' characters as https://www.ietf.org/rfc/rfc2396.txt, Section "2.2. Reserved Characters"
            // note: left out '/', '?', '@' since we technically expect user's need to escape those
            { "https://user:A;:&=+$,Z@example.com:8888", "https://user@example.com:8888" },
            // Optional Support: use of 'delims' characters as https://www.ietf.org/rfc/rfc2396.txt, Section "2.4.3. Excluded US-ASCII Characters"
            { "https://user:A<>#%\"Z@example.com:8888", "https://user@example.com:8888" },
            // Optional Support: use of 'unwise' characters as https://www.ietf.org/rfc/rfc2396.txt, Section "2.4.3. Excluded US-ASCII Characters"
            { "https://user:A{}|\\^[]`Z@example.com:8888", "https://user@example.com:8888" },
    };
    return Arrays.asList(data);
  }

  @Test
  @Parameters(method = "parameters")
  public void testStripCredentialsFromOriginUrl(String input, String expected) throws GitCommitIdExecutionException {
    GitDataProvider gitDataProvider = mock(GitDataProvider.class);
    when(gitDataProvider.stripCredentialsFromOriginUrl(ArgumentMatchers.any())).thenCallRealMethod();
    String result = gitDataProvider.stripCredentialsFromOriginUrl(input);
    assertEquals(expected, result);
  }

}
