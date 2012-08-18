package pl.project13.maven.git;

import java.io.File;

public enum AvailableGitTestRepo {
  WITH_ONE_COMMIT("src/test/resources/_git_one_commit"),
  GIT_COMMIT_ID("src/test/resources/_git_of_git_commit_id");

  String dir;
  AvailableGitTestRepo(String dir) {
    this.dir = dir;
  }

  public File getDir() {
    return new File(dir);
  }
}
