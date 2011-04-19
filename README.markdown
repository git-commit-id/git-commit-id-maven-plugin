Maven plugin: git-commit-id-plugin
==================================
git-commit-id-plugin is a plugin quite similar to https://fisheye.codehaus.org/browse/mojo/tags/buildnumber-maven-plugin-1.0-beta-4 fo example but as buildnumber only supports svn (which is very sad) and cvs (which is even more sad, and makes bunnies cry) I had to quickly develop an git version of such a plugin. For those who don't know the previous plugins, let me explain what this plugin does:

Sample scenario why this plugin is useful
-----------------------------------------
If you develop your maven project inside an git repository (which you hopefully already are docing) you may want to know exactly
what changeset is currently deployed online. Why is this useful? Well, the tester won't come to you screaming "heeey that bug ain't fixed" of course you'd reply "but I fixed it this morning!" and after some searching you notice "oh... it'll be online after the next deployment, sorry tester... :-(".

This scenario keeps repeating sometimes, thus you can state which commit fixes/closes the bug, note this in JIRA etc and then the tester will know if it's already online (by the commit date for example).

Usage
=====
Getting the plugin
------------------
Yeah! I got access to the **Sonatype** Maven Repo, so now you're able to use this plugin just by adding this to your POM:

Add this repository for **releases** of this plugin:

        <repository>
            <id>sonatype-releases</id>
            <name>Sonatype Releases</name>
            <url>https://oss.sonatype.org/content/repositories/releases/</url>
        </repository>

or use this one for it's **snapshots**:

        <repository>
            <id>sonatype-snapshots</id>
            <name>Sonatype Snapshots</name>
            <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
        </repository>

It'll soon be available from maven central, I hope by the way... :-)
You don't need to add it as a *dependency*, just use the plugin as described bellow.

To see what versions are currently deployed you may go to: <a href="https://oss.sonatype.org/index.html#nexus-search;quick~git-commit">https://oss.sonatype.org</a> (or just use IntelliJ IDEA ;-))

Also, don't be afraid to use SNAPSHOT versions as they've passed the testing process and really shouldn't break anything in your code :-)
Of course, use STABLE releases if you want to be sure of it, I'm also updating them each major improvement or bugfix.

Using the plugin
----------------
It's really simple to setup this plugin, here's a sample pom that you may base your **pom.xml** on:

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
           <url>http://www.blog.project13.pl</url>

           <parent/>

           <dependencies />

           <build>
               <!-- GIT COMMIT ID PLUGIN CONFIGURATION -->
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
                       <version>1.1</version>
                       <executions>
                           <execution>
                               <goals>
                                   <goal>revision</goal>
                               </goals>
                           </execution>
                       </executions>
                       <configuration>
                           <prefix>git</prefix> <!-- that's the default value -->
                           <dateFormat>dd.MM.yyyy '@' HH:mm:ss z</dateFormat> <!-- that's the default value -->
                           <verbose>true</verbose> <!-- false is default for this -->
                           <dotGitDirectory>${project.basedir}/.git</dotGitDirectory> <!-- that's the default value, you may really want to change this sometimes (in multi module projects) :-) -->
                       </configuration>
                   </plugin>
                   <!-- END OF GIT COMMIT ID PLUGIN CONFIGURATION -->

                   <!-- other plugins -->
               </plugins>
           </build>
       </project>

Based on the above part of a working POM you should be able to figure out the rest, I mean you are a maven user after all... ;-)
Note that the resources filtering is important for this plugin to work, don't omit it!

Now you just have to include such a properties file in your project under `/src/main/resources` (and call it **git.properties** for example) and maven will put the appropriate properties in the placeholders:

     git.branch=${git.branch}

     git.build.user.name=${git.build.user.name}
     git.build.user.email=${git.build.user.email}
     git.build.time=${git.build.time}

     git.commit.id=${git.commit.id}
     git.commit.user.name=${git.commit.user.name}
     git.commit.user.email=${git.commit.user.email}
     git.commit.message.full=${git.commit.message.full}
     git.commit.message.short=${git.commit.message.short}
     git.commit.time=${git.commit.time}

The `git` prefix may be configured in the plugin declaration above.

