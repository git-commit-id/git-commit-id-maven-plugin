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

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import pl.project13.git.api.GitDescribeConfig;
import pl.project13.git.api.GitException;
import pl.project13.git.impl.AbstractBaseGitProvider;
import pl.project13.maven.git.log.LoggerBridge;


public class NativeGitProvider extends AbstractBaseGitProvider<NativeGitProvider> {

  private transient ProcessRunner runner;

  final File dotGitDirectory;

  final File canonical;

  private static final int REMOTE_COLS = 3;

  @NotNull
  public static NativeGitProvider on(@NotNull File dotGitDirectory, @NotNull LoggerBridge loggerBridge) {
    return new NativeGitProvider(dotGitDirectory, loggerBridge);
  }

  NativeGitProvider(@NotNull File dotGitDirectory, @NotNull LoggerBridge loggerBridge) {
    super(loggerBridge);
    this.dotGitDirectory = dotGitDirectory;
    try {
      this.canonical = dotGitDirectory.getCanonicalFile();
    } catch (Exception ex) {
      throw new RuntimeException(new GitException("Passed a invalid directory, not a GIT repository: " + dotGitDirectory, ex));
    }
  }


  @NotNull
  public NativeGitProvider setVerbose(boolean verbose) {
    super.loggerBridge.setVerbose(verbose);
    return this;
  }

