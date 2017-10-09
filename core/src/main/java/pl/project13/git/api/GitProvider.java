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

package pl.project13.git.api;

public interface GitProvider {

  void init() throws GitException;

  String getBuildAuthorName() throws GitException;

  String getBuildAuthorEmail() throws GitException;

  void prepareGitToExtractMoreDetailedReproInformation() throws GitException;

  String getBranchName() throws GitException;

  String getGitDescribe() throws GitException;

  String getCommitId() throws GitException;

  String getAbbrevCommitId() throws GitException;

  boolean isDirty() throws GitException;

  String getCommitAuthorName() throws GitException;

  String getCommitAuthorEmail() throws GitException;

  String getCommitMessageFull() throws GitException;

  String getCommitMessageShort() throws GitException;

  String getCommitTime() throws GitException;

  String getRemoteOriginUrl() throws GitException;

  String getTags() throws GitException;

  String getClosestTagName() throws GitException;

  String getClosestTagCommitCount() throws GitException;

  void finalCleanUp() throws GitException;

  // setter
  void setAbbrevLength(int abbrevLength);

  void setGitDescribe(GitDescribeConfig gitDescribe);

  void setDateFormat(String dateFormat);

  void setDateFormatTimeZone(String dateFormatTimeZone);
}
