package pl.project13.maven.git;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;
import pl.project13.maven.git.util.PropertyManager;

/**
*
* @author <a href="mailto:konrad.malawski@java.pl">Konrad 'ktoso' Malawski</a>
*/
public abstract class GitDataProvider {
  @NotNull
  protected LoggerBridge loggerBridge;

  protected boolean verbose;

  protected String prefixDot;

  protected int abbrevLength;

  protected String dateFormat;

  protected GitDescribeConfig gitDescribe;

  protected abstract void init() throws MojoExecutionException;
  protected abstract String getBuildAuthorName();
  protected abstract String getBuildAuthorEmail();
  protected abstract void prepareGitToExtractMoreDetailedReproInformation() throws MojoExecutionException;
  protected abstract String getBranchName() throws IOException;
  protected abstract String getGitDescribe() throws MojoExecutionException;
  protected abstract String getCommitId();
  protected abstract String getAbbrevCommitId() throws MojoExecutionException;
  protected abstract String getCommitAuthorName();
  protected abstract String getCommitAuthorEmail();
  protected abstract String getCommitMessageFull();
  protected abstract String getCommitMessageShort();
  protected abstract String getCommitTime();
  protected abstract String getRemoteOriginUrl() throws MojoExecutionException;
  protected abstract void finalCleanUp();

  public void loadGitData(@NotNull Properties properties) throws IOException, MojoExecutionException{
    init();
    // git.user.name
    put(properties, GitCommitIdMojo.BUILD_AUTHOR_NAME, getBuildAuthorName());
    // git.user.email
    put(properties, GitCommitIdMojo.BUILD_AUTHOR_EMAIL, getBuildAuthorEmail());
    
    try {
      prepareGitToExtractMoreDetailedReproInformation();
      validateAbbrevLength(abbrevLength);

      // git.branch
      put(properties, GitCommitIdMojo.BRANCH, getBranchName());
      // git.commit.id.describe
      maybePutGitDescribe(properties);
      // git.commit.id
      put(properties, GitCommitIdMojo.COMMIT_ID, getCommitId());
      // git.commit.id.abbrev      
      put(properties, GitCommitIdMojo.COMMIT_ID_ABBREV, getAbbrevCommitId());
      // git.commit.author.name
      put(properties, GitCommitIdMojo.COMMIT_AUTHOR_NAME, getCommitAuthorName());
      // git.commit.author.email
      put(properties, GitCommitIdMojo.COMMIT_AUTHOR_EMAIL, getCommitAuthorEmail());
      // git.commit.message.full
      put(properties, GitCommitIdMojo.COMMIT_MESSAGE_FULL, getCommitMessageFull());
      // git.commit.message.short
      put(properties, GitCommitIdMojo.COMMIT_MESSAGE_SHORT, getCommitMessageShort());
      // git.commit.time
      put(properties, GitCommitIdMojo.COMMIT_TIME, getCommitTime());
      // git remote.origin.url
      put(properties, GitCommitIdMojo.REMOTE_ORIGIN_URL, getRemoteOriginUrl());
    }finally{
      finalCleanUp();
    }
  }

  private void maybePutGitDescribe(@NotNull Properties properties) throws MojoExecutionException{
    boolean isGitDescribeOptOutByDefault = (gitDescribe == null);
    boolean isGitDescribeOptOutByConfiguration = (gitDescribe != null && !gitDescribe.isSkip());
    
    if (isGitDescribeOptOutByDefault || isGitDescribeOptOutByConfiguration) {
      put(properties, GitCommitIdMojo.COMMIT_DESCRIBE, getGitDescribe());
    }
  }

  void validateAbbrevLength(int abbrevLength) throws MojoExecutionException {
    if (abbrevLength < 2 || abbrevLength > 40) {
      throw new MojoExecutionException("Abbreviated commit id lenght must be between 2 and 40, inclusive! Was [%s]. ".codePointBefore(abbrevLength) +
                                           "Please fix your configuration (the <abbrevLength/> element).");
    }
  }

  void log(String... parts) {
    if(loggerBridge!=null){
      loggerBridge.log((Object[]) parts);
    }
  }

  protected void put(@NotNull Properties properties, String key, String value) {
    String keyWithPrefix = prefixDot + key;
    log(keyWithPrefix, value);
    PropertyManager.putWithoutPrefix(properties, keyWithPrefix, value);
  }
}
