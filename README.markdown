Maven plugin: git-commit-id-plugin
==================================
git-commit-id-plugin is a plugin quite similar to https://fisheye.codehaus.org/browse/mojo/tags/buildnumber-maven-plugin-1.0-beta-4 fo example
but as buildnumber only supports svn (which is very sad) and cvs (which is even more sad, and makes bunnies cry) I had to quickly develop
an git version of such a plugin. For those who don't know the previous plugins, let me explain what this plugin does:

If you develop your maven project inside an git repository (which you hopefully already are docing) you may want to know exactly
what changeset is currently deployed online. Why is this useful? Well, the tester won't come to you screaming "heeey that bug ain't fixed"
of course you'd reply "but I fixed it this morning!" and after some searching you notice "oh... it'll be online after the next deployment, sorry tester... :-(".
This scenario keeps repeating sometimes, thus you can state which commit fixes/closes the bug, note this in JIRA etc and then the tester will know if it's already online (by the commit date for example).

Usage
=====
It's really simple to setup this plugin, here's a sample pom that you may base your pom on:

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
                       <version>1.0-SNAPSHOT</version>
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
                           <dotGitDirectory>${project.basedir}/../.git</dotGitDirectory> <!-- required, you have to specify this path -->
                       </configuration>
                   </plugin>
                   <!-- END OF GIT COMMIT ID PLUGIN CONFIGURATION -->

                   <plugin>
                       <groupId>org.mortbay.jetty</groupId>
                       <artifactId>maven-jetty-plugin</artifactId>
                       <version>6.1.10</version>
                       <configuration>
                           <contextPath>/management-ws</contextPath>
                           <stopKey>stop</stopKey>
                           <stopPort/>
                       </configuration>
                   </plugin>
                   <plugin>
                       <groupId>org.apache.maven.plugins</groupId>
                       <artifactId>maven-war-plugin</artifactId>
                       <version>2.1.1</version>
                       <configuration>
                           <warName>management-ws</warName>
                       </configuration>
                   </plugin>
               </plugins>
           </build>
       </project>

Based on the above part of a working POM you should be able to figure out the rest, I mean you are a maven user after all... ;-)
Note that the resources filtering is important for this plugin to work, don't omit it!
