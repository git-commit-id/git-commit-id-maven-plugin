Overview
====================================================================================================================
This file should give you an overview on how to use the generated properties within your project.

Basic configuration / Basic usage of the plugin
----------------
It's really simple to setup this plugin; below is a sample plugin configuration that you may paste into the `<plugins>` section of your **pom.xml** to get started quickly.
This will get you a properties file `git.properties` with build time, project version and git commit id (both abbreviated and full).

For more in-depth explanation of all options read the next section. 

```xml
            <plugin>
                <groupId>io.github.git-commit-id</groupId>
                <artifactId>git-commit-id-maven-plugin</artifactId>
                <version>5.0.0</version>
                <executions>
                    <execution>
                        <id>get-the-git-infos</id>
                        <goals>
                            <goal>revision</goal>
                        </goals>
                        <phase>initialize</phase>
                    </execution>
                </executions>
                <configuration>
                    <generateGitPropertiesFile>true</generateGitPropertiesFile>
                    <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties</generateGitPropertiesFilename>
                    <includeOnlyProperties>
                        <includeOnlyProperty>^git.build.(time|version)$</includeOnlyProperty>
                        <includeOnlyProperty>^git.commit.id.(abbrev|full)$</includeOnlyProperty>
                    </includeOnlyProperties>
                    <commitIdGenerationMode>full</commitIdGenerationMode>
                </configuration>
            </plugin>
```

