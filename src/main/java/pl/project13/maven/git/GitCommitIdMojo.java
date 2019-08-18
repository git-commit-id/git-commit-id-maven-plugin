/*
 * This file is part of git-commit-id-plugin by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.build.incremental.BuildContext;

import pl.project13.maven.git.build.BuildServerDataProvider;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Puts git build-time information into property files or maven's properties.
 *
 * @since 1.0
 */
@Mojo(name = "revision", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class GitCommitIdMojo extends AbstractMojo {
  private static final String CONTEXT_KEY = GitCommitIdMojo.class.getName() + ".properties";

  /**
   * The Maven Project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  MavenProject project;

  /**
   * The list of projects in the reactor.
   */
  @Parameter(defaultValue = "${reactorProjects}", readonly = true)
  List<MavenProject> reactorProjects;

  /**
   * The Maven Session Object.
   */
  @Parameter(property = "session", required = true, readonly = true)
  MavenSession session;

  /**
   * The Maven settings.
   */
  @Parameter(property = "settings", required = true, readonly = true)
  Settings settings;

  /**
   * <p>Set this to {@code 'true'} to inject git properties into all reactor projects, not just the current one.</p>
   *
   * <p>Injecting into all projects may slow down the build and you don't always need this feature.
   * See <a href="https://github.com/git-commit-id/maven-git-commit-id-plugin/pull/65">pull #65</a> for details about why you might want to skip this.
   * </p>
   */
  @Parameter(defaultValue = "false")
  boolean injectAllReactorProjects;

  /**
   * Set this to {@code 'true'} to print more info while scanning for paths.
   * It will make git-commit-id "eat its own dog food" :-)
   */
  @Parameter(defaultValue = "false")
  boolean verbose;

  /**
   * Set this to {@code 'false'} to execute plugin in 'pom' packaged projects.
   */
  @Parameter(defaultValue = "true")
  boolean skipPoms;

  /**
   * Set this to {@code 'true'} to generate {@code 'git.properties'} file.
   * By default plugin only adds properties to maven project properties.
   */
  @Parameter(defaultValue = "false")
  boolean generateGitPropertiesFile;

  /**
   * <p>The location of {@code 'git.properties'} file. Set {@code 'generateGitPropertiesFile'} to {@code 'true'}
   * to generate this file.</p>
   *
   * <p>The path here is relative to your project src directory.</p>
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}/git.properties")
  String generateGitPropertiesFilename;

  /**
   * The root directory of the repository we want to check.
   */
  @Parameter(defaultValue = "${project.basedir}/.git")
  File dotGitDirectory;

  /**
   * Configuration for the {@code 'git-describe'} command.
   * You can modify the dirty marker, abbrev length and other options here.
   */
  @Parameter
  GitDescribeConfig gitDescribe;

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
  int abbrevLength;

  /**
   * The format to save properties in: {@code 'properties'} or {@code 'json'}.
   */
  @Parameter(defaultValue = "properties")
  String format;

  /**
   * The prefix to expose the properties on. For example {@code 'git'} would allow you to access {@code ${git.branch}}.
   */
  @Parameter(defaultValue = "git")
  String prefix;
  // prefix with dot appended if needed
  private String prefixDot = "";

  /**
   * The date format to be used for any dates exported by this plugin. It should be a valid {@link SimpleDateFormat} string.
   */
  @Parameter(defaultValue = "yyyy-MM-dd'T'HH:mm:ssZ")
  String dateFormat;

  /**
   * <p>The timezone used in the date format of dates exported by this plugin.
   * It should be a valid Timezone string such as {@code 'America/Los_Angeles'}, {@code 'GMT+10'} or {@code 'PST'}.</p>
   *
   * <p>Try to avoid three-letter time zone IDs because the same abbreviation is often used for multiple time zones.
   * Please review <a href="https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html">https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html</a> for more information on this issue.</p>
   */
  @Parameter
  String dateFormatTimeZone;

  /**
   * Set this to {@code 'false'} to continue the build on missing {@code '.git'} directory.
   */
  @Parameter(defaultValue = "true")
  boolean failOnNoGitDirectory;

  /**
   * <p>Set this to {@code 'false'} to continue the build even if unable to get enough data for a complete run.
   * This may be useful during CI builds if the CI server does weird things to the repository.</p>
   *
   * <p>Setting this value to {@code 'false'} causes the plugin to gracefully tell you "I did my best"
   * and abort its execution if unable to obtain git meta data - yet the build will continue to run without failing.</p>
   *
   * <p>See <a href="https://github.com/git-commit-id/maven-git-commit-id-plugin/issues/63">issue #63</a>
   * for a rationale behind this flag.</p>
   */
  @Parameter(defaultValue = "true")
  boolean failOnUnableToExtractRepoInfo;

  /**
   * Set this to {@code 'true'} to use native Git executable to fetch information about the repository.
   * It is in most cases faster but requires a git executable to be installed in system.
   * By default the plugin will use jGit implementation as a source of information about the repository.
   *
   * NOTE / WARNING:
   * Do *NOT* set this property inside the configuration of your plugin, so it would be
   * possible to override it from command line.
   *
   * @since 2.1.9
   */
  @Parameter(property = "maven.gitcommitid.nativegit", defaultValue = "false")
  boolean useNativeGit;

  /**
   * Set this to {@code 'true'} to skip plugin execution.
   * @since 2.1.8
   */
  @Parameter(defaultValue = "false")
  boolean skip;


  /**
   * Set this to {@code 'true'} to skip plugin execution via commandline.
   * NOTE / WARNING:
   * Do *NOT* set this property inside the configuration of your plugin.
   * Please read
   * https://github.com/git-commit-id/maven-git-commit-id-plugin/issues/315
   * to find out why.
   * @since 2.2.4
   */
  @Parameter(property = "maven.gitcommitid.skip", defaultValue = "false")
  private boolean skipViaCommandLine;

  /**
   * <p>Set this to {@code 'true'} to only run once in a multi-module build.  This probably won't "do the right thing"
   * if your project has more than one git repository.  If you use this with {@code 'generateGitPropertiesFile'},
   * it will only generate (or update) the file in the directory where you started your build.</p>
   *
   * <p>The git.* maven properties are available in all modules.</p>
   * @since 2.1.12
   */
  @Parameter(defaultValue = "false")
  boolean runOnlyOnce;

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
  List<String> excludeProperties;

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
  List<String> includeOnlyProperties;

  /**
   * <p>The mode of {@code 'git.commit.id'} property generation.</p>
   *
   * <p>{@code 'git.commit.id'} property name is incompatible with json export
   * (see <a href="https://github.com/git-commit-id/maven-git-commit-id-plugin/issues/122">issue #122</a>).
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
  String commitIdGenerationMode;
  private CommitIdGenerationMode commitIdGenerationModeEnum;

  /**
   * Allow to replace certain characters or strings using regular expressions within the generated git properties.
   * On a configuration level it can be defined whether the replacement should affect all properties or just a single one.
   *
   * Please note that the replacement will only be applied to properties that are being generated by the plugin.
   * If you want to replace properties that are being generated by other plugins you may want to use the maven-replacer-plugin or any other alternative.
   * @since 2.2.3
   */
  @Parameter
  List<ReplacementProperty> replacementProperties;

  /**
   * Allow to tell the plugin what commit should be used as reference to generate the properties from.
   * By default this property is simply set to <p>HEAD</p> which should reference to the latest commit in your repository.
   *
   * In general this property can be set to something generic like <p>HEAD^1</p> or point to a branch or tag-name.
   * To support any kind or use-case this configuration can also be set to an entire commit-hash or it's abbreviated version.
   *
   * A use-case for this feature can be found in https://github.com/git-commit-id/maven-git-commit-id-plugin/issues/338.
   *
   * Please note that for security purposes not all references might be allowed as configuration.
   * If you have a specific use-case that is currently not white listed feel free to file an issue.
   * @since 2.2.4
   */
  @Parameter(defaultValue = "HEAD")
  String evaluateOnCommit;
  protected static final Pattern allowedCharactersForEvaluateOnCommit = Pattern.compile("[a-zA-Z0-9\\_\\-\\^\\/\\.]+");

  /**
   * Allow to specify a timeout (in milliseconds) for fetching information with the native Git executable.
   * Note that {@code useNativeGit} needs to be set to {@code 'true'} to use native Git executable.
   * @since 3.0.0
   */
  @Parameter(defaultValue = "30000")
  long nativeGitTimeoutInMs;

  /**
   * Use branch name from build environment. Set to {@code 'false'} to use JGit/GIT to get current branch name.
   * Useful when using the JGitflow maven plugin.
   * Note: If not using "Check out to specific local branch' and setting this to false may result in getting
   * detached head state and therefore a commit id as branch name.
   * @since 3.0.0
   */
  @Parameter(defaultValue = "true")
  boolean useBranchNameFromBuildEnvironment;


  /**
   * Controls if this plugin should expose the generated properties into System.properties
   * @since 3.0.0
   */
  @Parameter(defaultValue = "true")
  boolean injectIntoSysProperties;

  /**
   * Controls whether the git plugin tries to access remote repos to fetch latest information
   * or only use local information.
   * @since 3.0.1
   */
  @Parameter(defaultValue = "false")
  boolean offline;

  /**
   * Injected {@link BuildContext} to recognize incremental builds.
   */
  @Component
  private BuildContext buildContext;

  /**
   * The properties we store our data in and then expose them.
   */
  private Properties properties;

  /**
   * Charset to read-write project sources.
   */
  private Charset sourceCharset = StandardCharsets.UTF_8;

  @Nonnull
  private final LoggerBridge log = new MavenLoggerBridge(this, false);

  @Nonnull
  private PropertiesFilterer propertiesFilterer = new PropertiesFilterer(log);

  @Nonnull
  PropertiesReplacer propertiesReplacer = new PropertiesReplacer(log);

  @Override
  public void execute() throws MojoExecutionException {
    try {
      // Set the verbose setting: now it should be correctly loaded from maven.
      log.setVerbose(verbose);

      // Skip mojo execution on incremental builds.
      if (buildContext != null && buildContext.isIncremental()) {
        // Except if properties file is missing at all
        if (!generateGitPropertiesFile ||
                PropertiesFileGenerator.craftPropertiesOutputFile(
                        project.getBasedir(), generateGitPropertiesFilename).exists()) {
          return;
        }
      }

      // read source encoding from project properties for those who still doesn't use UTF-8
      String sourceEncoding = project.getProperties().getProperty("project.build.sourceEncoding");
      if (null != sourceEncoding) {
        sourceCharset = Charset.forName(sourceEncoding);
      } else {
        sourceCharset = Charset.defaultCharset();
      }

      if (skip || skipViaCommandLine) {
        log.info("skip is enabled, skipping execution!");
        return;
      }

      if (runOnlyOnce) {
        if (!session.getExecutionRootDirectory().equals(session.getCurrentProject().getBasedir().getAbsolutePath())) {
          log.info("runOnlyOnce is enabled and this project is not the top level project, skipping execution!");
          return;
        }
      }

      if (isPomProject(project) && skipPoms) {
        log.info("isPomProject is true and skipPoms is true, return");
        return;
      }

      dotGitDirectory = lookupGitDirectory();
      if (failOnNoGitDirectory && !directoryExists(dotGitDirectory)) {
        throw new GitCommitIdExecutionException(".git directory is not found! Please specify a valid [dotGitDirectory] in your pom.xml");
      }

      if (gitDescribe == null) {
        gitDescribe = new GitDescribeConfig();
      }

      if (dotGitDirectory != null) {
        log.info("dotGitDirectory {}", dotGitDirectory.getAbsolutePath());
      } else {
        log.info("dotGitDirectory is null, aborting execution!");
        return;
      }

      if ((evaluateOnCommit == null) || !allowedCharactersForEvaluateOnCommit.matcher(evaluateOnCommit).matches()) {
        log.error("suspicious argument for evaluateOnCommit, aborting execution!");
        return;
      }

      try {
        try {
          commitIdGenerationModeEnum = CommitIdGenerationMode.valueOf(commitIdGenerationMode.toUpperCase());
        } catch (IllegalArgumentException e) {
          log.warn("Detected wrong setting for 'commitIdGenerationMode'. Falling back to default 'flat' mode!");
          commitIdGenerationModeEnum = CommitIdGenerationMode.FLAT;
        }

        properties = new Properties();

        String trimmedPrefix = prefix.trim();
        prefixDot = trimmedPrefix.equals("") ? "" : trimmedPrefix + ".";

        // check if properties have already been injected
        Properties contextProperties = getContextProperties(project);
        boolean alreadyInjected = injectAllReactorProjects && contextProperties != null;
        if (alreadyInjected) {
          log.info("injectAllReactorProjects is enabled - attempting to use the already computed values");
          properties = contextProperties;
        }

        loadGitData(properties);
        loadBuildData(properties);
        propertiesReplacer.performReplacement(properties, replacementProperties);
        propertiesFilterer.filter(properties, includeOnlyProperties, this.prefixDot);
        propertiesFilterer.filterNot(properties, excludeProperties, this.prefixDot);

        logProperties();

        if (generateGitPropertiesFile) {
          new PropertiesFileGenerator(log, buildContext, format, prefixDot, project.getName()).maybeGeneratePropertiesFile(
                  properties, project.getBasedir(), generateGitPropertiesFilename, sourceCharset);
        }

        publishPropertiesInto(project.getProperties());
        // some plugins rely on the user properties (e.g. flatten-maven-plugin)
        publishPropertiesInto(session.getUserProperties());

        if (injectAllReactorProjects) {
          appendPropertiesToReactorProjects();
        }

        if (injectIntoSysProperties) {
          publishPropertiesInto(System.getProperties());
          publishPropertiesInto(session.getSystemProperties());
        }

      } catch (Exception e) {
        handlePluginFailure(e);
      }
    } catch (GitCommitIdExecutionException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  @Nullable
  private Properties getContextProperties(MavenProject project) {
    Object stored = project.getContextValue(CONTEXT_KEY);
    if (stored instanceof Properties) {
      return (Properties)stored;
    }
    return null;
  }

  private void loadBuildData(Properties properties) {
    BuildServerDataProvider buildServerDataProvider = BuildServerDataProvider.getBuildServerProvider(System.getenv(),log);
    buildServerDataProvider
        .setDateFormat(dateFormat)
        .setDateFormatTimeZone(dateFormatTimeZone)
        .setProject(project)
        .setPrefixDot(prefixDot)
        .setExcludeProperties(excludeProperties)
        .setIncludeOnlyProperties(includeOnlyProperties);

    buildServerDataProvider.loadBuildData(properties);
  }

  private void publishPropertiesInto(Properties p) {
    p.putAll(properties);
  }

  /**
   * Reacts to an exception based on the {@code failOnUnableToExtractRepoInfo} setting.
   * If it's true, an GitCommitIdExecutionException will be thrown, otherwise we just log an error message.
   *
   * @throws GitCommitIdExecutionException which will be thrown in case the
   *                                {@code failOnUnableToExtractRepoInfo} setting was set to true.
   */
  private void handlePluginFailure(Exception e) throws GitCommitIdExecutionException {
    if (failOnUnableToExtractRepoInfo) {
      throw new GitCommitIdExecutionException("Could not complete Mojo execution...", e);
    } else {
      log.error(e.getMessage(), e);
    }
  }

  private void appendPropertiesToReactorProjects() {
    for (MavenProject mavenProject : reactorProjects) {
      log.debug("Adding properties to project: {}", mavenProject.getName());

      publishPropertiesInto(mavenProject.getProperties());
      mavenProject.setContextValue(CONTEXT_KEY, properties);
    }
    log.info("Added properties to {} projects", reactorProjects.size());
  }

  /**
   * Find the git directory of the currently used project.
   * If it's not already specified, this method will try to find it.
   *
   * @return the File representation of the .git directory
   */
  private File lookupGitDirectory() throws GitCommitIdExecutionException {
    return new GitDirLocator(project, reactorProjects).lookupGitDirectory(dotGitDirectory);
  }

  private void logProperties() {
    for (Object key : properties.keySet()) {
      String keyString = key.toString();
      log.info("including property {} in results", keyString);
    }
  }

  private void loadGitData(@Nonnull Properties properties) throws GitCommitIdExecutionException {
    if (useNativeGit) {
      loadGitDataWithNativeGit(properties);
    } else {
      loadGitDataWithJGit(properties);
    }
  }

  private void loadGitDataWithNativeGit(@Nonnull Properties properties) throws GitCommitIdExecutionException {
    try {
      final File basedir = project.getBasedir().getCanonicalFile();

      GitDataProvider nativeGitProvider = NativeGitProvider
              .on(basedir, nativeGitTimeoutInMs, log)
              .setPrefixDot(prefixDot)
              .setAbbrevLength(abbrevLength)
              .setDateFormat(dateFormat)
              .setDateFormatTimeZone(dateFormatTimeZone)
              .setGitDescribe(gitDescribe)
              .setCommitIdGenerationMode(commitIdGenerationModeEnum)
              .setUseBranchNameFromBuildEnvironment(useBranchNameFromBuildEnvironment)
              .setExcludeProperties(excludeProperties)
              .setIncludeOnlyProperties(includeOnlyProperties)
              .setOffline(offline || settings.isOffline());

      nativeGitProvider.loadGitData(evaluateOnCommit, properties);
    } catch (IOException e) {
      throw new GitCommitIdExecutionException(e);
    }
  }

  private void loadGitDataWithJGit(@Nonnull Properties properties) throws GitCommitIdExecutionException {
    GitDataProvider jGitProvider = JGitProvider
        .on(dotGitDirectory, log)
        .setPrefixDot(prefixDot)
        .setAbbrevLength(abbrevLength)
        .setDateFormat(dateFormat)
        .setDateFormatTimeZone(dateFormatTimeZone)
        .setGitDescribe(gitDescribe)
        .setCommitIdGenerationMode(commitIdGenerationModeEnum)
        .setUseBranchNameFromBuildEnvironment(useBranchNameFromBuildEnvironment)
        .setExcludeProperties(excludeProperties)
        .setIncludeOnlyProperties(includeOnlyProperties)
        .setOffline(offline || settings.isOffline());

    jGitProvider.loadGitData(evaluateOnCommit, properties);
  }

  private boolean isPomProject(@Nonnull MavenProject project) {
    return project.getPackaging().equalsIgnoreCase("pom");
  }

  private boolean directoryExists(@Nullable File fileLocation) {
    return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory();
  }
}
