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

package pl.project13.jgit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import pl.project13.jgit.dummy.DatedRevTag;
import pl.project13.maven.git.GitDescribeConfig;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.util.Pair;

import java.io.IOException;
import java.util.*;

/**
 * Implements git's <pre>describe</pre> command.
 */
public class DescribeCommand extends GitCommand<DescribeResult> {

  private LoggerBridge log;
  private JGitCommon jGitCommon;

  //  TODO not yet implemented options:
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
   * @param repo the {@link Repository} this command should interact with
   * @param log logger bridge to direct logs to
   */
  @NotNull
  public static DescribeCommand on(Repository repo, LoggerBridge log) {
    return new DescribeCommand(repo, log);
  }

  /**
   * Creates a new describe command which interacts with a single repository
   *
   * @param repo the {@link org.eclipse.jgit.lib.Repository} this command should interact with
   */
  private DescribeCommand(Repository repo, @NotNull LoggerBridge log) {
    super(repo);
    this.jGitCommon = new JGitCommon(log);
    this.log = log;
  }

  /**
   * <pre>--always</pre>
   *
   * Show uniquely abbreviated commit object as fallback.
   *
   * <pre>true</pre> by default.
   */
  @NotNull
  public DescribeCommand always(boolean always) {
    this.alwaysFlag = always;
    log.info("--always = {}", always);
    return this;
  }

  /**
   * <pre>--long</pre>
   *
   * Always output the long format (the tag, the number of commits and the abbreviated commit name)
   * even when it matches a tag. This is useful when you want to see parts of the commit object name
   * in "describe" output, even when the commit in question happens to be a tagged version. Instead
   * of just emitting the tag name, it will describe such a commit as v1.2-0-gdeadbee (0th commit
   * since tag v1.2 that points at object deadbee....).
   *
   * <pre>false</pre> by default.
   */
  @NotNull
  public DescribeCommand forceLongFormat(@Nullable Boolean forceLongFormat) {
    if (forceLongFormat != null && forceLongFormat) {
      this.forceLongFormat = true;
      log.info("--long = {}", true);
    }
    return this;
  }

  /**
   * <pre>--abbrev=N</pre>
   *
   * Instead of using the default <em>7 hexadecimal digits</em> as the abbreviated object name,
   * use <b>N</b> digits, or as many digits as needed to form a unique object name.
   *
   * An `n` of 0 will suppress long format, only showing the closest tag.
   */
  @NotNull
  public DescribeCommand abbrev(@Nullable Integer n) {
    if (n != null) {
      Preconditions.checkArgument(n < 41, String.format("N (commit abbrev length) must be < 41. (Was:[%s])", n));
      Preconditions.checkArgument(n >= 0, String.format("N (commit abbrev length) must be positive! (Was [%s])", n));
      log.info("--abbrev = {}", n);
      abbrev = n;
    }
    return this;
  }