  @Override
  public String getBuildAuthorName() {
    try {
      return runGitCommand(canonical, "config --get user.name");
    } catch (NativeCommandException e) {
      if (e.getExitCode() == 1) { // No config file found
        return "";
      }
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String getBuildAuthorEmail() {
    try {
      return runGitCommand(canonical, "config --get user.email");
    } catch (NativeCommandException e) {
      if (e.getExitCode() == 1) { // No config file found
        return "";
      }
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String getBranchName() {
    return getBranch(canonical);
  }

  private String getBranch(File canonical) {
    String branch = null;
    try{
      branch = runGitCommand(canonical, "symbolic-ref HEAD");
      if (branch != null) {
        branch = branch.replace("refs/heads/", "");
      }
    } catch(NativeCommandException e) {
      // it seems that git repro is in 'DETACHED HEAD'-State, using Commid-Id as Branch
      String err = e.getStderr();
      if (err != null && err.contains("ref HEAD is not a symbolic ref")) {
        branch = getCommitId();
      } else {
        throw Throwables.propagate(e);
      }
    }
    return branch;
  }

  @Override
  public String getGitDescribe() {
    final String argumentsForGitDescribe = getArgumentsForGitDescribe(gitDescribe);
    final String gitDescribe = runQuietGitCommand(canonical, "describe" + argumentsForGitDescribe);
    return gitDescribe;
  }

  private String getArgumentsForGitDescribe(GitDescribeConfig describeConfig) {
    if (describeConfig == null) return "";

    StringBuilder argumentsForGitDescribe = new StringBuilder();

    if (describeConfig.isAlways()) {
      argumentsForGitDescribe.append(" --always");
    }

    final String dirtyMark = describeConfig.getDirty();
    if (dirtyMark != null && !dirtyMark.isEmpty()) {
      argumentsForGitDescribe.append(" --dirty=" + dirtyMark);
    }

    final String matchOption = describeConfig.getMatch();
    if (matchOption != null && !matchOption.isEmpty()) {
      argumentsForGitDescribe.append(" --match=" + matchOption);
    }

    argumentsForGitDescribe.append(" --abbrev=" + describeConfig.getAbbrev());

    if (describeConfig.getTags()) {
      argumentsForGitDescribe.append(" --tags");
    }

    if (describeConfig.getForceLongFormat()) {
      argumentsForGitDescribe.append(" --long");
    }
    return argumentsForGitDescribe.toString();
  }

  @Override
  public String getCommitId() {
    return runQuietGitCommand(canonical, "rev-parse HEAD");
  }

  @Override
  public String getAbbrevCommitId() {
    // we could run: tryToRunGitCommand(canonical, "rev-parse --short="+abbrevLength+" HEAD");
    // but minimum length for --short is 4, our abbrevLength could be 2
    String commitId = getCommitId();
    String abbrevCommitId = "";

    if (commitId != null && !commitId.isEmpty()) {
      abbrevCommitId = commitId.substring(0, abbrevLength);
    }

    return abbrevCommitId;
  }

  @Override
  public boolean isDirty() {
    return !tryCheckEmptyRunGitCommand(canonical, "status -s");
  }

  @Override
  public String getCommitAuthorName() {
    return runQuietGitCommand(canonical, "log -1 --pretty=format:%an");
  }

  @Override
  public String getCommitAuthorEmail() {
    return runQuietGitCommand(canonical, "log -1 --pretty=format:%ae");
  }

  @Override
  public String getCommitMessageFull() {
    return runQuietGitCommand(canonical, "log -1 --pretty=format:%B");
  }

  @Override
  public String getCommitMessageShort() {
    return runQuietGitCommand(canonical, "log -1 --pretty=format:%s");
  }

  @Override
  public String getCommitTime() {
    String value =  runQuietGitCommand(canonical, "log -1 --pretty=format:%ct");
    SimpleDateFormat smf = getSimpleDateFormatWithTimeZone();
    return smf.format(Long.parseLong(value)*1000L);
  }

  @Override
  public String getTags() {
    final String result = runQuietGitCommand(canonical, "tag --contains");
    return result.replace('\n', ',');
  }

  @Override
  public String getRemoteOriginUrl() throws GitException {
    return getOriginRemote(canonical);
  }
  
  @Override
  public String getClosestTagName(){
    try {
      return runGitCommand(canonical, "describe --abbrev=0 --tags");
    } catch (NativeCommandException ignore) {
      // could not find any tags to describe
    }
    return "";
  }

  @Override
  public String getClosestTagCommitCount(){
    String closestTagName = getClosestTagName();
    if(closestTagName != null && !closestTagName.trim().isEmpty()){
      return runQuietGitCommand(canonical, "rev-list "+closestTagName+"..HEAD --count");
    }
    return "";
  }

  private String getOriginRemote(File directory) throws GitException {
    String remoteUrl = null;
    String remotes = runQuietGitCommand(directory, "remote -v");

    // welcome to text output parsing hell! - no `\n` is not enough
    for (String line : Splitter.onPattern("\\((fetch|push)\\)?").split(remotes)) {
      String trimmed = line.trim();

      if (trimmed.startsWith("origin")) {
        String[] splited = trimmed.split("\\s+");
        if (splited.length != REMOTE_COLS - 1) { // because (fetch/push) was trimmed
          throw new GitException("Unsupported GIT output (verbose remote address): " + line);
        }
        remoteUrl = splited[1];
      }
    }
    return remoteUrl;
  }

  /**
   * Runs a maven command and returns {@code true} if output was non empty.
   * Can be used to short cut reading output from command when we know it may be a rather long one.
   *
   * Return true if the result is empty.
   *
   **/
  private boolean tryCheckEmptyRunGitCommand(File directory, String gitCommand) {
    try {
      String env = System.getenv("GIT_PATH");
      String exec = (env == null) ? "git" : env;
      String command = String.format("%s %s", exec, gitCommand);

      return getRunner().runEmpty(directory, command);
    } catch (IOException ex) {
        // Error means "non-empty"
      return false;
      // do nothing...
    }
  }

  private String runQuietGitCommand(File directory, String gitCommand) {
    final String env = System.getenv("GIT_PATH");
    final String exec = (env == null) ? "git" : env;
    final String command = String.format("%s %s", exec, gitCommand);

    try {
      return getRunner().run(directory, command.trim()).trim();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private String runGitCommand(File directory, String gitCommand) throws NativeCommandException {
    final String env = System.getenv("GIT_PATH");
    final String exec = (env == null) ? "git" : env;
    final String command = String.format("%s %s", exec, gitCommand);

    try {
      return getRunner().run(directory, command.trim()).trim();
    } catch (NativeCommandException e) {
      throw e;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private ProcessRunner getRunner() {
    if (runner == null) {
      runner = new JavaProcessRunner();
    }
    return runner;
  }

  public interface ProcessRunner {
    /** Run a command and return the entire output as a String - naive, we know. */
    String run(File directory, String command) throws IOException;
    /** Run a command and return false if it contains at least one output line*/
    boolean runEmpty(File directory, String command) throws IOException;
  }

  public static class NativeCommandException extends IOException
  {
    private final int exitCode;
    private final String command;
    private final File directory;
    private final String stdout;
    private final String stderr;

    public NativeCommandException(int exitCode,
                                  String command,
                                  File directory,
                                  String stdout,
                                  String stderr) {
      this.exitCode = exitCode;
      this.command = command;
      this.directory = directory;
      this.stdout = stdout;
      this.stderr = stderr;
    }

    public int getExitCode() {
      return exitCode;
    }

    public String getCommand() {
      return command;
    }

    public File getDirectory() {
      return directory;
    }

    public String getStdout() {
      return stdout;
    }

    public String getStderr() {
      return stderr;
    }

    @Override
    public String getMessage() {
      return format("Git command exited with invalid status [%d]: stdout: `%s`, stderr: `%s`", exitCode, stdout, stderr);
    }
  }

  protected static class JavaProcessRunner implements ProcessRunner {
    @Override
        public String run(File directory, String command) throws IOException {
          String output = "";
          try {
            ProcessBuilder builder = new ProcessBuilder(command.split("\\s"));
            final Process proc = builder.directory(directory).start();
            proc.waitFor();
            final InputStream is = proc.getInputStream();
            final InputStream err = proc.getErrorStream();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            final StringBuilder commandResult = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
              commandResult.append(line).append("\n");
            }

            if (proc.exitValue() != 0) {
              final StringBuilder errMsg = readStderr(err);
              throw new NativeCommandException(proc.exitValue(), command, directory, output, errMsg.toString());
            }
            output = commandResult.toString();
          } catch (InterruptedException ex) {
            throw new IOException(ex);
          }
          return output;
        }

    private StringBuilder readStderr(InputStream err) throws IOException {
      String line;
      final BufferedReader errReader = new BufferedReader(new InputStreamReader(err));
      final StringBuilder errMsg = new StringBuilder();
      while((line = errReader.readLine())!=null){
        errMsg.append(line);
      }
      return errMsg;
    }

//    @Override
//    public String run(File directory, String command) throws IOException {
//      String output;
//      try {
//        final ProcessBuilder builder = new ProcessBuilder(command.split("\\s"));
//        final Process proc = builder.directory(directory).start();
//        proc.waitFor();
//        InputStream is = proc.getInputStream();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//        final StringBuilder commandResult = new StringBuilder();
//
//        String line;
//        while ((line = reader.readLine()) != null) {
//          commandResult.append(line);
//        }
//
//        output = commandResult.toString();
//
//        if (proc.exitValue() != 0) {
//          String message = String.format("Git command exited with invalid status [%d]: `%s`", proc.exitValue(), output);
//          throw new IOException(message);
//        }
//      } catch (InterruptedException e) {
//        throw new RuntimeException("Unable to attach to git process!", e);
//      }
//      return output;
//    }

    @Override
    public boolean runEmpty(File directory, String command) throws IOException {
      boolean empty = true;

      try {
        ProcessBuilder builder = new ProcessBuilder(Lists.asList("/bin/sh", "-c", command.split("\\s")));
        final Process proc = builder.directory(directory).start();
        proc.waitFor();
        final InputStream is = proc.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        if (reader.readLine() != null) {
          empty = false;
        }

      } catch (InterruptedException ex) {
        throw new IOException(ex);
      }
      return empty; // was non-empty
    }
  }
}
