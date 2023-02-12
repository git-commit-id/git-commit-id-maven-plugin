package pl.project13.maven.git;

import pl.project13.core.*;
import pl.project13.core.cibuild.BuildServerDataProvider;
import pl.project13.core.git.GitDescribeConfig;
import pl.project13.core.log.LoggerBridge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

public class Externalize {
    protected interface Callback {
        /**
         * @return Supplier that provides the version of the project that is currently evaluated.
         *         Used to determine {@link GitCommitPropertyConstant#BUILD_VERSION}.
         */
        Supplier<String> supplyProjectVersion();

        /**
         * @return Logging Interface
         */
        @Nonnull
        LoggerBridge getLoggerBridge();

        /**
         * @return The date format to be used for any dates exported by this plugin.
         *         It should be a valid {@link SimpleDateFormat} string.
         */
        @Nonnull
        String getDateFormat();

        /**
         * <p>The timezone used in the date format of dates exported by this plugin.
         * It should be a valid Timezone string such as {@code 'America/Los_Angeles'}, {@code 'GMT+10'} or {@code 'PST'}.</p>
         *
         * <p>Try to avoid three-letter time zone IDs because the same abbreviation is often used for multiple time zones.
         * Please review <a href="https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html">https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html</a> for more information on this issue.</p>
         */
        @Nonnull
        String getDateFormatTimeZone();

        /**
         * The prefix to expose the properties on. For example {@code 'git'} would allow you to access {@code ${git.branch}}.
         */
        @Nonnull
        String getPrefixDot();

        /**
         * <p>List of properties to exclude from the resulting file.
         * May be useful when you want to hide {@code 'git.remote.origin.url'} (maybe because it contains your repo password?)
         * or the email of the committer.</p>
         *
         * <p>Supports wildcards: you can write {@code 'git.commit.user.*'} to exclude both the {@code 'name'}
         * as well as {@code 'email'} properties from being emitted into the resulting files.</p>
         *
         * <p><b>Note:</b> The strings here are Java regular expressions: {@code '.*'} is a wildcard, not plain {@code '*'}.</p>
         */
        List<String> getExcludeProperties();

        /**
         * <p>List of properties to include into the resulting file. Only properties specified here will be included.
         * This list will be overruled by the {@code 'excludeProperties'}.</p>
         *
         * <p>Supports wildcards: you can write {@code 'git.commit.user.*'} to include both the {@code 'name'}
         * as well as {@code 'email'} properties into the resulting files.</p>
         *
         * <p><b>Note:</b> The strings here are Java regular expressions: {@code '.*'} is a wildcard, not plain {@code '*'}.</p>
         */
        List<String> getIncludeOnlyProperties();

        /**
         * Timestamp for reproducible output archive entries
         * (https://maven.apache.org/guides/mini/guide-reproducible-builds.html).
         * The value from <code>${project.build.outputTimestamp}</code> is either formatted as ISO 8601
         * <code>yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like
         * <a href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>.
         */
        @Nullable
        Date getReproducibleBuildOutputTimestamp() throws GitCommitIdExecutionException;

        /**
         * Set this to {@code 'true'} to use native Git executable to fetch information about the repository.
         * It is in most cases faster but requires a git executable to be installed in system.
         * By default the plugin will use jGit implementation as a source of information about the repository.
         */
        boolean useNativeGit();

        /**
         * Allow to specify a timeout (in milliseconds) for fetching information with the native Git executable.
         * Note that {@code useNativeGit} needs to be set to {@code 'true'} to use native Git executable.
         */
        long getNativeGitTimeoutInMs();

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
         */
        int getAbbrevLength();

        /**
         * Configuration for the {@code 'git-describe'} command.
         * You can modify the dirty marker, abbrev length and other options here.
         */
        GitDescribeConfig getGitDescribe();

        /**
         * <p>The mode of {@code 'git.commit.id'} property generation.</p>
         *
         * <p>{@code 'git.commit.id'} property name is incompatible with json export
         * (see <a href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/122">issue #122</a>).
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
         */
        CommitIdGenerationMode getCommitIdGenerationMode();

        /**
         * Use branch name from build environment. Set to {@code 'false'} to use JGit/GIT to get current branch name.
         * Useful when using the JGitflow maven plugin.
         * Note: If not using "Check out to specific local branch' and setting this to false may result in getting
         * detached head state and therefore a commit id as branch name.
         */
        boolean getUseBranchNameFromBuildEnvironment();

