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
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
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
import java.util.regex.Pattern;

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
//  private Optional<Integer> candidatesOption = Optional.of(10);
//  private boolean exactMatchFlag = false;

  private Optional<String> matchOption = Optional.absent();

  /**
   * How many chars of the commit hash should be displayed? 7 is the default used by git.
   */
  private int abbrev = 7;

  /**
   * Skipping lightweight tags by default - that's how git-describe works by default.
   * {@link DescribeCommand#tags(Boolean)} for more details.
   */
  private boolean tagsFlag = false;

  private boolean alwaysFlag = true;

  /**
   * Corresponds to <pre>--long</pre>. Always use the <pre>TAG-N-HASH</pre> format, even when ON a tag.
   */
  private boolean forceLongFormat = false;

  /**
   * The string marker (such as "DEV") to be suffixed to the describe result when the working directory is dirty
   */
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
   * <pre>--long</pre>
   * <p/>
   * Always output the long format (the tag, the number of commits and the abbreviated commit name)
   * even when it matches a tag. This is useful when you want to see parts of the commit object name
   * in "describe" output, even when the commit in question happens to be a tagged version. Instead
   * of just emitting the tag name, it will describe such a commit as v1.2-0-gdeadbee (0th commit
   * since tag v1.2 that points at object deadbee....).
   * <p/>
   * <pre>false</pre> by default.
   */
  @NotNull
  public DescribeCommand forceLongFormat(@Nullable Boolean forceLongFormat) {
    if (forceLongFormat != null) {
      this.forceLongFormat = forceLongFormat;
      log("--long = %s", forceLongFormat);
    }
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
   * <pre>--tags</pre>
   * <p>
   *   Instead of using only the annotated tags, use any tag found in .git/refs/tags.
   *   This option enables matching a lightweight (non-annotated) tag.
   * </p>
   *
   * <p>Searching for lightweight tags is <b>false</b> by default.</p>
   *
   * Example:
   * <pre>
   *    b6a73ed - (HEAD, master)
   *    d37a598 - (v1.0-fixed-stuff) - a lightweight tag (with no message)
   *    9597545 - (v1.0) - an annotated tag
   *
   *  > git describe
   *    annotated-tag-2-gb6a73ed     # the nearest "annotated" tag is found
   *
   *  > git describe --tags
   *    lightweight-tag-1-gb6a73ed   # the nearest tag (including lightweights) is found
   * </pre>
   *
   * <p>
   *   Using only annotated tags to mark builds may be useful if you're using tags to help yourself with annotating
   *   things like "i'll get back to that" etc - you don't need such tags to be exposed. But if you want lightweight
   *   tags to be included in the search, enable this option.
   * </p>
   */
  @NotNull
  public DescribeCommand tags(@Nullable Boolean includeLightweightTagsInSearch) {
    if(includeLightweightTagsInSearch != null) {
      tagsFlag = includeLightweightTagsInSearch;
      log("--tags %s", includeLightweightTagsInSearch);
    }
    return this;
  }

  /**
   * Alias for {@link DescribeCommand#tags(Boolean)} with <b>true</b> value
   */
  public DescribeCommand tags() {
    return tags(true);
  }

  /**
   * Apply all configuration options passed in with {@param config}.
   * If a setting is null, it will not be applied - so for abbrev for example, the default 7 would be used.
   *
   * @return itself, after applying the settings
   */
  @NotNull
  public DescribeCommand apply(@Nullable GitDescribeConfig config) {
    if (config != null) {
      always(config.isAlways());
      dirty(config.getDirty());
      abbrev(config.getAbbrev());
      forceLongFormat(config.getForceLongFormat());
      tags(config.getTags());
      match(config.getMatch());
    }
    return this;
  }

  /**
   * <pre>--dirty[=mark]</pre>
   * Describe the working tree. It means describe HEAD and appends mark (<pre>-dirty</pre> by default) if the
   * working tree is dirty.
   *
   * @param dirtyMarker the marker name to be appended to the describe output when the workspace is dirty
   * @return itself, to allow fluent configuration
   */
  @NotNull
  public DescribeCommand dirty(@Nullable String dirtyMarker) {
    Optional<String> option = Optional.fromNullable(dirtyMarker);
    log("--dirty = \"%s\"", option.or(""));
    this.dirtyOption = option;
    return this;
  }

  /**
   * <pre>--match glob-pattern</pre>
   * Consider only those tags which match the given glob pattern.
   *
   * @param match the glob style pattern to match against the tag names
   * @return itself, to allow fluent configuration
   */
  @NotNull
  public DescribeCommand match(@Nullable String pattern) {
    matchOption = Optional.fromNullable(pattern);
    log("--match = \"%s\"", matchOption.or(""));
    return this;
  }

  @Override
  public DescribeResult call() throws GitAPIException {
    // needed for abbrev id's calculation
    ObjectReader objectReader = repo.newObjectReader();

    // get tags
    Map<ObjectId, String> tagObjectIdToName = findTagObjectIds(repo, tagsFlag);

    // get current commit
    RevCommit headCommit = findHeadObjectId(repo);
    ObjectId headCommitId = headCommit.getId();

    // check if dirty
    boolean dirty = findDirtyState(repo);

    if (isATag(headCommit, tagObjectIdToName)) {
      String tagName = tagObjectIdToName.get(headCommit);
      log("The commit we're on is a Tag ([%s]), returning.", tagName);

      return new DescribeResult(tagName, dirty, dirtyOption);
    }

    if (foundZeroTags(tagObjectIdToName)) {
      return new DescribeResult(objectReader, headCommitId, dirty, dirtyOption)
          .withCommitIdAbbrev(abbrev);
    }

    // get commits, up until the nearest tag
    List<RevCommit> commits = findCommitsUntilSomeTag(repo, headCommit, tagObjectIdToName);

    // check how far away from a tag we are

    int distance = distanceBetween(repo, headCommit, commits.get(0));
    String tagName = tagObjectIdToName.get(commits.get(0));
    Pair<Integer, String> howFarFromWhichTag = Pair.of(distance, tagName);

    // if it's null, no tag's were found etc, so let's return just the commit-id
    return createDescribeResult(objectReader, headCommitId, dirty, howFarFromWhichTag);
  }

  /**
   * Prepares the final result of this command.
   * It tries to put as much information as possible into the result,
   * and will fallback to a plain commit hash if nothing better is returnable.
   * <p/>
   * The exact logic is following what <pre>git-describe</pre> would do.
   */
  private DescribeResult createDescribeResult(ObjectReader objectReader, ObjectId headCommitId, boolean dirty, @Nullable Pair<Integer, String> howFarFromWhichTag) {
    if (howFarFromWhichTag == null) {
      return new DescribeResult(objectReader, headCommitId, dirty, dirtyOption)
          .withCommitIdAbbrev(abbrev);

    } else if (howFarFromWhichTag.first > 0 || forceLongFormat) {
      return new DescribeResult(objectReader, howFarFromWhichTag.second, howFarFromWhichTag.first, headCommitId, dirty, dirtyOption)
          .withCommitIdAbbrev(abbrev); // we're a bit away from a tag

    } else if (howFarFromWhichTag.first == 0) {
      return new DescribeResult(howFarFromWhichTag.second)
          .withCommitIdAbbrev(abbrev); // we're ON a tag

    } else if (alwaysFlag) {
      return new DescribeResult(objectReader, headCommitId)
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

  private List<RevCommit> findCommitsUntilSomeTag(Repository repo, RevCommit head, @NotNull Map<ObjectId, String> tagObjectIdToName) {
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
  private Map<ObjectId, String> findTagObjectIds(@NotNull Repository repo, boolean tagsFlag) {
    Map<ObjectId, String> commitIdsToTagNames = newHashMap();

    RevWalk walk = new RevWalk(repo);
    try {
      walk.markStart(walk.parseCommit(repo.resolve("HEAD")));

      List<Ref> tagRefs = Git.wrap(repo).tagList().call();

      String matchPattern = createMatchPattern();
      Pattern regex = Pattern.compile(matchPattern);
      for (Ref tagRef : tagRefs) {
        walk.reset();
        String name = tagRef.getName();
        if(!regex.matcher(name).matches()) {
            log("Skipping tagRef with name " + name + " as it doesn't match " + matchPattern);
            continue;
        }
        ObjectId resolvedCommitId = repo.resolve(name);

        // todo that's a bit of a hack...
        try {
          RevTag revTag = walk.parseTag(resolvedCommitId);
          ObjectId taggedCommitId = revTag.getObject().getId();

          commitIdsToTagNames.put(taggedCommitId, trimFullTagName(name));
        } catch (IncorrectObjectTypeException ex) {
          // it's an lightweight tag! (yeah, really)
          if(tagsFlag) {
            // --tags means "include lightweight tags"
            log("Including lightweight tag [%s]", name);
            commitIdsToTagNames.put(resolvedCommitId, trimFullTagName(name));
          }
        } catch (Exception ignored) {
          log("Failed while parsing [%s] -- %s", tagRef, ignored);
        }
      }

      return commitIdsToTagNames;
    } catch (Exception e) {
      log("Unable to locate tags\n[%s]", Throwables.getStackTraceAsString(e));
    } finally {
      walk.release();
    }

    return Collections.emptyMap();
  }

  private String createMatchPattern() {
    if(!matchOption.isPresent())
      return ".*";

    StringBuffer buf = new StringBuffer();
    buf.append("^refs/tags/\\Q");
    buf.append(matchOption.get().replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q"));
    buf.append("\\E$");
    return buf.toString();
  }

  @VisibleForTesting
  static String trimFullTagName(@NotNull String tagName) {
    return tagName.replaceFirst("refs/tags/", "");
  }

  private void log(String msg, Object... interpolations) {
    loggerBridge.log(msg, interpolations);
  }

}

