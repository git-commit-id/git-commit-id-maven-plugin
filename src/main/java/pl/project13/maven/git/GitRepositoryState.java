package pl.project13.maven.git;

//import org.codehaus.jackson.annotate.JsonWriteNullProperties;

/**
 * A spring controlled bean that will be injected
 * with properties about the repository state at build time.
 * This information is supplied by my plugin - <b>pl.project13.maven.git-commit-id-plugin</b>
 *
 * @author <a href="mailto:konrad.malawski@project13.pl">Konrad 'ktoso' Malawski</a>
 * @since 1.0
 */
//@JsonWriteNullProperties(true)
public class GitRepositoryState {
  String branch;                  // =${git.branch}
  String commitId;                // =${git.commit.id}
  String buildUserName;           // =${git.build.user.name}
  String buildUserEmail;          // =${git.build.user.email}
  String buildTime;               // =${git.build.time}
  String commitUserName;          // =${git.commit.user.name}
  String commitUserEmail;         // =${git.commit.user.email}
  String commitMessageFull;       // =${git.commit.message.full}
  String commitMessageShort;      // =${git.commit.message.short}
  String commitTime;              // =${git.commit.time}

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

  /**
   * If you need it as json but don't have jackson installed etc
   * @return the JSON representation of this resource
   */
  public String toJson() {
    StringBuilder sb = new StringBuilder("{");
    appendProperty(sb, "branch", branch);

    appendProperty(sb, "commitId", commitId);
    appendProperty(sb, "commitTime", commitTime);
    appendProperty(sb, "commitUserName", commitUserName);
    appendProperty(sb, "commitUserEmail", commitUserEmail);
    appendProperty(sb, "commitMessageShort", commitMessageShort);
    appendProperty(sb, "commitMessageFull", commitMessageFull);

    appendProperty(sb, "buildTime", buildTime);
    appendProperty(sb, "buildUserName", buildUserName);
    appendProperty(sb, "buildUserEmail", buildUserEmail);

    return sb.append("}").toString();
  }

  private void appendProperty(StringBuilder sb, String label, String value) {
    sb.append(String.format("\"%s\" = \"%s\",", label, value));
  }
}
