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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.sun.tools.javac.util.Pair;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.jetbrains.annotations.Nullable;

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

  //  boolean containsFlag = false;
//  boolean allFlag = false;
//  boolean tagsFlag = false;
//  boolean longFlag = false;
//  Optional<Integer> abbrevOption = Optional.absent();
//  Optional<Integer> candidatesOption = Optional.of(10);
//  boolean exactMatchFlag = false;
  boolean alwaysFlag = false;
  //  Optional<String> matchOption = Optional.absent();
  Optional<String> dirtyOption = Optional.absent();

  /**
   * Creates a new describe command which interacts with a single repository
   *
   * @param repo the {@link org.eclipse.jgit.lib.Repository} this command should interact with
   */
  static DescribeCommand on(Repository repo) {
    return new DescribeCommand(repo);
  }

  /**
   * Creates a new describe command which interacts with a single repository
   *
   * @param repo the {@link org.eclipse.jgit.lib.Repository} this command should interact with
   */
  protected DescribeCommand(Repository repo) {
    super(repo);
  }

  @Override
  public DescribeResult call() throws GitAPIException {
    // get tags
    Map<ObjectId, String> tagObjectIdToName = findTagObjectIds(repo);

    // get current commit
    RevCommit headCommit = findHeadObjectId(repo);

    if (isATag(headCommit, tagObjectIdToName)) {
      String tagName = tagObjectIdToName.get(headCommit);
      return new DescribeResult(tagName);
    }

    // get commits, up until the nearest tag
    List<RevCommit> commits = findCommitsUntilSomeTag(repo, headCommit, tagObjectIdToName);

    // check how far away from a tag we are
    Pair<Integer, String> howFarFromWhichTag = findDistanceFromTag(repo, headCommit, tagObjectIdToName);

    // if it's null, no tag's were found etc, so let's return just the commit-id
    if(howFarFromWhichTag == null) {
      return new DescribeResult(headCommit);
    }

    if(howFarFromWhichTag.fst > 0) {
      return new DescribeResult(howFarFromWhichTag.snd, howFarFromWhichTag.fst, headCommit.getId()); // we're a bit away from a tag
    } else if(howFarFromWhichTag.fst == 0) {
      return new DescribeResult(howFarFromWhichTag.snd); // we're ON a tag
    } else {
      return new DescribeResult(headCommit.getId()); // we have no tags! display the commit
    }
  }

  @Nullable
  private Pair<Integer, String> findDistanceFromTag(final Repository repo, final RevCommit headCommit, Map<ObjectId, String> tagObjectIdToName) {
    final RevWalk revWalk = new RevWalk(repo);

    Collection<Pair<Integer, String>> distancesFromTags = Collections2.transform(tagObjectIdToName.entrySet(), new Function<Map.Entry<ObjectId, String>, Pair<Integer, String>>() {
      @Override
      public Pair<Integer, String> apply(Map.Entry<ObjectId, String> input) {
        RevCommit endCommit = revWalk.lookupCommit(input.getKey());
        try {
          revWalk.markUninteresting(endCommit);
          return Pair.of(RevWalkUtils.count(revWalk, headCommit, endCommit), input.getValue());
        } catch (IOException e) {
          return Pair.of(Integer.MAX_VALUE, input.getValue());
        }
      }
    });

    if(distancesFromTags.isEmpty()) {
      return null;
    }

    // I want Scala collections back -_-

    Map<Integer, String> distancesFromTagsMap = newHashMap();
    for (Pair<Integer, String> distancesFromTag : distancesFromTags) {
      distancesFromTagsMap.put(distancesFromTag.fst, distancesFromTag.snd);
    }

    Integer nearestDistance = Collections.min(distancesFromTagsMap.keySet());

    return Pair.of(nearestDistance, distancesFromTagsMap.get(nearestDistance));
  }

  boolean isATag(RevCommit headCommit, Map<ObjectId, String> tagObjectIdToName) {
    return tagObjectIdToName.containsKey(headCommit);
  }

  RevCommit findHeadObjectId(Repository repo) throws RuntimeException {
    try {
      ObjectId headId = repo.resolve("HEAD");

      RevWalk walk = new RevWalk(repo);
      RevCommit headCommit = walk.lookupCommit(headId);
      walk.dispose();

      return headCommit;
    } catch (IOException ex) {
      throw new RuntimeException("Unable to obtain HEAD commit!", ex);
    }
  }

  static List<RevCommit> findCommitsUntilSomeTag(Repository repo, RevCommit head, Map<ObjectId, String> tagObjectIdToName) {
    RevWalk revWalk = new RevWalk(repo);

    Queue<RevCommit> q = newLinkedList();
    q.add(head);

    List<RevCommit> taggedcommits = newLinkedList();
    Set<ObjectId> seen = newHashSet();

    while (q.size() > 0) {
      RevCommit commit = q.remove();
      if (tagObjectIdToName.containsKey(commit.getId())) {
        taggedcommits.add(commit);
        // don't consider commits that are farther away than this tag
        continue;
      }

      try {
        if (commit.getParentCount() == 0) {
          continue;
        }
      } catch (NullPointerException ex) {
        continue; // erm... I'd expect parentCount not to fail when no parents... but it does.
      }

      for (ObjectId oid : commit.getParents()) {
        if (!seen.contains(oid)) {
          seen.add(oid);
          q.add(revWalk.lookupCommit(oid));
        }
      }
    }

    revWalk.dispose();

    return taggedcommits;
  }


  static Map<ObjectId, String> findTagObjectIds(Repository repo) {
    Map<String, Ref> tags = repo.getTags();
    Map<ObjectId, String> refToName = newHashMap();

    for (Map.Entry<String, Ref> stringRefEntry : tags.entrySet()) {
      refToName.put(stringRefEntry.getValue().getObjectId(), stringRefEntry.getKey());
    }

    return ImmutableMap.copyOf(refToName);
  }
}

