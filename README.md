maven git commit id plugin
==================================

[![Join the chat at https://gitter.im/git-commit-id/maven-git-commit-id-plugin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/git-commit-id/maven-git-commit-id-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://secure.travis-ci.org/git-commit-id/maven-git-commit-id-plugin.svg?branch=master)](http://travis-ci.org/git-commit-id/maven-git-commit-id-plugin)
[![Coverage Status](https://coveralls.io/repos/github/git-commit-id/maven-git-commit-id-plugin/badge.svg?branch=master)](https://coveralls.io/github/git-commit-id/maven-git-commit-id-plugin?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/pl.project13.maven/git-commit-id-plugin/badge.svg)](http://search.maven.org/#search|ga|1|pl.project13.maven)


git-commit-id-plugin is a plugin quite similar to https://fisheye.codehaus.org/browse/mojo/tags/buildnumber-maven-plugin-1.0-beta-4 for example but as buildnumber at the time when I started this plugin only supported CVS and SVN, something had to be done.
I had to quickly develop an git version of such a plugin. For those who don't know the previous plugins, it basically helps you to help you with the following tasks and answer related questions
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

Getting the plugin
==================
The plugin **is available from Maven Central** (<a href="http://search.maven.org/#search%7Cga%7C1%7Cpl.project13">see here</a>), so you don't have to configure any additional repositories to use this plugin.

A detailed description of using the plugin is available in the [Using the plugin](maven/docs/using-the-plugin.md) section. All you need to do in the basic setup is to include that plugin definition in your `pom.xml`.
For more advanced users we also prepared a [guide to provide a brief overview of the more advanced configurations](maven/docs/using-the-plugin.md)<a>... read on!

Versions
--------
The current version is **4.0.0** ([changelist](https://github.com/git-commit-id/maven-git-commit-id-plugin/issues?q=milestone%3A4.0.0)).

You can check the available versions by visiting [search.maven.org](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22pl.project13.maven%22%20AND%20a%3A%22git-commit-id-plugin%22), though using the newest is obviously the best choice.

Plugin compatibility with Java
-------------------------------
| Plugin Version  | Required Java Version |
| --------------- | ---------------------:|
| 2.1.X           | Java 1.6              |
| 2.2.X           | Java 1.7              |
| 3.0.0           | Java 1.8              |


Plugin compatibility with maven
-----------------------------
Even though this plugin tries to be compatible with every Maven version there are some known limitations with specific versions. Here is a list that tries to outline the current state of the art:

| Maven Version               | Plugin Version  | Notes                                                                           |
| --------------------------- | ---------------:|:-------------------------------------------------------------------------------:|
| Maven 3.1.0 (and below)     | up to 2.1.13    |                                                                                 |
| Maven 3.1.1 (and onwards)   |          any    |                                                                                 |
| Maven 3.0   (and onwards)   |   from 2.2.4    | With Maven 3.0.X SLF4J fails to load class "org.slf4j.impl.StaticLoggerBinder". |
| Maven 3.3.1                 |          any    | plugin version 2.1.14 doesn't work                                              |
| Maven 3.3.3                 |          any    | plugin version 2.1.14 doesn't work                                              |


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
* all the other contributors (as of writing 50) which can be on the [contributors tab](https://github.com/git-commit-id/maven-git-commit-id-plugin/graphs/contributors) - thanks guys,
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
<img style="float:right; padding:3px; " src="https://github.com/git-commit-id/maven-git-commit-id-plugin/raw/master/lgplv3-147x51.png" alt="GNU LGPL v3"/>

I'm releasing this plugin under the **GNU Lesser General Public License 3.0**.

You're free to use it as you wish, the full license text is attached in the LICENSE file.

The best way to ask for features / improvements is [via the Issues section on github - it's better than email](https://github.com/git-commit-id/maven-git-commit-id-plugin/issues) because I won't loose when I have a "million emails inbox" day,
and maybe someone else has some idea or would like to upvote your issue.

That's all folks! **Happy hacking!**