  /**
   * <pre>--tags</pre>
   * <p>
   * Instead of using only the annotated tags, use any tag found in .git/refs/tags.
   * This option enables matching a lightweight (non-annotated) tag.
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
   *  $ git describe
   *    annotated-tag-2-gb6a73ed     # the nearest "annotated" tag is found
   *
   *  $ git describe --tags
   *    lightweight-tag-1-gb6a73ed   # the nearest tag (including lightweights) is found
   * </pre>
   *
   * <p>
   * Using only annotated tags to mark builds may be useful if you're using tags to help yourself with annotating
   * things like "i'll get back to that" etc - you don't need such tags to be exposed. But if you want lightweight
   * tags to be included in the search, enable this option.
   * </p>
   */
  @NotNull
  public DescribeCommand tags(@Nullable Boolean includeLightweightTagsInSearch) {
    if (includeLightweightTagsInSearch != null && includeLightweightTagsInSearch) {
      tagsFlag = includeLightweightTagsInSearch;
      log.info("--tags = {}", includeLightweightTagsInSearch);
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
   * Apply all configuration options passed in with `config`.
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
    log.info("--dirty = {}", option.or(""));
    this.dirtyOption = option;
    return this;
  }

  /**
   * <pre>--match glob-pattern</pre>
   * Consider only those tags which match the given glob pattern.
   *
   * @param pattern the glob style pattern to match against the tag names
   * @return itself, to allow fluent configuration
   */
  @NotNull
  public DescribeCommand match(@Nullable String pattern) {
    if (!"*".equals(pattern)) {
      matchOption = Optional.fromNullable(pattern);
      log.info("--match = {}", matchOption.or(""));
    }
    return this;
  }

  @Override
  public DescribeResult call() throws GitAPIException {
    // needed for abbrev id's calculation
    ObjectReader objectReader = repo.newObjectReader();

    // get tags
    String matchPattern = createMatchPattern();
    Map<ObjectId, List<String>> tagObjectIdToName = jGitCommon.findTagObjectIds(repo, tagsFlag, matchPattern);

    // get current commit
    RevCommit headCommit = findHeadObjectId(repo);
    ObjectId headCommitId = headCommit.getId();

    // check if dirty
    boolean dirty = findDirtyState(repo);

    if (hasTags(headCommit, tagObjectIdToName) && !forceLongFormat) {
      String tagName = tagObjectIdToName.get(headCommit).iterator().next();
      log.info("The commit we're on is a Tag ([{}]) and forceLongFormat == false, returning.", tagName);

      return new DescribeResult(tagName, dirty, dirtyOption);
    }

    // get commits, up until the nearest tag
    List<RevCommit> commits;
    try {
      commits = jGitCommon.findCommitsUntilSomeTag(repo, headCommit, tagObjectIdToName);
    } catch (Exception e) {
      if (alwaysFlag) {
        // Show uniquely abbreviated commit object as fallback
        commits = Collections.emptyList();
      } else {
        throw e;
      }
    }

    // if there is no tags or any tag is not on that branch then return generic describe
    if (foundZeroTags(tagObjectIdToName) || commits.isEmpty()) {
      return new DescribeResult(objectReader, headCommitId, dirty, dirtyOption)
          .withCommitIdAbbrev(abbrev);
    }

    // check how far away from a tag we are

    int distance = jGitCommon.distanceBetween(repo, headCommit, commits.get(0));
    String tagName = tagObjectIdToName.get(commits.get(0)).iterator().next();
    Pair<Integer, String> howFarFromWhichTag = Pair.of(distance, tagName);

    // if it's null, no tag's were found etc, so let's return just the commit-id
    return createDescribeResult(objectReader, headCommitId, dirty, howFarFromWhichTag);
  }

  /**
   * Prepares the final result of this command.
   * It tries to put as much information as possible into the result,
   * and will fallback to a plain commit hash if nothing better is returnable.
   *
   * The exact logic is following what <pre>git-describe</pre> would do.
   */
  private DescribeResult createDescribeResult(ObjectReader objectReader, ObjectId headCommitId, boolean dirty, @Nullable Pair<Integer, String> howFarFromWhichTag) {
    if (howFarFromWhichTag == null) {
      return new DescribeResult(objectReader, headCommitId, dirty, dirtyOption)
          .withCommitIdAbbrev(abbrev);

    } else if (howFarFromWhichTag.first > 0 || forceLongFormat) {
      return new DescribeResult(objectReader, howFarFromWhichTag.second, howFarFromWhichTag.first, headCommitId, dirty, dirtyOption, forceLongFormat)
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

  private static boolean foundZeroTags(@NotNull Map<ObjectId, List<String>> tags) {
    return tags.isEmpty();
  }

  @VisibleForTesting
  boolean findDirtyState(Repository repo) throws GitAPIException {
    return JGitCommon.isRepositoryInDirtyState(repo);
  }

  @VisibleForTesting
  static boolean hasTags(ObjectId headCommit, @NotNull Map<ObjectId, List<String>> tagObjectIdToName) {
    return tagObjectIdToName.containsKey(headCommit);
  }

  RevCommit findHeadObjectId(@NotNull Repository repo) throws RuntimeException {
    return jGitCommon.findHeadObjectId(repo);
  }

  private String createMatchPattern() {
    if (!matchOption.isPresent()) {
      return ".*";
    }

    return jGitCommon.createMatchPattern(matchOption.get());
  }
}
