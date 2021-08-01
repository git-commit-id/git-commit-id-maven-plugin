Use cases
=========

Which version had the bug? Is that deployed already?
----------------------------------------------------
If you develop your maven project inside an git repository you may want to know exactly what changeset is currently deployed. Why is this useful? 

I worked in a team where the testers would come up to the development team and say: "hey, feature X is still broken!", to which a dev would reply "But I fixed it this morning!". Then they'd investigate a bit, only to see that the next version which would be deployed very soon included the needed fix, yet the developer already marked it as "ready for testing".

The fix here is obvious: include the version you fixed some bug in the issue comment where you mark it as "ready for testing". You can either do this via smart tooling (recommended), or just manually put in a comment like "fixed in v1.4.3-324-g45xhbghv" (that's a git-describe output - explained in detail bellow), so the testing crew knows it doesn't make sense to pickup testing of this feature until at least "324" (or greater) is included in the version output (it means "number of commits away from the mentioned tag" - readup on git-describe to understand how it works).

Make your distributed deployment aware of versions
---------------------------------------------
Let's say you have a large distributed deployment where the servers need to talk to each other using some protocol. You have them configured to keep talking with servers of the same major + minor version number. So a server running "3.3.233" may still talk with one that has "3.3.120" - the protocol is guaranteed to not have changed in these versions.

And now imagine that you need to deploy a drastic API change - so the new version of the servers will be "3.4.0". You can't afford to bring the system down for the deployment. But as the servers are configured to only talk with compatible versions - you're in luck. You can start the deployment process and each node, one by one will be replaced with the new version - the old servers simply stop communicating with them, and the new versions start talking with each other until only the "new" nodes are left.

Using this plugin, you can easily expose the information needed - based on git tags for example. 
One might say that this is usually accomplished by using `${project.version}` and I generally would agree, but maybe tags would fit your use case better than a plain version. :-)

Validate if properties are set as expected
---------------------------------------------
Since version **2.2.2** the git-commit-id-maven-plugin comes equipped with an additional validation utility which can be used to verify if your project properties are set as you would like to have them set.
The validation can be used for *any* property that is visible inside the pom.xml and therefore can be used for a various amount of different use cases.
In order to understand the ideology and intention here are some pretty useful ideas you could achieve by using the validation:
* validate if the version of your project does not end with SNAPSHOT
* validate if you are currently on a tag
* ensure that your repository is not dirty
* may other's :-)

With the current version of the validation the user can decide if the build should fail if *at least one* of the defined criteria do not match with the desired values.

For flexibility and due to the fact that this validation has a different scope than the git-commit-id-maven-plugin this validation needs to be configured as additional execution inside the configuration of the pom.xml.
Once configured, the validation is executed during the verification-phase. However since the validation is done in a separate execution the phase can easily be changed by adding the desired phase to the execution configuration.

Note that this feature needs to be enabled properly, before it can be used. Checkout the `Validation Usage Example` in the [using the plugin](using-the-plugin.md) guide to find out more.

Other
-----
If you have a nice use case to share, please do fork this file and file a pull request with your story :-)

