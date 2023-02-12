/*
 * This file is part of git-commit-id-maven-plugin by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-maven-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-maven-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.maven.git;

import org.sonatype.plexus.build.incremental.BuildContext;
import pl.project13.core.*;
import pl.project13.core.cibuild.BuildServerDataProvider;
import pl.project13.core.git.GitDescribeConfig;
import pl.project13.core.log.LoggerBridge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
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
     *
     * @return The timezone used in the date format of dates exported by this plugin.
     */
    @Nonnull
    String getDateFormatTimeZone();

    /**
     * The prefix to expose the properties on. For example {@code 'git'} would allow you to access {@code ${git.branch}}.
     *
     * @return The prefix to expose the properties on.
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
     *
     * @return List of properties to exclude
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
     *
     * @return List of properties to include
     */
    List<String> getIncludeOnlyProperties();

    /**
     * Timestamp for reproducible output archive entries
     * (https://maven.apache.org/guides/mini/guide-reproducible-builds.html).
     * The value from <code>${project.build.outputTimestamp}</code> is either formatted as ISO 8601
     * <code>yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like
     * <a href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>.
     *
     * @throws GitCommitIdExecutionException In case the user provided time stamp is invalid a GitCommitIdExecutionException is thrown
     * @return Timestamp for reproducible output archive entries.
     */
    @Nullable
    Date getReproducibleBuildOutputTimestamp() throws GitCommitIdExecutionException;

    /**
     * Set this to {@code 'true'} to use native Git executable to fetch information about the repository.
     * It is in most cases faster but requires a git executable to be installed in system.
     * By default the plugin will use jGit implementation as a source of information about the repository.
     *
     * @return Controls if this plugin should use the native Git executable.
     */
    boolean useNativeGit();

    /**
     * Allow to specify a timeout (in milliseconds) for fetching information with the native Git executable.
     * Note that {@code useNativeGit} needs to be set to {@code 'true'} to use native Git executable.
     *
     * @return A timeout (in milliseconds) for fetching information with the native Git executable.
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
     *
     * @return Minimum length of {@code 'git.commit.id.abbrev'} property.
     */
    int getAbbrevLength();

    /**
     * Configuration for the {@code 'git-describe'} command.
     * You can modify the dirty marker, abbrev length and other options here.
     *
     * @return Configuration for the {@code 'git-describe'} command.
     */
    GitDescribeConfig getGitDescribe();

    /**
     * <p>The mode of {@code 'git.commit.id'} property generation.</p>
     *
     * {@code 'git.commit.id'} property name is incompatible with json export
     * (see <a href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/122">issue #122</a>).
     * This property allows one either to preserve backward compatibility or to enable fully valid json export:
     *
     * <ol>
     * <li>{@code 'flat'} (default) generates the property {@code 'git.commit.id'}, preserving backwards compatibility.</li>
     * <li>{@code 'full'} generates the property {@code 'git.commit.id.full'}, enabling fully valid json object export.</li>
     * </ol>
     *
     * <p><b>Note:</b> Depending on your plugin configuration you obviously can choose the `prefix` of your properties
     * by setting it accordingly in the plugin's configuration. As a result this is therefore only an illustration
     * what the switch means when the 'prefix' is set to it's default value.</p>
     * <p><b>Note:</b> If you set the value to something that's not equal to {@code 'flat'} or {@code 'full'} (ignoring the case)
     * the plugin will output a warning and will fallback to the default {@code 'flat'} mode.</p>
     *
     * @return The mode of {@code 'git.commit.id'} property generation.
     */
    CommitIdGenerationMode getCommitIdGenerationMode();

    /**
     * Use branch name from build environment. Set to {@code 'false'} to use JGit/GIT to get current branch name.
     * Useful when using the JGitflow maven plugin.
     * Note: If not using "Check out to specific local branch' and setting this to false may result in getting
     * detached head state and therefore a commit id as branch name.
     *
     * @return Controls if the branch name from build environment should be used.
     */
    boolean getUseBranchNameFromBuildEnvironment();

    /**
     * Controls whether the git plugin tries to access remote repos to fetch latest information
     * or only use local information.
     *
     * :warning: Before version 5.X.X the default was set to {@code false} causing the plugin to operate
     * in online-mode by default.
     *
     * @return Controls whether the git plugin tries to access remote repos to fetch latest information.
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
     *
     * @return Tell the plugin what commit should be used as reference to generate the properties from.
     */
    String getEvaluateOnCommit();

    /**
     * @return The root directory of the repository we want to check.
     */
    File getDotGitDirectory();

    /**
     * Set this to {@code 'true'} to generate a {@code 'git.properties'} file.
     * By default plugin only adds properties to maven project properties.
     *
     * @return Control if the plugin should generate a {@code 'git.properties'} file.
     */
    boolean shouldGenerateGitPropertiesFile();

    /**
     * Callback when the plugin wants to publish a set of properties.
     * @param properties The properties the plugin want's to publish to the user.
     */
    void performPublishToAllSystemEnvironments(Properties properties);

    /**
     * Callback when the plugin wants to perform the properties replacement.
     * @param properties The current set of properties.
     */
    void performPropertiesReplacement(Properties properties);

    /**
     * @return The output format of the generated properties file. Can either be "properties" or "json".
     */
    // TODO: Why is this a String and not an enum?
    String getPropertiesOutputFormat();

    /**
     * @return The BuildContext
     */
    // TODO: Huh why is this here? Isn't this too maven specific?
    @Deprecated
    BuildContext getBuildContext();

    /**
     * @return The project name
     */
    String getProjectName();

    /**
     * @return The project base dir
     */
    File getProjectBaseDir();

    /**
     * @return The optional name of the properties filename where properties should be dumped into
     */
    String getGenerateGitPropertiesFilename();

    /**
     * @return The Charset in which format the properties should be dumped (e.g. 'UTF-8')
     */
    Charset getPropertiesSourceCharset();
  }

  protected static void runPlugin(@Nonnull Callback cb, @Nullable Properties contextProperties) throws GitCommitIdExecutionException {
    PropertiesFilterer propertiesFilterer = new PropertiesFilterer(cb.getLoggerBridge());

    // The properties we store our data in and then expose them.
    Properties properties = contextProperties == null
            ? new Properties()
            : contextProperties;

    loadGitData(cb, properties);
    loadBuildData(cb, properties);
    // first round of publication and filtering
    // (we need to make variables available for the ParameterExpressionEvaluator)
    propertiesFilterer.filter(properties, cb.getIncludeOnlyProperties(), cb.getPrefixDot());
    propertiesFilterer.filterNot(properties, cb.getExcludeProperties(), cb.getPrefixDot());
    cb.performPublishToAllSystemEnvironments(properties);

    cb.performPropertiesReplacement(properties);
    if (cb.shouldGenerateGitPropertiesFile()) {
      new PropertiesFileGenerator(
              cb.getLoggerBridge(),
              cb.getBuildContext(),
              cb.getPropertiesOutputFormat(),
              cb.getPrefixDot(),
              cb.getProjectName()
      ).maybeGeneratePropertiesFile(
              properties,
              cb.getProjectBaseDir(),
              cb.getGenerateGitPropertiesFilename(),
              cb.getPropertiesSourceCharset()
      );
    }

    // publish properties again since we might have new properties gained by the replacement
    cb.performPublishToAllSystemEnvironments(properties);
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
