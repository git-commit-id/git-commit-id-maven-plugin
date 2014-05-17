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
import org.apache.maven.plugin.MojoExecutionException;

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

  private static final int REMOTE_COLS = 3;

  static {
    InputStream inputStream = NativeGitProvider.class.getResourceAsStream("git-log-format.xml");
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
   */
  public Map<String, String> loadGitData(File directory) throws MojoExecutionException {
    File canonical;
    try{
      canonical = directory.getCanonicalFile();
    } catch (IOException ex) {
      throw new MojoExecutionException("Passed a invalid directory, not a GIT repository: " + directory, ex);
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
    } catch (Exception ex) {
      throw new MojoExecutionException("Unsupported GIT output - has it changed?!", ex);
    }
  }

  private String getOriginRemote(File directory) throws IOException, MojoExecutionException {
    String remotes = runGitCommand(directory, "remote -v");
    for (String line : remotes.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("origin")) {
        String[] splited = trimmed.split("\\s+");
        if (splited.length != REMOTE_COLS) {
          throw new MojoExecutionException("Unsupported GIT output - verbose remote address:" + line);
        }
        return splited[1];
      }
    }
    return null;
  }

  private String runGitCommand(File directory, String gitCommand) throws MojoExecutionException {
    try {
      String env = System.getenv("GIT_PATH");
      String exec = (env == null) ? "git" : env;
      String command = String.format("%s %s", exec, gitCommand);
      return getRunner().run(directory, command).trim();
    }catch(IOException ex) {
      throw new MojoExecutionException("Could not run GIT command - GIT is not installed or not exists in system path? " + "Tried to run: " + gitCommand, ex);
    }
  }

  private String tryToRunGitCommand(File directory, String gitCommand, String defaultValue) {
    String retValue;
    try {
      retValue = runGitCommand(directory, gitCommand);
    } catch (MojoExecutionException ex) {
      retValue = defaultValue;
    }
    return retValue;
  }

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


  public interface CliRunner {
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
