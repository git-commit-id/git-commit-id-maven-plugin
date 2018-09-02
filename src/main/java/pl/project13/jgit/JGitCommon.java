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

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;

import pl.project13.jgit.dummy.DatedRevTag;

import com.google.common.annotations.VisibleForTesting;
import pl.project13.maven.git.GitDescribeConfig;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.util.Pair;

import javax.annotation.Nonnull;

public class JGitCommon {

  private final LoggerBridge log;

  public JGitCommon(LoggerBridge log) {
    this.log = log;
  }

  public Collection<String> getTags(Repository repo, final ObjectId objectId) throws GitAPIException {
    try (Git git = Git.wrap(repo)) {
      try (RevWalk walk = new RevWalk(repo)) {
        Collection<String> tags = getTags(git, objectId, walk);
        walk.dispose();
        return tags;
      }
    }
  }

  private Collection<String> getTags(final Git git, final ObjectId objectId, final RevWalk finalWalk) throws GitAPIException {
    return git.tagList().call()
            .stream()
            .filter(tagRef -> {
              try {
                final RevCommit tagCommit = finalWalk.parseCommit(tagRef.getObjectId());
                final RevCommit objectCommit = finalWalk.parseCommit(objectId);
                if (finalWalk.isMergedInto(objectCommit, tagCommit)) {
                  return true;
                }
              } catch (Exception ignored) {
                log.debug("Failed while getTags [{}] -- ", tagRef, ignored);
              }
              return false;
            })
            .map(tagRef -> tagRef.getName().replaceAll("refs/tags/", ""))
            .collect(Collectors.toList());
  }

  public String getClosestTagName(@Nonnull String evaluateOnCommit, @Nonnull Repository repo, GitDescribeConfig gitDescribe) {
    // TODO: Why does some tests fail when it gets headCommit from JGitprovider?
    RevCommit headCommit = findEvalCommitObjectId(evaluateOnCommit, repo);
    Pair<RevCommit, String> pair = getClosestRevCommit(repo, headCommit, gitDescribe);
    return pair.second;
  }

  public String getClosestTagCommitCount(@Nonnull String evaluateOnCommit, @Nonnull Repository repo, GitDescribeConfig gitDescribe) {
    // TODO: Why does some tests fail when it gets headCommit from JGitprovider?
    RevCommit headCommit = findEvalCommitObjectId(evaluateOnCommit, repo);
    Pair<RevCommit, String> pair = getClosestRevCommit(repo, headCommit, gitDescribe);
    RevCommit revCommit = pair.first;
    int distance = distanceBetween(repo, headCommit, revCommit);
    return String.valueOf(distance);
  }

  private Pair<RevCommit, String> getClosestRevCommit(@Nonnull Repository repo, RevCommit headCommit, GitDescribeConfig gitDescribe) {
    boolean includeLightweightTags = false;
    String matchPattern = ".*";
    if (gitDescribe != null) {
      includeLightweightTags = gitDescribe.getTags();
      if (!"*".equals(gitDescribe.getMatch())) {
        matchPattern = createMatchPattern(gitDescribe.getMatch());
      }
    }
    Map<ObjectId, List<String>> tagObjectIdToName = findTagObjectIds(repo, includeLightweightTags, matchPattern);
    if (tagObjectIdToName.containsKey(headCommit)) {
      String tagName = tagObjectIdToName.get(headCommit).iterator().next();
      return Pair.of(headCommit, tagName);
    }
    List<RevCommit> commits = findCommitsUntilSomeTag(repo, headCommit, tagObjectIdToName);
    RevCommit revCommit = commits.get(0);
    String tagName = tagObjectIdToName.get(revCommit).iterator().next();

    return Pair.of(revCommit, tagName);
  }

  protected String createMatchPattern(String pattern) {
    return "^refs/tags/\\Q" +
            pattern.replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q") +
            "\\E$";
  }

