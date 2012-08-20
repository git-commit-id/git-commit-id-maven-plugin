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
import com.google.common.base.*;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
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
import org.jetbrains.annotations.Nullable;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.util.Pair;
import pl.project13.maven.git.log.StdOutLoggerBridge;

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

  private boolean verbose = false;
  private LoggerBridge loggerBridge;

  //  boolean containsFlag = false;
//  boolean allFlag = false;
//  boolean tagsFlag = false;
//  boolean longFlag = false;
//  Optional<Integer> abbrevOption = Optional.absent();
//  Optional<Integer> candidatesOption = Optional.of(10);
//  boolean exactMatchFlag = false;
  boolean alwaysFlag = true;
  //  Optional<String> matchOption = Optional.absent();
  Optional<String> dirtyOption = Optional.absent();

  /**
   * Creates a new describe command which interacts with a single repository
   *
   * @param repo the {@link org.eclipse.jgit.lib.Repository} this command should interact with
   */
  public static DescribeCommand on(Repository repo) {
    return new DescribeCommand(repo);
  }

  /**
   * Creates a new describe command which interacts with a single repository
   *
   * @param repo the {@link org.eclipse.jgit.lib.Repository} this command should interact with
   */
  protected DescribeCommand(Repository repo) {
    super(repo);
    initDefaultLoggerBridge(verbose);
  }

  private void initDefaultLoggerBridge(boolean verbose) {
    loggerBridge = new StdOutLoggerBridge(verbose);
  }

  public DescribeCommand setVerbose(boolean verbose) {
    this.verbose = verbose;
    initDefaultLoggerBridge(verbose);
    return this;
  }

  private void log(String msg, Object... interpolations) {
    loggerBridge.log(msg, interpolations);
  }

  @Override
  public DescribeResult call() throws GitAPIException {
    // get tags
    Map<ObjectId, String> tagObjectIdToName = findTagObjectIds(repo);

    // get current commit
    RevCommit headCommit = findHeadObjectId(repo);

    // check if dirty
    boolean dirty = findDirtyState(repo);

    if (isATag(headCommit, tagObjectIdToName)) {
      String tagName = tagObjectIdToName.get(headCommit);
      log("The commit we're on is a Tag ([%s]), returning.", tagName);

      return new DescribeResult(tagName, dirty);
    }

    if (foundZeroTags(tagObjectIdToName)) {
      return new DescribeResult(headCommit.getId(), dirty, dirtyOption);
    }

    // get commits, up until the nearest tag
    List<RevCommit> commits = findCommitsUntilSomeTag(repo, headCommit, tagObjectIdToName);

    // check how far away from a tag we are
//    RevWalk walk = new RevWalk(repo);
//    RevCommit commit;
//    try {
//    commit = walk.parseCommit(repo.resolve("HEAD"));
//      commit = walk.parseCommit(commits.get(0));
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
//    walk.dispose();
//
//    Pair<Integer, String> howFarFromWhichTag = findDistanceFromTag(repo, commit, tagObjectIdToName);

    int distance = distanceBetween(repo, headCommit, commits.get(0));
    String tagName = tagObjectIdToName.get(commits.get(0));
    Pair<Integer, String> howFarFromWhichTag = Pair.of(distance, tagName);

    System.out.println("howFarFromWhichTag = " + howFarFromWhichTag);

    // if it's null, no tag's were found etc, so let's return just the commit-id
    if (howFarFromWhichTag == null) {
      return new DescribeResult(headCommit.getId(), dirty, dirtyOption);
    } else if (howFarFromWhichTag.first > 0) {
      return new DescribeResult(howFarFromWhichTag.second, howFarFromWhichTag.first, headCommit.getId()); // we're a bit away from a tag
    } else if (howFarFromWhichTag.first == 0) {
      return new DescribeResult(howFarFromWhichTag.second); // we're ON a tag
    } else if (alwaysFlag) {
      return new DescribeResult(headCommit.getId()); // we have no tags! display the commit
    } else {
      return DescribeResult.EMPTY;
    }
  }

  private boolean foundZeroTags(Map<ObjectId, String> tags) {
    return tags.isEmpty();
  }

  @VisibleForTesting
  boolean findDirtyState(Repository repo) throws GitAPIException {
    Git git = Git.wrap(repo);
    Status status = git.status().call();

//    System.out.println("add  = " + status.getAdded());
//    System.out.println("chng = " + status.getChanged());
//    System.out.println("conf = " + status.getConflicting());
//    System.out.println("miss = " + status.getMissing());
//    System.out.println("mod  = " + status.getModified());
//    System.out.println("rm   = " + status.getRemoved());
//    System.out.println("un   = " + status.getUntracked());

    boolean isDirty = !status.isClean();

    log("Repo is in dirty state = [%s] ", isDirty);
    return isDirty;
  }

  @Nullable
  Pair<Integer, String> findDistanceFromTag(final Repository repo, final RevCommit headCommit, Map<ObjectId, String> tagObjectIdToName) {
    final RevWalk revWalk = new RevWalk(repo);

    try {
      Collection<RevTag> tagCommits = Collections2.transform(tagObjectIdToName.values(), new Function<String, RevTag>() {
        @Override
        public RevTag apply(String input) {
          try {
            return revWalk.parseTag(repo.resolve(input));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });

      System.out.println("tagCommits = " + tagCommits);

      int minDistance = 0;
      String found = null;
      for (RevTag tagCommit : Iterables.filter(tagCommits, notNull())) {
        log("tagCommit = [%s]", tagCommit);

        RevCommit taggedCommit = revWalk.parseCommit(tagCommit.getObject().getId());
        int maybeMin = distanceBetween(repo, headCommit, taggedCommit);

        if (found == null || maybeMin < minDistance) {
          minDistance = maybeMin;
          found = tagCommit.getTagName();
        }
      }

      System.out.println("found = " + found);
      System.out.println("minDistance = " + minDistance);

      return Pair.of(minDistance < 0 ? 0 : minDistance, found);
    } catch (Exception ex) {
      throw new RuntimeException("Unable to calculate distance from tags", ex);
    } finally {
      revWalk.dispose();
    }
  }

  private Predicate<? super RevTag> notNull() {
    return new Predicate<RevTag>() {
      @Override
      public boolean apply(RevTag input) {
        return input != null;
      }
    };
  }

  @VisibleForTesting
  static boolean isATag(ObjectId headCommit, Map<ObjectId, String> tagObjectIdToName) {
    return tagObjectIdToName.containsKey(headCommit);
  }

  RevCommit findHeadObjectId(Repository repo) throws RuntimeException {
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

  List<RevCommit> findCommitsUntilSomeTag(Repository repo, RevCommit head, Map<ObjectId, String> tagObjectIdToName) {
    System.out.println("repo = [" + repo + "], head = [" + head + "], tagObjectIdToName = [" + tagObjectIdToName + "]");
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
   * @param child
   * @param parent
   * @return distance (number of commits) between the given commits
   * @see <a href="https://github.com/mdonoughe/jgit-describe/blob/master/src/org/mdonoughe/JGitDescribeTask.java">mdonoughe/jgit-describe/blob/master/src/org/mdonoughe/JGitDescribeTask.java</a>
   */
  private int distanceBetween(Repository repo, RevCommit child, RevCommit parent) {
    Preconditions.checkNotNull(child, "Child commit must not be null.");
    Preconditions.checkNotNull(parent, "Parent commit must not be null.");

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

  private static void seeAllParents(RevWalk revWalk, RevCommit child, Set<ObjectId> seen) throws IOException {
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
  private Map<ObjectId, String> findTagObjectIds(Repository repo) {
    Map<ObjectId, String> commitIdsToTagNames = newHashMap();

    RevWalk walk = new RevWalk(repo);
    try {
      walk.markStart(walk.parseCommit(repo.resolve("HEAD")));


      List<Ref> tagRefs = Git.wrap(repo).tagList().call();
      System.out.println("tagRefs = " + tagRefs);

      for (Ref tagRef : tagRefs) {
        walk.reset();
        String name = tagRef.getName();
        ObjectId resolvedCommitId = repo.resolve(name);

        // todo that's a bit of a hack... FIX ME
        try {
          RevTag revTag = walk.parseTag(resolvedCommitId);
          ObjectId taggedCommitId = revTag.getObject().getId();

          commitIdsToTagNames.put(taggedCommitId, trimFullTagName(name));
          log("TAG: [%s] taged as [%s] ", taggedCommitId, name);
        } catch (Exception ex) {
          // ignore
        }

        commitIdsToTagNames.put(resolvedCommitId, trimFullTagName(name));
        log("TAG: [%s] taged as [%s] ", resolvedCommitId, name);
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
  static String trimFullTagName(String tagName) {
    return tagName.replaceFirst("refs/tags/", "");
  }
}

