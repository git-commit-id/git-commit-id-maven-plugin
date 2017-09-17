package pl.project13.maven.git;

public class GitCommitPropertyConstant {
  // these properties will be exposed to maven
  public static final String BRANCH = "branch";
  public static final String DIRTY = "dirty";
  // only one of the following two will be exposed, depending on the commitIdGenerationMode
  public static final String COMMIT_ID_FLAT = "commit.id";
  public static final String COMMIT_ID_FULL = "commit.id.full";
  public static final String COMMIT_ID_ABBREV = "commit.id.abbrev";
  public static final String COMMIT_DESCRIBE = "commit.id.describe";
  public static final String COMMIT_SHORT_DESCRIBE = "commit.id.describe-short";
  public static final String BUILD_AUTHOR_NAME = "build.user.name";
  public static final String BUILD_AUTHOR_EMAIL = "build.user.email";
  public static final String BUILD_TIME = "build.time";
  public static final String BUILD_VERSION = "build.version";
  public static final String BUILD_HOST = "build.host";
  public static final String COMMIT_AUTHOR_NAME = "commit.user.name";
  public static final String COMMIT_AUTHOR_EMAIL = "commit.user.email";
  public static final String COMMIT_MESSAGE_FULL = "commit.message.full";
  public static final String COMMIT_MESSAGE_SHORT = "commit.message.short";
  public static final String COMMIT_TIME = "commit.time";
  public static final String REMOTE_ORIGIN_URL = "remote.origin.url";
  public static final String TAGS = "tags";
  public static final String CLOSEST_TAG_NAME = "closest.tag.name";
  public static final String CLOSEST_TAG_COMMIT_COUNT = "closest.tag.commit.count";

}
