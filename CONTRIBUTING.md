# Contributing
In general pull requests and support for open issues is always welcome!

## Project layout
This project is a multi-module Maven project. It consists of the following modules:
- [core](core) (`git-commit-id-plugin-core`): The core framework of the plugin
- [maven](maven) (`git-commit-id-plugin`): The actual plugin, which depends on the `core` module

To build the project:
1. Install Maven
2. Install a JDK meeting the minimum requirements specified by Maven and in the [README](README.md)
3. Run the following command in the main directory of the project:
```
mvn clean verify
```
This will build the project and run all tests.

## Checkstyle
If you open a pull request or want to help out in any way please keep in mind that we currently use Checkstyle to ensure that the project somehow maintains a uniform code base. Fortunately the most common IDEs support the integration of Checkstyle via plugins. On top of more or less native integration into modern development tools the Checkstyle rules are currently also being verified by using the [maven-checkstyle-plugin](https://maven.apache.org/plugins/maven-checkstyle-plugin/) during the build. It should be worth to highlight that you are not required to install Checkstyle inside your IDE. If you feel more comfortable running the checks via Maven this would do the trick!
The Checkstyle rules for this project are currently residing in `.github/.checkstyle/` and for some IDEs those rules need to be imported manually (unfortunately there is no better solution available for this yet). The current rule set is pretty much the same as the `google_checks.xml` with certain checks disabled (e.g. line length). If you choose to integrate Checkstyle inside your IDE feel free to checkout some high level requirements to get started with Checkstyle within your IDE:
* Eclipse – for Eclipse / STS you would need to install the `Checkstyle Plug-in` via `Help -> Eclipse Marketplace -> Search` after restarting Eclipse it should pick-up the rules automatically.
* IntelliJ IDEA – for IntelliJ you would need to install the `CheckStyle-IDEA` plugin via `File -> Settings -> Plugins -> Search`. After restarting IntelliJ you would need to import the Checkstyle rules manually via `File -> Settings -> Checkstyle`. As Checkstyle version you may choose `8.2` and then click on the plus-sign on the right. As description you may choose `maven-git-commit-id-plugin` and as local Checkstyle file you may choose one of the Checkstyle rules residing in `.github/.checkstyle/`. Please note that the rule-file depends on the version you have selected in the previous step and thus it is essential to ensure that the version numbers match up. As next step you unfortunately will be prompted to enter the **full directory** of the `checkstyle-suppressions.xml`-file.
* NetBeans – feel free to open an issue and share your installation guide :-)
* Maven – if you want to run Checkstyle via Maven you simply can execute `mvn clean verify -Pcheckstyle -Dmaven.test.skip=true -B`.
