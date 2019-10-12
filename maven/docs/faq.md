Frequently Asked Question (FAQ)
=========

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

Generated properties are not being used in install and/or deploy
-------------------------------
If you try to use generated properties like ${git.commit.id} alongside with your artificat finalName you will soon notice that those properties are not being used in install and/or deploy. This specific behaviour is basically not intended / not supported by maven-install-plugin and/or maven-deploy-plugin (https://issues.apache.org/jira/browse/MINSTALL-1 / https://issues.apache.org/jira/browse/MDEPLOY-93). The naming format in the remote repo seems always $artifactId-$version-$classifier *by default* and thus any generated property will not end up inside the artifact being installed/deployed. If you for whatever reason still want to have something special you may want to [checkout a full workaround](https://github.com/git-commit-id/maven-git-commit-id-plugin/issues/256#issuecomment-321476196) that uses a specific configuration of `install-file` / `deploy-file`.

As a general note also ensure to use `mvn clean deploy` instead of `mvn deploy:deploy` (or you run into  https://issues.apache.org/jira/browse/MNG-6260) and ensure to set `<injectAllReactorProjects>true</injectAllReactorProjects>` inside the plugin's config.

How to contribute to the project
-------------------------------
In general pull requests and support for open issues is always welcome!
If you open a pull request or want to help out in any way please keep in mind that we currently use `checkstyle` to ensure that the project somehow maintains a uniform code base. Fortunately the most common IDEs support the integration of `checkstyle` via plugins. On top of more or less native integration into modern development tools the `checkstyle` rules are currently also being verified by using the [maven-checkstyle-plugin](https://maven.apache.org/plugins/maven-checkstyle-plugin/) during the build. it should be worth to highlight that you are not required to install `checkstyle` inside your IDE. If you feel more comfortable running the checks via maven this would to the trick!
The checkstyle rules for this project are currently residing in `src/test/resources/checks` and for some IDEs those rules need to be imported manually (unfortunately there is no better solution available for this yet). The current rule set is pretty much the same as the `google_checks.xml` with certain checks disabled (e.g. line length). If you choose to integrate `checkstyle` inside your IDE feel free to checkout some high level requirements to get started with checkstyle within your IDE:
* eclipse -- for eclipse / STS you would need to install the `checkstyle plug-in` via `Help -> Eclipse Marketplace -> Search` after restarting eclipse it should pick-up the rules automatically. If this does not work out of the box checkout the [official integration guide](http://checkstyle.sourceforge.net/eclipse.html) or use google to trace down your error message.
* IntelliJ IDEA -- for IntelliJ you would need to install the `CheckStyle-IDEA` via `File -> Settings -> Plugins -> Search`. After restarting IntelliJ you would need to import the `checkstyle` rules manually via `File -> Settings -> Checkstyle`. As checkstyle version you may choose `8.2` and then click on the plus-sign on the right. As description you may choose `maven-git-commit-id-plugin` and as local checkstyle file you may choose one of the checkstyle rule residing in `src/test/resources/checks`. Please note that the rule-file depend on the version you have selected in the previous step and thus it is essential to ensure that the version numbers match up. As next-step you unfortunately will be prompted to enter the **full directory** of the `checkstyle-suppressions.xml`-File. If this does not work out of the box checkout the [official integration guide](http://checkstyle.sourceforge.net/idea.html) or use google to trace down your error message.
* Netbeans -- feel free to open a ticket and share your installation guide :-) You can also check out the [official integration guide](http://checkstyle.sourceforge.net/netbeans.html) or use google to get any addtional help.
* Maven -- if you want to run `checkstyle` via maven you simply can execute `mvn clean verify -Pcheckstyle -Dmaven.test.skip=true -B`.

