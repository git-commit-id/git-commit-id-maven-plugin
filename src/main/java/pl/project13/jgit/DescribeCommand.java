/*
 * This file is part of git-commit-id-plugin by Konrad Malawski <konrad.malawski@java.pl>
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

package pl.project13.jgit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.project13.maven.git.GitDescribeConfig;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.StdOutLoggerBridge;
import pl.project13.maven.git.util.Pair;

import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Implements git's <pre>describe</pre> command.
 * <p/>
 * <code><pre>
 *  usage: git describe [options] <committish>*
 *  or: git describe [options] --dirty
 * <p/>
 *   --contains            find the tag that comes after the commit
 *   --debug               debug search strategy on stderr
 *   --all                 use any ref in .git/refs
 *   --tags                use any tag in .git/refs/tags
 *   --long                always use long format
 *   --abbrev[=<n>]        use <n> digits to display SHA-1s
 *   --exact-match         only output exact matches
 *   --candidates <n>      consider <n> most recent tags (default: 10)
 *   --match <pattern>     only consider tags matching <pattern>
 *   --always              show abbreviated commit object as fallback
 *   --dirty[=<mark>]      append <mark> on dirty working tree (default: "-dirty")
 * </pre></code>
 *
 * @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 */
public class DescribeCommand extends GitCommand<DescribeResult> {

  private LoggerBridge loggerBridge;

//   todo not yet implemented options:
//  private boolean containsFlag = false;
//  private boolean allFlag = false;
//  private boolean tagsFlag = false;
//  private boolean longFlag = false;
//  private Optional<Integer> candidatesOption = Optional.of(10);
//  private boolean exactMatchFlag = false;
//  private Optional<String> matchOption = Optional.absent();

  /** How many chars of the commit hash should be displayed? 7 is the default used by git. */
  private int abbrev = 7;
  private boolean alwaysFlag = true;
  private Optional<String> dirtyOption = Optional.absent();

  /**
   * Creates a new describe command which interacts with a single repository
   *
   * @param repo the {@link org.eclipse.jgit.lib.Repository} this command should interact with
   */
  @NotNull
  public static DescribeCommand on(Repository repo) {
    return new DescribeCommand(repo);
  }

  /**
   * Creates a new describe command which interacts with a single repository
   *
   * @param repo the {@link org.eclipse.jgit.lib.Repository} this command should interact with
   */
  private DescribeCommand(Repository repo) {
    this(repo, true);
  }

  private DescribeCommand(Repository repo, boolean verbose) {
    super(repo);
    initDefaultLoggerBridge(verbose);
    setVerbose(verbose);
  }

  private void initDefaultLoggerBridge(boolean verbose) {
    loggerBridge = new StdOutLoggerBridge(verbose);
  }

  @NotNull
  public DescribeCommand setVerbose(boolean verbose) {
    loggerBridge.setVerbose(verbose);
    return this;
  }

  @NotNull
  public DescribeCommand withLoggerBridge(LoggerBridge bridge) {
    this.loggerBridge = bridge;
    return this;
  }

  /**
   * <pre>--always</pre>
   * <p/>
   * Show uniquely abbreviated commit object as fallback.
   * <p/>
   * <pre>true</pre> by default.
   */
  @NotNull
  public DescribeCommand always(boolean always) {
    this.alwaysFlag = always;
    log("--always = %s", always);
    return this;
  }

  /**
   * <pre>--abbrev=N</pre>
   * <p/>
   * Instead of using the default <em>7 hexadecimal digits</em> as the abbreviated object name,
   * use <b>N</b> digits, or as many digits as needed to form a unique object name.
   * <p/>
   * An <n> of 0 will suppress long format, only showing the closest tag.
   */
  @NotNull
  public DescribeCommand abbrev(@Nullable Integer n) {
    if (n != null) {
      Preconditions.checkArgument(n < 41, String.format("N (commit abbres length) must be < 41. (Was:[%s])", n));
      Preconditions.checkArgument(n >= 0, String.format("N (commit abbrev length) must be positive! (Was [%s])", n));
      log("--abbrev = %s", n);
      abbrev = n;
    }
    return this;
  }

  /**
   * Apply all configuration options passed in with {@param config}.
   * If a setting is null, it will not be applied - so for abbrev for example, the default 7 would be used.
   * @return itself, after applying the settings
   */
  @NotNull
  public DescribeCommand apply(@Nullable GitDescribeConfig config) {
    if (config != null) {
      always(config.isAlways());
      dirty(config.getDirty());
      abbrev(config.getAbbrev());
    }
    return this;
  }

