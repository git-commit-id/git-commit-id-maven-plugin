
package pl.project13.maven.git;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import pl.project13.maven.git.util.DisplayedException;
import pl.project13.maven.git.util.EidRuntimeException;

import static java.nio.charset.Charset.defaultCharset;

/**
 *
 * @author Krzysztof Suszyński <krzysztof.suszynski@gmail.com>
 */
public class NativeGitProvider implements Serializable {

  private static final long serialVersionUID = 1L;

  private transient CliRunner runner;

  private static final String FORMAT;

  private static final Map<String, String> PARSE_MAP;

  private final String dateFormat;

  static {
    final InputStream inputStream = NativeGitProvider.class.getResourceAsStream("git-log-format.xml");
    FORMAT = convertStreamToString(inputStream);
    PARSE_MAP = new LinkedHashMap<String, String>();
    PARSE_MAP.put(GitCommitIdMojo.COMMIT_ID, "commit/hash");
    PARSE_MAP.put(GitCommitIdMojo.COMMIT_ID_ABBREV, "commit/abbr");
    PARSE_MAP.put(GitCommitIdMojo.COMMIT_AUTHOR_NAME, "committer/name");
    PARSE_MAP.put(GitCommitIdMojo.COMMIT_AUTHOR_EMAIL, "committer/email");
    PARSE_MAP.put(GitCommitIdMojo.COMMIT_MESSAGE_SHORT, "commit/subject");
    PARSE_MAP.put(GitCommitIdMojo.COMMIT_MESSAGE_FULL, "commit/body");
    PARSE_MAP.put(GitCommitIdMojo.COMMIT_TIME, "committer/date");
    PARSE_MAP.put(GitCommitIdMojo.BUILD_AUTHOR_NAME, "author/name");
    PARSE_MAP.put(GitCommitIdMojo.BUILD_AUTHOR_EMAIL, "author/email");
  }

  private static final int REMOTE_COLS = 3;

  /**
   * A constructor with extra runner
   *
   * @param runner a cli runner to use
   * @param dateFormat a date format
   */
  public NativeGitProvider(CliRunner runner, String dateFormat) {
    this.runner = runner;
    this.dateFormat = dateFormat;
  }

  /**
   * A default constructor
   *
   * @param dateFormat a date format
   */
  public NativeGitProvider(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  /**
   * Loads a git repository information using native git executable.
   *
   * @param directory a directory to run command into
   * @return map of all git properties
   */
  public Map<String, String> loadGitData(File directory) throws DisplayedException {
    File canonical;
    try {
      canonical = directory.getCanonicalFile();
    } catch (IOException ex) {
      throw new EidRuntimeException("20140224:194232", "Passed a invalid directory, not a GIT repository: " + directory,
              ex);
    }
    try {
      Map<String, String> map = new LinkedHashMap<String, String>();
      map.put(GitCommitIdMojo.COMMIT_DESCRIBE, tryToRunGitCommand(canonical, "describe --tags", null));
      map.put(GitCommitIdMojo.BRANCH, getBranch(canonical));
      map.put(GitCommitIdMojo.REMOTE_ORIGIN_URL, getOriginRemote(canonical));
      String format = FORMAT.replaceFirst("<\\?xml.+\\?>", "").replaceAll("\\s+", "");
      String logCommand = String.format("log -1 --format=%s", format);
      String xml = runGitCommand(canonical, logCommand);
      readFromFormatedXml(map, xml);
      return map;
    } catch (DisplayedException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new EidRuntimeException("20140224:194451", "Unsupported GIT output - has it changed?!", ex);
    }
  }

  /**
   * Gets a default origin remote address.
   *
   * @param directory a directory to fetch a remote to
   * @return a remote address
   */
  private String getOriginRemote(File directory) throws DisplayedException {
    String remotes = runGitCommand(directory, "remote -v");
    for (String line : remotes.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("origin")) {
        String[] splited = trimmed.split("\\s+");
        if (splited.length != REMOTE_COLS) {
          throw new EidRuntimeException("20140224:194851", "Unsupported GIT output - verbose remote address:" + line);
        }
        return splited[1];
      }
    }
    return null;
  }

