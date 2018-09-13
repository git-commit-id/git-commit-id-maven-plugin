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

}
