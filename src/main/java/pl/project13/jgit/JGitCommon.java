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
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
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
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import pl.project13.jgit.dummy.DatedRevTag;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.release.Feature;
import pl.project13.maven.git.release.ReleaseNotes;
import pl.project13.maven.git.release.Tag;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JGitCommon {

  private final LoggerBridge log;

  public JGitCommon(LoggerBridge log) {
    this.log = log;
  }

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
    } finally {
      if (walk != null) {
        walk.dispose();
      }
    }
  }

  public String getClosestTagName(@NotNull Repository repo){
    Map<ObjectId, List<DatedRevTag>> map = getClosestTagAsMap(repo);
    for(Map.Entry<ObjectId, List<DatedRevTag>> entry : map.entrySet()){
      return trimFullTagName(entry.getValue().get(0).tagName);
    }
    return "";
  }

  public String getClosestTagCommitCount(@NotNull Repository repo, RevCommit headCommit){
    HashMap<ObjectId, List<String>> map = transformRevTagsMapToDateSortedTagNames(getClosestTagAsMap(repo));
    ObjectId obj = (ObjectId) map.keySet().toArray()[0];

    RevWalk walk = new RevWalk(repo);
    RevCommit commit = walk.lookupCommit(obj);
    walk.dispose();

    int distance = distanceBetween(repo, headCommit, commit);
    return String.valueOf(distance);
  }

  private Map<ObjectId, List<DatedRevTag>> getClosestTagAsMap(@NotNull Repository repo){
    Map<ObjectId, List<DatedRevTag>> mapWithClosestTagOnly = new HashMap<>();
    String matchPattern = ".*";
    Map<ObjectId, List<DatedRevTag>> commitIdsToTags = getCommitIdsToTags(repo, true, matchPattern);
    LinkedHashMap<ObjectId, List<DatedRevTag>> sortedCommitIdsToTags = sortByDatedRevTag(commitIdsToTags);

    for (Map.Entry<ObjectId, List<DatedRevTag>> entry: sortedCommitIdsToTags.entrySet()){
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

  protected Map<ObjectId, List<DatedRevTag>> getCommitIdsToTags(@NotNull Repository repo, boolean includeLightweightTags, String matchPattern){
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

    /**
     * Generates release notes using the release tags specified in the plugin configuration
     *
     * @param repo
     * @param startTag
     * @param endTag
     * @param commitMessageRegex
     * @param tagNameRegex
     * @return
     * @throws Exception
     */
    public ReleaseNotes generateReleaseNotesBetweenTags(Repository repo, String startTag, String endTag,
                                                        String commitMessageRegex, String tagNameRegex) throws Exception {

        ReleaseNotes notes = null;
        RevWalk walk = new RevWalk(repo);
        try {
            List<Ref> tagList = this.getAllTags(repo, tagNameRegex);

            RevCommit startingCommit = this.getCommitOfTag(walk, tagList, startTag);
            if (startingCommit == null) {
                log.debug("Returning because starting tag was not found.");
                return null;
            }
            notes = new ReleaseNotes();

            RevCommit endingCommit = this.getCommitOfTag(walk, tagList, endTag);
            Map<RevCommit, List<Ref>> commitToTagListMap = getAllCommitsAndTags(walk, tagList, startingCommit);
            if (endingCommit == null) {
                endingCommit = getDefaultEndingCommit(commitToTagListMap);
            }
            RevCommit nonMergedEndingCommit = this.findNonMergedEndingCommit(commitToTagListMap, endingCommit);
            Map<RevCommit, List<Ref>> filledOutAllCommitsWithTags = this.fillOutCommitsWithNoTags(commitToTagListMap);

            //Start walking to find the commits that matter...
            walk.reset();

            walk.setRevFilter(RevFilter.NO_MERGES);
            walk.markStart(startingCommit);
            RevCommit c = walk.next();
            Map<RevCommit, List<Ref>> relevantCommitToTagListMap = new HashMap();
            boolean foundLastCommit = false;
            while (c != null) {
                relevantCommitToTagListMap.put(c, filledOutAllCommitsWithTags.get(c));
                if (foundLastCommit) {
                    break;
                }

                c = walk.next();
                if (c == null || c.equals(nonMergedEndingCommit)) {
                    foundLastCommit = true;
                }
            }
            Map<Ref, List<RevCommit>> tagToCommitListMap = reverseMap(relevantCommitToTagListMap);
            List<Ref> orderedTagList = this.getOrderedTagList(commitToTagListMap);
            notes = this.generateReleaseNotes(tagToCommitListMap, orderedTagList, startTag, endTag,
                    commitMessageRegex, tagNameRegex);
        } catch (Exception e) {
            throw new RuntimeException("Error generating release notes", e);
        } finally {
            if (walk != null) {
                walk.dispose();
            }
        }

        return notes;
    }

    /**
     * Comparator that sorts commits by date, latest to earliest
     * @return
     */
    private Comparator<RevCommit> getRevCommitChronologicalComparator() {
        return new Comparator<RevCommit>() {
            @Override
            public int compare(RevCommit c1, RevCommit c2) {
                return c2.getCommitTime() - c1.getCommitTime();
            }
        };
    }

    /**
     * Returns the commit that is one before the last commit on this walk.
     * @param commitToTagListMap
     * @return
     */
    private RevCommit getDefaultEndingCommit(Map<RevCommit, List<Ref>> commitToTagListMap) {
        RevCommit earliestCommit = null;
        if (commitToTagListMap != null) {
            Set set = commitToTagListMap.keySet();
            List<RevCommit> commitList = new ArrayList(set);
            Collections.sort(commitList, getRevCommitChronologicalComparator());
            earliestCommit = commitList.get(commitList.size() - 1);
        }
        return earliestCommit;
    }

    /**
     * Returns ordered tags, most recent to the earliest
     * @param commitToTagListMap
     * @return
     */
    private List<Ref> getOrderedTagList(Map<RevCommit, List<Ref>> commitToTagListMap) {
        List<Ref> orderedTagList = new ArrayList();
        List<RevCommit> allCommitList = new ArrayList(commitToTagListMap.keySet());
        Collections.sort(allCommitList, getRevCommitChronologicalComparator());

        for (RevCommit c : allCommitList) {
            if (commitToTagListMap.get(c) != null && commitToTagListMap.get(c).size() > 0) {
                orderedTagList.addAll(commitToTagListMap.get(c));
            }
        }
        return orderedTagList;
    }

    /**
     * Fill out the commits that do not have tags with the latest tag.
     * So in the following lists of commits
     * c1 - T1
     * c2
     * c3
     * c4 - T2
     * c5
     * c6
     * Will result in:
     * c1 - T1
     * c2 - T1
     * c3 - T1
     * c4 - T2
     * c5 - T2
     * c6 - T2
     * @param allCommitsWithTags
     * @return
     */
    private Map<RevCommit, List<Ref>> fillOutCommitsWithNoTags(Map<RevCommit, List<Ref>> allCommitsWithTags) {
        Map<RevCommit, List<Ref>> filledOutCommitsWithTags = new HashMap();
        if (allCommitsWithTags.keySet() != null) {

            List<RevCommit> allCommitList = new ArrayList(allCommitsWithTags.keySet());
            Collections.sort(allCommitList, getRevCommitChronologicalComparator());
            List<Ref> previousTagList = null;
            for (RevCommit c : allCommitList) {
                if (allCommitsWithTags.get(c) != null && allCommitsWithTags.get(c).size() > 0) {
                    previousTagList = new ArrayList(allCommitsWithTags.get(c));
                } else {
                    filledOutCommitsWithTags.put(c, previousTagList);
                }
            }
        }
        return filledOutCommitsWithTags;
    }

    /**
     * Given a commit to tagList map, this method returns tag to commitList map
     * @param map
     * @return
     */
    private Map<Ref, List<RevCommit>> reverseMap(Map<RevCommit, List<Ref>> map) {
        Map<Ref, List<RevCommit>> m = new HashMap();
        if (map != null) {
            for (RevCommit c : map.keySet()) {
                List<Ref> tl = map.get(c);
                if (tl != null) {
                    for (Ref t : tl) {
                        if (m.get(t) == null) {
                            List<RevCommit> commitList = new ArrayList();
                            commitList.add(c);
                            m.put(t, commitList);
                        } else {
                            m.get(t).add(c);
                        }
                    }
                }
            }
        }
        return m;
    }

    /**
     * Based on the map passed in, generates a ReleaseNotes object such that the Tags are ordered by the tagList
     * passed in
     * @param tagToCommitListMap
     * @param orderedTagList
     * @param startTag
     * @param endTag
     * @param commitMessageRegex
     * @param tagNameRegex
     * @return
     */
    private ReleaseNotes generateReleaseNotes(Map<Ref, List<RevCommit>> tagToCommitListMap, List<Ref> orderedTagList,
                                              String startTag, String endTag, String commitMessageRegex, String tagNameRegex) {
        ReleaseNotes notes = new ReleaseNotes();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
        Date now = new Date();
        notes.setGenerationTime(sdf.format(now));
        notes.setStartTag(startTag);
        notes.setEndTag(endTag);
        notes.setCommitMessageRegex(commitMessageRegex);
        notes.setTagNameRegex(tagNameRegex);
        Pattern p = null;
        if (!StringUtils.isEmptyOrNull(commitMessageRegex)) {
            p = Pattern.compile(commitMessageRegex);
        }

        List<Tag> aTagList = new ArrayList();
        if (tagToCommitListMap != null) {
            if (tagToCommitListMap.keySet() != null) {
                for (Ref tag : orderedTagList) {
                    Tag t = new Tag();
                    t.setName(tag.getName());
                    List<Feature> fList = new ArrayList();
                    if (tagToCommitListMap.get(tag) != null && tagToCommitListMap.get(tag).size() > 0) {
                        List<RevCommit> commitList = tagToCommitListMap.get(tag);
                        Collections.sort(commitList, getRevCommitChronologicalComparator());
                        for (RevCommit c : commitList) {
                            if(!meetsCommitMessageRegex(c.getFullMessage(), p)) {
                                continue;
                            }
                            Feature f = new Feature();
                            f.setAuthor(c.getAuthorIdent() != null ? c.getAuthorIdent().getName() : null);
                            Date d = new Date(c.getCommitTime()*1000L);
                            String dateString = sdf.format(d);
                            f.setCommitTime(dateString);
                            f.setCommitHashLong(c.getName());
                            f.setCommitHashShort(c.getName() != null?c.getName().substring(0,7):null);
                            f.setDescription(c.getFullMessage());
                            fList.add(f);
                        }
                        t.setFeatureList(fList);
                        aTagList.add(t);
                    }
                }
            }
        }
        notes.setTagList(aTagList);
        return notes;
    }

    /**
     * Returns if the commit message passed in meets the regex pattern passed in
     * @param message
     * @param p
     * @return
     */
    private boolean meetsCommitMessageRegex(String message, Pattern p) {
        if (p == null) {
            return true;
        }
        Matcher m = p.matcher(message);
        return m.find();
    }

    /**
     * Returns the commit immediately previously (date-wise) to the commit that is specified and that which is non-merged.
     *
     * @param endingCommit
     * @return
     */
    private RevCommit findNonMergedEndingCommit(Map<RevCommit, List<Ref>> commitToTagListMap, RevCommit endingCommit) {
        RevCommit nonMergedEndingCommit = null;
        Set<RevCommit> allCommitSet = commitToTagListMap == null ? null : commitToTagListMap.keySet();
        if (allCommitSet != null) {
            List<RevCommit> allCommitList = new ArrayList(allCommitSet);
            Collections.sort(allCommitList, getRevCommitChronologicalComparator());
            if (allCommitList != null && allCommitList.size() > 0) {
                int i = 0;
                for (RevCommit c : allCommitList) {
                    if (c.equals(endingCommit)) {
                        if (i > 0) {
                            nonMergedEndingCommit = allCommitList.get(i - 1);
                        }
                    }
                    i++;
                }
            }
        }
        return nonMergedEndingCommit;
    }

    /**
     * Returns all the commits on this walk, with all tags(Ref) associated to each commit against each
     *
     * @param walk
     * @param tagList
     * @param startingCommit
     * @return
     */
    private Map<RevCommit, List<Ref>> getAllCommitsAndTags(RevWalk walk, List<Ref> tagList, RevCommit startingCommit) {
        Map<RevCommit, List<Ref>> map = new HashMap();
        try {
            walk.setRevFilter(RevFilter.ALL);
            walk.markStart(startingCommit);
            RevCommit c = walk.next();
            while (c != null) {
                List<Ref> tagListForCommit = this.getTagListOfCommit(walk, tagList, c);
                map.put(c, tagListForCommit);
                c = walk.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving all commits!", e);
        }
        return map;
    }


    /**
     * Finds all the tags (Ref) that are associated to the passed in commit
     *
     * @param walk
     * @param tagList
     * @param commit
     * @return
     * @throws Exception
     */
    public List<Ref> getTagListOfCommit(RevWalk walk, List<Ref> tagList, RevCommit commit) throws Exception {
        List<Ref> relevantTagList = new ArrayList();
        if (tagList != null) {
            for (Ref tag : tagList) {
                RevCommit c = walk.parseCommit(tag.getObjectId());
                if (c.equals(commit)) {
                    relevantTagList.add(tag);
                }
            }
        }
        return relevantTagList;
    }

    /**
     * Returns the commit on a certain tag(Ref)
     *
     * @param walk
     * @param tagList
     * @param tagName
     * @return
     * @throws Exception
     */
    public RevCommit getCommitOfTag(RevWalk walk, List<Ref> tagList, String tagName) throws Exception {
        //Find the tag
        Ref tag = null;
        if (tagList != null && !StringUtils.isEmptyOrNull(tagName)) {
            for (Ref r : tagList) {
                if (StringUtils.equalsIgnoreCase(r.getName(), "refs/tags/" + tagName)) {
                    tag = r;
                    break;
                }
            }
        }
        ObjectId objectId = tag == null ? null : tag.getObjectId();
        RevCommit commit = null;
        if (objectId != null) {
            commit = walk.parseCommit(objectId);
        }
        return commit;
    }

    /**
     * Returns all tags that match the regular expression passed in (optional).
     * If the regular expression passed in is null, then the tags are returned unfiltered.
     * @param repo
     * @param tagNameRegex
     * @return
     */
    public List<Ref> getAllTags(Repository repo, String tagNameRegex) {
        List<Ref> rList = null;
        List<Ref> finalList = new ArrayList();
        try {
            Git git = Git.wrap(repo);
            //Get all tags
            rList = git.tagList().call();
            if (rList != null && !StringUtils.isEmptyOrNull(tagNameRegex)) {
                Pattern p = Pattern.compile(tagNameRegex);
                for (Ref r: rList) {
                    String justTheName = r.getName().substring(10); //Remove refs/tags/ from the fully qualified tagRef
                    Matcher m = p.matcher(justTheName);
                    if (m.find()) {
                        finalList.add(r);
                    }
                }
            } else {
                finalList = rList;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting all tags", e);
        }
        return finalList;
    }
}
