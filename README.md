maven git commit id plugin
==================================

[![Join the chat at https://gitter.im/ktoso/maven-git-commit-id-plugin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/ktoso/maven-git-commit-id-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://secure.travis-ci.org/ktoso/maven-git-commit-id-plugin.svg?branch=master)](http://travis-ci.org/ktoso/maven-git-commit-id-plugin)

git-commit-id-plugin is a plugin quite similar to https://fisheye.codehaus.org/browse/mojo/tags/buildnumber-maven-plugin-1.0-beta-4 for example but as buildnumber at the time when I started this plugin only supported CVS and SVN, something had to be done.
I had to quickly develop an git version of such a plugin. For those who don't know the previous plugins, let me explain what this plugin does:

Use cases
=========
Which version had the bug? Is that deployed already?
----------------------------------------------------
If you develop your maven project inside an git repository you may want to know exactly what changeset is currently deployed. Why is this useful? 

I worked in a team where the testers would come up to the development team and say: "hey, feature X is still broken!", to which a dev would reply "But I fixed it this morning!". Then they'd investigate a bit, only to see that the next version which would be deployed very soon included the needed fix, yet the developer already marked it as "ready for testing".

The fix here is obvious: include the version you fixed some bug in the issue comment where you mark it as "ready for testing". You can either do this via smart tooling (recommended), or just manually put in a comment like "fixed in v1.4.3-324-g45xhbghv" (that's a git-describe output - explained in detail bellow), so the testing crew knows it doesn't make sense to pickup testing of this feature until at least "324" (or greater) is included in the version output (it means "number of commits away from the mentioned tag" - readup on git-describe to understand how it works).

Make your distributed deployment aware of versions
---------------------------------------------
Let's say you have a large distributed deployment where the servers need to talk to each other using some protocol. You have them configured to keep talking with servers of the same major + minor version number. So a server running "3.3.233" may still talk with one that has "3.3.120" - the protocol is guaranteed to not have changed in these versions.

And now imagine that you need to deploy a drastic API change - so the new version of the servers will be "3.4.0". You can't afford to bring the system down for the deployment. But as the servers are configured to only talk with compatible versions - you're in luck. You can start the deployment process and each node, one by one will be replaced with the new version - the old servers simply stop communicating with them, and the new versions start talking with each other until only the "new" nodes are left.

Using this plugin, you can easily expose the information needed - based on git tags for example. 
One might say that this is usually accomplished by using `${project.version}` and I generally would agree, but maybe tags would fit your use case better than a plain version. :-)

Validate if properties are set as expected
---------------------------------------------
Since version **2.2.2** the maven-git-commit-id-plugin comes equipped with an additional validation utility which can be used to verify if your project properties are set as you would like to have them set.
The validation can be used for *any* property that is visible inside the pom.xml and therefore can be used for a various amount of different use cases.
In order to understand the ideology and intention here are some pretty usefull ideas you could achive by using the validation:
* validate if the version of your project does not end with SNAPSHOT
* validate if you are currently on a tag
* ensure that your repository is not dirty
* may other's :-)

With the current version of the validation the user can decide if the build should fail if *at least one* of the defined criterias do not match with the desired values.

For flexibility and due to the fact that this validation has a different scope than the maven-git-commit-id-plugin this validation needs to be configured as additional execution inside the configuration of the pom.xml.
Once configured, the validation is executed during the verification-phase. However since the validation is done in a seperate execution the phase can easily be changed by adding the desired phase to the execution configuration.

Usage Example:

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

*Note* : In order to be able to validate the generated git-properties inside the pom itself you may need to set the configuration `<injectAllReactorProjects>true</injectAllReactorProjects>`.

Other
-----
If you have a nice use case to share, please do fork this file and file a pull request with your story :-)

Getting the plugin
==================
The plugin **is available from Maven Central** (<a href="http://search.maven.org/#search%7Cga%7C1%7Cpl.project13">see here</a>), so you don't have to configure any additional repositories to use this plugin.

A detailed description of using the plugin is available in the <a href="https://github.com/ktoso/maven-git-commit-id-plugin#using-the-plugin">Using the plugin</a> section. All you need to do in the basic setup is to include that plugin definition in your `pom.xml` - more advanced configurations are also explained so... read on!

Versions
--------
The current version is **2.2.2** ([changelist](https://github.com/ktoso/maven-git-commit-id-plugin/issues?q=milestone%3A2.2.2)).

You can check the available versions by visiting [search.maven.org](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22pl.project13.maven%22%20AND%20a%3A%22git-commit-id-plugin%22), though using the newest is obviously the best choice.

Migration Issues you may come across when using the latest 2.2.X
-----------------------------
If you are already using the git-commit-id-plugin and would like to move to the latest major release (`2.2.X`) there are some design choices that resulted in the fact that we needed to dropped the support of Java `1.6` (if you still rely on this version, the `2.1.15` version still has support for this and you may want to check the fixed issues since then before reporting a new one).

Plugin compatibility with maven
-----------------------------
Even though this plugin tries to be compatible with every Maven version there are some known limitations with specific versions. Here is a list that tries to outline the current state of the art:

| Maven Version               | Plugin Version  | Notes                              |
| --------------------------- | ---------------:|:----------------------------------:|
| Maven 3.1.0 (and below)     | up to 2.1.13    |                                    |
| Maven 3.1.1 (and onwards)   |          any    |                                    |
| Maven 3.3.1                 |          any    | plugin version 2.1.14 doesn't work |
| Maven 3.3.3                 |          any    | plugin version 2.1.14 doesn't work |


Starting with Maven 3.1.1 any plugin version is currently compatible. Only known exception is for Maven 3.3.1 and Maven 3.3.3 where the plugin version 2.1.14 is not working properly.


Getting SNAPSHOT versions of the plugin
---------------------------------------
If you really want to use **snapshots**, here's the repository they are deployed to. 
But I highly recommend using only stable versions, from maven central... :-)

```xml
<pluginRepositories>
    <pluginRepository>
        <id>sonatype-snapshots</id>
        <name>Sonatype Snapshots</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    </pluginRepository>
</pluginRepositories>
```

If you just would like to see what the plugin can do, you can clone the repository and run
```
mvn clean install -Dmaven.test.skip=true && mvn clean package -Pdemo -Dmaven.test.skip=true
```

Using the plugin
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
                <groupId>pl.project13.maven</groupId>
                <artifactId>git-commit-id-plugin</artifactId>
                <version>2.2.1</version>
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

                <configuration>
                    <!--
                        If you'd like to tell the plugin where your .git directory is,
                        use this setting, otherwise we'll perform a search trying to
                        figure out the right directory. It's better to add it explicitly IMHO.
                    -->
                    <dotGitDirectory>${project.basedir}/.git</dotGitDirectory>

                    <!-- that's the default value, you don't have to set it -->
                    <prefix>git</prefix>

                    <!-- that's the default value -->
                    <dateFormat>dd.MM.yyyy '@' HH:mm:ss z</dateFormat>

                    <!-- @since 2.2.0 -->
                    <!-- 
                         If you want to set the timezone of the dateformat to anything in particular you can do this by using this option. 
                         As a general warning try to avoid three-letter time zone IDs because the same abbreviation are often used for multiple time zones. 
                         The default value we'll use the timezone use the timezone that's shipped with java (java.util.TimeZone.getDefault().getID()). 
                         *Note*: If you plan to set the java's timezone by using `MAVEN_OPTS=-Duser.timezone=UTC mvn clean package`, `mvn clean package -Duser.timezone=UTC` or any other configuration keep in mind that this option will override those settings and will not take other configurations into account!
                    -->
                    <dateFormatTimeZone>${user.timezone}</dateFormatTimeZone>

                    <!-- false is default here, it prints some more information during the build -->
                    <verbose>false</verbose>

                    <!-- ALTERNATE SETUP - GENERATE FILE -->
                    <!--
                        If you want to keep git information, even in your WAR file etc,
                        use this mode, which will generate a properties file (with filled out values)
                        which you can then normally read using new Properties().load(/**/)
                    -->

                    <!-- 
                        This is false by default, forces the plugin to generate the git.properties file.
                        Note that the functional meaning of git.build.time becomes different in a very subtle way (see later in this README)
                    -->
                    <generateGitPropertiesFile>true</generateGitPropertiesFile>

                    <!-- 
                        The path for the to be generated properties file, it's relative to ${project.basedir} 
                        The default value is ${project.build.outputDirectory}/git.properties

                        *Note*: If you plan to set the generateGitPropertiesFilename-Path to a location where usually the source-files
                                comes from (e.g. src/main/resources) and experience that your IDE (e.g. eclipse)
                                invokes "Maven Project Builder" once every second, the chances that you are using
                                an IDE where the src-folder is a watched folder for files that are *only* edited by humans is pretty high.
                                For further information refer to the manual for your specific IDE and check the workflow of "incremental project builders".
                                In order to fix this problem we recommend to set the generateGitPropertiesFilename-Path
                                to a target folder (e.g. ${project.build.outputDirectory}) since this is
                                the place where all derived/generated resources should go.
                    -->
                    <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties</generateGitPropertiesFilename>

                    <!-- Denotes the format to save properties in. Valid options are "properties" (default) and "json". Properties will be saved to the generateGitPropertiesFilename if generateGitPropertiesFile is set to `true`. -->
                    <format>properties</format>

                    <!--
                        this is true by default; You may want to set this to false, if the plugin should run inside a
                        <packaging>pom</packaging> project. Most projects won't need to override this property.

                        For an use-case for this kind of behaviour see: https://github.com/ktoso/maven-git-commit-id-plugin/issues/21
                    -->
                    <skipPoms>true</skipPoms>

                    <!-- @since 2.1.4 -->
                    <!-- 
                        Tell maven-git-commit-id to inject the git properties into all reactor projects not just the current one.
                        For details about why you might want to skip this, read this issue: https://github.com/ktoso/maven-git-commit-id-plugin/pull/65
                        The property is set to ``false`` by default to prevent the overriding of properties that may be unrelated to the project.
                    -->
                    <injectAllReactorProjects>false</injectAllReactorProjects>

                    <!-- @since 2.0.4 -->
                    <!-- true by default, controls whether the plugin will fail when no .git directory is found, when set to false the plugin will just skip execution -->                    
                    <failOnNoGitDirectory>true</failOnNoGitDirectory>

                    <!-- @since 2.1.5 -->
                    <!-- true by default, controls whether the plugin will fail if it was unable to obtain enough data for a complete run, if you don't care about this, you may want to set this value to false. -->
                    <failOnUnableToExtractRepoInfo>true</failOnUnableToExtractRepoInfo>

                    <!-- @since 2.1.8 -->
                    <!--
                        skip the plugin execution completely. This is useful for e.g. profile activated plugin invocations or
                        to use properties to enable / disable pom features. Default value is 'false'.
                        With version 2.2.3 you can also skip the plugin by using the commandline option -Dmaven.gitcommitid.skip=true
                    -->
                    <skip>false</skip>

                    <!-- @since 2.1.12 -->
                    <!--
                       Use with caution!

                       In a multi-module build, only run once. This means that the plugins effects will only execute once, for the parent project.
                       This probably won't "do the right thing" if your project has more than one git repository.

                       Important: If you're using `generateGitPropertiesFile`, setting `runOnlyOnce` will make the plugin
                       only generate the file in the directory where you started your build (!).

                       The `git.*` maven properties are available in all modules.
                       Default value is `false`.
                    -->
                    <runOnlyOnce>false</runOnlyOnce>

                    <!-- @since 2.1.9 -->
                    <!--
                        Can be used to exclude certain properties from being emitted into the resulting file.
                        May be useful when you want to hide {@code git.remote.origin.url} (maybe because it contains your repo password?),
                        or the email of the committer etc.

                        Each value may be globbing, that is, you can write {@code git.commit.user.*} to exclude both, the {@code name},
                        as well as {@code email} properties from being emitted into the resulting files.

                        Please note that the strings here are Java regexes ({@code .*} is globbing, not plain {@code *}).
                    -->
                    <excludeProperties>
                      <!-- <excludeProperty>git.user.*</excludeProperty> -->
                    </excludeProperties>

                    <!-- @since 2.1.14 -->
                    <!--
                        Can be used to include only certain properties into the resulting file.
                        Will be overruled by the exclude properties.

                        Each value may be globbing, that is, you can write {@code git.commit.user.*} to include both, the {@code name},
                        as well as {@code email} properties into the resulting files.

                        Please note that the strings here are Java regexes ({@code .*} is globbing, not plain {@code *}).
                    -->
                    <includeOnlyProperties>
                      <!-- <includeOnlyProperty>^git.commit.id.full$</includeOnlyProperty> -->
                    </includeOnlyProperties>

                    <!-- @since 2.1.10 -->
                    <!-- 
                      false is default here, if set to true it uses native `git` executable for extracting all data.
                      This usually has better performance than the default (jgit) implementation, but requires you to 
                      have git available as executable for the build as well as *might break unexpectedly* when you 
                      upgrade your system-wide git installation.
                       
                      As rule of thumb - stay on `jgit` (keep this `false`) until you notice performance problems.
                    -->
                    <useNativeGit>false</useNativeGit>

                    <!-- @since v2.0.4 -->
                    <!--
                         Controls the length of the abbreviated git commit it (git.commit.id.abbrev)

                         Defaults to `7`.
                         `0` carries the special meaning.
                         Maximum value is `40`, because of max SHA-1 length.
                     -->
                    <abbrevLength>7</abbrevLength>


                    <!-- @since v2.2.0 -->
                    <!--
                         The option can be used to tell the plugin how it should generate the 'git.commit.id' property. Due to some naming issues when exporting the properties as an json-object (https://github.com/ktoso/maven-git-commit-id-plugin/issues/122) we needed to make it possible to export all properties as a valid json-object.
                         Due to the fact that this is one of the major properties the plugin is exporting we just don't want to change the exporting mechanism and somehow throw the backwards compatibility away.
                         We rather provide a convenient switch where you can choose if you would like the properties as they always had been, or if you rather need to support full json-object compatibility.
                         In the case you need to fully support json-object we unfortunately need to change the 'git.commit.id' property from 'git.commit.id' to 'git.commit.id.full' in the exporting mechanism to allow the generation of a fully valid json object.

                         Currently the switch allows two different options:
                         1. By default this property is set to 'flat' and will generate the formerly known property 'git.commit.id' as it was in the previous versions of the plugin. Keeping it to 'flat' by default preserve backwards compatibility and does not require further adjustments by the end user.
                         2. If you set this switch to 'full' the plugin will export the formerly known property 'git.commit.id' as 'git.commit.id.full' and therefore will generate a fully valid json object in the exporting mechanism.

                         *Note*: Depending on your plugin configuration you obviously can choose the 'prefix' of your properties by setting it accordingly in the plugin's configuration. As a result this is therefore only an illustration what the switch means when the 'prefix' is set to it's default value.
                         *Note*: If you set the value to something that's not equal to 'flat' or 'full' (ignoring the case) the plugin will output a warning and will fallback to the default 'flat' mode.
                    -->
                    <commitIdGenerationMode>flat</commitIdGenerationMode>

                    <!-- @since 2.1.0 -->
                    <!-- 
                        read up about git-describe on the in man, or it's homepage - it's a really powerful versioning helper 
                        and the recommended way to use git-commit-id-plugin. The configuration bellow is optional, 
                        by default describe will run "just like git-describe on the command line", even though it's a JGit reimplementation.
                    -->
                    <gitDescribe>
                        
                        <!-- don't generate the describe property -->
                        <skip>false</skip>
                        
                        <!-- 
                            if no tag was found "near" this commit, just print the commit's id instead, 
                            helpful when you always expect this field to be not-empty 
                        -->
                        <always>false</always>
                        <!--
                             how many chars should be displayed as the commit object id? 
                             7 is git's default, 
                             0 has a special meaning (see end of this README.md), 
                             and 40 is the maximum value here 
                        -->
                        <abbrev>7</abbrev>
                        
                        <!-- when the build is triggered while the repo is in "dirty state", append this suffix -->
                        <dirty>-dirty</dirty>

                        <!-- Only consider tags matching the given pattern. This can be used to avoid leaking private tags from the repository. -->
                        <match>*</match>

                        <!--
                            when you run git-describe it only looks only for *annotated tags* by default
                            if you wish to consider *lightweight tags* in your describe as well you would need to switch this to *true*

                            The difference between *annotated tags* and *lightweight tags* is outlined in more depth here:
                            https://github.com/ktoso/maven-git-commit-id-plugin/#git-describe-and-a-small-gotcha-with-tags
                        -->
                        <tags>false</tags>

                        <!-- 
                             always print using the "tag-commits_from_tag-g_commit_id-maybe_dirty" format, even if "on" a tag. 
                             The distance will always be 0 if you're "on" the tag. 
                        -->
                        <forceLongFormat>false</forceLongFormat>
                    </gitDescribe>
                    <!-- @since 2.2.2 -->
                    <!-- 
                        Since version **2.2.2** the maven-git-commit-id-plugin comes equipped with an additional validation utility which can be used to verify if your project properties are set as you would like to have them set.
                        *Note*: This configuration will only be taken into account when the additional goal `validateRevision` is configured inside an execution.
                    -->
                    <validationProperties>
                        <validationProperty>
                            <!--
                                 A descriptive name that will be used to be able to identify the validation that does not match up (will be displayed in the error message).
                            -->
                            <name>validating project version</name>
                            <!-- 
                                 the value that needs the validation
                                 *Note* : In order to be able to validate the generated git-properties inside the pom itself you may need to set the configuration `<injectAllReactorProjects>true</injectAllReactorProjects>`. 
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
                        true by default, controls whether the validation will fail if *at least one* of the validationProperties does not match with it's expected values. 
                        If you don't care about this, you may want to set this value to false (this makes the configuration of validationProperties useless).
                        *Note*: This configuration will only be taken into account when the additional goal `validateRevision` is configured inside an execution and at least one validationProperty is defined.
                    -->
                    <validationShouldFailIfNoMatch>true</validationShouldFailIfNoMatch>
                </configuration>

            </plugin>
            <!-- END OF GIT COMMIT ID PLUGIN CONFIGURATION -->

            <!-- other plugins -->
        </plugins>
    </build>
</project>
```

Based on the above part of a working POM you should be able to figure out the rest, I mean you are a maven user after all... ;-)
Note that the resources filtering is important for this plugin to work, don't omit it!

Now you just have to include such a properties file in your project under `/src/main/resources` (and call it **git.properties** for example) and maven will put the appropriate properties in the placeholders:

```
git.tags=${git.tags}
git.branch=${git.branch}
git.dirty=${git.dirty}
git.remote.origin.url=${git.remote.origin.url}
  git.commit.id=${git.commit.id}
  OR (depends on commitIdGenerationMode)
  git.commit.id.full=${git.commit.id.full}
git.commit.id.abbrev=${git.commit.id.abbrev}
git.commit.id.describe=${git.commit.id.describe}
git.commit.id.describe-short=${git.commit.id.describe-short}
git.commit.user.name=${git.commit.user.name}
git.commit.user.email=${git.commit.user.email}
git.commit.message.full=${git.commit.message.full}
git.commit.message.short=${git.commit.message.short}
git.commit.time=${git.commit.time}
git.closest.tag.name=${git.closest.tag.name}
git.closest.tag.commit.count=${git.closest.tag.commit.count}

git.build.user.name=${git.build.user.name}
git.build.user.email=${git.build.user.email}
git.build.time=${git.build.time}
git.build.host=${git.build.host}
git.build.version=${git.build.version}
```

The `git` prefix may be configured in the plugin declaration above.

Maven resource filtering + Spring = GitRepositoryState Bean
-----------------------------------------------------------
You'll most probably want to wire these plugins somehow to get easy access to them during runtime. We'll use spring as an example of doing this.
Start out with with adding the above steps to your project, next paste this **git-bean.xml** into the `/src/main/resources/` directory (or any other, just adjust the paths later on):

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">

    <bean name="gitRepositoryInformation" class="pl.project13.maven.example.git.GitRepositoryState">
        <property name="tags" value="${git.tags}"/>
        <property name="branch" value="${git.branch}"/>
        <property name="dirty" value="${git.dirty}"/>
        <property name="remoteOriginUrl" value="${git.remote.origin.url}"/>

        <property name="commitId" value="${git.commit.id.full}"/>
        <!-- OR value="${git.commit.id}" depending on your configuration of commitIdGenerationMode -->
        <property name="commitIdAbbrev" value="${git.commit.id.abbrev}"/>
        <property name="describe" value="${git.commit.id.describe}"/>
        <property name="describeShort"  value="${git.commit.id.describe-short}"/>
        <property name="commitUserName" value="${git.commit.user.name}"/>
        <property name="commitUserEmail" value="${git.commit.user.email}"/>
        <property name="commitMessageFull" value="${git.commit.message.full}"/>
        <property name="commitMessageShort" value="${git.commit.message.short}"/>
        <property name="commitTime" value="${git.commit.time}"/>
        <property name="closestTagName" value="${git.closest.tag.name}"/>
        <property name="closestTagCommitCount" value="${git.closest.tag.commit.count}"/>

        <property name="buildUserName" value="${git.build.user.name}"/>
        <property name="buildUserEmail" value="${git.build.user.email}"/>
        <property name="buildTime" value="${git.build.time}"/>
        <property name="buildHost" value="${git.build.host}"/>
        <property name="buildVersion" value="${git.build.version}"/>
    </bean>
</beans>
```

And here's the source of the bean we're binding here:

```java
package pl.project13.maven.example.git;

import org.codehaus.jackson.annotate.JsonWriteNullProperties;

/**
* A spring controlled bean that will be injected
* with properties about the repository state at build time.
* This information is supplied by my plugin - <b>pl.project13.maven.git-commit-id-plugin</b>
*/
@JsonWriteNullProperties(true)
public class GitRepositoryState {
  String tags;                    // =${git.tags} // comma separated tag names
  String branch;                  // =${git.branch}
  String dirty;                   // =${git.dirty}
  String remoteOriginUrl;         // =${git.remote.origin.url}

  String commitId;                // =${git.commit.id.full} OR ${git.commit.id}
  String commitIdAbbrev;          // =${git.commit.id.abbrev}
  String describe;                // =${git.commit.id.describe}
  String describeShort;           // =${git.commit.id.describe-short}
  String commitUserName;          // =${git.commit.user.name}
  String commitUserEmail;         // =${git.commit.user.email}
  String commitMessageFull;       // =${git.commit.message.full}
  String commitMessageShort;      // =${git.commit.message.short}
  String commitTime;              // =${git.commit.time}
  String closestTagName;          // =${git.closest.tag.name}
  String closestTagCommitCount;   // =${git.closest.tag.commit.count}

  String buildUserName;           // =${git.build.user.name}
  String buildUserEmail;          // =${git.build.user.email}
  String buildTime;               // =${git.build.time}
  String buildHost;               // =${git.build.host}
  String buildVersion             // =${git.build.version}

  public GitRepositoryState() {
  }
  /* Generate setters and getters here */
}
```

The source for it is also on the repo of this plugin. Of course, *feel free to drop out the jackson annotation* if you won't be using it.

The last configuration related thing we need to do is to load up this bean in your appContext, so open up your **applicationContext.xml** or whatever you call it in your project and add these lines in the <beans/> section:

    <context:property-placeholder location="classpath:*.properties" />
    <import resource="classpath:/git-bean.xml"/>

Of course, you may adjust the paths and file locations as you please, no problems here... :-)
*Now you're ready to use your GitRepositoryState Bean!* Let's create an sample **Spring MVC Controller** to test it out:

```java
@Controller
@RequestMapping("/git")
public class GitService extends BaseWebService {

  @Autowired
  GitRepositoryState gitRepoState;

  @RequestMapping("/status")
  public ModelAndView checkGitRevision() throws WebServiceAuthenticationException {
    ServerResponse<GitRepositoryState> response = new ServerResponse<GitRepositoryState>(gitRepoState);
    return createMAV(response);
  }
}
```

Don't mind the createMAV and responses stuff, it's just example code. And feel free to use constructor injection, it's actually a better idea ;-)

In the end *this is what this service would return*:

```json
     {
         "tags" : "v2.1.14,testing",
         "branch" : "testing-maven-git-plugin",
         "dirty" : "false",
         "remoteOriginUrl" : "git@github.com\:ktoso/maven-git-commit-id-plugin.git",
         "commitId" : "787e39f61f99110e74deed68ab9093088d64b969",
         "commitIdAbbrev" : "787e39f",
         "describe" : "v2.1.0-2-g2346463",
         "describeShort" : "v2.1.0-2",
         "commitUserName" : "Konrad Malawski",
         "commitUserEmail" : "konrad.malawski@java.pl",
         "commitMessageFull" : "releasing my fun plugin :-)
                                + fixed some typos
                                + cleaned up directory structure
                                + added license etc",
         "commitMessageShort" : "releasing my fun plugin :-)",
         "commitTime" : "06.01.1970 @ 16:16:26 CET",
         "closestTagName" : "v2.1.0",
         "closestTagCommitCount" : "2",
         
         "buildUserName" : "Konrad Malawski",
         "buildUserEmail" : "konrad.malawski@java.pl",
         "buildTime" : "06.01.1970 @ 16:17:53 CET",
         "buildHost" : "github.com",
         "buildVersion" : "v2.1.0-SNAPSHOT"
     }
```

The easier way: generate git.properties
=======================================
There's another way to use the plugin, it's a little bit easier I guess. First, configure it to generate a properties file on each run, goto the pom.xml and set:

```xml
<configuration>
    <!-- ... -->

    <!-- this is false by default, forces the plugin to generate the git.properties file -->
    <generateGitPropertiesFile>true</generateGitPropertiesFile>

    <!-- The path for the properties file to be generated. See Super Pom for default variable reference https://maven.apache.org/guides/introduction/introduction-to-the-pom.html -->
    <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties</generateGitPropertiesFilename>
</configuration>
```

Then run the project as you would normally, the file will be created for you. And you may access it as you'd access any other properties file, for example like this:

```java
public GitRepositoryState getGitRepositoryState() throws IOException
{
   if (gitRepositoryState == null)
   {
      Properties properties = new Properties();
      properties.load(getClass().getClassLoader().getResourceAsStream("git.properties"));

      gitRepositoryState = new GitRepositoryState(properties);
   }
   return gitRepositoryState;
}
```

You'd have to add such an constructor to your GitRepositoryState bean:

```java
public GitRepositoryState(Properties properties)
{
  this.tags = String.valueOf(properties.get("git.tags"));
  this.branch = String.valueOf(properties.get("git.branch"));
  this.dirty = String.valueOf(properties.get("git.dirty"));
  this.remoteOriginUrl = String.valueOf(properties.get("git.remote.origin.url"));

  this.commitId = String.valueOf(properties.get("git.commit.id.full")); // OR properties.get("git.commit.id") depending on your configuration
  this.commitIdAbbrev = String.valueOf(properties.get("git.commit.id.abbrev"));
  this.describe = String.valueOf(properties.get("git.commit.id.describe"));
  this.describeShort = String.valueOf(properties.get("git.commit.id.describe-short"));
  this.commitUserName = String.valueOf(properties.get("git.commit.user.name"));
  this.commitUserEmail = String.valueOf(properties.get("git.commit.user.email"));
  this.commitMessageFull = String.valueOf(properties.get("git.commit.message.full"));
  this.commitMessageShort = String.valueOf(properties.get("git.commit.message.short"));
  this.commitTime = String.valueOf(properties.get("git.commit.time"));
  this.closestTagName = String.valueOf(properties.get("git.closest.tag.name"));
  this.closestTagCommitCount = String.valueOf(properties.get("git.closest.tag.commit.count"));

  this.buildUserName = String.valueOf(properties.get("git.build.user.name"));
  this.buildUserEmail = String.valueOf(properties.get("git.build.user.email"));
  this.buildTime = String.valueOf(properties.get("git.build.time"));
  this.buildHost = String.valueOf(properties.get("git.build.host"));
  this.buildVersion = String.valueOf(properties.get("git.build.version"));
}
```

Note on the generated git.build.time
------------------------------------
Note that when writing the git.properties file the value *git.build.time* will only be updated when things in the commit information have changed.
If you only change a bit of your code and rebuild/rerun you will see an older timestamp that you may have expected.

Essentially the functional meaning becomes **The latest build time when the git information was written to the git.properties file** .

The reason why this was done can be found in [issue 151](https://github.com/ktoso/maven-git-commit-id-plugin/issues/151).

If you need the actual *build time* then simply use the a filtered properties file that contains something like this
```
git.build.time=${git.build.time}
```

Yet another way to use the plugin
=================================

Rather than reading properties files at runtime or injecting with spring, you can filter a Java source file directly and place it into src/main/java with an ignore, or into generated sources directory within the target directory. This has some minor advantages and disadvantages, but is useful for avoiding runtime injection or lookup from properties files that might get lost during repackaging later if used within a library. 

Git describe - short intro to an awesome command
==================================================
Git's [describe command](http://www.kernel.org/pub/software/scm/git/docs/git-describe.html) is the best way to really see "where" a commit is in the repositories "timeline". 

In svn you could easily determine by looking at two revisions (their numbers) which one is "newer" (they look like that `r239`, `r240` ...). Since git is using SHA-1 checksums to identify commits, it's hard to tell which one is "newer" (or can you tell me? `b6a73ed` or `9597545`?). Using describe you can get a part of this back, and even more - you can know the "nearest" tag for a commit. And as tags are used for versioning most of the time that's super useful to track development progress.

Let's get an example to explain git-describe on it:

```
* 2414721 - (HEAD, master) third addition (8 hours ago) <Konrad Malawski>
| d37a598 - second line (8 hours ago) <Konrad Malawski>
| 9597545 - (v1.0) initial commit (8 hours ago) <Konrad Malawski>
```

Running git-describe when you're on the HEAD here, will yield:

```
> git describe
  v.1.0-2-b2414721
```

The format of a describe result is defined as:

```
v1.0-2-g2414721-DEV
 ^   ^  ^       ^
 |   |  |       \-- if a dirtyMarker was given, it will appear here if the repository is in "dirty" state
 |   |  \---------- the "g" prefixed commit id. The prefix is compatible with what git-describe would return - weird, but true.
 |   \------------- the number of commits away from the found tag. So "2414721" is 2 commits ahead of "v1.0", in this example.
 \----------------- the "nearest" tag, to the mentioned commit.
```

Other outputs may look like:   

* **v1.0** - if the repository is "on a tag" (though describe can be forced to print **v1.0.4-0-g2414721** instead if you want -- use the `full` config option),
* **v1.0-DEV** - if the repository is "on a tag", but in "dirty" state. This dirty marker can, and will be included wherever possible,
* **2414721** - a plain commit id hash if not tags were defined (of determined "near" this commit). 
                *It does NOT include the "g" prefix, that is used in the "full" describe output format!*

For more details (on when what output will be returned etc), see <code>man git-describe</code> (or here: [git-describe](http://www.kernel.org/pub/software/scm/git/docs/git-describe.html)). In general, you can assume it's a "best effort" approach, to give you as much info about the repo state as possible.

**describe-short** is also provided, in case you want to display this property to non-techy users, which would panic on the sight of a hash (last part of the describe string) - this property is simply
*the describe output, with the hash part stripped out*.

git-describe and a small "gotcha" with tags
-------------------------------------------
You probably know that git has two kinds of tags:

* **lightweight tags** - which are only a pointer to some object,
* **annotated tags** - which are the same as a lightweight tag and contain additional information, such as a message linked with the tag.

Knowing this, I now can tell you that when you run git-describe, it (by default) looks only for **annotated** tags.
What this means in a real life scenario can be explained on such repository:

```
b6a73ed - (HEAD, master)
d37a598 - (v1.0-fixed-stuff) - a lightweight tag (with no message)
9597545 - (v1.0) - an annotated tag
```

When you run git describe without any options (note that git-commit-id is "acting like" plain git, so all behaviour is as described here, unless you configure it to act otherwise (using the `<tags>true</tags>` option)):

```
> git describe
  annotated-tag-2-gb6a73ed     # the nearest "annotated" tag is found
```

So it did not find the lightweight tag! Do not panic, there's a flag to help with that:

```
> git describe --tags
  lightweight-tag-1-gb6a73ed   # the nearest tag (including lightweights) is found
```

Using only annotated tags to mark builds may be useful if you're using tags to help yourself with annotating
things like "i'll get back to that" etc - you don't need such tags to be exposed. But if you want lightweight
tags to be included in the search, enable this option.

<blockquote>
TIP: If you're using maven's `release:prepare` and `release:perform` it's using <em>annotated</em> tags.
</blockquote>

Configuration options in depth
==============================
An in depth recap of the available configuration parameters.
Note that all of them are optional, though you will probably want to fine tune the plugin to act exactly the way you want.

Optional parameters:

* **dotGitDirectory** - `(default: ${project.basedir}/.git)` the location of your .git folder. `${project.basedir}/.git` is the default value and will most probably be ok for single module projects, in other cases please use `../` to get higher up in the dir tree. An example would be: `${project.basedir}/../.git` which I'm currently using in my projects :-)
* **prefix** - `(default: git)` is the "namespace" for all exposed properties
* **dateFormat** - `(default: dd.MM.yyyy '@' HH:mm:ss z)` is a normal SimpleDateFormat String and will be used to represent git.build.time and git.commit.time
* **dateFormatTimeZone** - `(default: empty)` *(available since v2.2.0)* is a TimeZone String (e.g. 'America/Los_Angeles', 'GMT+10', 'PST') and can be used to set the timezone of the *dateFormat* to anything in particular. As a general warning try to avoid three-letter time zone IDs because the same abbreviation are often used for multiple time zones. The default value we'll use the timezone use the timezone that's shipped with java (java.util.TimeZone.getDefault().getID()). *Note*: If you plan to set the java's timezone by using `MAVEN_OPTS=-Duser.timezone=UTC mvn clean package`, `mvn clean package -Duser.timezone=UTC` or any other configuration keep in mind that this option will override those settings and will not take other configurations into account!
* **verbose** - `(default: false)` if true the plugin will print a summary of all collected properties when it's done
* **generateGitPropertiesFile** -`(default: false)` this is false by default, forces the plugin to generate the git.properties file
* **generateGitPropertiesFilename** - `(default: ${project.build.outputDirectory}/git.properties)` - The path for the to be generated properties file. The path can be relative to ${project.basedir} (e.g. target/classes/git.properties) or can be a full path (e.g. ${project.build.outputDirectory}/git.properties).
* **format** - `(default: properties)` Denotes the format to save properties in. Valid options are "properties" (default) and "json". Properties will be saved to the generateGitPropertiesFilename if generateGitPropertiesFile is set to `true`.
* **skipPoms** - `(default: true)` - Force the plugin to run even if you're inside of an pom packaged project.
* **injectAllReactorProjects** - `(default: false)` - Tell maven-git-commit-id to inject the git properties into all reactor projects not just the current one. The property is set to ``false`` by default to prevent the overriding of properties that may be unrelated to the project. If you need to expose your git properties to another maven module (e.g. maven-antrun-plugin) you need to set it to ``true``. However, setting this option can have an impact on your build. For details about why you might want to skip this, read this issue: https://github.com/ktoso/maven-git-commit-id-plugin/pull/65
* **failOnNoGitDirectory** - `(default: true)` *(available since v2.0.4)* - Specify whether the plugin should fail when a .git directory cannot be found. When set to false and no .git directory is found the plugin will skip execution.
* **failOnUnableToExtractRepoInfo** - `(default: true)` *(available since v2.1.5)* - By default the plugin will fail the build if unable to obtain enough data for a complete run, if you don't care about this, you may want to set this value to false.
* **skip** - `(default: false)` *(available since v2.1.8)* - Skip the plugin execution completely.
* **runOnlyOnce** - `(default: false)` *(available since v2.1.12)* - Use with caution! In a multi-module build, only run once. This means that the plugins effects will only execute once, for the parent project. This probably won't "do the right thing" if your project has more than one git repository. Important: If you're using `generateGitPropertiesFile`, setting `runOnlyOnce` will make the plugin only generate the file in the directory where you started your build :warning:. The `git.*` maven properties are available in all modules.
* **excludeProperties** - `(default: empty)` *(available since v2.1.9)* - Allows to filter out properties that you *don't* want to expose. This feature was implemented in response to [this issue](https://github.com/ktoso/maven-git-commit-id-plugin/issues/91), so if you're curious about the use-case, check that issue.
* **includeOnlyProperties** - `(default: empty)` *(available since v2.1.14)* - Allows to include only properties that you want to expose. This feature was implemented to avoid big exclude properties tag when we only want very few specific properties.
* **useNativeGit** - `(default: false)` *(available since v2.1.10)* - Uses the native `git` binary instead of the custom `jgit` implementation shipped with this plugin to obtain all information. Although this should usually give your build some performance boost, it may randomly break if you upgrade your git version and it decides to print information in a different format suddenly. As rule of thumb, keep using the default `jgit` implementation (keep this option set to `false`) until you notice performance problems within your build (usually when you have *hundreds* of maven modules).
* **abbrevLength** - `(default: 7)` *(available since v2.0.4)* - Configure the "git.commit.id.abbrev" property to be at least of length N (see gitDescribe abbrev for special case abbrev = 0).
* **commitIdGenerationMode** - `(default: flat)` *(available since v2.2.0)* is an option that can be used to tell the plugin how it should generate the 'git.commit.id' property. Due to some naming issues when exporting the properties as an json-object (https://github.com/ktoso/maven-git-commit-id-plugin/issues/122) we needed to make it possible to export all properties as a valid json-object. Currently the switch allows two different options:
1. By default this property is set to 'flat' and will generate the formerly known property 'git.commit.id' as it was in the previous versions of the plugin. Keeping it to 'flat' by default preserve backwards compatibility and does not require further adjustments by the end user.
2. If you set this switch to 'full' the plugin will export the formerly known property 'git.commit.id' as 'git.commit.id.full' and therefore will generate a fully valid json object in the exporting mechanism.

*Note*: Depending on your plugin configuration you obviously can choose the 'prefix' of your properties by setting it accordingly in the plugin's configuration. As a result this is therefore only an illustration what the switch means when the 'prefix' is set to it's default value.
*Note*: If you set the value to something that's not equal to 'flat' or 'full' (ignoring the case) the plugin will output a warning and will fallback to the default 'flat' mode.

**gitDescribe**:
Worth pointing out is, that git-commit-id tries to be 1-to-1 compatible with git's plain output, even though the describe functionality has been reimplemented manually using JGit (you don't have to have a git executable to use the plugin). So if you're familiar with [git-describe](https://github.com/ktoso/maven-git-commit-id-plugin#git-describe---short-intro-to-an-awesome-command), you probably can skip this section, as it just explains the same options that git provides.

* **abbrev** - `(default: 7)` in the describe output, the object id of the hash is always abbreviated to N letters, by default 7. The typical describe output you'll see therefore is: `v2.1.0-1-gf5cd254`, where `-1-` means the number of commits away from the mentioned tag and the `-gf5cd254` part means the first 7 chars of the current commit's id `f5cd254`. **Please note that the `g` prefix is included to notify you that it's a commit id, it is NOT part of the commit's object id** - *this is default git behaviour, so we're doing the same*. You can set this to any value between 0 and 40 (inclusive). 
* **abbrev = 0** is a special case. Setting *abbrev* to `0` has the effect of hiding the "distance from tag" and "object id" parts of the output, so you end up with just the "nearest tag" (that is, instead `tag-12-gaaaaaaa` with `abbrev = 0` you'd get `tag`).
* **dirty** - `(default: "-dirty")` when you run describe on a repository that's in "dirty state" (has uncommitted changes), the describe output will contain an additional suffix, such as "-devel" in this example: `v3.5-3-g2222222-devel`. You can configure that suffix to be anything you want, "-DEV" being a nice example. The "-" sign should be included in the configuration parameter, as it will not be added automatically. If in doubt run `git describe --dirty=-my_thing` to see how the end result will look like.
* **tags** - `(default: false)` if true this option enables matching a lightweight (non-annotated) tag.
* **match** - `(default: *)` only consider tags matching the given pattern (can be used to avoid leaking private tags made from the repository)
* **long** - `(default: false)` git-describe, by default, returns just the tag name, if the current commit is tagged. Use this option to force it to format the output using the typical describe format. An example would be: `tagname-0-gc0ffebabe` - notice that the distance from the tag is 0 here, if you don't use **forceLongFormat** mode, the describe for such commit would look like this: `tagname`.
* **always** - `(default: true)` if unable to find a tag, print out just the object id of the current commit. Useful when you always want to return something meaningful in the describe property.
* **skip** - `(default: false)` when you don't use `git-describe` information in your build, you can opt to be calculate it.


All options are documented in the code, so just use `ctrl + q` (intellij @ linux) or `f1` (intellij @ osx) when writing the options in pom.xml - you'll get examples and detailed information about each option (even more than here).

Frequently Asked Question (FAQ)
=========

Supported Maven & Java Versions
-------------------------------
Checkout the `Plugin compatibility with maven` section of this readme to find out what Maven version this plugin currently supports.
If you still rely on Java `1.6` you can use the plugin version `2.1.15` - more Information can be found in `Migration Issues you may come across when using the latest 2.2.X`

Generated properties are not usable inside the pom / properties don't get exposed by the plugin
-------------------------------
Since version `2.1.4` there is a switch to control if you want the plugin to expose the generated properties to your pom as well.
This switch is set to `false` by default to ensure that properties of reactor builds can't be overwritten by accident.
Thus if you need this feature set `<injectAllReactorProjects>true</injectAllReactorProjects>` inside the plugin's config.

If the properties are empty for some reason verify with the maven-antrun-plugin if they are correctly exposed.
Example:
```
<plugin>
  <artifactId>maven-antrun-plugin</artifactId>
  <version>1.8</version>
  <executions>
    <execution>
      <phase>package</phase>
      <configuration>
        <target>
          <echo>Git-Infos: ${git.commit.id}</echo>
        </target>
      </configuration>
      <goals>
        <goal>run</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

If you are using the maven build with [Maven's Plugin Prefix Resolution](https://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html) (e.g. `mvn somePrefix:goal`) please note that this currently seems to be [not supported by maven](https://issues.apache.org/jira/browse/MNG-6260).
Instead of using the Plugin Prefix Resolution add an execution tag that calls the desired goal of the plugin within a normal maven life cycle (e.g. `mvn clean package`).


Maintainers
===========
This project is currently maintained thanks to: @ktoso (founder), @TheSnoozer


Notable contributions
=====================
I'd like to give a big thanks to some of these folks, for their suggestions and / or pull requests that helped make this plugin as popular as it is today:

* @mostr - for bugfixes and a framework to do integration testing,
* @fredcooke - for consistent feedback and suggestions,
* @MrOnion - for a small yet fast bugfix,
* @cardil and @TheSnoozer - for helping with getting the native git support shipped,
* all the other contributors (as of writing 25) which can be on the [contributors tab](https://github.com/ktoso/maven-git-commit-id-plugin/graphs/contributors) - thanks guys,
* ... many others - thank you for your contributions,
* ... you! - for using the plugin :-)

Notable happy users
===================

* [neo4j](http://www.neo4j.org/) – graph database
* [foundationdb](http://foundationdb.com) – another open source database
* [Spring Boot](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready-git-commit-information) – yes, the upstream Spring project is using us
* Akamai, Sabre, EasyDITA, and many many others,
* many others I don't know of.

License
=======
<img style="float:right; padding:3px; " src="https://github.com/ktoso/maven-git-commit-id-plugin/raw/master/lgplv3-147x51.png" alt="GNU LGPL v3"/>
I'm releasing this plugin under the **GNU Lesser General Public License 3.0**.

You're free to use it as you wish, the full license text is attached in the LICENSE file.

The best way to ask for features / improvements is [via the Issues section on github - it's better than email](https://github.com/ktoso/maven-git-commit-id-plugin/issues) because I won't loose when I have a "million emails inbox" day,
and maybe someone else has some idea or would like to upvote your issue.

That's all folks! **Happy hacking!**
