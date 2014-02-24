
package pl.project13.maven.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.fest.util.Arrays;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Krzysztof Suszyński <krzysztof.suszynski@gmail.com>
 */
public class NativeGitProviderTest {

  private File directory;

  @Before
  public void setUp() {
    directory = new File(System.getProperty("java.io.tmpdir"));
  }

  private String loadSample(String sample) {
    final InputStream stream = this.getClass().getResourceAsStream(sample + ".xml");
    return convertStreamToString(stream).replaceFirst("<\\?xml.+\\?>", "").replaceAll(">\\s+<", "><");
  }

  private static String convertStreamToString(final InputStream inputStream) {
    final Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }

  /**
   * Test of loadGitData method, of class NativeGitProvider.
   */
  @Test
  public void testLoadGitData() throws Exception {
    Map<String, String> map = new HashMap<String, String>();
    map.put("git describe --tags", "v0.0.3-2557-gd79f2d5");
    map.put("git symbolic-ref HEAD", "refs/heads/master");
    map.put("git log -1 --format=", loadSample("sample-onbranch-with-tags"));
    map.put("git remote -v", "origin	https://git.example.org/sample-project.git (fetch)\n"
            + "origin	https://git.example.org/sample-project.git (push)");
    FakeRunner runner = new FakeRunner(map);
    NativeGitProvider instance = new NativeGitProvider(runner, "dd.MM.yyyy '@' HH:mm:ss z");
    Map<String, String> result = instance.loadGitData(directory);
    String[] expectedKeys = Arrays.array(
            GitCommitIdMojo.COMMIT_DESCRIBE,
            GitCommitIdMojo.BRANCH,
            GitCommitIdMojo.REMOTE_ORIGIN_URL,
            GitCommitIdMojo.COMMIT_ID,
            GitCommitIdMojo.COMMIT_ID_ABBREV,
            GitCommitIdMojo.COMMIT_AUTHOR_NAME,
            GitCommitIdMojo.COMMIT_AUTHOR_EMAIL,
            GitCommitIdMojo.COMMIT_MESSAGE_SHORT,
            GitCommitIdMojo.COMMIT_MESSAGE_FULL,
            GitCommitIdMojo.COMMIT_TIME,
            GitCommitIdMojo.BUILD_AUTHOR_NAME,
            GitCommitIdMojo.BUILD_AUTHOR_EMAIL
    );
    String[] expectedValues = Arrays.array(
            "v0.0.3-2557-gd79f2d5",
            "master",
            "https://git.example.org/sample-project.git",
            "d79f2d589079c591852ee610135ecd3acb6faf0c",
            "d79f2d5",
            "Krzysztof Suszyński",
            "krzysztof.suszynski@example.org",
            "A sample git subject line",
            "A sample git body contents.\n"
            + "      A other sample git body contents. Even with XML tags: <3 <xml />.",
            "04.02.2014 @ 14:53:35 CET",
            "Jan Kowalski",
            "jan.kowalski@example.org"
    );
    assertArrayEquals(expectedKeys, result.keySet().toArray());
    assertArrayEquals(expectedValues, result.values().toArray());
  }