  /**
   *
   * @param dirtyMarker the marker name to be appended to the describe output when the workspace is dirty
   * @return itself, to allow fluent configuration
   */
  @NotNull
  public DescribeCommand dirty(@Nullable String dirtyMarker) {
    if (dirtyMarker != null && dirtyMarker.length() > 0) {
      log("--dirty = \"-%s\"", dirtyMarker);
      this.dirtyOption = Optional.fromNullable(dirtyMarker);
    }
    return this;
  }

  @Override
  public DescribeResult call() throws GitAPIException {
    // get tags
    Map<ObjectId, String> tagObjectIdToName = findTagObjectIds(repo);

    // get current commit
    RevCommit headCommit = findHeadObjectId(repo);
    ObjectId headCommitId = headCommit.getId();

    // check if dirty
    boolean dirty = findDirtyState(repo);

    if (isATag(headCommit, tagObjectIdToName)) {
      String tagName = tagObjectIdToName.get(headCommit);
      log("The commit we're on is a Tag ([%s]), returning.", tagName);

      return new DescribeResult(tagName, dirty);
    }

    if (foundZeroTags(tagObjectIdToName)) {
      return new DescribeResult(headCommitId, dirty, dirtyOption)
          .withCommitIdAbbrev(abbrev);
    }

    // get commits, up until the nearest tag
    List<RevCommit> commits = findCommitsUntilSomeTag(repo, headCommit, tagObjectIdToName);

    // check how far away from a tag we are

    int distance = distanceBetween(repo, headCommit, commits.get(0));
    String tagName = tagObjectIdToName.get(commits.get(0));
    Pair<Integer, String> howFarFromWhichTag = Pair.of(distance, tagName);

    // if it's null, no tag's were found etc, so let's return just the commit-id
    return createDescribeResult(headCommitId, dirty, howFarFromWhichTag);
  }

  /**
   * Prepares the final result of this command.
   * It tries to put as much information as possible into the result,
   * and will fallback to a plain commit hash if nothing better is returnable.
   *
   * The exact logic is following what <pre>git-describe</pre> would do.
   */
  private DescribeResult createDescribeResult(ObjectId headCommitId, boolean dirty, @Nullable Pair<Integer, String> howFarFromWhichTag) {
    if (howFarFromWhichTag == null) {
      return new DescribeResult(headCommitId, dirty, dirtyOption)
          .withCommitIdAbbrev(abbrev);

    } else if (howFarFromWhichTag.first > 0) {
      return new DescribeResult(howFarFromWhichTag.second, howFarFromWhichTag.first, headCommitId, dirty, dirtyOption)
          .withCommitIdAbbrev(abbrev); // we're a bit away from a tag

    } else if (howFarFromWhichTag.first == 0) {
      return new DescribeResult(howFarFromWhichTag.second)
          .withCommitIdAbbrev(abbrev); // we're ON a tag

    } else if (alwaysFlag) {
      return new DescribeResult(headCommitId)
          .withCommitIdAbbrev(abbrev); // we have no tags! display the commit

    } else {
      return DescribeResult.EMPTY;
    }
  }

  private static boolean foundZeroTags(@NotNull Map<ObjectId, String> tags) {
    return tags.isEmpty();
  }

  @VisibleForTesting
  boolean findDirtyState(Repository repo) throws GitAPIException {
    Git git = Git.wrap(repo);
    Status status = git.status().call();

    boolean isDirty = !status.isClean();

    log("Repo is in dirty state = [%s] ", isDirty);
    return isDirty;
  }

  @VisibleForTesting
  static boolean isATag(ObjectId headCommit, @NotNull Map<ObjectId, String> tagObjectIdToName) {
    return tagObjectIdToName.containsKey(headCommit);
  }

  RevCommit findHeadObjectId(@NotNull Repository repo) throws RuntimeException {
    try {
      ObjectId headId = repo.resolve("HEAD");

      RevWalk walk = new RevWalk(repo);
      RevCommit headCommit = walk.lookupCommit(headId);
      walk.dispose();

      log("HEAD is [%s] ", headCommit.getName());
      return headCommit;
    } catch (IOException ex) {
      throw new RuntimeException("Unable to obtain HEAD commit!", ex);
    }
  }

