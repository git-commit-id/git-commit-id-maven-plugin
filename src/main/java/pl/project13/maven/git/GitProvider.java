package pl.project13.maven.git;

public interface GitProvider {
  public void init() throws GitCommitIdExecutionException;

  public String getBuildAuthorName() throws GitCommitIdExecutionException;

  public String getBuildAuthorEmail() throws GitCommitIdExecutionException;

  public void prepareGitToExtractMoreDetailedRepoInformation() throws GitCommitIdExecutionException;

  public String getBranchName() throws GitCommitIdExecutionException;

  public String getGitDescribe() throws GitCommitIdExecutionException;

  public String getCommitId() throws GitCommitIdExecutionException;

  public String getAbbrevCommitId() throws GitCommitIdExecutionException;

  public boolean isDirty() throws GitCommitIdExecutionException;

  public String getCommitAuthorName() throws GitCommitIdExecutionException;

  public String getCommitAuthorEmail() throws GitCommitIdExecutionException;

  public String getCommitMessageFull() throws GitCommitIdExecutionException;

  public String getCommitMessageShort() throws GitCommitIdExecutionException;

  public String getCommitTime() throws GitCommitIdExecutionException;

  public String getRemoteOriginUrl() throws GitCommitIdExecutionException;

  public String getTags() throws GitCommitIdExecutionException;

  public String getClosestTagName() throws GitCommitIdExecutionException;

  public String getClosestTagCommitCount() throws GitCommitIdExecutionException;

  public String getTotalCommitCount() throws GitCommitIdExecutionException;

  public void finalCleanUp() throws GitCommitIdExecutionException;

}
