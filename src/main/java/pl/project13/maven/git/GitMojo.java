package pl.project13.maven.git;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;

/**
 * Created by pankaj on 10/21/16.
 */
public abstract class GitMojo extends AbstractMojo {
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

    // TODO fix access modifier
    /**
     * The Maven Project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    /**
     * The list of projects in the reactor.
     */
    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    protected List<MavenProject> reactorProjects;

    /**
     * The Maven Session Object.
     */
    @Parameter(property = "session", required = true, readonly = true)
    protected MavenSession session;

    /**
     * <p>Set this to {@code 'true'} to inject git properties into all reactor projects, not just the current one.</p>
     *
     * <p>Injecting into all projects may slow down the build and you don't always need this feature.
     * See <a href="https://github.com/ktoso/maven-git-commit-id-plugin/pull/65">pull #65</a> for details about why you might want to skip this.
     * </p>
     */
    @Parameter(defaultValue = "false")
    protected boolean injectAllReactorProjects;

    /**
     * Set this to {@code 'true'} to print more info while scanning for paths.
     * It will make git-commit-id "eat its own dog food" :-)
     */
    @Parameter(defaultValue = "false")
    protected boolean verbose;

    /**
     * Set this to {@code 'false'} to execute plugin in 'pom' packaged projects.
     */
    @Parameter(defaultValue = "true")
    protected boolean skipPoms;

    /**
     * Set this to {@code 'true'} to generate {@code 'git.properties'} file.
     * By default plugin only adds properties to maven project properties.
     */
    @Parameter(defaultValue = "false")
    protected boolean generateGitPropertiesFile;

    /**
     * <p>The location of {@code 'git.properties'} file. Set {@code 'generateGitPropertiesFile'} to {@code 'true'}
     * to generate this file.</p>
     *
     * <p>The path here is relative to your project src directory.</p>
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/git.properties")
    protected String generateGitPropertiesFilename;

    /**
     * The root directory of the repository we want to check.
     */
    @Parameter(defaultValue = "${project.basedir}/.git")
    protected File dotGitDirectory;

    /**
     * Configuration for the {@code 'git-describe'} command.
     * You can modify the dirty marker, abbrev length and other options here.
     */
    @Parameter
    protected GitDescribeConfig gitDescribe;

    /**
     * <p>Minimum length of {@code 'git.commit.id.abbrev'} property.
     * Value must be from 2 to 40 (inclusive), other values will result in an exception.</p>
     *
     * <p>An abbreviated commit is a shorter version of commit id. However, it is guaranteed to be unique.
     * To keep this contract, the plugin may decide to print an abbreviated version
     * that is longer than the value specified here.</p>
     *
     * <p><b>Example:</b> You have a very big repository, yet you set this value to 2. It's very probable that you'll end up
     * getting a 4 or 7 char long abbrev version of the commit id. If your repository, on the other hand,
     * has just 4 commits, you'll probably get a 2 char long abbreviation.</p>
     *
     */
    @Parameter(defaultValue = "7")
    protected int abbrevLength;

    /**
     * The format to save properties in: {@code 'properties'} or {@code 'json'}.
     */
    @Parameter(defaultValue = "properties")
    protected String format;

    /**
     * The prefix to expose the properties on. For example {@code 'git'} would allow you to access {@code ${git.branch}}.
     */
    @Parameter(defaultValue = "git")
    protected String prefix;
    // prefix with dot appended if needed
    protected String prefixDot = "";

    /**
     * The date format to be used for any dates exported by this plugin. It should be a valid {@link SimpleDateFormat} string.
     */
    @Parameter(defaultValue = "dd.MM.yyyy '@' HH:mm:ss z")
    protected String dateFormat;

    /**
     * <p>The timezone used in the date format of dates exported by this plugin.
     * It should be a valid Timezone string such as {@code 'America/Los_Angeles'}, {@code 'GMT+10'} or {@code 'PST'}.</p>
     *
     * <p>Try to avoid three-letter time zone IDs because the same abbreviation is often used for multiple time zones.
     * Please review <a href="https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html">https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html</a> for more information on this issue.</p>
     */
    @Parameter
    protected String dateFormatTimeZone;

    /**
     * Set this to {@code 'false'} to continue the build on missing {@code '.git'} directory.
     */
    @Parameter(defaultValue = "true")
    protected boolean failOnNoGitDirectory;

    /**
     * <p>Set this to {@code 'false'} to continue the build even if unable to get enough data for a complete run.
     * This may be useful during CI builds if the CI server does weird things to the repository.</p>
     *
     * <p>Setting this value to {@code 'false'} causes the plugin to gracefully tell you "I did my best"
     * and abort its execution if unable to obtain git meta data - yet the build will continue to run without failing.</p>
     *
     * <p>See <a href="https://github.com/ktoso/maven-git-commit-id-plugin/issues/63">issue #63</a>
     * for a rationale behind this flag.</p>
     */
    @Parameter(defaultValue = "true")
    protected boolean failOnUnableToExtractRepoInfo;

