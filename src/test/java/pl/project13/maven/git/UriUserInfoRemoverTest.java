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

import static org.junit.Assert.assertEquals;

public class UriUserInfoRemoverTest {

  @Test
  public void testHttpsUriWithoutUserInfo() throws Exception {
    String result = GitDataProvider.stripCredentialsFromOriginUrl("https://example.com");
    assertEquals("https://example.com", result);
  }

  @Test
  public void testHttpsUriWithUserInfo() throws Exception {
    String result = GitDataProvider.stripCredentialsFromOriginUrl("https://user@example.com");
    assertEquals("https://user@example.com", result);
  }

  @Test
  public void testHttpsUriWithUserInfoAndPassword() throws Exception {
    String result = GitDataProvider.stripCredentialsFromOriginUrl("https://user:password@example.com");
    assertEquals("https://user@example.com", result);
  }

  @Test
  public void testWithSCPStyleSSHProtocolGitHub() throws Exception {
    String result = GitDataProvider.stripCredentialsFromOriginUrl("git@github.com");
    assertEquals("git@github.com", result);
  }

  @Test
  public void testWithSCPStyleSSHProtocol() throws Exception {
    String result = GitDataProvider.stripCredentialsFromOriginUrl("user@host.xz:~user/path/to/repo.git");
    assertEquals("user@host.xz:~user/path/to/repo.git", result);
  }

  @Test
  public void testWithSSHUri() throws Exception {
    String result = GitDataProvider.stripCredentialsFromOriginUrl("ssh://git@github.com/");
    assertEquals("ssh://git@github.com/", result);
  }
}