  /**
   * Runs a git command
   *
   * @param directory a directory to run command into
   * @param gitCommand a git command to run
   * @return output of a git command
   * @throws DisplayedException if could not run git command
   */
  private String runGitCommand(final File directory, final String gitCommand) throws DisplayedException {
    try {
      final String env = System.getenv("GIT_PATH");
      final String exec = (env == null) ? "git" : env;
      final String command = String.format("%s %s", exec, gitCommand);
      return getRunner().run(directory, command).trim();
    } catch (IOException ex) {
      throw new DisplayedException("Could not run GIT command - GIT is not installed or not exists in system path? "
              + "Tried to run: " + gitCommand, ex);
    }
  }

  /**
   * Tries to runs a git command
   *
   * @param directory a directory to run command into
   * @param gitCommand a git command to run
   * @param defaultValue a default value for command
   * @return output of a git command
   */
  private String tryToRunGitCommand(final File directory, final String gitCommand, final String defaultValue) {
    String retValue;
    try {
      retValue = runGitCommand(directory, gitCommand);
    } catch (DisplayedException ex) {
      retValue = defaultValue;
    }
    return retValue;
  }

  /**
   * Gets a runner.
   *
   * @return cli runner
   */
  private CliRunner getRunner() {
    if (runner == null) {
      runner = new Runner();
    }
    return runner;
  }

  private void readFromFormatedXml(Map<String, String> map, String xml) throws
          SAXException, IOException, ParserConfigurationException, XPathExpressionException, ParseException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    String withProlog = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
    ByteArrayInputStream bais = new ByteArrayInputStream(withProlog.getBytes(defaultCharset()));
    Document document = builder.parse(bais);
    XPath xPath = XPathFactory.newInstance().newXPath();
    for (Map.Entry<String, String> entry : PARSE_MAP.entrySet()) {
      String propertyName = entry.getKey();
      String path = entry.getValue();
      String value = xPath.compile("/props/" + path).evaluate(document);
      map.put(propertyName, postprocesValue(value, propertyName));
    }
  }

  private String getBranch(File canonical) {
    String branch = tryToRunGitCommand(canonical, "symbolic-ref HEAD", null);
    if (branch != null) {
      branch = branch.replace("refs/heads/", "");
    }
    return branch;
  }

  private String postprocesValue(String value, String propertyName) throws ParseException {
    if (propertyName.equals(GitCommitIdMojo.COMMIT_TIME)) {
      SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);
      Date date = parser.parse(value);
      SimpleDateFormat smf = new SimpleDateFormat(dateFormat, Locale.US);
      return smf.format(date);
    } else {
      return value;
    }
  }

  /**
   * Cli runner
   */
  public interface CliRunner {

    /**
     * Runs command in CLI
     *
     * @param directory a directory to run command into
     * @param command to run at CLI
     * @return Executed output STDOUT
     * @throws IOException if error occurd
     */
    String run(File directory, String command) throws IOException;
  }

  protected static class Runner implements CliRunner {

    @Override
    public String run(File directory, String command) throws IOException {
      try {
        ProcessBuilder builder = new ProcessBuilder(command.split("\\s"));
        Process proc = builder.directory(directory).start();
        proc.waitFor();
        String output = convertStreamToString(proc.getInputStream());
        if (proc.exitValue() != 0) {
          String message = String.format("Git command exited with invalid status [%d]: `%s`", proc.exitValue(),
                  output);
          throw new IOException(message);
        }
        return output;
      } catch (InterruptedException ex) {
        throw new IOException(ex);
      }
    }

  }

  private static String convertStreamToString(InputStream inputStream) {
    Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }
}