    /**
     * Set this to {@code 'true'} to use native Git executable to fetch information about the repository.
     * It is in most cases faster but requires a git executable to be installed in system.
     * By default the plugin will use jGit implementation as a source of information about the repository.
     * @since 2.1.9
     */
    @Parameter(defaultValue = "false")
    protected boolean useNativeGit;

    /**
     * Set this to {@code 'true'} to skip plugin execution.
     * @since 2.1.8
     */
    @Parameter(defaultValue = "false")
    protected boolean skip;

    /**
     * <p>Set this to {@code 'true'} to only run once in a multi-module build.  This probably won't "do the right thing"
     * if your project has more than one git repository.  If you use this with {@code 'generateGitPropertiesFile'},
     * it will only generate (or update) the file in the directory where you started your build.</p>
     *
     * <p>The git.* maven properties are available in all modules.</p>
     * @since 2.1.12
     */
    @Parameter(defaultValue = "false")
    protected boolean runOnlyOnce;

    /**
     * <p>List of properties to exclude from the resulting file.
     * May be useful when you want to hide {@code 'git.remote.origin.url'} (maybe because it contains your repo password?)
     * or the email of the committer.</p>
     *
     * <p>Supports wildcards: you can write {@code 'git.commit.user.*'} to exclude both the {@code 'name'}
     * as well as {@code 'email'} properties from being emitted into the resulting files.</p>
     *
     * <p><b>Note:</b> The strings here are Java regular expressions: {@code '.*'} is a wildcard, not plain {@code '*'}.</p>
     * @since 2.1.9
     */
    @Parameter
    protected List<String> excludeProperties;

    /**
     * <p>List of properties to include into the resulting file. Only properties specified here will be included.
     * This list will be overruled by the {@code 'excludeProperties'}.</p>
     *
     * <p>Supports wildcards: you can write {@code 'git.commit.user.*'} to include both the {@code 'name'}
     * as well as {@code 'email'} properties into the resulting files.</p>
     *
     * <p><b>Note:</b> The strings here are Java regular expressions: {@code '.*'} is a wildcard, not plain {@code '*'}.</p>
     * @since 2.1.14
     */
    @Parameter
    protected List<String> includeOnlyProperties;

    /**
     * <p>The mode of {@code 'git.commit.id'} property generation.</p>
     *
     * <p>{@code 'git.commit.id'} property name is incompatible with json export
     * (see <a href="https://github.com/ktoso/maven-git-commit-id-plugin/issues/122">issue #122</a>).
     * This property allows one either to preserve backward compatibility or to enable fully valid json export:
     *
     * <ol>
     * <li>{@code 'flat'} (default) generates the property {@code 'git.commit.id'}, preserving backwards compatibility.</li>
     * <li>{@code 'full'} generates the property {@code 'git.commit.id.full'}, enabling fully valid json object export.</li>
     * </ol>
     * </p>
     *
     * <p><b>Note:</b> Depending on your plugin configuration you obviously can choose the `prefix` of your properties
     * by setting it accordingly in the plugin's configuration. As a result this is therefore only an illustration
     * what the switch means when the 'prefix' is set to it's default value.</p>
     * <p><b>Note:</b> If you set the value to something that's not equal to {@code 'flat'} or {@code 'full'} (ignoring the case)
     * the plugin will output a warning and will fallback to the default {@code 'flat'} mode.</p>
     * @since 2.2.0
     */
    @Parameter(defaultValue = "flat")
    protected String commitIdGenerationMode;
    protected CommitIdGenerationMode commitIdGenerationModeEnum;

    /**
     * The properties we store our data in and then expose them.
     */
    protected Properties properties;

    /**
     * Charset to read-write project sources.
     */
    protected Charset sourceCharset = StandardCharsets.UTF_8;

    @NotNull
    protected final LoggerBridge log = new MavenLoggerBridge(this, false);

    public abstract void execute() throws MojoExecutionException, MojoFailureException ;

    // SETTERS FOR TESTS ----------------------------------------------------

    public void setFormat(String format) {
        this.format = format;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setDotGitDirectory(File dotGitDirectory) {
        this.dotGitDirectory = dotGitDirectory;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setGitDescribe(GitDescribeConfig gitDescribe) {
        this.gitDescribe = gitDescribe;
    }

    public void setAbbrevLength(int abbrevLength) {
        this.abbrevLength = abbrevLength;
    }

    public void setExcludeProperties(List<String> excludeProperties) {
        this.excludeProperties = excludeProperties;
    }

    public void setIncludeOnlyProperties(List<String> includeOnlyProperties) {
        this.includeOnlyProperties = includeOnlyProperties;
    }

    public void useNativeGit(boolean useNativeGit) {
        this.useNativeGit = useNativeGit;
    }

    public void setCommitIdGenerationMode(String commitIdGenerationMode){
        this.commitIdGenerationMode = commitIdGenerationMode;
    }
}
