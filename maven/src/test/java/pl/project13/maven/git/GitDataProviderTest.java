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
import pl.project13.core.log.LoggerBridge;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class GitDataProviderTest  {
  @Test
  public void loadShortDescribe() throws GitCommitIdExecutionException {
    assertShortDescribe("1.0.2-12-g19471", "1.0.2-12");
    assertShortDescribe("v1.0.0-0-gde4db35917", "v1.0.0-0");
    assertShortDescribe("1.0.2-12-g19471-DEV", "1.0.2-12-DEV");
    assertShortDescribe("V-1.0.2-12-g19471-DEV", "V-1.0.2-12-DEV");

    assertShortDescribe(null, null);
    assertShortDescribe("12.4.0-1432", "12.4.0-1432");
    assertShortDescribe("12.6.0", "12.6.0");
    assertShortDescribe("", "");
  }

  private void assertShortDescribe(String commitDescribe, String expectedShortDescribe) throws GitCommitIdExecutionException {
    Properties prop = new Properties();
    if (commitDescribe != null) {
      prop.put(GitCommitPropertyConstant.COMMIT_DESCRIBE, commitDescribe);
    }

    TestGitDataProvider gitDataProvider = spy(TestGitDataProvider.class);
    gitDataProvider.loadShortDescribe(prop);
    assertThat(prop.getProperty(GitCommitPropertyConstant.COMMIT_SHORT_DESCRIBE)).isEqualTo(expectedShortDescribe);
  }

  private abstract static class TestGitDataProvider extends GitDataProvider {
    TestGitDataProvider() {
      super(mock(LoggerBridge.class));
      setPrefixDot("");
    }
  }
}
