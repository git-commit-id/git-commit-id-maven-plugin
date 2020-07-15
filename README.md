maven git commit id plugin
==================================

[![Build Status](https://secure.travis-ci.org/git-commit-id/git-commit-id-maven-plugin.svg?branch=master)](https://travis-ci.org/github/git-commit-id/git-commit-id-maven-plugin)
[![Coverage Status](https://coveralls.io/repos/github/git-commit-id/git-commit-id-maven-plugin/badge.svg?branch=master)](https://coveralls.io/github/git-commit-id/git-commit-id-maven-plugin?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/pl.project13.maven/git-commit-id-plugin/badge.svg)](https://search.maven.org/artifact/pl.project13.maven/git-commit-id-plugin)


git-commit-id-plugin is a plugin quite similar to [Build Number Maven Plugin](https://www.mojohaus.org/buildnumber-maven-plugin/index.html) for example but as the Build Number plugin at the time when I started this plugin only supported CVS and SVN, something had to be done.
I had to quickly develop a Git version of such a plugin. For those who don't know the plugin, it basically helps you with the following tasks and answers related questions
* Which version had the bug? Is that deployed already?
* Make your distributed deployment aware of versions
* Validate if properties are set as expected

If you are more interested in the different use-cases, feel free to [read about them in more detail](maven/docs/use-cases.md).

Quicklinks (all relevant documentation)
==================
* [Use case documentation](maven/docs/use-cases.md)
* [Using the plugin documentation (all details for configuration, properties, ...)](maven/docs/using-the-plugin.md)
* [A more technical documentation  on how to use the leverage the generated properties from this plugin](maven/docs/using-the-plugin-in-more-depth.md)
* [A general documentation for git describe (usefull feature in this plugin, if you are not familiar with the command)](maven/docs/git-describe.md)
* [Frequently Asked Question (FAQ)](maven/docs/faq.md)
* [Contributing](CONTRIBUTING.md)

Getting the plugin
==================
The plugin **is available from Maven Central** ([see here](https://search.maven.org/artifact/pl.project13.maven/git-commit-id-plugin)), so you don't have to configure any additional repositories to use this plugin.

A detailed description of using the plugin is available in the [Using the plugin](maven/docs/using-the-plugin.md) document. All you need to do in the basic setup is to include that plugin definition in your `pom.xml`.
For more advanced users we also prepared a [guide to provide a brief overview of the more advanced configurations](maven/docs/using-the-plugin.md)... read on!

Versions
--------
The current version is **4.0.1** ([changelist](https://github.com/git-commit-id/git-commit-id-maven-plugin/issues?q=milestone%3A4.0.1)).

You can check the available versions by visiting [search.maven.org](https://search.maven.org/artifact/pl.project13.maven/git-commit-id-plugin), though using the newest is obviously the best choice.

Plugin compatibility with Java
-------------------------------
| Plugin Version  | Required Java Version |
| --------------- | ---------------------:|
| 2.1.X           | Java 1.6              |
| 2.2.X           | Java 1.7              |
| 3.0.0           | Java 1.8              |


Plugin compatibility with Maven
-----------------------------
Even though this plugin tries to be compatible with every Maven version there are some known limitations with specific versions. Here is a list that tries to outline the current state of the art:

| Maven Version               | Plugin Version  | Notes                                                                                                           |
| --------------------------- | ---------------:|:---------------------------------------------------------------------------------------------------------------:|
| Maven 2.0.11                | up to 2.2.6     | Maven 2 is EOL, git-commit-id-plugin:1.0 doesn't work -- requires maven version 2.2.1                           |
| Maven 2.2.1                 | up to 2.2.6     | Maven 2 is EOL                                                                                                  |
| Maven 3.0.X                 | any             | git-commit-id-plugin:2.1.14, 2.1.15, 2.2.0, 2.2.1, 2.2.3  doesn't work  -- requires maven version 3.1.1         |
| Maven 3.0.X                 | any             | For git-commit-id-plugin 2.2.4 or higher: works, but failed to load class "org.slf4j.impl.StaticLoggerBinder"   |
| Maven 3.1.0                 | any             | git-commit-id-plugin:2.1.14, 2.1.15, 2.2.0, 2.2.1, 2.2.3 doesn't work -- requires maven version 3.1.1           |
| Maven 3.3.1                 | any             | git-commit-id-plugin:2.1.14 doesn't work                                                                        |
| Maven 3.3.3                 | any             | git-commit-id-plugin:2.1.14 doesn't work                                                                        |
| Maven 3.X.X                 | any             | Any other non listed version here should work with any plugin version                                           |


Note:
As an example -- this table should be read as: For `Maven 3.1.0` `any` Plugin Version should work, besides the ones listed in the `Notes` have the limitations listed.

Getting SNAPSHOT versions of the plugin
---------------------------------------
If you really want to use **snapshots**, here's the repository they are deployed to. 
But I highly recommend using only stable versions, from Maven Central... :-)

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
* all the other contributors (as of writing 50) which can be on the [contributors tab](https://github.com/git-commit-id/git-commit-id-maven-plugin/graphs/contributors) - thanks guys,
* ... many others - thank you for your contributions,
* ... you! - for using the plugin :-)

Notable happy users
===================

* [neo4j](https://neo4j.com/) – graph database
* [FoundationdDB](https://www.foundationdb.org/) – another open source database
* [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#using-boot-maven) – yes, the upstream Spring project is using us
* Akamai, Sabre, EasyDITA, and many many others,
* many others I don't know of.

License
=======
<img style="float:right; padding:3px; " src="https://github.com/git-commit-id/git-commit-id-maven-plugin/raw/master/lgplv3-147x51.png" alt="GNU LGPL v3"/>

I'm releasing this plugin under the **GNU Lesser General Public License 3.0**.

You're free to use it as you wish, the full license text is attached in the LICENSE file.

Feature requests
================
The best way to ask for features / improvements is [via the Issues section on GitHub - it's better than email](https://github.com/git-commit-id/git-commit-id-maven-plugin/issues) because I won't loose when I have a "million emails inbox" day,
and maybe someone else has some idea or would like to upvote your issue.

That's all folks! **Happy hacking!**