Maven resource filtering + Spring = GitRepositoryState Bean
-----------------------------------------------------------
You'll most probably want to wire these plugins somehow to get easy access to them during runtime. We'll use spring as an example of doing this.
Start out with with adding the above steps to your project, next paste this **git-bean.xml** into the `/src/main/resources/` directory (or any other, just adjust the paths later on):

    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">

        <bean name="gitRepositoryInformation" class="pl.project13.maven.example.git.GitRepositoryState">
            <property name="branch" value="${git.branch}"/>
            <property name="commitId" value="${git.commit.id}"/>
            <property name="commitTime" value="${git.commit.time}"/>
            <property name="buildUserName" value="${git.build.user.name}"/>
            <property name="buildUserEmail" value="${git.build.user.email}"/>
            <property name="commitMessageFull" value="${git.commit.message.full}"/>
            <property name="commitMessageShort" value="${git.commit.message.short}"/>
            <property name="commitUserName" value="${git.commit.user.name}"/>
            <property name="commitUserEmail" value="${git.commit.user.email}"/>
        </bean>

    </beans>

And here's the source of the bean we're binding here:

     package pl.project13.maven.example.git;
     
     import org.codehaus.jackson.annotate.JsonWriteNullProperties;
     
     /**
      * A spring controlled bean that will be injected
      * with properties about the repository state at build time.
      * This information is supplied by my plugin - <b>pl.project13.maven.git-commit-id-plugin</b>
      *
      * @author Konrad Malawski
      */
     @JsonWriteNullProperties(true)
     public class GitRepositoryState {
       String branch;                  // =${git.branch}
       String commitId;                // =${git.commit.id}
       String buildUserName;           // =${git.build.user.name}
       String buildUserEmail;          // =${git.build.user.email}
       String buildTime;               // =${git.build.time}
       String commitUserName;          // =${git.commit.user.name}
       String commitUserEmail;         // =${git.commit.user.email}
       String commitMessageFull;       // =${git.commit.message.full}
       String commitMessageShort;      // =${git.commit.message.short}
       String commitTime;              // =${git.commit.time}

       public GitRepositoryState() {
       }
       /* Generate setters and getters here */
      }

The source for it is also on the repo of this plugin. Of course, *feel free to drop out the jackson annotation* if you won't be using it.

The last configuration related thing we need to do is to load up this bean in your appContext, so open up your **applicationContext.xml** or whatever you call it in your project and add these lines in the <beans/> section:

    <context:property-placeholder location="classpath:*.properties" />
    <import resource="classpath:/git-bean.xml"/>

Of course, you may adjust the paths and file locations as you please, no problems here... :-)
*Now you're ready to use your GitRepositoryState Bean!* Let's create an sample **Spring MVC Controller** to test it out:

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

Don't mind the createMAV and responses stuff, it's just example code. And feel free to use constructor injection, it's actually a better idea ;-)

In the end *this is what this service would return*:

     {
         "branch" : "testing-maven-git-plugin",
         "commitTime" : "06.01.1970 @ 16:16:26 CET",
         "commitId" : "787e39f61f99110e74deed68ab9093088d64b969",
         "commitUserName" : "Konrad Malawski",
         "commitUserEmail" : "konrad.malawski@java.pl",
         "commitMessageFull" : "releasing my fun plugin :-)
                                + fixed some typos
                                + cleaned up directory structure
                                + added license etc",
         "commitMessageShort" : "releasing my fun plugin :-)",
         "buildTime" : "06.01.1970 @ 16:17:53 CET",
         "buildUserName" : "Konrad Malawski",
         "buildUserEmail" : "konrad.malawski@java.pl"
     }

That's all folks! **Happy hacking!**

Configuration details
=====================
Just a short recap of the available parameters...

Optional parameters:

* **dotGitDirectory** - `(default: ${project.basedir}/.git)` the location of your .git folder. `${project.basedir}/.git` is the default value and will most probably be ok for single module projects, in other cases please use `../` to get higher up in the dir tree. An example would be: `${project.basedir}/../.git` which I'm currently using in my projects :-)
* **prefix** - `(default: git)` is the "namespace" for all exposed properties
* **dateFormat** - `(default: dd.MM.yyyy '@' HH:mm:ss z)` is a normal SimpleDateFormat String and will be used to represent git.build.time and git.commit.time
* **verbose** - `(default: false)` if true the plugin will print a summary of all collected properties when it's done

License
=======
<img style="float:right; padding:3px; " src="https://github.com/ktoso/maven-git-commit-id-plugin/raw/master/lgplv3-147x51.png" alt="GNU LGPL v3"/>
I'm releasing this plugin under the **GNU Lesser General Public License 3.0**.

You're free to use it as you wish, the license text is attached in the LICENSE file.
You may contact me if you want this to be released on a different license, just send me an email `konrad.malawski@java.pl` :-)