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

If you are using the Maven build with [Maven's Plugin Prefix Resolution](https://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html) (e.g. `mvn somePrefix:goal`) please note that this currently seems to be [not supported by Maven](https://issues.apache.org/jira/browse/MNG-6260).
Instead of using the Plugin Prefix Resolution add an execution tag that calls the desired goal of the plugin within a normal Maven life cycle (e.g. `mvn clean package`).

Generated properties are not being used in install and/or deploy
-------------------------------
If you try to use generated properties like `${git.commit.id}` alongside with your artifact finalName you will soon notice that those properties are not being used in install and/or deploy. This specific behaviour is basically not intended / not supported by maven-install-plugin and/or maven-deploy-plugin (https://issues.apache.org/jira/browse/MINSTALL-1 / https://issues.apache.org/jira/browse/MDEPLOY-93). The naming format in the remote repo seems to be always `$artifactId-$version-$classifier` *by default* and thus any generated property will not end up inside the artifact being installed/deployed. If you for whatever reason still want to have something special you may want to [checkout a full workaround](https://github.com/git-commit-id/maven-git-commit-id-plugin/issues/256#issuecomment-321476196) that uses a specific configuration of `install-file` / `deploy-file`.

As a general note also ensure to use `mvn clean deploy` instead of `mvn deploy:deploy` (or you run into  https://issues.apache.org/jira/browse/MNG-6260) and ensure to set `<injectAllReactorProjects>true</injectAllReactorProjects>` inside the plugin's config.
