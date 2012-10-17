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

package pl.project13.maven.git;

//import org.codehaus.jackson.annotate.JsonWriteNullProperties;

import org.jetbrains.annotations.NotNull;

/**
 * A spring controlled bean that will be injected
 * with properties about the repository state at build time.
 * This information is supplied by my plugin - <b>pl.project13.maven.git-commit-id-plugin</b>
 *
 * @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
 * @since 1.0
 */
//@JsonWriteNullProperties(true)
public class GitRepositoryState {
  String branch;                  // =${git.branch}
  String commitId;                // =${git.commit.id}
  String commitIdAbbrev;          // =${git.commit.id.abbrev}
  String buildUserName;           // =${git.build.user.name}
  String buildUserEmail;          // =${git.build.user.email}
  String buildTime;               // =${git.build.time}
  String commitUserName;          // =${git.commit.user.name}
  String commitUserEmail;         // =${git.commit.user.email}
  String commitMessageFull;       // =${git.commit.message.full}
  String commitMessageShort;      // =${git.commit.message.short}
  String commitTime;              // =${git.commit.time}

  String mavenProjectVersion;     // =${maven.project.version}

  public GitRepositoryState() {
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getCommitId() {
    return commitId;
  }

  public void setCommitId(String commitId) {
    this.commitId = commitId;
  }

  public void setCommitIdAbbrev(String commitIdAbbrev) {
    this.commitIdAbbrev = commitIdAbbrev;
  }

  public String getCommitIdAbbrev() {
    return commitIdAbbrev;
  }

  public String getBuildUserName() {
    return buildUserName;
  }

  public void setBuildUserName(String buildUserName) {
    this.buildUserName = buildUserName;
  }

  public String getBuildUserEmail() {
    return buildUserEmail;
  }

  public void setBuildUserEmail(String buildUserEmail) {
    this.buildUserEmail = buildUserEmail;
  }

  public String getCommitUserName() {
    return commitUserName;
  }

  public void setCommitUserName(String commitUserName) {
    this.commitUserName = commitUserName;
  }

  public String getCommitUserEmail() {
    return commitUserEmail;
  }

  public void setCommitUserEmail(String commitUserEmail) {
    this.commitUserEmail = commitUserEmail;
  }

  public String getCommitMessageFull() {
    return commitMessageFull;
  }

  public void setCommitMessageFull(String commitMessageFull) {
    this.commitMessageFull = commitMessageFull;
  }

  public String getCommitMessageShort() {
    return commitMessageShort;
  }

  public void setCommitMessageShort(String commitMessageShort) {
    this.commitMessageShort = commitMessageShort;
  }

  public String getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(String commitTime) {
    this.commitTime = commitTime;
  }

  public String getBuildTime() {
    return buildTime;
  }

  public void setBuildTime(String buildTime) {
    this.buildTime = buildTime;
  }

  public String getMavenProjectVersion() {
    return mavenProjectVersion;
  }

  public void setMavenProjectVersion(String mavenProjectVersion) {
    this.mavenProjectVersion = mavenProjectVersion;
  }

  /**
   * If you need it as json but don't have jackson installed etc
   *
   * @return the JSON representation of this resource
   */
  public String toJson() {
    StringBuilder sb = new StringBuilder("{");
    appendProperty(sb, "branch", branch);

    appendProperty(sb, "commitId", commitId);
    appendProperty(sb, "commitIdAbbrev", commitIdAbbrev);
    appendProperty(sb, "commitTime", commitTime);
    appendProperty(sb, "commitUserName", commitUserName);
    appendProperty(sb, "commitUserEmail", commitUserEmail);
    appendProperty(sb, "commitMessageShort", commitMessageShort);
    appendProperty(sb, "commitMessageFull", commitMessageFull);

    appendProperty(sb, "buildTime", buildTime);
    appendProperty(sb, "buildUserName", buildUserName);
    appendProperty(sb, "buildUserEmail", buildUserEmail);

    appendProperty(sb, "mavenProjectVersion", mavenProjectVersion);

    return sb.append("}").toString();
  }

  private void appendProperty(@NotNull StringBuilder sb, String label, String value) {
    sb.append(String.format("\"%s\": \"%s\",", label, value));
  }

}
