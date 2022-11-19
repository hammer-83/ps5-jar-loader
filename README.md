# PlayStation 5 Remote JAR Loader
This project uses vulnerabilities discovered in BD-J layer of PS5 firmware version 4.51 and earlier to deploy a loader that is able to listen to JAR files and execute their main class.
This makes it easy to burn the BD-R disc with the loader just once and then keep on running new versions of the experimental code.
This repository provides all the necessary setup needed to create both the loader BD-R disc filesystem and the JAR to send to the PS5.

## Prerequisites
* JDK 11 (PS5 uses Java 11 runtime)
* Apache Maven
* IntelliJ IDEA Community Edition (optional, but recommended)

## Structure
The project comprises the following components:
* Root `pom.xml` defines the common properties and Maven plugin configuration for all the projects.
* `assembly` subproject creates the directory that should be burned to a BD-R disc. I recommend `ImgBurn` software to do this. Make sure to use the UDF 2.50 filesystem, then simply drag the contents of `assembly/target/assembly-[version]` directory to in the disc layout editor.
* `bdj-tools` subproject does not need to be touched. These are the utilities from HD Cookbook, adapted to run on JDK 11 and integrated into the build process of the BD-R disc filesystem.
* `stubs` subproject contains the build script to download BD-J class files from HD Cookbook and organize them for use with local JDK 11. It's also a place where PS5-specific stub files should be declared so that they can be used in the Xlet and the remote JAR.
* `xlet` subproject contains the code of the Xlet that starts when BD-R disc is launched on PS5. It simply starts the JAR loader (by default on port 9025).
* `xploit` subject contains the code to be sent for execution on PS5. The code can reference classes from `xlet`, such as the [Status](xlet/src/main/java/org/ps5jb/loader/Status.java) class to output on screen. The project produces a JAR that is able to send itself for execution.

## Configuration
The following properties in [pom.xml](pom.xml) can be adjusted before burning the JAR Loader to disk:
* `loader.port` - Port on which JAR loader will listen for data.
* `loader.resolution.width`, `loader.resolution.height` - Screen resolution to set in various files. Not sure how this affects anything, I did not experiment with this enough.
* `remote.logger.host` - IP address where to echo the messages shown on screen. If blank, remote logging will not be used. This host can also receive binary data, see [RemoteLogger#sendBytes](xlet/src/main/java/org/ps5jb/loader/RemoteLogger.java).
* `remote.logger.port` - Port on which remote logger will send the status messages.
* `remote.logger.timeout` - Number of milliseconds to wait before abandoning attempts to connect to the remote logging host. If host is down after this timeout on the first send attempt, no further tries to do remote logging will be done.

Either modify the POM directly, or pass the new values from command line, example: `mvn ... -Dloader.port=9025`. To listen for remote messages, use `socat udp-recv:[remote.logger.port] stdout`.

## Usage
1. Make sure environment variable `JAVA_HOME` points to the root of JDK 11. Add `${JAVA_HOME}/bin` directory to `${PATH}`.
2. Also make sure that `MAVEN_HOME` points to the root of Apache Maven installation. Add `${MAVEN_HOME}/bin` directory to `${PATH}`.
3. Execute `mvn package` from the root of the project. It should produce the following artifacts:
    * Directory `assembly/target/assembly-[version]` contains all the files that should be burned to the BD-R.
    * File `xploit/target/xploit-[version].jar` contains the code that can be sent repeatedly to the PS5 once the loader is deployed.
4. **IMPORTANT:** Maven-compiler-plugin has a bug that causes an NPE if unpatched. Execution of step #3 will likely fail on the first run. To fix the issue, replace the plugin in your local Maven repository with the patched version located in [lib](lib/maven-compiler-plugin-3.10.1.jar). Normally the replacement goes to `${HOME}/.m2/repository/org/apache/maven/plugins/maven-compiler-plugin/3.10.1`.   
5. Burn the BD-R (better yet BD-RE), then insert it into the PS5 and launch.
6. A message on screen should inform about loader waiting for JAR.
7. Send the JAR using the command: `java --add-opens java.base/jdk.internal.loader=ALL-UNNAMED -jar xploit/target/xploit-[version].jar <ps5 ip address> [<ps5 port]`. PS5 should inform on screen about status of the upload and the execution.
8. Once execution is complete, the loader will wait for a new JAR. Do the necessary modifications in `xploit` project, recompile using `mvn package` and re-execute #7 to retry.

## Notes
1. To use with IntelliJ, simply point `File -> Open` dialog to the root of the project.
2. If any of POMs are modified, it's necessary to do `Maven -> Reload Project` in IntelliJ to sync the project files. Syncing Maven project unfortunately modifies [.idea/compiler.xml](.idea/compiler.xml) to contain absolute system paths. Simply replace those with `$PROJECT_DIR$` macro again.
3. Project should be built once from command-line before attempting to open in IntelliJ. This is so that bdjstack JARs are downloaded. 
4. Javadoc plugin is integrated into the build, but it is bound to the `verify` phase so that `package` phase is not slowed down. To generate the Javadoc, use `mvn verify` instead of `mvn package`.
5. If you prefer Maven not to rescan all the subprojects for changes (it's a few seconds at most), use `mvn install` to put all the artifacts into your local maven repo. Then run all `mvn package` commands from [xploit](xploit) directory rather than from the root of the project.

## Credits
There are so many who decided to share the knowledge with the community to make this project possible. Please see the Credits section in the [Webkit PS5 Exploit repo](https://github.com/Cryptogenic/PS5-IPV6-Kernel-Exploit#contributors--special-thanks). None of this would be possible without all these contributors. Additionally, big thanks to [psxdev](https://github.com/psxdev) and [John Törnblom](https://github.com/john-tornblom) for their work specifically on BD-J.
