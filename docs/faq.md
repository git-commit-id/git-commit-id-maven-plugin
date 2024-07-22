Frequently Asked Question (FAQ)
=========

Plugin options and configurations
-------------------------------
This plugin is around for quite some time and has various options and configurations.
The most up-to-date documentation about the various options can be found in the [plugin itself](https://github.com/git-commit-id/git-commit-id-maven-plugin/blob/master/src/main/java/pl/project13/maven/git/GitCommitIdMojo.java#L105).

On top of the various options that can be configured in the `pom.xml` the plugin has the following command-line arguments:
* `-Dmaven.gitcommitid.skip=true` - skip the plugin execution
* `-Dmaven.gitcommitid.nativegit=true` - by default this plugin will use the `jgit` implementation to interact with your git repository, specify this command-line option to use the native `git` binary instead
Note that all command-line options can also be configured in the `pom.xml`.


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

If you are using the Maven build with [Maven's Plugin Prefix Resolution](https://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html) (e.g. `mvn somePrefix:goal`) please note that this currently seems to be [not supported by Maven](https://issues.apache.org/jira/browse/MNG-6260).
Instead of using the Plugin Prefix Resolution add an execution tag that calls the desired goal of the plugin within a normal Maven life cycle (e.g. `mvn clean package`).

Generated properties are not being used in install and/or deploy
-------------------------------
If you try to use generated properties like `${git.commit.id}` alongside with your artifact finalName you will soon notice that those properties are not being used in install and/or deploy.
This specific behaviour is basically **not intended / not supported** by maven-install-plugin and/or maven-deploy-plugin (https://issues.apache.org/jira/browse/MINSTALL-1 / https://issues.apache.org/jira/browse/MDEPLOY-93). The naming format in the remote repo seems to be always `$artifactId-$version-$classifier` *by default* and thus any generated property will not end up inside the artifact being installed/deployed.

If you for whatever reason still want to have something special you may want to 
[checkout a full project](https://github.com/git-commit-id/git-commit-id-maven-plugin/files/14440383/example-594.zip) that uses a specific configuration of `install-file` / `deploy-file` (you may also view the comment in [issue 594](https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/594#issuecomment-1970003328). The intial instructions are from [issue 256](https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/256#issuecomment-321476196).

In a nutshell the workaround disables the default install and default deploy and replaces it with a "custom" install and a "custom" deploy (the one that deploys your custom named artifact).
Depending on the target location where you deploy the file to you might need add an extra dependency (as outlined in https://maven.apache.org/plugins/maven-site-plugin/examples/adding-deploy-protocol.html). Supported protocols are mentioned in https://maven.apache.org/wagon/.
However I need to stress that this **might not be something fully supported** or intended by the maven eco-system. So use this at your **own risk**.


As a general note also ensure to use `mvn clean deploy` instead of `mvn deploy:deploy` (or you run into  https://issues.apache.org/jira/browse/MNG-6260) and ensure to set `<injectAllReactorProjects>true</injectAllReactorProjects>` inside the plugin's config.
