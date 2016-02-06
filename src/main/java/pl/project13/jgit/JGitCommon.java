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
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.eclipse.jgit.api.Git;
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
import pl.project13.maven.git.log.LoggerBridge;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class JGitCommon {
  public Collection<String> getTags(Repository repo, final ObjectId headId) throws GitAPIException{
    RevWalk walk = null;
    try {
      Git git = Git.wrap(repo);
      walk = new RevWalk(repo);
      List<Ref> tagRefs = git.tagList().call();

      final RevWalk finalWalk = walk;
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
    } catch (GitAPIException e) {
      throw e;
    } finally {
      if (walk != null) {
        walk.dispose();
      }
    }
  }

  public String getClosestTagName(@NotNull LoggerBridge loggerBridge,@NotNull Repository repo){
    Map<ObjectId, List<DatedRevTag>> map = getClosestTagAsMap(loggerBridge,repo);
    for(Map.Entry<ObjectId, List<DatedRevTag>> entry : map.entrySet()){
      return trimFullTagName(entry.getValue().get(0).tagName);
    }
    return "";
  }

  public String getClosestTagCommitCount(@NotNull LoggerBridge loggerBridge,@NotNull Repository repo, RevCommit headCommit){    
    HashMap<ObjectId, List<String>> map = transformRevTagsMapToDateSortedTagNames(getClosestTagAsMap(loggerBridge,repo));
    ObjectId obj = (ObjectId) map.keySet().toArray()[0];
    
    RevWalk walk = new RevWalk(repo);
    RevCommit commit = walk.lookupCommit(obj);
    walk.dispose();
    
    int distance = distanceBetween(repo, headCommit, commit);
    return String.valueOf(distance);
  }

  private Map<ObjectId, List<DatedRevTag>> getClosestTagAsMap(@NotNull LoggerBridge loggerBridge,@NotNull Repository repo){
    Map<ObjectId, List<DatedRevTag>> mapWithClosestTagOnly = newHashMap();
    boolean includeLightweightTags = true;
    String matchPattern = ".*";
    Map<ObjectId, List<DatedRevTag>> commitIdsToTags = getCommitIdsToTags(loggerBridge,repo,includeLightweightTags,matchPattern);
    LinkedHashMap<ObjectId, List<DatedRevTag>> sortedCommitIdsToTags = sortByDatedRevTag(commitIdsToTags);

    for(Map.Entry<ObjectId, List<DatedRevTag>> entry: sortedCommitIdsToTags.entrySet()){
      mapWithClosestTagOnly.put(entry.getKey(), entry.getValue());
      break;
    }

    return mapWithClosestTagOnly;
  }

  private LinkedHashMap<ObjectId, List<DatedRevTag>> sortByDatedRevTag(Map<ObjectId, List<DatedRevTag>> map) {
    List<Map.Entry<ObjectId, List<DatedRevTag>>> list = new LinkedList<Map.Entry<ObjectId, List<DatedRevTag>>>(map.entrySet());

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

    LinkedHashMap<ObjectId, List<DatedRevTag>> result = new LinkedHashMap<ObjectId, List<DatedRevTag>>();
    for (Map.Entry<ObjectId, List<DatedRevTag>> entry : list) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  protected Map<ObjectId, List<DatedRevTag>> getCommitIdsToTags(@NotNull LoggerBridge loggerBridge,@NotNull Repository repo, boolean includeLightweightTags, String matchPattern){
    Map<ObjectId, List<DatedRevTag>> commitIdsToTags = newHashMap();

    RevWalk walk = new RevWalk(repo);
    try {
      walk.markStart(walk.parseCommit(repo.resolve("HEAD")));

      List<Ref> tagRefs = Git.wrap(repo).tagList().call();
      Pattern regex = Pattern.compile(matchPattern);
      loggerBridge.log("Tag refs [", tagRefs, "]");

      for (Ref tagRef : tagRefs) {
        walk.reset();
        String name = tagRef.getName();
        if (!regex.matcher(name).matches()) {
          loggerBridge.log("Skipping tagRef with name [", name, "] as it doesn't match [", matchPattern, "]");
          continue;
        }
        ObjectId resolvedCommitId = repo.resolve(name);

        // TODO that's a bit of a hack...
        try {
          final RevTag revTag = walk.parseTag(resolvedCommitId);
          ObjectId taggedCommitId = revTag.getObject().getId();
          loggerBridge.log("Resolved tag [",revTag.getTagName(),"] [",revTag.getTaggerIdent(),"], points at [",taggedCommitId,"] ");

          // sometimes a tag, may point to another tag, so we need to unpack it
          while (isTagId(taggedCommitId)) {
            taggedCommitId = walk.parseTag(taggedCommitId).getObject().getId();
          }

          if (commitIdsToTags.containsKey(taggedCommitId)) {
            commitIdsToTags.get(taggedCommitId).add(new DatedRevTag(revTag));
          } else {
            commitIdsToTags.put(taggedCommitId, newArrayList(new DatedRevTag(revTag)));
          }

        } catch (IncorrectObjectTypeException ex) {
          // it's an lightweight tag! (yeah, really)
          if (includeLightweightTags) {
            // --tags means "include lightweight tags"
            loggerBridge.log("Including lightweight tag [", name, "]");

            DatedRevTag datedRevTag = new DatedRevTag(resolvedCommitId, name);

            if (commitIdsToTags.containsKey(resolvedCommitId)) {
              commitIdsToTags.get(resolvedCommitId).add(datedRevTag);
            } else {
              commitIdsToTags.put(resolvedCommitId, newArrayList(datedRevTag));
            }
          }
        } catch (Exception ignored) {
          loggerBridge.error("Failed while parsing [",tagRef,"] -- ", Throwables.getStackTraceAsString(ignored));
        }
      }

      for (Map.Entry<ObjectId, List<DatedRevTag>> entry : commitIdsToTags.entrySet()) {
        loggerBridge.log("key [",entry.getKey(),"], tags => [",entry.getValue(),"] ");
      }
        return commitIdsToTags;
    } catch (Exception e) {
      loggerBridge.log("Unable to locate tags\n[",Throwables.getStackTraceAsString(e),"]");
    } finally {
      walk.close();
    }
    return Collections.emptyMap();
  }

  /** Checks if the given object id resolved to a tag object */
  private boolean isTagId(ObjectId objectId) {
    return objectId.toString().startsWith("tag ");
  }

  protected HashMap<ObjectId, List<String>> transformRevTagsMapToDateSortedTagNames(Map<ObjectId, List<DatedRevTag>> commitIdsToTags) {
    HashMap<ObjectId, List<String>> commitIdsToTagNames = newHashMap();
    for (Map.Entry<ObjectId, List<DatedRevTag>> objectIdListEntry : commitIdsToTags.entrySet()) {
      List<String> tagNames = transformRevTagsMapEntryToDateSortedTagNames(objectIdListEntry);

      commitIdsToTagNames.put(objectIdListEntry.getKey(), tagNames);
    }
    return commitIdsToTagNames;
  }

  private List<String> transformRevTagsMapEntryToDateSortedTagNames(Map.Entry<ObjectId, List<DatedRevTag>> objectIdListEntry) {
    List<DatedRevTag> tags = objectIdListEntry.getValue();

    List<DatedRevTag> newTags = newArrayList(tags);
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
          return revTag.date.compareTo(revTag2.date);
        }
      };
  }

  @VisibleForTesting
  protected String trimFullTagName(@NotNull String tagName) {
    return tagName.replaceFirst("refs/tags/", "");
  }

  public List<RevCommit> findCommitsUntilSomeTag(Repository repo, RevCommit head, @NotNull Map<ObjectId, List<String>> tagObjectIdToName) {
    RevWalk revWalk = new RevWalk(repo);
    try {
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

  private void seeAllParents(@NotNull RevWalk revWalk, RevCommit child, @NotNull Set<ObjectId> seen) throws IOException {
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

}
