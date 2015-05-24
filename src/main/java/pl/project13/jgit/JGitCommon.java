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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jetbrains.annotations.NotNull;

import pl.project13.jgit.dummy.DatedRevTag;
import pl.project13.maven.git.log.LoggerBridge;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 */
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
  
  protected Map<ObjectId, List<DatedRevTag>> getCommitIdsToTags(@NotNull LoggerBridge loggerBridge,@NotNull Repository repo, boolean tagsFlag, String matchPattern){
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
          if (tagsFlag) {
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
      walk.release();
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
      List<DatedRevTag> tags = objectIdListEntry.getValue();

      List<DatedRevTag> newTags = newArrayList(tags);
      Collections.sort(newTags, new Comparator<DatedRevTag>() {
        @Override
        public int compare(DatedRevTag revTag, DatedRevTag revTag2) {
          return revTag2.date.compareTo(revTag.date);
        }
      });

      List<String> tagNames = Lists.transform(newTags, new Function<DatedRevTag, String>() {
        @Override
        public String apply(DatedRevTag input) {
          return trimFullTagName(input.tagName);
        }
      });

      commitIdsToTagNames.put(objectIdListEntry.getKey(), tagNames);
    }
    return commitIdsToTagNames;
  }

  @VisibleForTesting
  protected String trimFullTagName(@NotNull String tagName) {
    return tagName.replaceFirst("refs/tags/", "");
  }

}
