# Access Version Info At Runtime
This file demonstrates multiple ways to access at runtime the git version info that was exported at buildtime.

## Generate A Java Source File with Compile Time Constants
This strategy generates a Java source file from a template and writes it to the `generated-sources` directory within the `target` directory. This is useful for avoiding runtime injection and/or lookup from properties files.  

Add the [templating-maven-plugin](https://github.com/mojohaus/templating-maven-plugin) to your pom.xml:
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>templating-maven-plugin</artifactId>
    <version>3.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>filter-sources</goal>
            </goals>
            <phase>generate-sources</phase>
        </execution>
    </executions>
</plugin>
```

Add the template file to `src/main/java-templates`:
```java
package com.example.demo;

public interface Version {
	String TAGS = "${git.tags}";
	String BRANCH = "${git.branch}";
	String DIRTY = "${git.dirty}";
	String REMOTE_ORIGIN_URL = "${git.remote.origin.url}";

	String COMMIT_ID = "${git.commit.id.full}";
	String COMMIT_ID_ABBREV = "${git.commit.id.abbrev}";
	String DESCRIBE = "${git.commit.id.describe}";
	String DESCRIBE_SHORT = "${git.commit.id.describe-short}";
	String COMMIT_USER_NAME = "${git.commit.user.name}";
	String COMMIT_USER_EMAIL = "${git.commit.user.email}";
	String COMMIT_MESSAGE_FULL = "${git.commit.message.full}";
	String COMMIT_MESSAGE_SHORT = "${git.commit.message.short}";
	String COMMIT_TIME = "${git.commit.time}";
	String CLOSEST_TAG_NAME = "${git.closest.tag.name}";
	String CLOSEST_TAG_COMMIT_COUNT = "${git.closest.tag.commit.count}";

	String BUILD_USER_NAME = "${git.build.user.name}";
	String BUILD_USER_EMAIL = "${git.build.user.email}";
	String BUILD_TIME = "${git.build.time}";
	String BUILD_HOST = "${git.build.host}";
	String BUILD_VERSION = "${git.build.version}";
	String BUILD_NUMBER = "${git.build.number}";
	String BUILD_NUMBER_UNIQUE = "${git.build.number.unique}";
}
```
Use the same package declaration as your program's entry point, presumably in `src/main/java`.  
This example would have a relative path of `src/main/java-templates/com/example/demo/Version.java`.

Use the version info as you would any other constant:
```java
package com.example.demo;

public class Main {
    public static void main(String[] args) {
        System.out.println("Version: " + Version.COMMIT_ID);
    }
}
```

## Export a `git.properties` File Inside the Build Artifact
This strategy writes a `git.properties` file to the build artifact, and reads it at runtime.  
The file will be written at buildtime and can be inspected in the build artifact.  

Ensure the plugin is configured to generate the `git.properties` file:
```xml
<configuration>
    <!-- Enable generation of git.properties file -->
    <generateGitPropertiesFile>true</generateGitPropertiesFile>
    <!-- Specify the path to write the properties file to -->
    <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties</generateGitPropertiesFilename>
</configuration>
```

Include code to read the `git.properties` file at runtime and parse it:
```java
package com.example.demo;

import java.io.IOException;
import java.util.Properties;

public final class Version {
	public static final String TAGS;
	public static final String BRANCH;
	public static final String DIRTY;
	public static final String REMOTE_ORIGIN_URL;

	public static final String COMMIT_ID;
	public static final String COMMIT_ID_ABBREV;
	public static final String DESCRIBE;
	public static final String DESCRIBE_SHORT;
	public static final String COMMIT_USER_NAME;
	public static final String COMMIT_USER_EMAIL;
	public static final String COMMIT_MESSAGE_FULL;
	public static final String COMMIT_MESSAGE_SHORT;
	public static final String COMMIT_TIME;
	public static final String CLOSEST_TAG_NAME;
	public static final String CLOSEST_TAG_COMMIT_COUNT;

	public static final String BUILD_USER_NAME;
	public static final String BUILD_USER_EMAIL;
	public static final String BUILD_TIME;
	public static final String BUILD_HOST;
	public static final String BUILD_VERSION;
	public static final String BUILD_NUMBER;
	public static final String BUILD_NUMBER_UNIQUE;

	static {
		try {
			Properties properties = new Properties();
			properties.load(Version2.class.getClassLoader().getResourceAsStream("git.properties"));

			TAGS = String.valueOf(properties.get("git.tags"));
			BRANCH = String.valueOf(properties.get("git.branch"));
			DIRTY = String.valueOf(properties.get("git.dirty"));
			REMOTE_ORIGIN_URL = String.valueOf(properties.get("git.remote.origin.url"));

			COMMIT_ID = String.valueOf(properties.get("git.commit.id.full")); // OR properties.get("git.commit.id") depending on your configuration
			COMMIT_ID_ABBREV = String.valueOf(properties.get("git.commit.id.abbrev"));
			DESCRIBE = String.valueOf(properties.get("git.commit.id.describe"));
			DESCRIBE_SHORT = String.valueOf(properties.get("git.commit.id.describe-short"));
			COMMIT_USER_NAME = String.valueOf(properties.get("git.commit.user.name"));
			COMMIT_USER_EMAIL = String.valueOf(properties.get("git.commit.user.email"));
			COMMIT_MESSAGE_FULL = String.valueOf(properties.get("git.commit.message.full"));
			COMMIT_MESSAGE_SHORT = String.valueOf(properties.get("git.commit.message.short"));
			COMMIT_TIME = String.valueOf(properties.get("git.commit.time"));
			CLOSEST_TAG_NAME = String.valueOf(properties.get("git.closest.tag.name"));
			CLOSEST_TAG_COMMIT_COUNT = String.valueOf(properties.get("git.closest.tag.commit.count"));

			BUILD_USER_NAME = String.valueOf(properties.get("git.build.user.name"));
			BUILD_USER_EMAIL = String.valueOf(properties.get("git.build.user.email"));
			BUILD_TIME = String.valueOf(properties.get("git.build.time"));
			BUILD_HOST = String.valueOf(properties.get("git.build.host"));
			BUILD_VERSION = String.valueOf(properties.get("git.build.version"));
			BUILD_NUMBER = String.valueOf(properties.get("git.build.number"));
			BUILD_NUMBER_UNIQUE = String.valueOf(properties.get("git.build.number.unique"));
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Version() {}
}
```

Use the version info as you would any other constant:
```java
package com.example.demo;

public class Main {
    public static void main(String[] args) {
        System.out.println("Version: " + Version.COMMIT_ID);
    }
}
```
