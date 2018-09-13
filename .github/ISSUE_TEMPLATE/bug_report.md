---
name: Bug report
about: Create a bug report to help us fix an issue

---

### Describe the bug
Please provide a brief summary of the issue and provide a summarised description in the title above.

### Steps to Reproduce
- Please include the **full** configuration of the plugin

```xml
<plugin>
...
</plugin>
```

- Is there a (public) project where this issue can be reproduced? If so please provide a link.
- Include any stack-traces or any error messages

### Expected behavior
- Please provide a description of what you expected to happen.

### Additional context
For reproducibility please provide the following:

- the plugin version is being used (if not included in the configuration)
- the Java-Version is being used (output of ``java -version`` and also include details about oracle-jdk VS open-jdk)
- the Maven-Version is being used (output of ``mvn --version``)
- on what Operating System you experience the bug (on Linux run ``lsb_release -a`` or ``cat /etc/*release*``)
- in what context maven is being executed (e.g. inside Windows Terminal, Powershell, Git Bash, /bin/bash, ...)
- how maven is being executed (e.g. ``mvn clean deploy`` VS ``mvn deploy:deploy``)

Feel free to add any other context or screenshots about the bug.
