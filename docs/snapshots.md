# Getting SNAPSHOT versions of the plugin
If you really want to use **snapshots**, here's the repository they are deployed to.
But I highly recommend using only stable versions, from Maven Central... :-)

```xml
<pluginRepositories>
    <pluginRepository>
        <id>sonatype-snapshots</id>
        <name>Sonatype Snapshots</name>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
    </pluginRepository>
</pluginRepositories>
```

Older Snapshots (prior version 5.X) are available via `<url>https://oss.sonatype.org/content/repositories/snapshots/</url>`.