  protected Map<ObjectId, List<String>> findTagObjectIds(@Nonnull Repository repo, boolean includeLightweightTags, String matchPattern) {
    Map<ObjectId, List<DatedRevTag>> commitIdsToTags = getCommitIdsToTags(repo, includeLightweightTags, matchPattern);
    Map<ObjectId, List<String>> commitIdsToTagNames = transformRevTagsMapToDateSortedTagNames(commitIdsToTags);
    log.info("Created map: [{}]", commitIdsToTagNames);

    return commitIdsToTagNames;
  }

  protected RevCommit findEvalCommitObjectId(@Nonnull String evaluateOnCommit, @Nonnull Repository repo) throws RuntimeException {
    try {
      ObjectId evalCommitId = repo.resolve(evaluateOnCommit);

      try (RevWalk walk = new RevWalk(repo)) {
        RevCommit evalCommit = walk.parseCommit(evalCommitId);
        walk.dispose();

        log.info("evalCommit is [{}]", evalCommit.getName());
        return evalCommit;
      }
    } catch (IOException ex) {
      throw new RuntimeException("Unable to obtain " + evaluateOnCommit + " commit!", ex);
    }
  }

  protected Map<ObjectId, List<DatedRevTag>> getCommitIdsToTags(@Nonnull Repository repo, boolean includeLightweightTags, String matchPattern) {
    Map<ObjectId, List<DatedRevTag>> commitIdsToTags = new HashMap<>();

    try (RevWalk walk = new RevWalk(repo)) {
      walk.markStart(walk.parseCommit(repo.resolve("HEAD")));

      List<Ref> tagRefs = Git.wrap(repo).tagList().call();
      Pattern regex = Pattern.compile(matchPattern);
      log.info("Tag refs [{}]", tagRefs);

      for (Ref tagRef : tagRefs) {
        walk.reset();
        String name = tagRef.getName();
        if (!regex.matcher(name).matches()) {
          log.info("Skipping tagRef with name [{}] as it doesn't match [{}]", name, matchPattern);
          continue;
        }
        ObjectId resolvedCommitId = repo.resolve(name);

        // TODO that's a bit of a hack...
        try {
          final RevTag revTag = walk.parseTag(resolvedCommitId);
          ObjectId taggedCommitId = revTag.getObject().getId();
          log.info("Resolved tag [{}] [{}], points at [{}] ", revTag.getTagName(), revTag.getTaggerIdent(), taggedCommitId);

          // sometimes a tag, may point to another tag, so we need to unpack it
          while (isTagId(taggedCommitId)) {
            taggedCommitId = walk.parseTag(taggedCommitId).getObject().getId();
          }

          if (commitIdsToTags.containsKey(taggedCommitId)) {
            commitIdsToTags.get(taggedCommitId).add(new DatedRevTag(revTag));
          } else {
            commitIdsToTags.put(taggedCommitId, new ArrayList<>(Collections.singletonList(new DatedRevTag(revTag))));
          }

        } catch (IncorrectObjectTypeException ex) {
          // it's an lightweight tag! (yeah, really)
          if (includeLightweightTags) {
            // --tags means "include lightweight tags"
            log.info("Including lightweight tag [{}]", name);

            DatedRevTag datedRevTag = new DatedRevTag(resolvedCommitId, name);

            if (commitIdsToTags.containsKey(resolvedCommitId)) {
              commitIdsToTags.get(resolvedCommitId).add(datedRevTag);
            } else {
              commitIdsToTags.put(resolvedCommitId, new ArrayList<>(Collections.singletonList(datedRevTag)));
            }
          }
        } catch (Exception ignored) {
          log.info("Failed while parsing [{}] -- ", tagRef, ignored);
        }
      }

      for (Map.Entry<ObjectId, List<DatedRevTag>> entry : commitIdsToTags.entrySet()) {
        log.info("key [{}], tags => [{}] ", entry.getKey(), entry.getValue());
      }
      return commitIdsToTags;
    } catch (Exception e) {
      log.info("Unable to locate tags", e);
    }
    return Collections.emptyMap();
  }