  List<RevCommit> findCommitsUntilSomeTag(Repository repo, RevCommit head, @NotNull Map<ObjectId, String> tagObjectIdToName) {
    RevWalk revWalk = new RevWalk(repo);
    try {
      revWalk.markStart(head);

      for (RevCommit commit : revWalk) {
        ObjectId objId = commit.getId();
        String lookup = tagObjectIdToName.get(objId);
        if (lookup != null) {
          return Collections.singletonList(commit);
        }
      }

      throw new RuntimeException("Did not find any commits until some tag");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Calculates the distance (number of commits) between the given parent and child commits.
   *
   *
   * @return distance (number of commits) between the given commits
   * @see <a href="https://github.com/mdonoughe/jgit-describe/blob/master/src/org/mdonoughe/JGitDescribeTask.java">mdonoughe/jgit-describe/blob/master/src/org/mdonoughe/JGitDescribeTask.java</a>
   */
  private int distanceBetween(@NotNull Repository repo, @NotNull RevCommit child, @NotNull RevCommit parent) {
    RevWalk revWalk = new RevWalk(repo);

    try {
      revWalk.markStart(child);

      Set<ObjectId> seena = newHashSet();
      Set<ObjectId> seenb = newHashSet();
      Queue<RevCommit> q = newLinkedList();

      q.add(revWalk.parseCommit(child));
      int distance = 0;
      ObjectId parentId = parent.getId();

      while (q.size() > 0) {
        RevCommit commit = q.remove();
        ObjectId commitId = commit.getId();

        if (seena.contains(commitId)) {
          continue;
        }
        seena.add(commitId);

        if (parentId.equals(commitId)) {
          // don't consider commits that are included in this commit
          seeAllParents(revWalk, commit, seenb);
          // remove things we shouldn't have included
          for (ObjectId oid : seenb) {
            if (seena.contains(oid)) {
              distance--;
            }
          }
          seena.addAll(seenb);
          continue;
        }

        for (ObjectId oid : commit.getParents()) {
          if (!seena.contains(oid)) {
            q.add(revWalk.parseCommit(oid));
          }
        }
        distance++;
      }

      return distance;

    } catch (Exception e) {
      throw new RuntimeException(String.format("Unable to calculate distance between [%s] and [%s]", child, parent), e);
    } finally {
      revWalk.dispose();
    }
  }

  private static void seeAllParents(@NotNull RevWalk revWalk, RevCommit child, @NotNull Set<ObjectId> seen) throws IOException {
    Queue<RevCommit> q = newLinkedList();
    q.add(child);

    while (q.size() > 0) {
      RevCommit commit = q.remove();
      for (ObjectId oid : commit.getParents()) {
        if (seen.contains(oid)) {
          continue;
        }
        seen.add(oid);
        q.add(revWalk.parseCommit(oid));
      }
    }
  }

  // git commit id -> its tag
  private Map<ObjectId, String> findTagObjectIds(@NotNull Repository repo) {
    Map<ObjectId, String> commitIdsToTagNames = newHashMap();

    RevWalk walk = new RevWalk(repo);
    try {
      walk.markStart(walk.parseCommit(repo.resolve("HEAD")));


      List<Ref> tagRefs = Git.wrap(repo).tagList().call();

      for (Ref tagRef : tagRefs) {
        walk.reset();
        String name = tagRef.getName();
        ObjectId resolvedCommitId = repo.resolve(name);

        // todo that's a bit of a hack... FIX ME
        try {
          RevTag revTag = walk.parseTag(resolvedCommitId);
          ObjectId taggedCommitId = revTag.getObject().getId();

          commitIdsToTagNames.put(taggedCommitId, trimFullTagName(name));
        } catch (Exception ex) {
          // ignore
        }

        commitIdsToTagNames.put(resolvedCommitId, trimFullTagName(name));
      }

      return commitIdsToTagNames;
    } catch (Exception e) {
      log("Unable to locate tags\n[%s]", Throwables.getStackTraceAsString(e));
    } finally {
      walk.release();
    }

    return Collections.emptyMap();
  }

  @VisibleForTesting
  static String trimFullTagName(@NotNull String tagName) {
    return tagName.replaceFirst("refs/tags/", "");
  }

  private void log(String msg, Object... interpolations) {
    loggerBridge.log(msg, interpolations);
  }

}