Configuration options in-depth / Full usage of the plugin
----------------
It's really simple to setup this plugin; below is a sample pom that you may base your **pom.xml** on. Note that it binds to the initialize phase by default such that all Git properties are available for use throughout the build lifecycle.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>

    <groupId>pl.project13.maven</groupId>
    <artifactId>my-git-plugin-sample-app</artifactId>
    <packaging>war</packaging>
    <version>0.1</version>
    <name>my-git-plugin-sample-app</name>
    <url>http://www.project13.pl</url>

    <dependencies />

    <build>
        <!-- GIT COMMIT ID PLUGIN CONFIGURATION -->

        <!-- SKIP SETTING UP FILTERING LIKE THIS IF YOU USE THE GENERATE FILE MODE :-) -->
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
                <includes>
                    <include>**/*.properties</include>
                    <include>**/*.xml</include>
                </includes>
            </resource>
        </resources>

        <plugins>
            <plugin>
                <groupId>io.github.git-commit-id</groupId>
                <artifactId>git-commit-id-maven-plugin</artifactId>
                <version>5.0.0</version>
                <executions>
                    <execution>
                        <id>get-the-git-infos</id>
                        <goals>
                            <goal>revision</goal>
                        </goals>
                        <!-- *NOTE*: The default phase of revision is initialize, but in case you want to
                                     change it, you can do so by adding the phase here -->
                        <phase>initialize</phase>
                    </execution>
                    <execution>
                        <id>validate-the-git-infos</id>
                        <goals>
                            <goal>validateRevision</goal>
                        </goals>
                        <!-- *NOTE*: The default phase of validateRevision is verify, but in case you want to
                                     change it, you can do so by adding the phase here -->
                        <phase>package</phase>
                    </execution>
                </executions>

                <configuration>
                    <!--
                        Default (optional):
                        ${project.basedir}/.git

                        Explanation:
                        If you'd like to tell the plugin where your .git directory is, use this setting,
                        otherwise we'll perform a search trying to figure out the right directory. 
                        The default value and will most probably be ok for single module projects, in other
                        cases please use `../` to get higher up in the dir tree.
                        An example would be: `${project.basedir}/../.git`
                        It seems reasonable to always add this configuration to have this set explicitly.
                    -->
                    <dotGitDirectory>${project.basedir}/.git</dotGitDirectory>

                    <!--
                        Default (optional):
                        git

                        Explanation:
                        This property will be used as the "namespace" prefix for all exposed/generated properties.
                        An example the plugin may generate the property `${configured-prefix}.commit.id`.
                        Such behaviour can be used to generate properties for multiple git repositories (see
                        https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/137#issuecomment-418144756
                        for a full example).
                    -->
                    <prefix>git</prefix>

                    <!-- @since 2.2.0 -->
                    <!--
                        Default (optional):
                        The current dateFormat is set to match maven's default ``yyyy-MM-dd'T'HH:mm:ssZ``
                        Please note that in previous versions (2.2.0 - 2.2.2) the default dateFormat was set to:
                        ``dd.MM.yyyy '@' HH:mm:ss z``. However the `RFC 822 time zone` seems to give a more
                        reliable option in parsing the date and it's being used in maven as default.

                        Explanation:
                        This property will be used to format the time of any exposed/generated property
                        (e.g. `git.commit.time` and `git.build.time`).
                     -->
                    <dateFormat>yyyy-MM-dd'T'HH:mm:ssZ</dateFormat>

                    <!-- @since 2.2.0 -->
                    <!-- 
                        Default (optional):
                        The default value we'll use the timezone use the timezone that's shipped with java
                        (java.util.TimeZone.getDefault().getID()). 
                        *Note*: If you plan to set the java's timezone by using
                        `MAVEN_OPTS=-Duser.timezone=UTC mvn clean package`, `mvn clean package -Duser.timezone=UTC`
                        or any other configuration keep in mind that this option will override those settings and
                        will not take other configurations into account!

                        Explanation:
                        If you want to set the timezone (e.g. 'America/Los_Angeles', 'GMT+10', 'PST') of the
                        dateformat to anything in particular you can do this by using this option. As a general
                        warning try to avoid three-letter time zone IDs because the same abbreviation are often
                        used for multiple time zones.
                        This property will be used to format the time of any exposed/generated property
                        (e.g. `git.commit.time` and `git.build.time`).
                    -->
                    <dateFormatTimeZone>${user.timezone}</dateFormatTimeZone>

                    <!--
                        Default (optional):
                        false

                        Explanation:
                        If enabled (set to `true`) the plugin prints some more more verbose information during
                        the build (e.g. a summary of all collected properties when it's done).
                    -->
                    <verbose>false</verbose>

                    <!--
                        Default (optional):
                        false

                        Explanation:
                        If you want an easy way to expose your git information into your final artifact (jar,
                        war, ...) you can set this to `true`, which will generate a properties file (with filled
                        out values) that can be configured to end up in the final artifact (see the configuration
                        `generateGitPropertiesFilename` that helps you setup that final path). Such generated
                        property file, can then normally read using `new Properties().load(/**/)` during runtime.

                        Note:
                        When writing the `git.properties` file the value *git.build.time* will only be updated
                        when things in the commit information have changed. If you only change a bit of your code
                        and rebuild/rerun you will see an older timestamp that you may have expected. Essentially
                        the functional meaning becomes **The latest build time when the git information was written
                        to the git.properties file**. The reason why this was done can be found in
                        [issue 151](https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/151).

                        If you need the actual *build time* then simply use the a filtered properties file that
                        contains something like this
                        ```
                        git.build.time=${git.build.time}
                        ```
                    -->
                    <generateGitPropertiesFile>true</generateGitPropertiesFile>

                    <!-- 
                        Default (optional):
                        ${project.build.outputDirectory}/git.properties

                        Explanation:
                        The path can be relative to ${project.basedir} (e.g. target/classes/git.properties) or
                        can be a full path (e.g. ${project.build.outputDirectory}/git.properties).

                        Note:
                        If you plan to set the generateGitPropertiesFilename-Path to a location where usually
                        the source-files comes from (e.g. src/main/resources) and experience that your IDE 
                        (e.g. eclipse) invokes "Maven Project Builder" once every second, the chances that you
                        are using an IDE where the src-folder is a watched folder for files that are *only*
                        edited by humans is pretty high. For further information refer to the manual for your
                        specific IDE and check the workflow of "incremental project builders".
                        In order to fix this problem we recommend to set the generateGitPropertiesFilename-Path
                        to a target folder (e.g. ${project.build.outputDirectory}) since this is
                        the place where all derived/generated resources should go.
                        With version 3.0.0 we introduced a smarter way to counter that issue, but that might not
                        be supported by your IDE.
                        See: https://github.com/git-commit-id/git-commit-id-maven-plugin/pull/385
                    -->
                    <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties</generateGitPropertiesFilename>

                    <!-- @since 6.0.0 -->
                    <!--
                        Default (optional):
                        true

                        Explanation:
                        Controls whether special characters in the properties
                        within the generateGitPropertiesFilename should be unicode escaped.
                        By default properties are escaped (e.g. \\u6E2C\\u8A66\\u4E2D\\u6587).
                        If you write commit messages in chinese and want to extract the mess
                        you may want to set this to 'false'.

                        See https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/590
                    -->
                    <generateGitPropertiesFileWithEscapedUnicode>true</generateGitPropertiesFileWithEscapedUnicode>

                    <!--
                        Default (optional):
                        properties

                        Explanation:
                        Denotes the format to save properties in. Valid options are "properties" (default)
                        and "json". Properties will be saved to the generateGitPropertiesFilename if
                        generateGitPropertiesFile is set to `true`.

                        Note:
                        If you set this to "json", you might also should checkout the documentation about
                        `commitIdGenerationMode` and may want to set
                        `<commitIdGenerationMode>full</commitIdGenerationMode>`.
                    -->
                    <format>properties</format>

                    <!--
                        Default (optional):
                        true

                        Explanation:
                        If set to `true` the plugin will not run in a pom packaged project
                        (e.g. `<packaging>pom</packaging>`). You may want to set this to `false`, if the plugin
                        should also run inside a pom packaged project.
                        Most projects won't need to override this property.

                        For an use-case for this kind of behaviour see:
                        https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/21

                        Note:
                        The plugin might not execute at all, if you also set `<runOnlyOnce>true</runOnlyOnce>`
                    -->
                    <skipPoms>true</skipPoms>

                    <!-- @since 2.1.4 -->
                    <!--
                        Default (optional):
                        false

                        Explanation:
                        Tell maven-git-commit-id to inject the git properties into all reactor projects not just
                        the current one. The property is set to `false` by default to prevent the overriding of
                        properties that may be unrelated to the project. If you need to expose your git properties
                        to another maven module (e.g. maven-antrun-plugin) you need to set it to `true`.
                        However, setting this option can have an impact on your build.
                        For details about why you might want to skip this, read this issue:
                        https://github.com/git-commit-id/git-commit-id-maven-plugin/pull/65
                    -->
                    <injectAllReactorProjects>false</injectAllReactorProjects>

                    <!-- @since 2.0.4 -->
                    <!--
                        Default (optional):
                        true

                        Explanation:
                        Specify whether the plugin should fail when a .git directory cannot be found.
                        When set to `false` and no .git directory is found the plugin will skip execution.
                    -->
                    <failOnNoGitDirectory>true</failOnNoGitDirectory>

                    <!-- @since 2.1.5 -->
                    <!--
                        Default (optional):
                        true

                        Explanation:
                        By default the plugin will fail the build if unable to obtain enough data for a complete
                        run, if you don't care about this, you may want to set this value to false.
                    -->
                    <failOnUnableToExtractRepoInfo>true</failOnUnableToExtractRepoInfo>

                    <!-- @since 2.1.8 -->
                    <!--
                        Default (optional):
                        false

                        Explanation:
                        When set to `true` the plugin execution will completely skip.
                        This is useful for e.g. profile activated plugin invocations or to use properties to
                        enable / disable pom features.
                        With version *2.2.3*  you can also skip the plugin by using the commandline option
                        `-Dmaven.gitcommitid.skip=true`
                    -->
                    <skip>false</skip>

                    <!-- @since 3.0.1 -->
                    <!--
                        Default (optional):
                        true

                        Explanation:
                        When set to `true`, the plugin will not try to contact any remote repositories.
                        Any operations will only use the local state of the repo. If set to `false`, it will
                        execute `git fetch` operations e.g. to determine the `ahead` and `behind` branch
                        information.

                        Warning:
                        Before version 5.X.X the default was set to `false` causing the plugin to operate
                        in online-mode by default. Be advised that in offline-mode the plugin might generate
                        inaccurate `git.local.branch.ahead` and `git.local.branch.behind` branch information.
                    -->
                    <offline>true</offline>

                    <!-- @since 2.1.12 -->
                    <!--
                        Default (optional):
                        false

                        Explanation:
                        Use with caution!

                        In a multi-module build, only run once. This means that the plugins effects will only
                        execute once for the first project in the execution graph. If `skipPoms` is set to
                        true (default) the plugin will run for the first non pom project in the execution graph
                        (as listed in the reactor build order).
                        This probably won't "do the right thing" if your project has more than one git repository.

                        Important: If you're using `generateGitPropertiesFile`, setting `runOnlyOnce` will make
                        the plugin only generate the file in the project build directory which is the first one
                        based on the execution graph (!).

                        Important: Please note that the git-commit-id-maven-plugin also has an option to skip pom
                        project (`<packaging>pom</packaging>`). If you plan to use the `runOnlyOnce` option
                        alongside with an aggregator pom you may want to set `<skipPoms>false</skipPoms>`.

                        For multi-module build you might also want to set `injectAllReactorProjects` to make
                        the `git.*` maven properties available in all modules.

                        Note:
                        Prior to version 4.0.0 the plugin was simply using the execute once applied for the parent
                        project (which might have skipped execution if the parent project was a pom project).
                    -->
                    <runOnlyOnce>false</runOnlyOnce>

                    <!-- @since 2.1.9 -->
                    <!--
                        Default (optional):
                        empty list / not set (meaning no properties are being filtered by default)

                        Explanation:
                        Can be used to exclude certain properties from being emitted (e.g. filter out properties
                        that you *don't* want to expose). May be useful when you want to hide
                        `git.remote.origin.url` (maybe because it contains your repo password?),
                        or the email of the committer etc.

                        Each value may be globbing, that is, you can write `git.commit.user.*` to exclude both
                        the `name`, as well as `email` properties from being emitted into the resulting files.

                        Please note that the strings here are Java regexes (`.*` is globbing, not plain `*`).

                        This feature was implemented in response to [this issue](https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/91),
                        so if you're curious about the use-case, check that issue.

                        Prior to version 3.0.0 the plugin used the 'naive' approach to ask for all properties
                        and then apply filtering. However with the growing numbers of properties each property
                        eat more and more of execution time that will be filtered out afterwards.
                        With 3.0.0 this behaviour was readjusted to a 'selective running' approach whereby the
                        plugin will not even try to get the property when excluded. Such behaviour can result in
                        an overall reduced execution time of the plugin
                        (see https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/408 for details).
                    -->
                    <excludeProperties>
                      <!-- <excludeProperty>git.user.*</excludeProperty> -->
                    </excludeProperties>

                    <!-- @since 2.1.14 -->
                    <!--
                        Default (optional):
                        empty list / not set (meaning no properties are being filtered by default)

                        Explanation:
                        Can be used to include only certain properties into the resulting file (e.g. include only
                        properties that you *want* to expose). This feature was implemented to avoid big exclude
                        properties tag when we only want very few specific properties.
                        The inclusion rules, will be overruled by the exclude rules (e.g. you can write an
                        inclusion rule that applies for multiple properties and then exclude a subset of them).

                        Each value may be globbing, that is, you can write `git.commit.user.*` to include
                        both the `name`, as well as `email` properties into the resulting files.

                        Please note that the strings here are Java regexes (`.*` is globbing, not plain `*`).

                        Prior to version 3.0.0 the plugin used the 'naive' approach to ask for all properties
                        and then apply filtering. However with the growing numbers of properties each property
                        eat more and more of execution time that will be filtered out afterwards.
                        With 3.0.0 this behaviour was readjusted to a 'selective running' approach whereby the
                        plugin will not even try to get the property when included. Such behaviour can result in
                        an overall reduced execution time of the plugin
                        (see https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/408 for details).
                    -->
                    <includeOnlyProperties>
                      <!-- <includeOnlyProperty>^git.commit.id.full$</includeOnlyProperty> -->
                    </includeOnlyProperties>

                    <!-- @since 2.2.3 -->
                    <!--
                        Default (optional):
                        empty list / not set (meaning no properties are being replaced by default)

                        Explanation:
                        Can be used to replace certain characters or strings using regular expressions within the
                        exposed properties. Sample use case (see below): replace the '/' with '-' in the branch
                        name when using branches like 'feature/feature_name'. 

                        Replacements can be configured with a replacementProperty. A replacementProperty can
                        have a `property` and a `regex`-tag. If the replacementProperty configuration has a
                        `property`-tag the replacement will only be performed on that specific property
                        (e.g. `<property>git.branch</property>` will only be performed on `git.branch`).
                        In case this specific element is not defined or left empty the replacement will be
                        performed *on all generated properties*.
                        The optional `regex`-tag can either be `true` to perform a replacement with regular
                        expressions or `false` to perform a replacement with java's string.replace-function.
                        By default the replacement will be performed with regular expressions (`true`).
                        Furthermore each replacementProperty need to be configured with a token and a value.
                        The token can be seen as the needle and the value as the text to be written over any
                        found tokens. If using regular expressions the value can reference grouped regex matches
                        by using $1, $2, etc.

                        Since 2.2.4 the plugin allows to define a even more sophisticated ruleset and allows to
                        set an `propertyOutputSuffix` within each replacement property. If this option is empty
                        the original property will be overwritten (default behaviour in 2.2.3). However when this
                        configuration is set to `something` and a user wants to modify the `git.branch` property
                        the plugin will keep `git.branch` as the original one (w/o modifications) but also will
                        be creating a new `git.branch.something` property with the requested replacement.
                        Furthermore with 2.2.4 the plugin allows to perform certain types of string manipulation
                        either before or after the evaluation of the replacement. With this feature a user can
                        currently easily manipulate the case (e.g. lower case VS upper case) of the input/output
                        property. This behaviour can be achieved by defining a list of `transformationRules` for
                        the property where those rules should take effect. Each `transformationRule` consist of
                        two required fields `apply` and `action`. The `apply`-tag controls when the rule should
                        be applied and can be set to `BEFORE` to have the rule being applied before or it can be
                        set to `AFTER` to have the rule being applied after the replacement. The `action`-tag
                        determines the string conversion rule that should be applied. Currently supported is
                        `LOWER_CASE` and `UPPER_CASE`. Potential candidates in the feature are `CAPITALIZATION`
                        and `INVERT_CASE` (open a ticket if you need them...).

                        Please note that the replacement will *only be applied to properties that are being
                        generated by the plugin*. If you want to replace properties that are being generated by
                        other plugins you may want to use the maven-replacer-plugin or any other alternative.

                        Since 4.0.1 the plugin allows to define a `forceValueEvaluation`-switch which forces the
                        plugin to evaluate the given value on *every* project.
                        This might come handy if *every* project needs a unique value and a user wants to
                        project specific variables like `${project.artifactId}`.
                        Be adviced that this essentially means that the plugin *must* run for every child-project of a
                        reactor build and thus might cause some overhead (the git properties should be cached).
                        For a use-case refer to https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/457.
                    -->
                    <replacementProperties>
                      <!--
                          example:
                          apply replacement only to the specific property git.branch and replace '/' with '-'
                          see also [issue 138](https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/138)
                      <replacementProperty>
                        <property>git.branch</property>
                        <propertyOutputSuffix>something</propertyOutputSuffix>
                        <token>^([^\/]*)\/([^\/]*)$</token>
                        <value>$1-$2</value>
                        <regex>true</regex>
                        <forceValueEvaluation>false</forceValueEvaluation>
                        <transformationRules>
                          <transformationRule>
                            <apply>BEFORE</apply>
                            <action>UPPER_CASE</action>
                          </transformationRule>
                          <transformationRule>
                            <apply>AFTER</apply>
                            <action>LOWER_CASE</action>
                          </transformationRule>
                        </transformationRules>
                      </replacementProperty>
                      -->
                    </replacementProperties>

                    <!-- @since 2.1.10 -->
                    <!--
                        Default (optional):
                        false

                        Explanation:
                        This plugin ships with custom `jgit` implementation that is being used to obtain all
                        relevant information. If set to to `true` this plugin will use the native `git` binary
                        instead of the custom `jgit` implementation.

                        Although this should usually give your build some performance boost, it may randomly
                        break if you upgrade your git version and it decides to print information in a different
                        format suddenly. As rule of thumb, keep using the default `jgit` implementation (keep
                        this `false`) until you notice performance problems within your build (usually when you
                        have *hundreds* of maven modules).

                        With version *3.0.2*  you can also control it using the commandline option
                        `-Dmaven.gitcommitid.nativegit=true`
                    -->
                    <useNativeGit>false</useNativeGit>

                    <!-- @since 3.0.0 -->
                    <!--
                        Default (optional):
                        By default this timeout is set to 30000 (30 seconds) and can be altered based on
                        individual use cases.

                        Explanation:
                        Allow to specify a timeout (in milliseconds) for fetching information with the native
                        Git executable. This option might come in handy in cases where fetching information
                        about the repository with the native Git executable does not terminate (see
                        https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/336 for an example)

                        *Note*: This option will only be taken into consideration when using the native git
                        executable (`useNativeGit` is set to `true`).
                    -->
                    <nativeGitTimeoutInMs>30000</nativeGitTimeoutInMs>

                    <!-- @since v2.0.4 -->
                    <!--
                        Default (optional):
                        Defaults to `7`.

                        Explanation:
                        Configure the the length of the abbreviated git commit id (`git.commit.id.abbrev`) to
                        be at least of length N. `0` carries the special meaning (checkout the
                        [git describe documentation](docs/git-describe.md) for the special case abbrev = 0).
                        Maximum value is `40`, because of max SHA-1 length.
                     -->
                    <abbrevLength>7</abbrevLength>

                    <!-- @since v2.2.0 -->
                    <!--
                        Default (optional):
                        flat

                        Explanation:
                        The option can be used to tell the plugin how it should generate the 'git.commit.id'
                        property. Due to some naming issues when exporting the properties as an json-object
                        (https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/122) we needed to
                        make it possible to export all properties as a valid json-object.
                        Due to the fact that this is one of the major properties the plugin is exporting we
                        just don't want to change the exporting mechanism and somehow throw the backwards
                        compatibility away. We rather provide a convenient switch where you can choose if you
                        would like the properties as they always had been, or if you rather need to support
                        full json-object compatibility.
                        In the case you need to fully support json-object we unfortunately need to change the
                        'git.commit.id' property from 'git.commit.id' to 'git.commit.id.full' in the exporting
                        mechanism to allow the generation of a fully valid json object.

                        Currently the switch allows two different options:
                        1. By default this property is set to `flat` and will generate the formerly known
                           property `git.commit.id` as it was in the previous versions of the plugin. Keeping
                           it to `flat` by default preserve backwards compatibility and does not require further
                           adjustments by the end user.
                        2. If you set this switch to `full` the plugin will export the formerly known property
                           `git.commit.id` as `git.commit.id.full` and therefore will generate a fully valid
                           json object in the exporting mechanism.

                        Note:
                        If you set the value to something that's not equal to `flat` or `full` (ignoring the
                        case) the plugin will output a warning and will fallback to the default `flat` mode.
                    -->
                    <commitIdGenerationMode>flat</commitIdGenerationMode>

                    <!-- @since 2.1.0 -->
                    <!--
                        The following `gitDescribe` configuration below is optional and can be leveraged as a
                        really powerful versioning helper. If you are not familiar with
                        [git-describe](https://github.com/git-commit-id/git-commit-id-maven-plugin#git-describe-&#45;-short-intro-to-an-awesome-command)
                        it is highly recommended to go through this part of the documentation. More advanced
                        users can most likely skip the explanations in this section, as it just explains the
                        same options that git provides.
                        As a side note this plugin tries to be 1-to-1 compatible with git's plain output, even
                        though the describe functionality has been reimplemented manually using JGit (you don't
                        have to have a git executable to use the plugin).
                        See also https://git-scm.com/docs/git-describe
                    -->
                    <gitDescribe>
                        
                        <!--
                            Default (optional):
                            false

                            Explanation:
                            When you don't want to use `git-describe` information in your build, you can set this
                            to `true` to avoid to calculate it.
                        -->
                        <skip>false</skip>
                        
                        <!--
                            Default (optional):
                            true

                            Explanation:
                            In some cases no tag can be found `near` this commit (e.g. usually when performing a
                            shallow clone). If this is set to `true` and no tag was found, this property will
                            fallback to the commit's id instead (when `true` this property will not become empty). 
                            Set this to `true` when you *always* want to return something meaningful in the
                            describe property.
                        -->
                        <always>true</always>

                        <!--
                            Default (optional):
                            7

                            Explanation:
                            In the describe output, the object id of the hash is always abbreviated to N letters
                            (by default 7).
                            The typical describe output you'll see therefore is: `v2.1.0-1-gf5cd254`, where `-1-`
                            means the number of commits away from the mentioned tag and the `-gf5cd254` part means
                            the first 7 chars of the current commit's id `f5cd254`.
                            Setting *abbrev* to `0` has the effect of hiding the "distance from tag" and
                            "object id" parts of the output, so you end up with just the "nearest tag" (that is,
                            instead `tag-12-gaaaaaaa` with `abbrev = 0` you'd get `tag`).

                            **Please note that the `g` prefix is included to notify you that it's a commit id,
                            it is NOT part of the commit's object id** - *this is default git behaviour, so we're
                            doing the same*.
                            You can set this to any value between 0 and 40 (inclusive). `0` carries the special
                            meaning (checkout the [git describe documentation](docs/git-describe.md) for the
                            special case abbrev = 0).
                            Maximum value is `40`, because of max SHA-1 length.
                        -->
                        <abbrev>7</abbrev>
                        
                        <!--
                            Default (optional):
                            -dirty

                            Explanation:
                            When you run describe on a repository that's in "dirty state" (has uncommitted
                            changes), the describe output will contain an additional suffix, such as "-devel"
                            in this example: `v3.5-3-g2222222-devel`. This configuration allows you to alter
                            that additional suffix and gets appended to describe, while the repo is in
                            "dirty state". You can configure that suffix to be anything you want, "-DEV" being
                            a nice example. The "-" sign should be included in the configuration parameter, as it
                            will not be added automatically. If in doubt run `git describe &#45;-dirty=-my_thing`
                            to see how the end result will look like.
                        -->
                        <dirty>-dirty</dirty>

                        <!--
                            Default (optional):
                            * (include all tags)

                            Explanation:
                            Git describe may contain information to tag names. Set this configuration to only
                            consider tags matching the given pattern.
                            This can be used to avoid leaking private tags from the repository.
                        -->
                        <match>*</match>

                        <!--
                            Default (optional):
                            false

                            Explanation:
                            When you run git-describe it only looks only for *annotated tags* by default.
                            If you wish to consider *lightweight tags* in your describe as well you would need
                            to switch this to `true`.

                            The difference between *annotated tags* and *lightweight tags* is outlined in more
                            depth here: https://github.com/git-commit-id/git-commit-id-maven-plugin/#git-describe-and-a-small-gotcha-with-tags
                        -->
                        <tags>false</tags>

                        <!--
                            Default (optional):
                            false

                            Explanation:
                            git-describe, by default, returns just the tag name, if the current commit is tagged.
                            Set this option to `true` to force it to format the output using the typical describe
                            format ("${tag-name}-${commits_from_tag}-g${commit_id-maybe_dirty}"), even if "on" a tag.

                            An example would be: `tagname-0-gc0ffebabe` - notice that the distance from the tag is
                            0 here, if you don't use **forceLongFormat** mode, the describe for such commit would
                            look like this: `tagname`.
                        -->
                        <forceLongFormat>false</forceLongFormat>
                    </gitDescribe>

                    <!-- @since 2.2.2 -->
                    <!--
                        Default (optional):
                        empty list / not set (meaning no properties will be validated by default)

                        Explanation:
                        Since version **2.2.2** the git-commit-id-maven-plugin comes equipped with an additional
                        validation utility which can be used to verify if your project properties are set as you
                        would like to have them set.
                        This feature ships with an additional mojo execution and for instance allows to check if
                        the version is not a snapshot build. If you are interested in the config checkout the
                        [validation utility documentation](https://github.com/git-commit-id/git-commit-id-maven-plugin#validate-if-properties-are-set-as-expected).
                        *Note*: This configuration will only be taken into account when the additional goal
                        `validateRevision` is configured inside an execution.
                    -->
                    <validationProperties>
                        <validationProperty>
                            <!--
                                 A descriptive name that will be used to be able to identify the validation that
                                 does not match up (will be displayed in the error message).
                            -->
                            <name>validating project version</name>
                            <!-- 
                                 the value that needs the validation
                                 *Note* : In order to be able to validate the generated git-properties inside the
                                 pom itself you may need to set the configuration
                                 `<injectAllReactorProjects>true</injectAllReactorProjects>`. 
                            -->
                            <value>${project.version}</value>
                            <!--
                                the expected value
                            -->
                            <shouldMatchTo><![CDATA[^.*(?<!-SNAPSHOT)$]]></shouldMatchTo>
                        </validationProperty>
                        <!-- the next validationProperty you would like to validate -->
                    </validationProperties>

                    <!-- @since 2.2.2 -->
                    <!--
                        Default (optional):
                        true

                        Explanation:
                        Controls whether the validation will fail (`true`) if *at least one* of the
                        validationProperties does not match with it's expected values.
                        If you don't care about this, you may want to set this value to `false` (this makes
                        the configuration of validationProperties useless).
                        *Note*: This configuration will only be taken into account when the additional goal
                        `validateRevision` is configured inside an execution and at least one
                        validationProperty is defined.
                    -->
                    <validationShouldFailIfNoMatch>true</validationShouldFailIfNoMatch>

                    <!-- @since 2.2.4 -->
                    <!--
                        Default (optional):
                        By default this property is simply set to `HEAD` which should reference to the latest
                        commit in your repository.

                        Explanation:
                        Allow to tell the plugin what commit should be used as reference to generate the
                        properties from.

                        In general this property can be set to something generic like `HEAD^1` or point to a
                        branch or tag-name. To support any kind or use-case this configuration can also be set
                        to an entire commit-hash or it's abbreviated version.

                        A use-case for this feature can be found in
                        https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/338.

                        Please note that for security purposes not all references might be allowed as
                        configuration. If you have a specific use-case that is currently not white listed
                        feel free to file an issue.

                        Note with version 3.0.0:
                        When an user uses the `evaluateOnCommit` property to gather the branch for an
                        arbitrary commit (really anything besides the default `HEAD`) this plugin will
                        perform a `git branch &#45;-points-at` which might return a comma separated list
                        of branch names that points to the specified commit.
                    -->
                    <evaluateOnCommit>HEAD</evaluateOnCommit>

                    <!-- @since 3.0.0 -->
                    <!--
                        Default (optional):
                        true

                        Explanation:
                        When set to `true` this plugin will try to use the branch name from build environment.
                        Set to {@code 'false'} to use JGit/GIT to get current branch name which can be useful
                        when using the JGitflow maven plugin.
                        See https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/24#issuecomment-203285398

                        Note: If not using "Check out to specific local branch' and setting this to false may
                        result in getting detached head state and therefore a commit id as branch name.
                    -->
                    <useBranchNameFromBuildEnvironment>true</useBranchNameFromBuildEnvironment>

                    <!-- @since 3.0.0 -->
                    <!--
                        Default (optional):
                        true

                        Explanation:
                        When set to `true` this plugin will try to expose the generated properties into
                        `System.getProperties()`. Set to {@code 'false'} to avoid this exposure.
                        Note that parameters provided via command-line (e.g. `-Dgit.commit.id=value`) still
                        have precedence.
                    -->
                    <injectIntoSysProperties>true</injectIntoSysProperties>
                </configuration>
            </plugin>
            <!-- END OF GIT COMMIT ID PLUGIN CONFIGURATION -->

            <!-- other plugins -->
        </plugins>
    </build>
</project>
```

Based on the above part of a working POM you should be able to figure out the rest, I mean you are a maven user after all... ;-)

All options are documented in the code, so just use `ctrl + q` (intellij @ linux) or `f1` (intellij @ osx) when writing the options in pom.xml - you'll get examples and detailed information about each option (even more than here).


Validation Usage Example
----------------

```xml
<validationProperties>
  <!-- verify that the project version does not end with `-SNAPSHOT` -->
  <validationProperty>
    <name>validating project version</name>
    <value>${project.version}</value>
    <shouldMatchTo><![CDATA[^.*(?<!-SNAPSHOT)$]]></shouldMatchTo>
    <!-- for future reference on this particular regex, please refer to lookahead and lookbehind expressions -->
    <!-- we could also use: <shouldMatchTo>^[0-9\.]*$</shouldMatchTo> -->
  </validationProperty>
  <!-- verify that the current repository is not dirty -->
  <validationProperty>
    <name>validating git dirty</name>
    <value>${git.dirty}</value>
    <shouldMatchTo>false</shouldMatchTo>
   </validationProperty>
  <!-- verify that the current commit has a tag -->
  <validationProperty>
    <name>validating current commit has a tag</name>
    <value>${git.closest.tag.commit.count}</value>
    <shouldMatchTo>0</shouldMatchTo>
   </validationProperty>
</validationProperties>
```

Required Configuration for validation to work:
If you plan to use this feature you'll want to know that the validation will be executed inside an additional mojo.
Inside your pom you thus may want to add an additional execution tag that triggers the execution of the validation plugin.
You can also change the default phase of each execution by adding a `phase` definition.

```xml
<executions>
  <execution>
    <id>get-the-git-infos</id>
    <goals>
      <goal>revision</goal>
    </goals>
  </execution>
  <execution>
    <id>validate-the-git-infos</id>
    <goals>
      <goal>validateRevision</goal>
    </goals>
    <!-- *NOTE*: The default phase of validateRevision is verify, but in case you want to change it, you can do so by adding the phase here -->
    <phase>package</phase>
  </execution>
</executions>
```

*Note* : In order to be able to validate the generated git-properties inside the pom itself you may need to set the configuration `<injectAllReactorProjects>true</injectAllReactorProjects>`.


Generated properties
---------------------
Refer to [this](https://github.com/git-commit-id/git-commit-id-plugin-core/blob/master/src/main/java/pl/project13/core/GitCommitPropertyConstant.java)
to get an overview what properties can be generated by the plugin.
Keep in mind that all properties listed there will be prefixed with the configurable prefix (`git.` by default).