  /** Checks if the given object id resolved to a tag object */
  private boolean isTagId(ObjectId objectId) {
    return objectId.toString().startsWith("tag ");
  }

  protected HashMap<ObjectId, List<String>> transformRevTagsMapToDateSortedTagNames(Map<ObjectId, List<DatedRevTag>> commitIdsToTags) {
    HashMap<ObjectId, List<String>> commitIdsToTagNames = new HashMap<>();
    for (Map.Entry<ObjectId, List<DatedRevTag>> objectIdListEntry : commitIdsToTags.entrySet()) {
      List<String> tagNames = transformRevTagsMapEntryToDateSortedTagNames(objectIdListEntry);

      commitIdsToTagNames.put(objectIdListEntry.getKey(), tagNames);
    }
    return commitIdsToTagNames;
  }

  private List<String> transformRevTagsMapEntryToDateSortedTagNames(Map.Entry<ObjectId, List<DatedRevTag>> objectIdListEntry) {
    return objectIdListEntry.getValue()
            .stream()
            .sorted(datedRevTagComparator())
            .map(datedRevTag -> trimFullTagName(datedRevTag.tagName))
            .collect(Collectors.toList());
  }

  private Comparator<DatedRevTag> datedRevTagComparator() {
    return (revTag, revTag2) -> revTag2.date.compareTo(revTag.date);
  }

  @VisibleForTesting
  protected String trimFullTagName(@Nonnull String tagName) {
    return tagName.replaceFirst("refs/tags/", "");
  }

  public List<RevCommit> findCommitsUntilSomeTag(Repository repo, RevCommit head, @Nonnull Map<ObjectId, List<String>> tagObjectIdToName) {
    try (RevWalk revWalk = new RevWalk(repo)) {
      revWalk.markStart(head);

      for (RevCommit commit : revWalk) {
        ObjectId objId = commit.getId();
        if (tagObjectIdToName.size() > 0) {
          List<String> maybeList = tagObjectIdToName.get(objId);
          if (maybeList != null && maybeList.get(0) != null) {
            return Collections.singletonList(commit);
          }
        }
      }

      return Collections.emptyList();
    } catch (Exception e) {
      throw new RuntimeException("Unable to find commits until some tag", e);
    }
  }

  /**
   * Calculates the distance (number of commits) between the given parent and child commits.
   *
   * @return distance (number of commits) between the given commits
   * @see <a href="https://github.com/mdonoughe/jgit-describe/blob/master/src/org/mdonoughe/JGitDescribeTask.java">mdonoughe/jgit-describe/blob/master/src/org/mdonoughe/JGitDescribeTask.java</a>
   */
  protected int distanceBetween(@Nonnull Repository repo, @Nonnull RevCommit child, @Nonnull RevCommit parent) {
    try (RevWalk revWalk = new RevWalk(repo)) {
      revWalk.markStart(child);

      Set<ObjectId> seena = new HashSet<>();
      Set<ObjectId> seenb = new HashSet<>();
      Queue<RevCommit> q = new ArrayDeque<>();

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
    }
  }

  private void seeAllParents(@Nonnull RevWalk revWalk, RevCommit child, @Nonnull Set<ObjectId> seen) throws IOException {
    Queue<RevCommit> q = new ArrayDeque<>();
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

  public static boolean isRepositoryInDirtyState(Repository repo) throws GitAPIException {
    Git git = Git.wrap(repo);
    Status status = git.status().call();

    // Git describe doesn't mind about untracked files when checking if
    // repo is dirty. JGit does this, so we cannot use the isClean method
    // to get the same behaviour. Instead check dirty state without
    // status.getUntracked().isEmpty()
    boolean isDirty = !(status.getAdded().isEmpty()
        && status.getChanged().isEmpty()
        && status.getRemoved().isEmpty()
        && status.getMissing().isEmpty()
        && status.getModified().isEmpty()
        && status.getConflicting().isEmpty());

    return isDirty;
  }
}
