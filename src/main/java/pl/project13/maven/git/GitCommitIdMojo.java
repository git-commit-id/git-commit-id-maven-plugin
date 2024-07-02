/*
 * This file is part of git-commit-id-maven-plugin
 * Originally invented by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
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

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.joda.time.DateTime;
import org.sonatype.plexus.build.incremental.BuildContext;
import pl.project13.core.CommitIdGenerationMode;
import pl.project13.core.CommitIdPropertiesOutputFormat;
import pl.project13.core.GitCommitIdExecutionException;
import pl.project13.core.GitCommitIdPlugin;
import pl.project13.core.PropertiesFileGenerator;
import pl.project13.core.git.GitDescribeConfig;
import pl.project13.core.log.LogInterface;
import pl.project13.core.util.BuildFileChangeListener;

/**
 * Puts git build-time information into property files or maven's properties.
 *
 * @since 1.0
 */
@Mojo(name = "revision", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class GitCommitIdMojo extends AbstractMojo {
  private static final String CONTEXT_KEY = GitCommitIdMojo.class.getName() + ".properties";

  // ===============================================================================================
  // Parameter injected by maven itself can't be configured in the pom.xml!

  /**
   * This parameter can't be configured in the {@code pom.xml} it represents the Maven Project that
   * will be injected by maven itself.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  MavenProject project;

  /**
   * This parameter can't be configured in the {@code pom.xml} it represents the list of projects in
   * the reactor that will be injected by maven itself.
   */
  @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
  List<MavenProject> reactorProjects;

  /**
   * This parameter can't be configured in the {@code pom.xml} it represents the Mojo Execution that
   * will be injected by maven itself.
   */
  @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
  MojoExecution mojoExecution;

  /**
   * This parameter can't be configured in the {@code pom.xml} it represents the Maven Session
   * Object that will be injected by maven itself.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  MavenSession session;

  /**
   * This parameter can't be configured in the {@code pom.xml} it represents the Maven settings that
   * will be injected by maven itself.
   */
  @Parameter(defaultValue = "${settings}", readonly = true, required = true)
  Settings settings;

  // ===============================================================================================
  // Parameters that can be configured in the pom.xml

  /**
   * Configuration to tell the git-commit-id-maven-plugin if the plugin should inject the git
   * properties into all reactor projects not just the current one.
   *
   * <p>The property is set to {@code false} by default to prevent the overriding of properties that
   * may be unrelated to the project. If you need to expose your git properties to another maven
   * module (e.g. maven-antrun-plugin) you need to set it to {@code true}.
   *
   * <p>Inject git properties into all reactor projects, not just the current one may slow down the
   * build and you don't always need this feature.
   *
   * <p>For details about why you might want to skip this, read this issue: <a
   * href="https://github.com/git-commit-id/git-commit-id-maven-plugin/pull/65">pull #65</a>
   *
   * <p>Example:
   *
   * <pre>{@code
   * <injectAllReactorProjects>false</injectAllReactorProjects>
   * }</pre>
   *
   * @since 2.1.4
   */
  @Parameter(defaultValue = "false")
  boolean injectAllReactorProjects;

  /**
   * Configuration to tell the git-commit-id-maven-plugin to print some more verbose information
   * during the build (e.g. a summary of all collected properties when it's done).
   *
   * <p>By default this option is disabled (set to {@code false})
   *
   * <p>Note, If enabled (set to {@code true}) the plugin may print information you deem sensible,
   * so be extra cautious when you share those.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <verbose>false</verbose>
   * }</pre>
   */
  @Parameter(defaultValue = "false")
  boolean verbose;

  /**
   * Configuration to tell the git-commit-id-maven-plugin to <b>not</b> run in a pom packaged
   * project (e.g. {@code <packaging>pom</packaging>}).
   *
   * <p>By default 'pom' packaged projects will be skipped (to {@code true})
   *
   * <p>You may want to set this to {@code false}, if the plugin should also run inside a pom
   * packaged project. Most projects won't need to override this property. For an use-case for this
   * kind of behaviour see:
   *
   * <p><a href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/21">Issue 21</a>
   *
   * <p>Note: The plugin might not execute at all, if you also set {@code
   * <runOnlyOnce>true</runOnlyOnce>}
   *
   * <p>Example:
   *
   * <pre>{@code
   * <skipPoms>true</skipPoms>
   * }</pre>
   */
  @Parameter(defaultValue = "true")
  boolean skipPoms;

  /**
   * Configuration to tell the git-commit-id-maven-plugin to generate a {@code 'git.properties'}
   * file. By default the plugin will not <b>not</b> generate such a file (set to {@code false}),
   * and only adds properties to maven project properties.
   *
   * <p>Set this to {@code 'true'} if you want an easy way to expose your git information into your
   * final artifact (jar, war, ...), which will generate a properties file (with filled out values)
   * that can be configured to end up in the final artifact. Refer to the configuration of {@link
   * #generateGitPropertiesFilename}` that helps you setup that final path.
   *
   * <p>Such generated property file, can normally be read using during runtime.
   *
   * <pre>
   *     new Properties().load(...)
   * </pre>
   *
   * <p>Note: When writing the {@code git.properties} file the value *git.build.time* will only be
   * updated when things in the commit information have changed. If you only change a bit of your
   * code and rebuild/rerun you will see an older timestamp that you may have expected. Essentially
   * the functional meaning becomes **The latest build time when the git information was written to
   * the git.properties file**. The reason why this was done can be found in [issue
   * 151](https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/151).
   *
   * <p>Example:
   *
   * <pre>{@code
   * <generateGitPropertiesFile>true</generateGitPropertiesFile>
   * }</pre>
   */
  @Parameter(defaultValue = "false")
  boolean generateGitPropertiesFile;

  /**
   * Configuration to tell the git-commit-id-maven-plugin about the location where you want to
   * generate a {@code 'git.properties'} file.
   *
   * <p>By default the file would be generated under {@code
   * ${project.build.outputDirectory}/git.properties}, but you would need to set {@link
   * #generateGitPropertiesFile} to {@code true} first to "activate" the generation of this file.
   * You can also choose the format of the generated properties by specifying it under {@link
   * #format}.
   *
   * <p>The path can be relative to {@code ${project.basedir}} (e.g. {@code
   * target/classes/git.properties}) or can be a full path (e.g. {@code
   * ${project.build.outputDirectory}/git.properties}).
   *
   * <p>Note: If you plan to set the generateGitPropertiesFilename-Path to a location where usually
   * the source-files comes from (e.g. {@code src/main/resources}) and experience that your IDE
   * (e.g. eclipse) invokes "Maven Project Builder" once every second, the chances that you are
   * using an IDE where the src-folder is a watched folder for files that are <b>only</b> edited by
   * humans is pretty high. <br>
   * For further information refer to the manual for your specific IDE and check the workflow of
   * "incremental project builders". <br>
   * In order to fix this problem we recommend to set the generateGitPropertiesFilename-Path to a
   * target folder (e.g. {@code ${project.build.outputDirectory}}) since this is the place where all
   * derived/generated resources should go. <br>
   * With plugin version 3.0.0 we introduced a smarter way to counter that issue, but that might not
   * be supported by your IDE. See: <a
   * href="https://github.com/git-commit-id/git-commit-id-maven-plugin/pull/385">pull 385</a> for
   * further information
   *
   * <p>Example:
   *
   * <pre>{@code
   * <generateGitPropertiesFilename>
   *   ${project.build.outputDirectory}/git.properties
   * </generateGitPropertiesFilename>
   * }</pre>
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}/git.properties")
  String generateGitPropertiesFilename;

  /**
   * Controls whether special characters in the properties within the {@link
   * #generateGitPropertiesFilename} should be unicode escaped. By default properties are escaped
   * (e.g. \\u6E2C\\u8A66\\u4E2D\\u6587). If you write commit messages in chinese and want to
   * extract the message without any additional conversion from the generated properties you may
   * want to set this to {@code false}.
   *
   * <p>See <a href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/590">issue
   * 590</a> for further details.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <generateGitPropertiesFileWithEscapedUnicode>
   *   true
   * </generateGitPropertiesFileWithEscapedUnicode>
   * }</pre>
   *
   * @since 6.0.0
   */
  @Parameter(defaultValue = "true")
  boolean generateGitPropertiesFileWithEscapedUnicode;

  /**
   * Configuration to tell the git-commit-id-maven-plugin about the root directory of the git
   * repository we want to check. By default uses {@code ${project.basedir}/.git} will most probably
   * be ok for single module projects, in other cases please use `../` to get higher up in the dir
   * tree (e.g. {@code ${project.basedir}/../.git}).
   *
   * <p>Example:
   *
   * <pre>{@code
   * <dotGitDirectory>
   *   ${project.basedir}/.git
   * </dotGitDirectory>
   * }</pre>
   */
  @Parameter(defaultValue = "${project.basedir}/.git")
  File dotGitDirectory;

  /**
   * Configuration for the {@code 'git-describe'} command. You can modify the dirty marker, abbrev
   * length and other options here. The following `gitDescribe` configuration below is optional and
   * can be leveraged as a really powerful versioning helper. If you are not familiar with <a
   * href="https://github.com/git-commit-id/git-commit-id-maven-plugin#git-describe-&#45;-short-intro-to-an-awesome-command">git-describe</a>
   * it is highly recommended to go through this part of the documentation.
   *
   * <p>More advanced users can most likely skip the explanations in this section, as it just
   * explains the same options that git provides. As a side note this plugin tries to be 1-to-1
   * compatible with git's plain output, even though the describe functionality has been
   * reimplemented manually using JGit (you don't have to have a git executable to use the plugin).
   *
   * <p>For further information refer to <a href="https://git-scm.com/docs/git-describe">this</a>.
   *
   * <p>Example:
   *
   * <pre>{@code
   *     <gitDescribe>
   *         <!--
   *         Default (optional):
   *         false
   *
   *         Explanation:
   *         When you don't want to use `git-describe` information in your build,
   *         you can set this to `true` to avoid to calculate it.
   *         -->
   *         <skip>false</skip>
   *
   *         <!--
   *         Default (optional):
   *         true
   *
   *         Explanation:
   *         In some cases no tag can be found `near` this commit
   *         (e.g. usually when performing a shallow clone).
   *         If this is set to `true` and no tag was found, this property will
   *         fallback to the commit's id instead
   *         (when `true` this property will not become empty).
   *         Set this to `true` when you *always* want to return something meaningful in the
   *         describe property.
   *         -->
   *         <always>true</always>
   *
   *         <!--
   *         Default (optional):
   *         7
   *
   *         Explanation:
   *         In the describe output, the object id of the hash is always
   *         abbreviated to N letters (by default 7).
   *
   *         The typical describe output you'll see therefore is: `v2.1.0-1-gf5cd254`,
   *         where `-1-` means the number of commits away from the mentioned tag and
   *         the `-gf5cd254` part means the first 7 chars of the current commit's id `f5cd254`.
   *         Setting *abbrev* to `0` has the effect of hiding the "distance from tag" and
   *         "object id" parts of the output, so you end up with just the "nearest tag"
   *         (that is, instead `tag-12-gaaaaaaa` with `abbrev = 0` you'd get `tag`).
   *
   *         **Please note that the `g` prefix is included to notify you that it's a commit id,
   *         it is NOT part of the commit's object id** - *this is default git behaviour,
   *         so we're doing the same*.
   *
   *         You can set this to any value between 0 and 40 (inclusive).
   *         `0` carries the special meaning
   *         (checkout the [git describe documentation](docs/git-describe.md) for the
   *         special case abbrev = 0).
   *         Maximum value is `40`, because of max SHA-1 length.
   *         -->
   *         <abbrev>7</abbrev>
   *
   *         <!--
   *         Default (optional):
   *         -dirty
   *
   *         Explanation:
   *         When you run describe on a repository that's in "dirty state" (has uncommitted
   *         changes), the describe output will contain an additional suffix, such as "-devel"
   *         in this example: `v3.5-3-g2222222-devel`. This configuration allows you to alter
   *         that additional suffix and gets appended to describe, while the repo is in
   *         "dirty state". You can configure that suffix to be anything you want, "-DEV" being
   *         a nice example. The "-" sign should be included in the configuration parameter,
   *         as it will not be added automatically.
   *         If in doubt run `git describe &#45;-dirty=-my_thing`
   *         to see how the end result will look like.
   *         -->
   *         <dirty>-dirty</dirty>
   *
   *         <!--
   *         Default (optional):
   *         * (include all tags)
   *
   *         Explanation:
   *         Git describe may contain information to tag names. Set this configuration to only
   *         consider tags matching the given pattern.
   *         This can be used to avoid leaking private tags from the repository.
   *         -->
   *         <match>*</match>
   *
   *         <!--
   *         Default (optional):
   *         false
   *
   *         Explanation:
   *         When you run git-describe it only looks only for *annotated tags* by default.
   *         If you wish to consider *lightweight tags* in your describe as well you would need
   *         to switch this to `true`.
   *
   *         The difference between *annotated tags* and *lightweight tags* is outlined in more
   *         depth <a href="https://github.com/git-commit-id/git-commit-id-maven-plugin/#git-describe-and-a-small-gotcha-with-tags">here</a>
   *         -->
   *         <tags>false</tags>
   *
   *         <!--
   *         Default (optional):
   *         false
   *
   *         Explanation:
   *         git-describe, by default, returns the tag name, if the current commit is tagged.
   *         Set this option to `true` to force it to format the output using
   *         the typical describe format
   *         ("$tag_name-$commits_from_tag-g$commit_id-maybe_dirty"), even if "on" a tag.
   *
   *         An example would be: `tagname-0-gc0ffebabe` - notice that the distance from
   *         the tag is 0 here, if you don't use **forceLongFormat** mode,
   *         the describe for such commit would look like this: `tagname`.
   *         -->
   *         <forceLongFormat>false</forceLongFormat>
   *     </gitDescribe>
   * }
   *
   * </pre>
   *
   * @since 2.1.0
   */
  @Parameter GitDescribeConfig gitDescribe;

  /**
   * Minimum length of {@code 'git.commit.id.abbrev'} property. Value must be from 2 to 40
   * (inclusive), other values will result in an exception.
   *
   * <p>Defaults to `7`
   *
   * <p>An abbreviated commit is a shorter version of commit id. However, it is guaranteed to be
   * unique. To keep this contract, the plugin may decide to print an abbreviated version that is
   * longer than the value specified here.
   *
   * <p><b>Example:</b> You have a very big repository, yet you set this value to 2. It's very
   * probable that you'll end up getting a 4 or 7 char long abbrev version of the commit id. If your
   * repository, on the other hand, has just 4 commits, you'll probably get a 2 char long
   * abbreviation.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <abbrevLength>7</abbrevLength>
   * }</pre>
   *
   * @since 2.0.4
   */
  @Parameter(defaultValue = "7")
  int abbrevLength;

  /**
   * Denotes the format to save properties of the properties file that can be configured with {@link
   * #generateGitPropertiesFilename}.
   *
   * <p>Valid options are encoded in {@link CommitIdPropertiesOutputFormat} and currently would
   * allow "properties" (default) and "json". Future option like yml, toml, ... might be supported
   * at some point.
   *
   * <p>Note: If you set this to "json", you might also should checkout the documentation about
   * {@link #commitIdGenerationMode} and may want to set {@code
   * <commitIdGenerationMode>full</commitIdGenerationMode>}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <format>properties</format>
   * }</pre>
   */
  @Parameter(defaultValue = "properties")
  String format;

  /**
   * Not settable by any configuration in the {@code pom.xml}. For internal use only (represents the
   * {@link #format} the user has set as enum.
   */
  private CommitIdPropertiesOutputFormat commitIdPropertiesOutputFormat;

  /**
   * Configuration to tell the git-commit-id-maven-plugin about the property that will be used as
   * the "namespace" prefix for all exposed/generated properties. An example the plugin may generate
   * the property `${configured-prefix}.commit.id`. Such behaviour can be used to generate
   * properties for multiple git repositories (see <a
   * href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/137#issuecomment-418144756">issue
   * 173</a> for a full example).
   *
   * <p>By default is set to {@code 'git'} that for example would allow you to access {@code
   * ${git.branch}}
   *
   * <p>Example:
   *
   * <pre>{@code
   * <prefix>git</prefix>
   * }</pre>
   */
  @Parameter(defaultValue = "git")
  String prefix;

  /**
   * This date format will be used to format the time of any exposed/generated property that
   * represents dates or times exported by this plugin (e.g. {@code git.commit.time}, {@code
   * git.build.time}). It should be a valid {@link SimpleDateFormat} string.
   *
   * <p>The current dateFormat will be formatted as ISO 8601
   * {@code yyyy-MM-dd'T'HH:mm:ssXXX} and therefore can be used as input to maven's
   * <a href="https://maven.apache.org/guides/mini/guide-reproducible-builds.html">
   * reproducible build</a> feature.
   *
   * Please note that in previous versions
   * (2.2.2 - 7.0.1) the default format was set to {@code yyyy-MM-dd'T'HH:mm:ssZ}
   * which produces a {@code RFC 822 time zone}. While such format gives reliable
   * options in parsing the date, it does not comply with the requirements of
   * the reproducible build feature.
   * (2.2.0 - 2.2.2) the default dateFormat was set to: {@code
   * dd.MM.yyyy '@' HH:mm:ss z}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <dateFormat>yyyy-MM-dd'T'HH:mm:ssXXX</dateFormat>
   * }</pre>
   *
   * @since 2.2.0
   */
  @Parameter(defaultValue = "yyyy-MM-dd'T'HH:mm:ssXXX")
  String dateFormat;

  /**
   * The timezone used in the {@link #dateFormat} of dates exported by this plugin (e.g. {@code
   * git.commit.time}, {@code git.build.time}). It should be a valid Timezone string such as {@code
   * 'America/Los_Angeles'}, {@code 'GMT+10'} or {@code 'PST'}.
   *
   * <p>As a general warning try to avoid three-letter time zone IDs because the same abbreviation
   * are often used for multiple time zones. Please review <a
   * href="https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html">
   * https://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html</a> for more information on
   * this issue.
   *
   * <p>The default value we'll use the timezone use the timezone that's shipped with java ({@code
   * java.util.TimeZone.getDefault().getID()}). <b>Note</b>: If you plan to set the java's timezone
   * by using {@code MAVEN_OPTS=-Duser.timezone=UTC mvn clean package}, {@code mvn clean package
   * -Duser.timezone=UTC}, or any other configuration keep in mind that this option will override
   * those settings and will not take other configurations into account!
   *
   * <p>Example:
   *
   * <pre>{@code
   * <dateFormatTimeZone>${user.timezone}</dateFormatTimeZone>
   * }</pre>
   *
   * @since 2.2.0
   */
  @Parameter String dateFormatTimeZone;

  /**
   * Specify whether the plugin should fail when a {@code '.git'} directory cannot be found. When
   * set to {@code false} and no {@code .git} directory is found the plugin will skip execution.
   *
   * <p>Defaults to {@code true}, so a missing {@code '.git'} directory is treated as error and
   * should cause a failure in your build.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <failOnNoGitDirectory>true</failOnNoGitDirectory>
   * }</pre>
   *
   * @since 2.0.4
   */
  @Parameter(defaultValue = "true")
  boolean failOnNoGitDirectory;

  /**
   * Set this to {@code false} to continue the build even if unable to get enough data for a
   * complete run. This may be useful during CI builds if the CI server does weird things to the
   * repository.
   *
   * <p>Setting this value to {@code false} causes the plugin to gracefully tell you "I did my best"
   * and abort its execution if unable to obtain git meta data - yet the build will continue to run
   * without failing.
   *
   * <p>By default the plugin will fail the build (set to {@code true}) if unable to obtain enough
   * data for a complete run.
   *
   * <p>See <a href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/63">issue
   * #63</a> for a rationale behind this flag.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <failOnUnableToExtractRepoInfo>true</failOnUnableToExtractRepoInfo>
   * }</pre>
   *
   * @since 2.1.5
   */
  @Parameter(defaultValue = "true")
  boolean failOnUnableToExtractRepoInfo;

  /**
   * This plugin ships with custom {@code jgit} implementation that is being used to obtain all
   * relevant information. If set to {@code true} the plugin will use native git executable instead
   * of the custom {@code jgit} implementation to fetch information about the repository. Of course
   * if set to {@code true} will require a git executable to be installed in system.
   *
   * <p>Although setting this to {@code true} (use the native git executable) should usually give
   * your build some performance boost, it may randomly break if you upgrade your git version and it
   * decides to print information in a different format suddenly.
   *
   * <p>By default the plugin will use {@code jgit} implementation as a source of information about
   * the repository. As rule of thumb, keep using the default {@code jgit} implementation (set to
   * {@code false}) until you notice performance problems within your build (usually when you have
   * *hundreds* of maven modules).
   *
   * <p>With plugin version *3.0.2* you can also control it using the commandline option {@code
   * -Dmaven.gitcommitid.nativegit=true}. See {@link #useNativeGitViaCommandLine}
   *
   * <p>Example:
   *
   * <pre>{@code
   * <useNativeGit>true</useNativeGit>
   * }</pre>
   *
   * @since 2.1.9
   */
  @Parameter(defaultValue = "false")
  boolean useNativeGit;

  /**
   * Option to be used in command-line to override the value of {@link #useNativeGit} specified in
   * the pom.xml, or its default value if it's not set explicitly.
   *
   * <p>NOTE / WARNING: Do *NOT* set this property inside the configuration of your plugin. Please
   * read <a href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/315">issue
   * 315</a> to find out why.
   *
   * <p>Example:
   *
   * <pre>{@code
   * mvn clean package -Dmaven.gitcommitid.nativegit=true
   * }</pre>
   *
   * @since 3.0.2
   */
  @Parameter(property = "maven.gitcommitid.nativegit", defaultValue = "false")
  boolean useNativeGitViaCommandLine;

  /**
   * When set to {@code true} the plugin execution will completely skip. This is useful for e.g.
   * profile activated plugin invocations or to use properties to enable / disable pom features.
   *
   * <p>By default the execution is not skipped (set to {@code false})
   *
   * <p>With version *2.2.3* you can also skip the plugin by using the commandline option {@code
   * -Dmaven.gitcommitid.skip=true}. See {@link #skipViaCommandLine}
   *
   * <p>Example:
   *
   * <pre>{@code
   * <skip>false</skip>
   * }</pre>
   *
   * @since 2.1.8
   */
  @Parameter(defaultValue = "false")
  boolean skip;

  /**
   * Option to be used in command-line to override the value of {@link #skip} specified in the
   * pom.xml, or its default value if it's not set explicitly. Set this to {@code true} to skip
   * plugin execution via commandline.
   *
   * <p>NOTE / WARNING: Do *NOT* set this property inside the configuration of your plugin. Please
   * read <a href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/315">issue
   * 315</a> to find out why.
   *
   * <p>Example:
   *
   * <pre>{@code
   * mvn clean package -Dmaven.gitcommitid.skip=true
   * }</pre>
   *
   * @since 2.2.4
   */
  @Parameter(property = "maven.gitcommitid.skip", defaultValue = "false")
  private boolean skipViaCommandLine;

  /**
   * Use with caution!
   *
   * <p>Set this to {@code true} to only run once in a multi-module build. This means that the
   * plugins effects will only execute once for the first project in the execution graph. If {@code
   * skipPoms} is set to {@code true} (default) the plugin will run for the first non pom project in
   * the execution graph (as listed in the reactor build order). This probably won't "do the right
   * thing" if your project has more than one git repository.
   *
   * <p>Defaults to {@code false}, so the plugin may get executed multiple times in a reactor build!
   *
   * <p>Important: If you're using {@link #generateGitPropertiesFile}, setting {@code runOnlyOnce}
   * will make the plugin only generate the file in the project build directory which is the first
   * one based on the execution graph (!).
   *
   * <p>Important: Please note that the git-commit-id-maven-plugin also has an option to skip pom
   * project ({@code <packaging>pom</packaging>}). If you plan to use the {@code runOnlyOnce} option
   * alongside with an aggregator pom you may want to set {@code <skipPoms>false</skipPoms>}. Refer
   * to {@link #skipPoms} for more information
   *
   * <p>For multi-module build you might also want to set {@link #injectAllReactorProjects} to make
   * the {@code git.*} maven properties available in all modules.
   *
   * <p>Note: Prior to version 4.0.0 the plugin was simply using the execute once applied for the
   * parent project (which might have skipped execution if the parent project was a pom project).
   *
   * <p>Example:
   *
   * <pre>{@code
   * <runOnlyOnce>true</runOnlyOnce>
   * }</pre>
   *
   * @since 2.1.12
   */
  @Parameter(defaultValue = "false")
  boolean runOnlyOnce;

  /**
   * Can be used to exclude certain properties from being emitted (e.g. filter out properties that
   * you *don't* want to expose). May be useful when you want to hide {@code git.build.user.email}
   * (maybe because you don't want to expose your eMail?), or the email of the committer?
   *
   * <p>Each value may be globbing, that is, you can write {@code git.commit.user.*} to exclude both
   * the {@code name}, as well as {@code email} properties from being emitted.
   *
   * <p>Please note that the strings here are Java regexes ({@code .*} is globbing, not plain {@code
   * *}). If you have a very long list of exclusions you may want to use {@link
   * #includeOnlyProperties}.
   *
   * <p>This feature was implemented in response to <a
   * href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/91">issue 91</a>, so
   * if you're curious about the use-case, check that issue.
   *
   * <p>Prior to version 3.0.0 the plugin used the 'naive' approach to ask for all properties and
   * then apply filtering. However, with the growing numbers of properties each property eat more
   * and more of execution time that will be filtered out afterwards. With 3.0.0 this behaviour was
   * readjusted to a 'selective running' approach whereby the plugin will not even try to get the
   * property when excluded. Such behaviour can result in an overall reduced execution time of the
   * plugin (see <a
   * href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/408">issue 408</a> for
   * details).
   *
   * <p>Defaults to the empty list (= no properties are excluded).
   *
   * <p>Example:
   *
   * <pre>{@code
   * <excludeProperties>
   *      <excludeProperty>git.user.*</excludeProperty>
   * </excludeProperties>
   * }</pre>
   *
   * @since 2.1.9
   */
  @Parameter List<String> excludeProperties;

  /**
   * Can be used to include only certain properties into the emission (e.g. include only properties
   * that you <b>want</b> to expose). This feature was implemented to avoid big exclude properties
   * tag when we only want very few specific properties.
   *
   * <p>The inclusion rules, will be overruled by the {@link #excludeProperties} rules (e.g. you can
   * write an inclusion rule that applies for multiple properties and then exclude a subset of
   * them). You can therefor can be a bit broader in the inclusion rules and exclude more sensitive
   * ones in the {@link #excludeProperties} rules.
   *
   * <p>Each value may be globbing, that is, you can write {@code git.commit.user.*} to exclude both
   * the {@code name}, as well as {@code email} properties from being emitted.
   *
   * <p>Please note that the strings here are Java regexes ({@code .*} is globbing, not plain {@code
   * *}). If you have a short list of exclusions you may want to use {@link #excludeProperties}.
   *
   * <p>Prior to version 3.0.0 the plugin used the 'naive' approach to ask for all properties and
   * then apply filtering. However, with the growing numbers of properties each property eat more
   * and more of execution time that will be filtered out afterwards. With 3.0.0 this behaviour was
   * readjusted to a 'selective running' approach whereby the plugin will not even try to get the
   * property when excluded. Such behaviour can result in an overall reduced execution time of the
   * plugin (see <a
   * href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/408">issue 408</a> for
   * details).
   *
   * <p>Defaults to the empty list (= no properties are excluded).
   *
   * <p>Example:
   *
   * <pre>{@code
   * <includeOnlyProperties>
   *     <includeOnlyProperty>^git.commit.id.full$</includeOnlyProperty>
   * </includeOnlyProperties>
   * }</pre>
   *
   * @since 2.1.14
   */
  @Parameter List<String> includeOnlyProperties;

  /**
   * The option can be used to tell the plugin how it should generate the {@code 'git.commit.id'}
   * property. Due to some naming issues when exporting the properties as an json-object (<a
   * href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/122">issue 122</a>) we
   * needed to make it possible to export all properties as a valid json-object.
   *
   * <p>Due to the fact that this is one of the major properties the plugin is exporting we just
   * don't want to change the exporting mechanism and somehow throw the backwards compatibility
   * away. We rather provide a convenient switch where you can choose if you would like the
   * properties as they always had been, or if you rather need to support full json-object
   * compatibility.
   *
   * <p>In the case you need to fully support json-object we unfortunately need to change the {@code
   * 'git.commit.id'} property from {@code 'git.commit.id'} to {@code 'git.commit.id.full'} in the
   * exporting mechanism to allow the generation of a fully valid json object.
   *
   * <p>Currently, the switch allows two different options:
   *
   * <ol>
   *   <li>By default this property is set to {@code 'flat'} and will generate the formerly known
   *       property {@code 'git.commit.id'} as it was in the previous versions of the plugin.
   *       Keeping it to {@code 'flat'} by default preserve backwards compatibility and does not
   *       require further adjustments by the end user.
   *   <li>If you set this switch to {@code 'full'} the plugin will export the formerly known
   *       property {@code 'git.commit.id'} as {@code 'git.commit.id.full'} and therefore will
   *       generate a fully valid json object in the exporting mechanism.
   * </ol>
   *
   * <p><b>Note:</b> If you set the value to something that's not equal to {@code 'flat'} or {@code
   * 'full'} (ignoring the case) the plugin will output a warning and will fallback to the default
   * {@code 'flat'} mode.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <commitIdGenerationMode>flat</commitIdGenerationMode>
   * }</pre>
   *
   * @since 2.2.0
   */
  @Parameter(defaultValue = "flat")
  String commitIdGenerationMode;

  /**
   * Not settable by any configuration in the {@code pom.xml}. For internal use only (represents the
   * {@link #commitIdGenerationMode} the user has set as enum.
   */
  private CommitIdGenerationMode commitIdGenerationModeEnum;

  /**
   * Can be used to replace certain characters or strings using regular expressions within the
   * exposed properties. Replacements can be performed using regular expressions and on a
   * configuration level it can be defined whether the replacement should affect all properties or
   * just a single one.
   *
   * <p>Please note that the replacement will only be applied to properties that are being generated
   * by the plugin. If you want to replace properties that are being generated by other plugins you
   * may want to use the maven-replacer-plugin or any other alternative.
   *
   * <p>Replacements can be configured with a {@code replacementProperty}. A {@code
   * replacementProperty} can have a {@code property}` and a {@code regex}-tag. If the {@code
   * replacementProperty} configuration has a {@code property}-tag the replacement will only be
   * performed on that specific property (e.g. {@code <property>git.branch</property>} will only be
   * performed on {@code git.branch}).
   *
   * <p>In case this specific element is not defined or left empty the replacement will be performed
   * <b>on all generated properties</b>.
   *
   * <p>The optional {@code regex}-tag can either be {@code true} to perform a replacement with
   * regular expressions or {@code false} to perform a replacement with java's
   * string.replace-function.
   *
   * <p>By default the replacement will be performed with regular expressions ({@code true}).
   * Furthermore each {@code replacementProperty} need to be configured with a {@code token} and a
   * {@code value}. The {@code token} can be seen as the needle and the {@code value} as the text to
   * be written over any found tokens. If using regular expressions the value can reference grouped
   * regex matches by using $1, $2, etc.
   *
   * <p>Since 2.2.4 the plugin allows to define a even more sophisticated ruleset and allows to set
   * an {@code propertyOutputSuffix} within each {@code replacementProperty}. If this option is
   * empty the original property will be overwritten (default behaviour in 2.2.3). However when this
   * configuration is set to {@code something} and a user wants to modify the {@code git.branch}
   * property the plugin will keep {@code git.branch} as the original one (w/o modifications) but
   * also will be creating a new {@code git.branch.something} property with the requested
   * replacement.
   *
   * <p>Furthermore with 2.2.4 the plugin allows to perform certain types of string manipulation
   * either before or after the evaluation of the replacement. With this feature a user can
   * currently easily manipulate the case (e.g. lower case VS upper case) of the input/output
   * property. This behaviour can be achieved by defining a list of {@code transformationRules} for
   * the property where those rules should take effect. Each {@code transformationRule} consist of
   * two required fields {@code apply} and {@code action}. The {@code apply}-tag controls when the
   * rule should be applied and can be set to {@code BEFORE} to have the rule being applied before
   * or it can be set to {@code AFTER} to have the rule being applied after the replacement. The
   * {@code action}-tag determines the string conversion rule that should be applied. Currently
   * supported is {@code LOWER_CASE} and {@code UPPER_CASE}. Potential candidates in the feature are
   * {@code CAPITALIZATION} and {@code INVERT_CASE} (open a ticket if you need them...).
   *
   * <p>Since 4.0.1 the plugin allows to define a {@code forceValueEvaluation}-switch which forces
   * the plugin to evaluate the given value on <b>every</b> project.
   *
   * <p>This might come handy if <b>every</b> project needs a unique value and a user wants to
   * project specific variables like {@code ${project.artifactId}}. Be advised that this essentially
   * means that the plugin <b>must</b> run for every child-project of a reactor build and thus might
   * cause some overhead (the git properties should be cached). For a use-case refer to <a
   * href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/457">issue 457</a>
   *
   * <p>Defaults to the empty list / not set (= no properties are being replaced by default)
   *
   * <p>Example:
   *
   * <pre>{@code
   * <replacementProperties>
   *     <!--
   *         example:
   *         apply replacement only to the specific property git.branch and replace '/' with '-'
   *         see also https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/138
   *     -->
   *     <replacementProperty>
   *         <property>git.branch</property>
   *         <propertyOutputSuffix>something</propertyOutputSuffix>
   *         <token>^([^\/]*)\/([^\/]*)$</token>
   *         <value>$1-$2</value>
   *         <regex>true</regex>
   *         <forceValueEvaluation>false</forceValueEvaluation>
   *         <transformationRules>
   *             <transformationRule>
   *                 <apply>BEFORE</apply>
   *                 <action>UPPER_CASE</action>
   *             </transformationRule>
   *             <transformationRule>
   *                 <apply>AFTER</apply>
   *                 <action>LOWER_CASE</action>
   *             </transformationRule>
   *         </transformationRules>
   *     </replacementProperty>
   * </replacementProperties>
   * }</pre>
   *
   * @since 2.2.3
   */
  @Parameter List<ReplacementProperty> replacementProperties;

  /**
   * Allow to tell the plugin what commit should be used as reference to generate the properties
   * from.
   *
   * <p>In general this property can be set to something generic like {@code HEAD^1} or point to a
   * branch or tag-name. To support any kind or use-case this configuration can also be set to an
   * entire commit-hash or it's abbreviated version.
   *
   * <p>A use-case for this feature can be found in <a
   * href="https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/338">here</a>.
   *
   * <p>Please note that for security purposes not all references might be allowed as configuration.
   * If you have a specific use-case that is currently not white listed feel free to file an issue.
   *
   * <p>By default this property is simply set to {@code HEAD} which should reference to the latest
   * commit in your repository.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <evaluateOnCommit>HEAD</evaluateOnCommit>
   * }</pre>
   *
   * @since 2.2.4
   */
  @Parameter(defaultValue = "HEAD")
  String evaluateOnCommit;

  /**
   * Allow to specify a timeout (in milliseconds) for fetching information with the native Git
   * executable. This option might come in handy in cases where fetching information about the
   * repository with the native Git executable does not terminate.
   *
   * <p>Note: This option will only be taken into consideration when using the native git executable
   * ({@link #useNativeGit} is set to {@code true}).
   *
   * <p>By default this timeout is set to 30000 (30 seconds).
   *
   * <p>Example:
   *
   * <pre>{@code
   * <nativeGitTimeoutInMs>30000</nativeGitTimeoutInMs>
   * }</pre>
   *
   * @since 3.0.0
   */
  @Parameter(defaultValue = "30000")
  long nativeGitTimeoutInMs;

  /**
   * When set to {@code true} this plugin will try to use the branch name from build environment.
   * Set to {@code false} to use JGit/GIT to get current branch name which can be useful when using
   * the JGitflow maven plugin. See
   * https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/24#issuecomment-203285398
   *
   * <p>Note: If not using "Check out to specific local branch' and setting this to false may result
   * in getting detached head state and therefore a commit id as branch name.
   *
   * <p>By default this is set to {@code true}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <useBranchNameFromBuildEnvironment>true</useBranchNameFromBuildEnvironment>
   * }</pre>
   *
   * @since 3.0.0
   */
  @Parameter(defaultValue = "true")
  boolean useBranchNameFromBuildEnvironment;

  /**
   * Controls if this plugin should expose the generated properties into {@code System.properties}
   * When set to {@code true} this plugin will try to expose the generated properties into {@code
   * System.getProperties()}. Set to {@code false} to avoid this exposure.
   *
   * <p>Note that parameters provided via command-line (e.g. {@code -Dgit.commit.id=value}) still
   * have precedence.
   *
   * <p>By default this is set to {@code true}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <injectIntoSysProperties>true</injectIntoSysProperties>
   * }</pre>
   *
   * @since 3.0.0
   */
  @Parameter(defaultValue = "true")
  boolean injectIntoSysProperties;

  /**
   * The plugin can generate certain properties that represents the count of commits that your local
   * branch is ahead or behind in perspective to the remote branch.
   *
   * <p>When your branch is "ahead" it means your local branch has committed changes that are not
   * pushed yet to the remote branch. When your branch is "behind" it means there are commits in the
   * remote branch that are not yet integrated into your local branch.
   *
   * <p>This configuration allows you to control if the plugin should somewhat ensure that such
   * properties are more accurate. More accurate means that the plugin will perform a {@code git
   * fetch} before the properties are calculated. Certainly a {@code git fetch} is an operation that
   * may alter your local git repository and thus the plugin will operate not perform such operation
   * (offline is set to {@code true}). If you however desire more accurate properties you may want
   * to set this to {@code false}.
   *
   * <p>Before version 5.X.X the default was set to {@code false} causing the plugin to operate in
   * online-mode by default. Now the default is set to {@code true} (offline-mode) so the plugin
   * might generate inaccurate {@code git.local.branch.ahead} and {@code git.local.branch.behind}
   * branch information.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <offline>true</offline>
   * }</pre>
   *
   * @since 3.0.1
   */
  @Parameter(defaultValue = "true")
  boolean offline;

  /**
   * Timestamp for reproducible output archive entries
   * (https://maven.apache.org/guides/mini/guide-reproducible-builds.html). The value from <code>
   * ${project.build.outputTimestamp}</code> is either formatted as ISO 8601 <code>
   * yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like <a
   * href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>.
   *
   * @since 4.0.2
   */
  @Parameter(defaultValue = "${project.build.outputTimestamp}")
  private String projectBuildOutputTimestamp;

  // This is now the end of parameters that can be configured in the pom.xml
  // Happy hacking!
  // ===============================================================================================

  /**
   * Injected {@link BuildContext} to recognize incremental builds.
   */
  @Component private BuildContext buildContext;

  /** Charset to read-write project sources. */
  private Charset sourceCharset = StandardCharsets.UTF_8;

  /**
   * This method is used to mock the system environment in testing.
   *
   * @return unmodifiable string map view of the current system environment {@link System#getenv}.
   */
  protected Map<String, String> getCustomSystemEnv() {
    return System.getenv();
  }

  @Override
  public void execute() throws MojoExecutionException {
    LogInterface log =
        new LogInterface() {
          @Override
          public void debug(String msg) {
            if (verbose) {
              getLog().debug(msg);
            }
          }

          @Override
          public void info(String msg) {
            if (verbose) {
              getLog().info(msg);
            }
          }

          @Override
          public void warn(String msg) {
            if (verbose) {
              getLog().warn(msg);
            }
          }

          @Override
          public void error(String msg) {
            // TODO: Should we truly only report errors when verbose = true?
            if (verbose) {
              getLog().error(msg);
            }
          }

          @Override
          public void error(String msg, Throwable t) {
            // TODO: Should we truly only report errors when verbose = true?
            if (verbose) {
              getLog().error(msg, t);
            }
          }
        };

    try {
      // Skip mojo execution on incremental builds.
      if (buildContext != null && buildContext.isIncremental()) {
        // Except if properties file is missing at all
        if (!generateGitPropertiesFile
            || PropertiesFileGenerator.craftPropertiesOutputFile(
                    project.getBasedir(), new File(generateGitPropertiesFilename))
                .exists()) {
          log.info("Skip mojo execution on incremental builds.");
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
        List<MavenProject> sortedProjects =
            Optional.ofNullable(session.getProjectDependencyGraph())
                .map(graph -> graph.getSortedProjects())
                .orElseGet(
                    () -> {
                      log.warn(
                          "Maven's dependency graph is null. Assuming project is the only one"
                              + " executed.");
                      return Collections.singletonList(session.getCurrentProject());
                    });
        MavenProject firstProject =
            sortedProjects.stream()
                // skipPoms == true => find first project that is not pom project
                .filter(
                    p -> {
                      if (skipPoms) {
                        return !isPomProject(p);
                      } else {
                        return true;
                      }
                    })
                .findFirst()
                .orElse(session.getCurrentProject());

        log.info(
            "Current project: '"
                + session.getCurrentProject().getName()
                + "', first project to execute based on dependency graph: '"
                + firstProject.getName()
                + "'");

        if (!session.getCurrentProject().equals(firstProject)) {
          log.info(
              "runOnlyOnce is enabled and this project is not the first project (perhaps skipPoms"
                  + " is configured?), skipping execution!");
          return;
        }
      }

      if (isPomProject(project) && skipPoms) {
        log.info("isPomProject is true and skipPoms is true, return");
        return;
      }

      if (gitDescribe == null) {
        gitDescribe = new GitDescribeConfig();
      }

      try {
        commitIdGenerationModeEnum =
            CommitIdGenerationMode.valueOf(commitIdGenerationMode.toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn(
            "Detected wrong setting for 'commitIdGenerationMode'. Falling back to default 'flat'"
                + " mode!");
        commitIdGenerationModeEnum = CommitIdGenerationMode.FLAT;
      }

      try {
        commitIdPropertiesOutputFormat =
            CommitIdPropertiesOutputFormat.valueOf(format.toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn("Detected wrong setting for 'format'. Falling back to default 'properties' mode!");
        commitIdPropertiesOutputFormat = CommitIdPropertiesOutputFormat.PROPERTIES;
      }

      Properties properties = null;
      // check if properties have already been injected
      Properties contextProperties = getContextProperties(project);
      boolean alreadyInjected = injectAllReactorProjects && contextProperties != null;
      if (alreadyInjected) {
        log.info(
            "injectAllReactorProjects is enabled - attempting to use the already computed values");
        // makes sure the existing context properties are not mutated
        properties = new Properties();
        properties.putAll(contextProperties);
      }

      final GitCommitIdPlugin.Callback cb =
          new GitCommitIdPlugin.Callback() {
            @Override
            public Map<String, String> getSystemEnv() {
              return getCustomSystemEnv();
            }

            @Override
            public Supplier<String> supplyProjectVersion() {
              return () -> project.getVersion();
            }

            @Nonnull
            @Override
            public LogInterface getLogInterface() {
              return log;
            }

            @Nonnull
            @Override
            public String getDateFormat() {
              return dateFormat;
            }

            @Nonnull
            @Override
            public String getDateFormatTimeZone() {
              return dateFormatTimeZone;
            }

            @Nonnull
            @Override
            public String getPrefixDot() {
              String trimmedPrefix = prefix.trim();
              return trimmedPrefix.equals("") ? "" : trimmedPrefix + ".";
            }

            @Override
            public List<String> getExcludeProperties() {
              return excludeProperties;
            }

            @Override
            public List<String> getIncludeOnlyProperties() {
              return includeOnlyProperties;
            }

            @Nullable
            @Override
            public Date getReproducibleBuildOutputTimestamp() throws GitCommitIdExecutionException {
              return parseOutputTimestamp(projectBuildOutputTimestamp);
            }

            @Override
            public boolean useNativeGit() {
              return useNativeGit || useNativeGitViaCommandLine;
            }

            @Override
            public long getNativeGitTimeoutInMs() {
              return nativeGitTimeoutInMs;
            }

            @Override
            public int getAbbrevLength() {
              return abbrevLength;
            }

            @Override
            public GitDescribeConfig getGitDescribe() {
              return gitDescribe;
            }

            @Override
            public CommitIdGenerationMode getCommitIdGenerationMode() {
              return commitIdGenerationModeEnum;
            }

            @Override
            public boolean getUseBranchNameFromBuildEnvironment() {
              return useBranchNameFromBuildEnvironment;
            }

            @Override
            public boolean isOffline() {
              return offline || settings.isOffline();
            }

            @Override
            public String getEvaluateOnCommit() {
              return evaluateOnCommit;
            }

            @Override
            public File getDotGitDirectory() {
              return dotGitDirectory;
            }

            @Override
            public boolean shouldGenerateGitPropertiesFile() {
              return generateGitPropertiesFile;
            }

            @Override
            public void performPublishToAllSystemEnvironments(Properties properties) {
              publishToAllSystemEnvironments(getLogInterface(), properties, contextProperties);
            }

            @Override
            public void performPropertiesReplacement(Properties properties) {
              PropertiesReplacer propertiesReplacer =
                  new PropertiesReplacer(
                      log, new PluginParameterExpressionEvaluator(session, mojoExecution));
              propertiesReplacer.performReplacement(properties, replacementProperties);

              logProperties(getLogInterface(), properties);
            }

            @Override
            public CommitIdPropertiesOutputFormat getPropertiesOutputFormat() {
              return commitIdPropertiesOutputFormat;
            }

            @Override
            public BuildFileChangeListener getBuildFileChangeListener() {
              return file -> {
                // this should only be null in our tests
                if (buildContext != null) {
                  buildContext.refresh(file);
                }
              };
            }

            @Override
            public String getProjectName() {
              return project.getName();
            }

            @Override
            public File getProjectBaseDir() {
              return project.getBasedir();
            }

            @Override
            public File getGenerateGitPropertiesFile() {
              return new File(generateGitPropertiesFilename);
            }

            @Override
            public Charset getPropertiesSourceCharset() {
              return sourceCharset;
            }

            @Override
            public boolean shouldPropertiesEscapeUnicode() {
              return generateGitPropertiesFileWithEscapedUnicode;
            }

            @Override
            public boolean shouldFailOnNoGitDirectory() {
              return failOnNoGitDirectory;
            }
          };

      GitCommitIdPlugin.runPlugin(cb, properties);
    } catch (GitCommitIdExecutionException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void publishToAllSystemEnvironments(
      @Nonnull LogInterface log,
      @Nonnull Properties propertiesToPublish,
      @Nullable Properties contextProperties) {
    publishPropertiesInto(propertiesToPublish, project.getProperties());
    // some plugins rely on the user properties (e.g. flatten-maven-plugin)
    publishPropertiesInto(propertiesToPublish, session.getUserProperties());

    if (injectAllReactorProjects) {
      Properties diffPropertiesToPublish = new Properties();
      propertiesToPublish.forEach((k, v) -> {
        if (contextProperties == null || !contextProperties.contains(k)) {
          diffPropertiesToPublish.setProperty(k.toString(), v.toString());
        }
      });
      if (!diffPropertiesToPublish.isEmpty()) {
        appendPropertiesToReactorProjects(log, diffPropertiesToPublish);
      }
    }

    if (injectIntoSysProperties) {
      publishPropertiesInto(propertiesToPublish, System.getProperties());
      publishPropertiesInto(propertiesToPublish, session.getSystemProperties());
      publishPropertiesInto(propertiesToPublish, session.getRequest().getSystemProperties());
    }
  }

  @Nullable
  private Properties getContextProperties(MavenProject project) {
    Object stored = project.getContextValue(CONTEXT_KEY);
    if (stored instanceof Properties) {
      return (Properties) stored;
    }
    return null;
  }

  /**
   * Parse output timestamp configured for Reproducible Builds' archive entries
   * (https://maven.apache.org/guides/mini/guide-reproducible-builds.html). The value from <code>
   * ${project.build.outputTimestamp}</code> is either formatted as ISO 8601 <code>
   * yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like <a
   * href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>.
   *
   * <p>Inspired by
   * https://github.com/apache/maven-archiver/blob/7acb1db4a9754beacde3f21a69e5523ee901abd5/src/main/java/org/apache/maven/archiver/MavenArchiver.java#L755
   *
   * @param outputTimestamp the value of <code>${project.build.outputTimestamp}</code> (may be
   *     <code>null</code>)
   * @return the parsed timestamp, may be <code>null</code> if <code>null</code> input or input
   *     contains only 1 character
   */
  @VisibleForTesting
  protected static Date parseOutputTimestamp(String outputTimestamp) {
    if (outputTimestamp != null
        && !outputTimestamp.trim().isEmpty()
        && outputTimestamp.chars().allMatch(Character::isDigit)) {
      return Date.from(Instant.ofEpochSecond(Long.parseLong(outputTimestamp)));
    }

    if ((outputTimestamp == null) || (outputTimestamp.length() < 2)) {
      // no timestamp configured
      return null;
    }
    return new DateTime(outputTimestamp).toDate();
  }

  private void publishPropertiesInto(Properties propertiesToPublish, Properties propertiesTarget) {
    for (String propertyName : propertiesToPublish.stringPropertyNames()) {
      propertiesTarget.setProperty(propertyName, propertiesToPublish.getProperty(propertyName));
    }
  }

  private void appendPropertiesToReactorProjects(LogInterface log, Properties propertiesToPublish) {
    for (MavenProject mavenProject : reactorProjects) {
      log.debug(
          "Adding '" + propertiesToPublish.size() + "' properties "
          + "to project: '" + mavenProject.getName() + "'");
      if (mavenProject.equals(project)) {
        continue;
      }
      publishPropertiesInto(propertiesToPublish, mavenProject.getProperties());
      mavenProject.setContextValue(CONTEXT_KEY, propertiesToPublish);
    }
    log.info(
        "Added '" + propertiesToPublish.size() + "' properties "
        + "to '" + reactorProjects.size() + "' projects");
  }

  private void logProperties(LogInterface log, Properties propertiesToPublish) {
    for (String propertyName : propertiesToPublish.stringPropertyNames()) {
      log.info("including property '" + propertyName + "' in results");
    }
  }

  private boolean isPomProject(@Nonnull MavenProject project) {
    return project.getPackaging().equalsIgnoreCase("pom");
  }
}
