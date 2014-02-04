/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.project13.maven.git;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

import static java.nio.charset.Charset.defaultCharset;

/**
 *
 * @author Krzysztof Suszyński <krzysztof.suszynski@gmail.com>
 */
public class NativeGitProvider {

  private transient CliRunner runner;

  private static final String FORMAT;

  private static final Map<String, String> PARSE_MAP;

  private String dateFormat;

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

  public NativeGitProvider(CliRunner runner, String dateFormat) {
    this.runner = runner;
    this.dateFormat = dateFormat;
  }

  public NativeGitProvider(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  /**
   * Loads a git repository information using native git executable.
   *
   * @param directory a directory to run command into
   * @return map of all git properties
   * @throws java.io.IOException
   */
  public Map<String, String> loadGitData(final File directory) throws IOException {
    final File canonical = directory.getCanonicalFile();
    final Map<String, String> map = new LinkedHashMap<String, String>();
    map.put(GitCommitIdMojo.COMMIT_DESCRIBE, tryToRunGitCommand(canonical, "describe --tags", null));
    map.put(GitCommitIdMojo.BRANCH, getBranch(canonical));
    map.put(GitCommitIdMojo.REMOTE_ORIGIN_URL, getOriginRemote(canonical));
    final String format = FORMAT.replaceFirst("<\\?xml.+\\?>", "").replaceAll("\\s+", "");
    final String logCommand = String.format("log -1 --format=%s", format);
    final String xml = runGitCommand(canonical, logCommand);
    try {
      readFromFormatedXml(map, xml);
    } catch (SAXException ex) {
      throw new IOException(ex);
    } catch (ParserConfigurationException ex) {
      throw new IOException(ex);
    } catch (XPathExpressionException ex) {
      throw new IOException(ex);
    } catch (ParseException ex) {
      throw new IOException(ex);
    }
    return map;
  }

  private String getOriginRemote(final File directory) throws IOException {
    String remotes = runGitCommand(directory, "remote -v");
    for (String line : remotes.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("origin")) {
        String[] splited = trimmed.split("\\s+");
        if (splited.length != 3) {
          throw new IOException("Unsopported GIT output: " + line);
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
   * @throws IOException
   */
  private String runGitCommand(final File directory, final String gitCommand) throws IOException {
    final String env = System.getenv("GIT_PATH");
    final String exec = (env == null) ? "git" : env;
    final String command = String.format("%s %s", exec, gitCommand);
    return getRunner().run(directory, command).trim();
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
    } catch (IOException ex) {
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

  private void readFromFormatedXml(final Map<String, String> map, final String xml) throws
          SAXException, IOException, ParserConfigurationException, XPathExpressionException, ParseException {
    final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    final DocumentBuilder builder = builderFactory.newDocumentBuilder();
    final String withProlog = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
    final ByteArrayInputStream bais = new ByteArrayInputStream(withProlog.getBytes(defaultCharset()));
    final Document document = builder.parse(bais);
    final XPath xPath = XPathFactory.newInstance().newXPath();
    for (Map.Entry<String, String> entry : PARSE_MAP.entrySet()) {
      final String propertyName = entry.getKey();
      final String path = entry.getValue();
      final String value = xPath.compile("/props/" + path).evaluate(document);
      map.put(propertyName, postprocesValue(value, propertyName));
    }
  }

  private String getBranch(final File canonical) {
    String branch = tryToRunGitCommand(canonical, "symbolic-ref HEAD", null);
    if (branch != null) {
      branch = branch.replace("refs/heads/", "");
    }
    return branch;
  }

  private String postprocesValue(String value, String propertyName) throws ParseException {
    if (propertyName.equals(GitCommitIdMojo.COMMIT_TIME)) {
      final SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);
      final Date date = parser.parse(value);
      final SimpleDateFormat smf = new SimpleDateFormat(dateFormat, Locale.US);
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
    String run(final File directory, final String command) throws IOException;
  }

  protected static class Runner implements CliRunner {

    @Override
    public String run(final File directory, final String command) throws IOException {
      try {
        final ProcessBuilder builder = new ProcessBuilder(command.split("\\s"));
        final Process proc = builder.directory(directory).start();
        proc.waitFor();
        final String output = convertStreamToString(proc.getInputStream());
        if (proc.exitValue() != 0) {
          final String message = String.format("Git command exited with invalid status [%d]: `%s`", proc.exitValue(),
                  output);
          throw new IOException(message);
        }
        return output;
      } catch (InterruptedException ex) {
        throw new IOException(ex);
      }
    }

  }

  private static String convertStreamToString(final InputStream inputStream) {
    final Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }
}
