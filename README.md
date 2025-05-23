# maven git commit id plugin

[![Build Status](https://github.com/git-commit-id/git-commit-id-maven-plugin/workflows/Java%20CI/badge.svg?branch=master)](https://github.com/git-commit-id/git-commit-id-maven-plugin/actions)
[![Coverage Status](https://coveralls.io/repos/github/git-commit-id/git-commit-id-maven-plugin/badge.svg?branch=master)](https://coveralls.io/github/git-commit-id/git-commit-id-maven-plugin?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.git-commit-id/git-commit-id-maven-plugin/badge.svg)](https://central.sonatype.com/artifact/io.github.git-commit-id/git-commit-id-maven-plugin)

Exports git version info to maven as properties in the `pom.xml` and as a file in the build output. Code generation and resource loading enable access to the build's version info at runtime.  
Unsure if this addresses your problem? [Read about common use cases](docs/use-cases.md).

## Quick Start
The plugin is **available from [Maven Central](https://central.sonatype.com/artifact/io.github.git-commit-id/git-commit-id-maven-plugin)**. Simply add the following to your `pom.xml`:
```xml
<plugin>
    <groupId>io.github.git-commit-id</groupId>
    <artifactId>git-commit-id-maven-plugin</artifactId>
    <version>9.0.2</version>
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

## Minimum Requirements
* Java 11
* Maven 3.6.3

## Documentation
* [Use Cases](docs/use-cases.md)
* [Configuration & Properties](docs/configuration-and-properties.md)
* [Access Version Info At Runtime](docs/access-version-info-at-runtime.md)
* [git describe](docs/git-describe.md)
* [All Configuration Options as Javadoc](src/main/java/pl/project13/maven/git/GitCommitIdMojo.java)
* [Frequently Asked Questions](docs/faq.md)
* [Contributing](CONTRIBUTING.md)
* [Releases](https://github.com/git-commit-id/git-commit-id-maven-plugin/releases)
* [Old Versions](docs/old-versions.md)
* [Snapshots](docs/snapshots.md)

## Maintainers
This project is currently maintained thanks to: @ktoso (founder), @TheSnoozer

## Notable contributions
I'd like to give a big thanks to some of these folks, for their suggestions and / or pull requests that helped make this plugin as popular as it is today:

* @mostr - for bugfixes and a framework to do integration testing,
* @fredcooke - for consistent feedback and suggestions,
* @MrOnion - for a small yet fast bugfix,
* @cardil and @TheSnoozer - for helping with getting the native git support shipped,
* all the other contributors (as of writing 50) which can be on the [contributors tab](https://github.com/git-commit-id/git-commit-id-maven-plugin/graphs/contributors) - thanks guys,
* ... many others - thank you for your contributions,
* ... you! - for using the plugin :-)

## Notable happy users
* [neo4j](https://neo4j.com/) – graph database
* [FoundationdDB](https://www.foundationdb.org/) – another open source database
* [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#using-boot-maven) – yes, the upstream Spring project is using us
* Akamai, Sabre, EasyDITA, and many many others,
* many others I don't know of.

## License
<img style="float:right; padding:3px; " src="https://github.com/git-commit-id/git-commit-id-maven-plugin/raw/master/lgplv3-147x51.png" alt="GNU LGPL v3"/>

I'm releasing this plugin under the **GNU Lesser General Public License 3.0**.

You're free to use it as you wish, the full license text is attached in the LICENSE file.

## Feature requests
The best way to ask for features / improvements is [via the Issues section on GitHub - it's better than email](https://github.com/git-commit-id/git-commit-id-maven-plugin/issues) because I won't loose when I have a "million emails inbox" day,
and maybe someone else has some idea or would like to upvote your issue.

That's all folks! **Happy hacking!**