        /**
         * Controls whether the git plugin tries to access remote repos to fetch latest information
         * or only use local information.
         *
         * :warning: Before version 5.X.X the default was set to {@code false} causing the plugin to operate
         * in online-mode by default.
         */
        boolean isOffline();

        /**
         * Allow to tell the plugin what commit should be used as reference to generate the properties from.
         * By default this property is simply set to <p>HEAD</p> which should reference to the latest commit in your repository.
         *
         * In general this property can be set to something generic like <p>HEAD^1</p> or point to a branch or tag-name.
         * To support any kind or use-case this configuration can also be set to an entire commit-hash or it's abbreviated version.
         *
         * A use-case for this feature can be found in https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/338.
         *
         * Please note that for security purposes not all references might be allowed as configuration.
         * If you have a specific use-case that is currently not white listed feel free to file an issue.
         */
        String getEvaluateOnCommit();

        /**
         * The root directory of the repository we want to check.
         */
        File getDotGitDirectory();
    }

    protected static void loadBuildData(@Nonnull Callback cb, @Nonnull Properties properties) throws GitCommitIdExecutionException {
        Map<String, Supplier<String>> additionalProperties = Collections.singletonMap(
                GitCommitPropertyConstant.BUILD_VERSION, cb.supplyProjectVersion());
        BuildServerDataProvider buildServerDataProvider = BuildServerDataProvider.getBuildServerProvider(
                System.getenv(), cb.getLoggerBridge());
        buildServerDataProvider
                .setDateFormat(cb.getDateFormat())
                .setDateFormatTimeZone(cb.getDateFormatTimeZone())
                .setAdditionalProperties(additionalProperties)
                .setPrefixDot(cb.getPrefixDot())
                .setExcludeProperties(cb.getExcludeProperties())
                .setIncludeOnlyProperties(cb.getIncludeOnlyProperties());
        buildServerDataProvider.loadBuildData(properties, cb.getReproducibleBuildOutputTimestamp());
    }

    protected static void loadGitData(@Nonnull Callback cb, @Nonnull Properties properties) throws GitCommitIdExecutionException {
        if (cb.useNativeGit()) {
            loadGitDataWithNativeGit(cb, properties);
        } else {
            loadGitDataWithJGit(cb, properties);
        }
    }

    private static void loadGitDataWithNativeGit(@Nonnull Callback cb, @Nonnull Properties properties) throws GitCommitIdExecutionException {
        GitDataProvider nativeGitProvider = NativeGitProvider
                .on(cb.getDotGitDirectory().getParentFile(), cb.getNativeGitTimeoutInMs(), cb.getLoggerBridge())
                .setPrefixDot(cb.getPrefixDot())
                .setAbbrevLength(cb.getAbbrevLength())
                .setDateFormat(cb.getDateFormat())
                .setDateFormatTimeZone(cb.getDateFormatTimeZone())
                .setGitDescribe(cb.getGitDescribe())
                .setCommitIdGenerationMode(cb.getCommitIdGenerationMode())
                .setUseBranchNameFromBuildEnvironment(cb.getUseBranchNameFromBuildEnvironment())
                .setExcludeProperties(cb.getExcludeProperties())
                .setIncludeOnlyProperties(cb.getIncludeOnlyProperties())
                .setOffline(cb.isOffline());

        nativeGitProvider.loadGitData(cb.getEvaluateOnCommit(), properties);
    }

    private static void loadGitDataWithJGit(@Nonnull Callback cb, @Nonnull Properties properties) throws GitCommitIdExecutionException {
        GitDataProvider jGitProvider = JGitProvider
                .on(cb.getDotGitDirectory(), cb.getLoggerBridge())
                .setPrefixDot(cb.getPrefixDot())
                .setAbbrevLength(cb.getAbbrevLength())
                .setDateFormat(cb.getDateFormat())
                .setDateFormatTimeZone(cb.getDateFormatTimeZone())
                .setGitDescribe(cb.getGitDescribe())
                .setCommitIdGenerationMode(cb.getCommitIdGenerationMode())
                .setUseBranchNameFromBuildEnvironment(cb.getUseBranchNameFromBuildEnvironment())
                .setExcludeProperties(cb.getExcludeProperties())
                .setIncludeOnlyProperties(cb.getIncludeOnlyProperties())
                .setOffline(cb.isOffline());

        jGitProvider.loadGitData(cb.getEvaluateOnCommit(), properties);
    }
}