  /**
   * Test of loadGitData method, of class NativeGitProvider.
   */
  @Test
  public void testLoadGitDataDetachedWithTags() throws Exception {
    Map<String, String> map = new HashMap<String, String>();
    map.put("git describe --tags", "v0.0.3-2557-gd79f2d5");
    map.put("git log -1 --format=", loadSample("sample-onbranch-with-tags"));
    map.put("git remote -v", "origin	https://git.example.org/sample-project.git (fetch)\n"
            + "origin	https://git.example.org/sample-project.git (push)");
    FakeRunner runner = new FakeRunner(map);
    NativeGitProvider instance = new NativeGitProvider(runner, "dd.MM.yyyy '@' HH:mm:ss z");
    Map<String, String> result = instance.loadGitData(directory);
    String[] expectedKeys = Arrays.array(
            GitCommitIdMojo.COMMIT_DESCRIBE,
            GitCommitIdMojo.BRANCH,
            GitCommitIdMojo.REMOTE_ORIGIN_URL,
            GitCommitIdMojo.COMMIT_ID,
            GitCommitIdMojo.COMMIT_ID_ABBREV,
            GitCommitIdMojo.COMMIT_AUTHOR_NAME,
            GitCommitIdMojo.COMMIT_AUTHOR_EMAIL,
            GitCommitIdMojo.COMMIT_MESSAGE_SHORT,
            GitCommitIdMojo.COMMIT_MESSAGE_FULL,
            GitCommitIdMojo.COMMIT_TIME,
            GitCommitIdMojo.BUILD_AUTHOR_NAME,
            GitCommitIdMojo.BUILD_AUTHOR_EMAIL
    );
    String[] expectedValues = Arrays.array(
            "v0.0.3-2557-gd79f2d5",
            null,
            "https://git.example.org/sample-project.git",
            "d79f2d589079c591852ee610135ecd3acb6faf0c",
            "d79f2d5",
            "Krzysztof Suszyński",
            "krzysztof.suszynski@example.org",
            "A sample git subject line",
            "A sample git body contents.\n"
            + "      A other sample git body contents. Even with XML tags: <3 <xml />.",
            "04.02.2014 @ 14:53:35 CET",
            "Jan Kowalski",
            "jan.kowalski@example.org"
    );
    assertArrayEquals(expectedKeys, result.keySet().toArray());
    assertArrayEquals(expectedValues, result.values().toArray());
  }

  /**
   * Test of loadGitData method, of class NativeGitProvider.
   */
  @Test
  public void testLoadGitDataDetachedWithoutTags() throws Exception {
    Map<String, String> map = new HashMap<String, String>();
    map.put("git log -1 --format=", loadSample("sample-onbranch-with-tags"));
    map.put("git remote -v", "origin	https://git.example.org/sample-project.git (fetch)\n"
            + "origin	https://git.example.org/sample-project.git (push)");
    FakeRunner runner = new FakeRunner(map);
    NativeGitProvider instance = new NativeGitProvider(runner, "dd.MM.yyyy '@' HH:mm:ss z");
    Map<String, String> result = instance.loadGitData(directory);
    String[] expectedKeys = Arrays.array(
            GitCommitIdMojo.COMMIT_DESCRIBE,
            GitCommitIdMojo.BRANCH,
            GitCommitIdMojo.REMOTE_ORIGIN_URL,
            GitCommitIdMojo.COMMIT_ID,
            GitCommitIdMojo.COMMIT_ID_ABBREV,
            GitCommitIdMojo.COMMIT_AUTHOR_NAME,
            GitCommitIdMojo.COMMIT_AUTHOR_EMAIL,
            GitCommitIdMojo.COMMIT_MESSAGE_SHORT,
            GitCommitIdMojo.COMMIT_MESSAGE_FULL,
            GitCommitIdMojo.COMMIT_TIME,
            GitCommitIdMojo.BUILD_AUTHOR_NAME,
            GitCommitIdMojo.BUILD_AUTHOR_EMAIL
    );
    String[] expectedValues = Arrays.array(
            null,
            null,
            "https://git.example.org/sample-project.git",
            "d79f2d589079c591852ee610135ecd3acb6faf0c",
            "d79f2d5",
            "Krzysztof Suszyński",
            "krzysztof.suszynski@example.org",
            "A sample git subject line",
            "A sample git body contents.\n"
            + "      A other sample git body contents. Even with XML tags: <3 <xml />.",
            "04.02.2014 @ 14:53:35 CET",
            "Jan Kowalski",
            "jan.kowalski@example.org"
    );
    assertArrayEquals(expectedKeys, result.keySet().toArray());
    assertArrayEquals(expectedValues, result.values().toArray());
  }

  private class FakeRunner implements NativeGitProvider.CliRunner {

    private Map<String, String> map;

    public FakeRunner(Map<String, String> map) {
      this.map = map;
    }

    @Override
    public String run(File directory, String command) throws IOException {
      if (command == null) {
        throw new IOException("Can not run `null` command");
      }
      for (Map.Entry<String, String> entry : map.entrySet()) {
        if (command.trim().contains(entry.getKey())) {
          return entry.getValue();
        }
      }
      throw new IOException(String.format("Unknown command: `%s`", command));
    }

  }

}
