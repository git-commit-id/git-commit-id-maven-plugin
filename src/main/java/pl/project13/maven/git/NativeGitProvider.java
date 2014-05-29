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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.apache.maven.plugin.MojoExecutionException;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;

import static java.nio.charset.Charset.defaultCharset;


public class NativeGitProvider extends GitDataProvider {

  private transient CliRunner runner;

  private String dateFormat;

  File directory;

  File canonical;

  private static final int REMOTE_COLS = 3;

  private NativeGitProvider(CliRunner runner, String dateFormat) {
    this.runner = runner;
    this.dateFormat = dateFormat;
  }

  @NotNull
  public static NativeGitProvider on(@NotNull File dotGitDirectory) {
    return new NativeGitProvider(dotGitDirectory);
  }

  NativeGitProvider(@NotNull File dotGitDirectory){
    this.directory = dotGitDirectory;
  }


 @NotNull
  public NativeGitProvider withLoggerBridge(LoggerBridge bridge) {
    super.loggerBridge = bridge;
    return this;
  }

  @NotNull
  public NativeGitProvider setVerbose(boolean verbose) {
    super.verbose = verbose;
    super.loggerBridge.setVerbose(verbose);
    return this;
  }

  public NativeGitProvider setPrefixDot(String prefixDot) {
    super.prefixDot = prefixDot;
    return this;
  }

  public NativeGitProvider setAbbrevLength(int abbrevLength) {
    super.abbrevLength = abbrevLength;
    return this;
  }

  public NativeGitProvider setDateFormat(String dateFormat) {
    super.dateFormat = dateFormat;
    return this;
  }

  public NativeGitProvider setGitDescribe(GitDescribeConfig gitDescribe){
    super.gitDescribe = gitDescribe;
    return this;
  }

  @Override
  protected void init() throws MojoExecutionException{
    try{
      canonical = directory.getCanonicalFile();
    } catch (IOException ex) {
      throw new MojoExecutionException("Passed a invalid directory, not a GIT repository: " + directory, ex);
    }
  }

  @Override
  protected String getBuildAuthorName(){
    return tryToRunGitCommand(canonical, "config --global user.name", null);
  }

  @Override
  protected String getBuildAuthorEmail(){
    return tryToRunGitCommand(canonical, "config --global user.email", null);
  }

  @Override
  protected void prepareGitToExtractMoreDetailedReproInformation() throws MojoExecutionException{
  }

  @Override
  protected String getBranchName() throws IOException{
    return getBranch(canonical);
  }

  @Override
  protected String getGitDescribe() throws MojoExecutionException{
    String argumentsForGitDescribe = getArgumentsForGitDescribe(super.gitDescribe);
    String gitDescribe = tryToRunGitCommand(canonical, "describe " + argumentsForGitDescribe, null);
    return gitDescribe;
  }

  private String getArgumentsForGitDescribe(GitDescribeConfig gitDescribe){
    if(gitDescribe != null){
      return getArgumentsForGitDescribeAndDescibeNotNull(gitDescribe);
    }else{
      return "";
    }
  }
  
  private String getArgumentsForGitDescribeAndDescibeNotNull(GitDescribeConfig gitDescribe){
    StringBuilder argumentsForGitDescribe = new StringBuilder();

    if(gitDescribe.isAlways()){
      argumentsForGitDescribe.append("--always ");
    }

    String dirtyMark = gitDescribe.getDirty();
    if(dirtyMark != null && !dirtyMark.isEmpty()){
      // Option: --dirty[=<mark>]
      // TODO: Code Injection? Or does the CliRunner escape Arguments?
      argumentsForGitDescribe.append("--dirty=" + dirtyMark + " ");
    }

    argumentsForGitDescribe.append("--abbrev=" + gitDescribe.getAbbrev() + " ");

    if(gitDescribe.getTags()){
      argumentsForGitDescribe.append("--tags ");
    }

    if(gitDescribe.getForceLongFormat()){
      argumentsForGitDescribe.append("--long ");
    }
    return argumentsForGitDescribe.toString();
  }

  @Override
  protected String getCommitId(){
    return tryToRunGitCommand(canonical, "rev-parse HEAD", null);
  }

  @Override
  protected String getAbbrevCommitId() throws MojoExecutionException{
    // we could run: tryToRunGitCommand(canonical, "rev-parse --short="+abbrevLength+" HEAD", null);
    // but minimum length for --short is 4, our abbrevLength could be 2
    String commitId = getCommitId();
    String abbrevCommitId = "";

    if(commitId != null && !commitId.isEmpty()){
      abbrevCommitId = commitId.substring(0, abbrevLength);
    }

    return abbrevCommitId;
  }

  @Override
  protected String getCommitAuthorName(){
    return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%cn\"", null);
  }

  @Override
  protected String getCommitAuthorEmail(){
    return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%ce\"", null);
  }

  @Override
  protected String getCommitMessageFull(){
     return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%B\"", null);
  }

  @Override
  protected String getCommitMessageShort(){
    return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%s\"", null);
  }

  @Override
  protected String getCommitTime(){
    return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%ci\"", null);
  }

  @Override
  protected String getRemoteOriginUrl() throws MojoExecutionException{
    return getOriginRemote(canonical);
  }

  @Override
  protected void finalCleanUp(){
  }



  private String getOriginRemote(File directory) throws MojoExecutionException {
    String remoteUrl = null;
    try{
      String remotes = runGitCommand(directory, "remote -v");
      for (String line : remotes.split("\n")) {
        String trimmed = line.trim();
        if (trimmed.startsWith("origin")) {
          String[] splited = trimmed.split("\\s+");
          if (splited.length != REMOTE_COLS) {
            throw new MojoExecutionException("Unsupported GIT output - verbose remote address:" + line);
          }
          remoteUrl =  splited[1];
        }
      }
    }catch(Exception e){
      throw new MojoExecutionException("Error ", e);
    }
    return remoteUrl;
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


  private String getBranch(File canonical) {
    String branch = tryToRunGitCommand(canonical, "symbolic-ref HEAD", null);
    if (branch != null) {
      branch = branch.replace("refs/heads/", "");
    }
    return branch;
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
          String message = String.format("Git command exited with invalid status [%d]: `%s`", proc.exitValue(),output);
          throw new IOException(message);
        }
        return output;
      } catch (InterruptedException ex) {
        throw new IOException(ex);
      }
    }
  }

  private static String convertStreamToString(InputStream inputStream) {
    if(inputStream == null){
      IllegalArgumentException e = new IllegalArgumentException("Something went wrong InputStream is null");
      e.printStackTrace();
      throw e;
    }
    Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
    String nextContent = "";
    try{
      if(scanner.hasNext()){
        nextContent = scanner.next();
      }
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      scanner.close();
    }
    return nextContent;
  }
}
