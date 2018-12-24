Overview
==========================================================
This file should give you an overview on how to use the generated properties within your project.
Essentially every user can chose between the following alternatives:
* use plain resource filtering from maven
* use resource filtering from maven Maven in combination with Spring beans
* have the plugin generate a `git.properties` inside your artifact

The following should give you a broad overview about the different cases.

Maven resource filtering
-----------------------------------------------------------
You can setup this plugin to craft your own properties file. Such behaviour can be achieved by enabeling resources filtering inside your pom.

As an example consider that you want to place your own custom properties in your project under `/src/main/resources` (and call it **git.properties** for example).
Enable resource filtering, by configuring
```
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
```
Also include such a custom crafted properties file with unresolved property values like `${git.tags}`.
Example:

```
git.tags=${git.tags}
git.branch=${git.branch}
git.local.branch.ahead=${git.local.branch.ahead}
git.local.branch.behind=${git.local.branch.behind}
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
git.build.number=${git.build.number}
git.build.number.unique=${git.build.number.unique}
```

Maven will replace the placeholders with the appropriate properties during the build.

The `git` prefix may be configured in the plugin declaration above.

An easier way might be using the generation of a properties file via `generateGitPropertiesFilename`, but thats fully up to you.

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
        <property name="buildNumber" value="${git.build.number}"/>
        <property name="buildNumberUnique" value="${git.build.number.unique}"/>
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
  String buildVersion;            // =${git.build.version}
  String buildNumber;             // =${git.build.number}
  String buildNumberUnique;       // =${git.build.number.unique}

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
         "remoteOriginUrl" : "git@github.com\:git-commit-id/maven-git-commit-id-plugin.git",
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
  this.buildNumber = String.valueOf(properties.get("git.build.number"));
  this.buildNumberUnique = String.valueOf(properties.get("git.build.number.unique"));
}
```

Yet another way to use the plugin
=================================

Rather than reading properties files at runtime or injecting with spring, you can filter a Java source file directly and place it into `src/main/java` with an ignore, or into generated sources directory within the target directory. This has some minor advantages and disadvantages, but is useful for avoiding runtime injection or lookup from properties files that might get lost during repackaging later if used within a library.

