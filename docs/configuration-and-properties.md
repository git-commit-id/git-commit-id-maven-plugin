# Configuration & Properties
This file should give you an overview on how to use the generated properties within your project.

## Basic Configuration
Below is a sample configuration that will write a properties file `git.properties` containing all the git version info to the output directory of your project.
Note that the plugin binds to the initialize phase by default, so that all git properties are available for use throughout the build lifecycle.

```xml
<plugin>
    <groupId>io.github.git-commit-id</groupId>
    <artifactId>git-commit-id-maven-plugin</artifactId>
    <version>9.0.1</version>
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
        <commitIdGenerationMode>full</commitIdGenerationMode>
    </configuration>
</plugin>
```

## Full Configuration
Below is a sample of a full `pom.xml` using the plugin.

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
                <version>9.0.1</version>
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
                    <!-- Parameters for get-the-git-infos -->
                    <!-- see javadoc of GitCommitIdMojo, search for "Parameters that can be configured in the pom.xml" -->

                    <!-- Parameters for validate-the-git-infos -->
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
                </configuration>
            </plugin>
            <!-- END OF GIT COMMIT ID PLUGIN CONFIGURATION -->

            <!-- other plugins -->
        </plugins>
    </build>
</project>
```

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


All Properties
---------------------
Refer to [this](https://github.com/git-commit-id/git-commit-id-plugin-core/blob/master/src/main/java/pl/project13/core/GitCommitPropertyConstant.java) to view all properties that can be generated by the plugin.
Keep in mind that all properties will be prefixed with the configurable prefix (`git.` by default).
