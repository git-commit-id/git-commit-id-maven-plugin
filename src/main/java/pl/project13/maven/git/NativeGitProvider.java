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

import pl.project13.maven.git.log.LoggerBridge;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class NativeGitProvider extends GitDataProvider {

  private transient ProcessRunner runner;

  final File dotGitDirectory;

  final long nativeGitTimeoutInMs;

  final File canonical;

  @Nonnull
  public static NativeGitProvider on(@Nonnull File dotGitDirectory, long nativeGitTimeoutInMs, @Nonnull LoggerBridge log) {
    return new NativeGitProvider(dotGitDirectory, nativeGitTimeoutInMs, log);
  }

  NativeGitProvider(@Nonnull File dotGitDirectory, long nativeGitTimeoutInMs, @Nonnull LoggerBridge log) {
    super(log);
    this.dotGitDirectory = dotGitDirectory;
    this.nativeGitTimeoutInMs = nativeGitTimeoutInMs;
    try {
      this.canonical = dotGitDirectory.getCanonicalFile();
    } catch (IOException ex) {
      throw new RuntimeException(new GitCommitIdExecutionException("Passed a invalid directory, not a GIT repository: " + dotGitDirectory, ex));
    }
  }

  @Override
  public void init() throws GitCommitIdExecutionException {
    // noop ...
  }

  @Override
  public String getBuildAuthorName() throws GitCommitIdExecutionException {
    try {
      return runGitCommand(canonical, nativeGitTimeoutInMs, "config --get user.name");
    } catch (NativeCommandException e) {
      if (e.getExitCode() == 1) { // No config file found
        return "";
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getBuildAuthorEmail() throws GitCommitIdExecutionException {
    try {
      return runGitCommand(canonical, nativeGitTimeoutInMs, "config --get user.email");
    } catch (NativeCommandException e) {
      if (e.getExitCode() == 1) { // No config file found
        return "";
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public void prepareGitToExtractMoreDetailedRepoInformation() throws GitCommitIdExecutionException {
  }

  @Override
  public String getBranchName() throws GitCommitIdExecutionException {
    return getBranch(canonical);
  }

  private String getBranch(File canonical) throws GitCommitIdExecutionException {
    String branch;
    try {
      branch = runGitCommand(canonical, nativeGitTimeoutInMs, "symbolic-ref " + evaluateOnCommit);
      if (branch != null) {
        branch = branch.replace("refs/heads/", "");
      }
    } catch (NativeCommandException e) {
      // it seems that git repo is in 'DETACHED HEAD'-State, using Commit-Id as Branch
      String err = e.getStderr();
      if (err != null) {
        boolean noSymbolicRef = err.contains("ref " + evaluateOnCommit + " is not a symbolic ref");
        boolean noSuchRef = err.contains("No such ref: " + evaluateOnCommit);
        if (noSymbolicRef || noSuchRef) {
          branch = getCommitId();
        } else {
          throw new RuntimeException(e);
        }
      } else {
        throw new RuntimeException(e);
      }
    }
    return branch;
  }

  @Override
  public String getGitDescribe() throws GitCommitIdExecutionException {
    final String argumentsForGitDescribe = getArgumentsForGitDescribe(gitDescribe);
    return runQuietGitCommand(canonical, nativeGitTimeoutInMs, "describe" + argumentsForGitDescribe);
  }

  private String getArgumentsForGitDescribe(GitDescribeConfig describeConfig) {
    if (describeConfig == null) {
      return "";
    }

    StringBuilder argumentsForGitDescribe = new StringBuilder();
    boolean hasCommitish = (evaluateOnCommit != null) && !evaluateOnCommit.equals("HEAD");
    if (hasCommitish) {
      argumentsForGitDescribe.append(" " + evaluateOnCommit);
    }

    if (describeConfig.isAlways()) {
      argumentsForGitDescribe.append(" --always");
    }

    final String dirtyMark = describeConfig.getDirty();
    if ((dirtyMark != null) && !dirtyMark.isEmpty()) {
      // we can either have evaluateOnCommit or --dirty flag set
      if (hasCommitish) {
        log.warn("You might use strange arguments since it's unfortunately not supported to have evaluateOnCommit and the --dirty flag for the describe command set at the same time");
      } else {
        argumentsForGitDescribe.append(" --dirty=").append(dirtyMark);
      }
    }

    final String matchOption = describeConfig.getMatch();
    if (matchOption != null && !matchOption.isEmpty()) {
      argumentsForGitDescribe.append(" --match=").append(matchOption);
    }

    argumentsForGitDescribe.append(" --abbrev=").append(describeConfig.getAbbrev());

    if (describeConfig.getTags()) {
      argumentsForGitDescribe.append(" --tags");
    }

    if (describeConfig.getForceLongFormat()) {
      argumentsForGitDescribe.append(" --long");
    }
    return argumentsForGitDescribe.toString();
  }

  @Override
  public String getCommitId() throws GitCommitIdExecutionException {
    boolean evaluateOnCommitIsSet = (evaluateOnCommit != null) && !evaluateOnCommit.equals("HEAD");
    if (evaluateOnCommitIsSet) {
      // if evaluateOnCommit represents a tag we need to perform the rev-parse on the actual commit reference
      // in case evaluateOnCommit is not a reference rev-list will just return the argument given
      // and thus it's always safe(r) to unwrap it
      // however when evaluateOnCommit is not set we don't want to waste calls to the native binary
      String actualCommitId = runQuietGitCommand(canonical, nativeGitTimeoutInMs, "rev-list -n 1 " + evaluateOnCommit);
      return runQuietGitCommand(canonical, nativeGitTimeoutInMs, "rev-parse " + actualCommitId);
    } else {
      return runQuietGitCommand(canonical, nativeGitTimeoutInMs, "rev-parse HEAD");
    }
  }

  @Override
  public String getAbbrevCommitId() throws GitCommitIdExecutionException {
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
  public boolean isDirty() throws GitCommitIdExecutionException {
    return !tryCheckEmptyRunGitCommand(canonical, nativeGitTimeoutInMs, "status -s");
  }

  @Override
  public String getCommitAuthorName() throws GitCommitIdExecutionException {
    return runQuietGitCommand(canonical, nativeGitTimeoutInMs, "log -1 --pretty=format:%an " + evaluateOnCommit);
  }

  @Override
  public String getCommitAuthorEmail() throws GitCommitIdExecutionException {
    return runQuietGitCommand(canonical, nativeGitTimeoutInMs, "log -1 --pretty=format:%ae " + evaluateOnCommit);
  }

  @Override
  public String getCommitMessageFull() throws GitCommitIdExecutionException {
    return runQuietGitCommand(canonical, nativeGitTimeoutInMs, "log -1 --pretty=format:%B " + evaluateOnCommit);
  }

  @Override
  public String getCommitMessageShort() throws GitCommitIdExecutionException {
    return runQuietGitCommand(canonical, nativeGitTimeoutInMs, "log -1 --pretty=format:%s " + evaluateOnCommit);
  }

  @Override
  public String getCommitTime() throws GitCommitIdExecutionException {
    String value =  runQuietGitCommand(canonical, nativeGitTimeoutInMs, "log -1 --pretty=format:%ct " + evaluateOnCommit);
    SimpleDateFormat smf = getSimpleDateFormatWithTimeZone();
    return smf.format(Long.parseLong(value) * 1000L);
  }

  @Override
  public String getTags() throws GitCommitIdExecutionException {
    final String result = runQuietGitCommand(canonical, nativeGitTimeoutInMs, "tag --contains " + evaluateOnCommit);
    return result.replace('\n', ',');
  }

  @Override
  public String getRemoteOriginUrl() throws GitCommitIdExecutionException {
    return getOriginRemote(canonical, nativeGitTimeoutInMs);
  }

  @Override
  public String getClosestTagName() throws GitCommitIdExecutionException {
    try {
      StringBuilder argumentsForGitDescribe = new StringBuilder();
      argumentsForGitDescribe.append("describe " + evaluateOnCommit + " --abbrev=0");
      if (gitDescribe != null) {
        if (gitDescribe.getTags()) {
          argumentsForGitDescribe.append(" --tags");
        }

        final String matchOption = gitDescribe.getMatch();
        if (matchOption != null && !matchOption.isEmpty()) {
          argumentsForGitDescribe.append(" --match=").append(matchOption);
        }
      }
      return runGitCommand(canonical, nativeGitTimeoutInMs, argumentsForGitDescribe.toString());
    } catch (NativeCommandException ignore) {
      // could not find any tags to describe
    }
    return "";
  }

  @Override
  public String getClosestTagCommitCount() throws GitCommitIdExecutionException {
    String closestTagName = getClosestTagName();
    if (closestTagName != null && !closestTagName.trim().isEmpty()) {
      return runQuietGitCommand(canonical, nativeGitTimeoutInMs, "rev-list " + closestTagName + ".." + evaluateOnCommit + " --count");
    }
    return "";
  }

  @Override
  public String getTotalCommitCount() throws GitCommitIdExecutionException {
    return runQuietGitCommand(canonical, nativeGitTimeoutInMs, "rev-list " + evaluateOnCommit + " --count");
  }

  @Override
  public void finalCleanUp() throws GitCommitIdExecutionException {
  }

  private String getOriginRemote(File directory, long nativeGitTimeoutInMs) throws GitCommitIdExecutionException {
    try {
      String remoteUrl = runGitCommand(directory, nativeGitTimeoutInMs, "ls-remote --get-url");

      return stripCredentialsFromOriginUrl(remoteUrl);
    } catch (NativeCommandException ignore) {
      // No remote configured to list refs from
    }
    return null;
  }

  /**
   * Runs a maven command and returns {@code true} if output was non empty.
   * Can be used to short cut reading output from command when we know it may be a rather long one.
   * Return true if the result is empty.
   **/
  private boolean tryCheckEmptyRunGitCommand(File directory, long nativeGitTimeoutInMs, String gitCommand) {
    try {
      String env = System.getenv("GIT_PATH");
      String exec = env == null ? "git" : env;
      String command = String.format("%s %s", exec, gitCommand);

      return getRunner().runEmpty(directory, nativeGitTimeoutInMs, command);
    } catch (IOException ex) {
      // Error means "non-empty"
      return false;
      // do nothing...
    }
  }

  private String runQuietGitCommand(File directory, long nativeGitTimeoutInMs, String gitCommand) {
    final String env = System.getenv("GIT_PATH");
    final String exec = env == null ? "git" : env;
    final String command = String.format("%s %s", exec, gitCommand);

    try {
      return getRunner().run(directory, nativeGitTimeoutInMs, command.trim()).trim();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String runGitCommand(File directory, long nativeGitTimeoutInMs, String gitCommand) throws NativeCommandException {
    final String env = System.getenv("GIT_PATH");
    String exec = env == null ? "git" : env;
    final String command = String.format("%s %s", exec, gitCommand);

    try {
      return getRunner().run(directory, nativeGitTimeoutInMs, command.trim()).trim();
    } catch (NativeCommandException e) {
      throw e;
    } catch (IOException e) {
      throw new RuntimeException(e);
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
    String run(File directory, long nativeGitTimeoutInMs, String command) throws IOException;

    /** Run a command and return false if it contains at least one output line*/
    boolean runEmpty(File directory, long nativeGitTimeoutInMs, String command) throws IOException;
  }

  public static class NativeCommandException extends IOException {
    private static final long serialVersionUID = 3511033422542257748L;
    private final int exitCode;
    private final String command;
    private final File directory;
    private final String stdout;
    private final String stderr;

    public NativeCommandException(
            int exitCode,
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
    public String run(File directory, long nativeGitTimeoutInMs, String command) throws IOException {
      String output = "";
      try {
        final StringBuilder commandResult = new StringBuilder();

        final Function<String, Boolean> stdoutConsumer = line -> {
          if (line != null) {
            commandResult.append(line).append("\n");
          }
          // return true to indicate we want to read more content
          return true;
        };
        runProcess(directory, nativeGitTimeoutInMs, command, stdoutConsumer);

        output = commandResult.toString();
      } catch (final InterruptedException ex) {
        throw new IOException(ex);
      }
      return output;
    }

    @Override
    public boolean runEmpty(File directory, long nativeGitTimeoutInMs, String command) throws IOException {
      final AtomicBoolean empty = new AtomicBoolean(true);

      try {
        final Function<String, Boolean> stdoutConsumer = line -> {
          if (line != null) {
            empty.set(false);
          }
          // return false to indicate we don't need to read more content
          return false;
        };
        runProcess(directory, nativeGitTimeoutInMs, command, stdoutConsumer);
      } catch (final InterruptedException ex) {
        throw new IOException(ex);
      }
      return empty.get(); // was non-empty
    }

    private void runProcess(
            File directory,
            long nativeGitTimeoutInMs,
            String command,
            final Function<String, Boolean> stdoutConsumer) throws InterruptedException, IOException {

      final ProcessBuilder builder = new ProcessBuilder(command.split("\\s"));
      final Process proc = builder.directory(directory).start();

      final ExecutorService executorService = Executors.newFixedThreadPool(2);
      final StringBuilder errMsg = new StringBuilder();

      final Future<Optional<RuntimeException>> stdoutFuture = executorService.submit(
              new CallableBufferedStreamReader(proc.getInputStream(), stdoutConsumer));
      final Future<Optional<RuntimeException>> stderrFuture = executorService.submit(
              new CallableBufferedStreamReader(proc.getErrorStream(),
                  line -> {
                    errMsg.append(line);
                    // return true to indicate we want to read more content
                    return true;
                  }));

      if (!proc.waitFor(nativeGitTimeoutInMs, TimeUnit.MILLISECONDS)) {
        proc.destroy();
        executorService.shutdownNow();
        throw new RuntimeException(String.format("GIT-Command '%s' did not finish in %d milliseconds", command, nativeGitTimeoutInMs));
      }

      try {
        stdoutFuture.get()
            .ifPresent(e -> {
              throw e;
            });
        stderrFuture.get()
            .ifPresent(e -> {
              throw e;
            });
      } catch (final ExecutionException e) {
        throw new RuntimeException(String.format("Executing GIT-Command '%s' threw an '%s' exception.", command, e.getMessage()), e);
      }

      executorService.shutdown();
      if (proc.exitValue() != 0) {
        throw new NativeCommandException(proc.exitValue(), command, directory, "", errMsg.toString());
      }
    }

    private static class CallableBufferedStreamReader implements Callable<Optional<RuntimeException>> {
      private final InputStream is;
      private final Function<String, Boolean> streamConsumer;

      CallableBufferedStreamReader(final InputStream is, final Function<String, Boolean> streamConsumer) {
        this.is = is;
        this.streamConsumer = streamConsumer;
      }

      @Override
      public Optional<RuntimeException> call() {
        RuntimeException thrownException = null;
        try (final BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
          for (String line = br.readLine();
               line != null;
               line = br.readLine()) {
            if (!streamConsumer.apply(line)) {
              break;
            }
          }
        } catch (final IOException e) {
          thrownException = new RuntimeException(String.format("Executing GIT-Command threw an '%s' exception.", e.getMessage()), e);
        }

        return Optional.ofNullable(thrownException);
      }
    }
  }
}
