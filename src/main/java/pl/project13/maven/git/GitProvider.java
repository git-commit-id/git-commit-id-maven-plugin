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

package pl.project13.maven.git;

public interface GitProvider {
  void init() throws GitCommitIdExecutionException;

  String getBuildAuthorName() throws GitCommitIdExecutionException;

  String getBuildAuthorEmail() throws GitCommitIdExecutionException;

  void prepareGitToExtractMoreDetailedRepoInformation() throws GitCommitIdExecutionException;

  String getBranchName() throws GitCommitIdExecutionException;

  String getGitDescribe() throws GitCommitIdExecutionException;

  String getCommitId() throws GitCommitIdExecutionException;

  String getAbbrevCommitId() throws GitCommitIdExecutionException;

  boolean isDirty() throws GitCommitIdExecutionException;

  String getCommitAuthorName() throws GitCommitIdExecutionException;

  String getCommitAuthorEmail() throws GitCommitIdExecutionException;

  String getCommitMessageFull() throws GitCommitIdExecutionException;

  String getCommitMessageShort() throws GitCommitIdExecutionException;

  String getCommitTime() throws GitCommitIdExecutionException;

  String getRemoteOriginUrl() throws GitCommitIdExecutionException;

  String getTags() throws GitCommitIdExecutionException;

  String getClosestTagName() throws GitCommitIdExecutionException;

  String getClosestTagCommitCount() throws GitCommitIdExecutionException;

  String getTotalCommitCount() throws GitCommitIdExecutionException;

  void finalCleanUp() throws GitCommitIdExecutionException;
  
  AheadBehind getAheadBehind() throws GitCommitIdExecutionException;

}
