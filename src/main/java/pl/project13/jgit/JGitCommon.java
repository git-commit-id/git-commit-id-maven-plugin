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
import org.jetbrains.annotations.NotNull;

import pl.project13.jgit.dummy.DatedRevTag;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import pl.project13.maven.git.log.LoggerBridge;

public class JGitCommon {

  private final LoggerBridge log;

  public JGitCommon(LoggerBridge log) {
    this.log = log;
  }

  public Collection<String> getTags(Repository repo, final ObjectId headId) throws GitAPIException {
    try (Git git = Git.wrap(repo)) {
      try (RevWalk walk = new RevWalk(repo)) {
        Collection<String> tags = getTags(git, headId, walk);
        walk.dispose();
        return tags;
      }
    }
  }

  private Collection<String> getTags(final Git git, final ObjectId headId, final RevWalk finalWalk) throws GitAPIException {
    List<Ref> tagRefs = git.tagList().call();
    Collection<Ref> tagsForHeadCommit = Collections2.filter(tagRefs, new Predicate<Ref>() {
      @Override public boolean apply(Ref tagRef) {
        boolean lightweightTag = tagRef.getObjectId().equals(headId);
        try {
          // TODO make this configurable (most users shouldn't really care too much what kind of tag it is though)
          return lightweightTag || finalWalk.parseTag(tagRef.getObjectId()).getObject().getId().equals(headId); // or normal tag
        } catch (IOException e) {
          return false;
        }
      }
    });
    Collection<String> tags = Collections2.transform(tagsForHeadCommit, new Function<Ref, String>() {
      @Override public String apply(Ref input) {
        return input.getName().replaceAll("refs/tags/", "");
      }
    });
    return tags;
  }

  public String getClosestTagName(@NotNull Repository repo) {
    Map<ObjectId, List<DatedRevTag>> map = getClosestTagAsMap(repo);
    for (Map.Entry<ObjectId, List<DatedRevTag>> entry : map.entrySet()) {
      return trimFullTagName(entry.getValue().get(0).tagName);
    }
    return "";
  }

  public String getClosestTagCommitCount(@NotNull Repository repo, RevCommit headCommit) {
    HashMap<ObjectId, List<String>> map = transformRevTagsMapToDateSortedTagNames(getClosestTagAsMap(repo));
    ObjectId obj = (ObjectId) map.keySet().toArray()[0];
    
    try (RevWalk walk = new RevWalk(repo)) {
      RevCommit commit = walk.lookupCommit(obj);
      walk.dispose();

      int distance = distanceBetween(repo, headCommit, commit);
      return String.valueOf(distance);
    }
  }

  private Map<ObjectId, List<DatedRevTag>> getClosestTagAsMap(@NotNull Repository repo) {
    Map<ObjectId, List<DatedRevTag>> mapWithClosestTagOnly = new HashMap<>();
    String matchPattern = ".*";
    Map<ObjectId, List<DatedRevTag>> commitIdsToTags = getCommitIdsToTags(repo, true, matchPattern);
    LinkedHashMap<ObjectId, List<DatedRevTag>> sortedCommitIdsToTags = sortByDatedRevTag(commitIdsToTags);

    for (Map.Entry<ObjectId, List<DatedRevTag>> entry: sortedCommitIdsToTags.entrySet()) {
      mapWithClosestTagOnly.put(entry.getKey(), entry.getValue());
      break;
    }

    return mapWithClosestTagOnly;
  }

  private LinkedHashMap<ObjectId, List<DatedRevTag>> sortByDatedRevTag(Map<ObjectId, List<DatedRevTag>> map) {
    List<Map.Entry<ObjectId, List<DatedRevTag>>> list = new ArrayList<>(map.entrySet());

    Collections.sort(list, new Comparator<Map.Entry<ObjectId, List<DatedRevTag>>>() {
      public int compare(Map.Entry<ObjectId, List<DatedRevTag>> m1, Map.Entry<ObjectId, List<DatedRevTag>> m2) {
        // we need to sort the DatedRevTags to a commit first, otherwise we may get problems when we have two tags for the same commit
        Collections.sort(m1.getValue(), datedRevTagComparator());
        Collections.sort(m2.getValue(), datedRevTagComparator());

        DatedRevTag datedRevTag1 = m1.getValue().get(0);
        DatedRevTag datedRevTag2 = m2.getValue().get(0);
        return datedRevTagComparator().compare(datedRevTag1,datedRevTag2);
      }
    });

    LinkedHashMap<ObjectId, List<DatedRevTag>> result = new LinkedHashMap<>();
    for (Map.Entry<ObjectId, List<DatedRevTag>> entry : list) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  protected Map<ObjectId, List<DatedRevTag>> getCommitIdsToTags(@NotNull Repository repo, boolean includeLightweightTags, String matchPattern) {
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
    List<DatedRevTag> tags = objectIdListEntry.getValue();

    List<DatedRevTag> newTags = new ArrayList<>(tags);
    Collections.sort(newTags, datedRevTagComparator());

    List<String> tagNames = Lists.transform(newTags, new Function<DatedRevTag, String>() {
      @Override
      public String apply(DatedRevTag input) {
        return trimFullTagName(input.tagName);
      }
    });
    return tagNames;
  }

  private Comparator<DatedRevTag> datedRevTagComparator() {
    return new Comparator<DatedRevTag>() {
        @Override
        public int compare(DatedRevTag revTag, DatedRevTag revTag2) {
          return revTag2.date.compareTo(revTag.date);
        }
      };
  }

  @VisibleForTesting
  protected String trimFullTagName(@NotNull String tagName) {
    return tagName.replaceFirst("refs/tags/", "");
  }

  public List<RevCommit> findCommitsUntilSomeTag(Repository repo, RevCommit head, @NotNull Map<ObjectId, List<String>> tagObjectIdToName) {
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
  protected int distanceBetween(@NotNull Repository repo, @NotNull RevCommit child, @NotNull RevCommit parent) {
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

  private void seeAllParents(@NotNull RevWalk revWalk, RevCommit child, @NotNull Set<ObjectId> seen) throws IOException {
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
