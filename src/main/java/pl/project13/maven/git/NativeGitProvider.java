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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.maven.plugin.MojoExecutionException;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;
import com.google.common.annotations.VisibleForTesting;
import java.io.*;

/**
* The idea of native git and major parts of this code was introduced by Krzysztof Suszyński <krzysztof.suszynski@gmail.com>
* in PR#93 (https://github.com/ktoso/maven-git-commit-id-plugin/pull/93/)
*/
public class NativeGitProvider extends GitDataProvider {

  private transient CliRunner runner;

  private String dateFormat;

  File dotGitDirectory;

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
    this.dotGitDirectory = dotGitDirectory;
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
      canonical = dotGitDirectory.getCanonicalFile();
    } catch (Exception ex) {
      throw new MojoExecutionException("Passed a invalid directory, not a GIT repository: " + dotGitDirectory, ex);
    }
  }

  @Override
  protected String getBuildAuthorName(){
    return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%an\"");
  }

  @Override
  protected String getBuildAuthorEmail(){
    return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%ae\"");
  }

  @Override
  protected void prepareGitToExtractMoreDetailedReproInformation() throws MojoExecutionException{
  }

  @Override
  protected String getBranchName() throws IOException{
    return getBranch(canonical);
  }

  private String getBranch(File canonical) {
    String branch = tryToRunGitCommand(canonical, "symbolic-ref HEAD");
    if (branch != null) {
      branch = branch.replace("refs/heads/", "");
    }
    return branch;
  }

  @Override
  protected String getGitDescribe() throws MojoExecutionException{
    String argumentsForGitDescribe = getArgumentsForGitDescribe(super.gitDescribe);
    String gitDescribe = tryToRunGitCommand(canonical, "describe " + argumentsForGitDescribe);
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
    return tryToRunGitCommand(canonical, "rev-parse HEAD");
  }

  @Override
  protected String getAbbrevCommitId() throws MojoExecutionException{
    // we could run: tryToRunGitCommand(canonical, "rev-parse --short="+abbrevLength+" HEAD");
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
    return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%cn\"");
  }

  @Override
  protected String getCommitAuthorEmail(){
    return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%ce\"");
  }

  @Override
  protected String getCommitMessageFull(){
     return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%B\"");
  }

  @Override
  protected String getCommitMessageShort(){
    return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%s\"");
  }

  @Override
  protected String getCommitTime(){
    return tryToRunGitCommand(canonical, "log -1 --pretty=format:\"%ci\"");
  }

  @Override
  protected String getRemoteOriginUrl() throws MojoExecutionException{
    return getOriginRemote(canonical);
  }

  @Override
  protected void finalCleanUp(){
  }

  @VisibleForTesting
  String getOriginRemote(File directory) throws MojoExecutionException {
    String remoteUrl = "";
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

  private String tryToRunGitCommand(File directory, String gitCommand) {
    String retValue = "";
    try {
      retValue = runGitCommand(directory, gitCommand);
    } catch (MojoExecutionException ex) {
      // do nothing
    }
    return retValue;
  }

  @VisibleForTesting
  String runGitCommand(File directory, String gitCommand) throws MojoExecutionException {
    try {
      String env = System.getenv("GIT_PATH");
      String exec = (env == null) ? "git" : env;
      String command = String.format("%s %s", exec, gitCommand);

      String result = getRunner().run(directory, command).trim();
      return result;
    }catch(IOException ex) {
      throw new MojoExecutionException("Could not run GIT command - GIT is not installed or not exists in system path? " + "Tried to run: 'git " + gitCommand + "'", ex);
    }
  }

  private CliRunner getRunner() {
    if (runner == null) {
      runner = new Runner();
    }
    return runner;
  }


  // CLI RUNNER

  public interface CliRunner {
    String run(File directory, String command) throws IOException;
  }

  protected static class Runner implements CliRunner {
    @Override
    public String run(File directory, String command) throws IOException {
      String output = "";
      try{
        ProcessBuilder builder = new ProcessBuilder(command.split("\\s"));
        final Process proc = builder.directory(directory).start();
        proc.waitFor();
        final InputStream is = proc.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final StringBuilder commandResult = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          commandResult.append(line + "\n");
        }

        if (proc.exitValue() != 0) {
          String message = String.format("Git command exited with invalid status [%d]: `%s`", proc.exitValue(),output);
          throw new IOException(message);
        }
        output = commandResult.toString();
      } catch (InterruptedException ex) {
        throw new IOException(ex);
      }
      return output;
    }
  } 
}
