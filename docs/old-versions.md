# Old Versions

## Relocation of the Project
Older versions (4.x.x or older) are available via:
```xml
<groupId>pl.project13.maven</groupId>
<artifactId>git-commit-id-plugin</artifactId>
```

## Minimum Java Version
Here is an overview of the current plugin compatibility with Java

| Plugin Version  | Required Java Version |
| --------------- | ---------------------:|
| 2.1.X           | Java 1.6              |
| 2.2.X           | Java 1.7              |
| 3.X.X           | Java 1.8              |
| 4.X.X           | Java 1.8              |
| 5.X.X           | Java 11               |
| 6.X.X           | Java 11               |
| 7.X.X           | Java 11               |
| 8.X.X           | Java 11               |
| 9.X.X           | Java 11               |


## Minimum Maven Version
Even though this plugin tries to be compatible with every Maven version there are some known limitations with specific versions. Here is a list that tries to outline the current state of the art:

| Plugin Version |               Minimal Required Maven version               |
|----------------|:----------------------------------------------------------:|
| 2.1.X          | Maven 2.2.1 up to v2.1.13; Maven 3.1.1 for any later 2.1.X |
| 2.2.X          |  Maven 3.1.1 up to v2.2.3; Maven 3.0 for any later 2.2.X   |
| 3.X.X          |                         Maven 3.0                          |
| 4.X.X          |                         Maven 3.0                          |
| 5.X.X          |                    Maven 3.1.0-alpha-1                     |
| 6.X.X          |                    Maven 3.1.0-alpha-1                     |
| 7.X.X          |                        Maven 3.2.5                         |
| 8.X.X          |                        Maven 3.2.5                         |
| 9.X.X          |                        Maven 3.6.3                         |

Flipping the table to maven:
Please note that in theory maven 4.X should support all maven 3 plugins.
The plugin was first shipped with maven 3 support in version v2.1.14 (requiring maven version 3.1.1).
Hence the v2.1.14 should be the first supported version.
Only starting with 6.X.X this plugin was actually tested with 4.0.0-alpha-5,
but some releases might not work since Maven 4 announced that plugins require Maven 3.2.5 or later
which would only be the case for plugin versions 7.0.0 or later.

| Maven Version |  Plugin Version |                       Notes                        |
|---------------|----------------:|:--------------------------------------------------:|
| Maven 3.X     |             any | The plugin requires at least a maven 3.1.0-alpha-1 |
| Maven 4.X     |    from v2.1.14 |                                                    |


## Plugin compatibility with EOL Maven version
End of life (EOL) Maven versions are no longer supported by Maven, nor this plugin.
The following information is made available for reference.

| Maven Version               | Plugin Version  | Notes                                                                                                           |
| --------------------------- | ---------------:|:---------------------------------------------------------------------------------------------------------------:|
| Maven 2.0.11                | up to 2.2.6     | Maven 2 is EOL, git-commit-id-plugin:1.0 doesn't work -- requires maven version 2.2.1                           |
| Maven 2.2.1                 | up to 2.2.6     | Maven 2 is EOL                                                                                                  |
| Maven 3.0.X                 | up to 4.0.5     | git-commit-id-plugin:2.1.14, 2.1.15, 2.2.0, 2.2.1, 2.2.3  doesn't work  -- requires maven version 3.1.1         |
| Maven 3.0.X                 | up to 4.0.5     | For git-commit-id-plugin 2.2.4 or higher: works, but failed to load class "org.slf4j.impl.StaticLoggerBinder"   |
| Maven 3.1.0                 | any             | git-commit-id-plugin:2.1.14, 2.1.15, 2.2.0, 2.2.1, 2.2.3 doesn't work -- requires maven version 3.1.1           |
| Maven 3.3.1                 | any             | git-commit-id-plugin:2.1.14 doesn't work                                                                        |
| Maven 3.3.3                 | any             | git-commit-id-plugin:2.1.14 doesn't work                                                                        |

Note:
As an example -- this table should be read as: For `Maven 3.1.0` `any` Plugin Version should work, besides the ones listed in the `Notes` have the limitations listed.
