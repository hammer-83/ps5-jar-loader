# PlayStation 5 远程 JAR 加载器
该项目利用 PS5 固件版本 7.61 及更早版本中发现的 BD-J 层漏洞来部署一个能够监听 JAR 文件并执行其主类的加载器。
这使得只需使用加载程序刻录一次 BD-R 光盘然后继续运行实验代码的新版本变得容易。
该存储库提供了创建加载器 BD-R 光盘文件系统和发送到 PS5 的 JAR 所需的所有必要设置。

快速入门
1.下载 JAR Loader ISO 版本。
2. 将其刻录到 BD-R(E) 光盘并从 PS5“媒体”选项卡运行它。
3. 下载一个预编译的 JAR，或者按照以下步骤编译您自己的 JAR。
4.使用 NetCat 将 JAR 发送到 JAR Loader，或者如果机器安装了 Java，则使用 JAR 文件本身：`java -jar [jarfile].jar [ip] [host]`。

## 先决条件
* JDK 11（PS5 使用 Java 11 运行时）
* Apache Maven
* IntelliJ IDEA 社区版（可选，但推荐）

##  结构
该项目包括以下部分：
* 根“pom.xml”定义所有项目的公共属性和Maven插件配置。
* `assembly` 子项目创建应刻录到 BD-R 光盘的目录。我推荐使用 `ImgBurn` 软件来执行此操作。确保使用 UDF 2.50 文件系统，然后只需将 `assembly/target/assembly-[version]` 目录的内容拖到光盘布局编辑器中即可。
* 无需触及 `bdj-tools` 子项目。这些是来自 HD Cookbook 的实用程序，适合在 JDK 11 上运行，并集成到 BD-R 光盘文件系统的构建过程中。
*`stubs`子项目包含从 HD Cookbook 下载 BD-J 类文件并组织它们以供本地 JDK 11 使用构建脚本。它也是应声明 PS5 特定的存根文件的地方，以便它们可以在 Xlet 和远程 JAR 中使用。
* `sdk` 子项目包含辅助类，可简化执行代码中的本机调用。此模块中的类嵌入在最终的 JAR 中，将发送到 PS5 执行。
* `xlet` 子项目包含在 PS5 上启动 BD-R 光盘时启动的 Xlet 的代码。它只是启动 JAR 加载器（默认在端口 9025 上）。
* `xploit` 子项目包含要发送到 PS5 执行的各种有效负载。每个有效负载都是 `xploit` 模块的一个子模块，并生成自己的 JAR 文件。JAR 中的代码可以引用来自 `xlet` 的类，例如 [Status](xlet/src/main/java/org/ps5jb/loader/Status.java) 类以在屏幕上输出。
    * `jar` - 与 JAR 加载器交互的实用程序类。它本身不是有效负载，但打包在每个有效负载 JAR 中，以处理从 JAR 加载器到有效负载的 `run` 方法的交接。
    * `umtx/umtx1` - 实施 UMTX 漏洞以获取内核读/写功能。请注意，从固件 6.00 开始，内核数据段受到写入保护。
    * `umtx/umtx2`——UMTX 漏洞的替代实现。
    * `byepervisor`——Byepervisor 漏洞的实现，可以绕过 PS5 管理程序以获取 3.00 以下固件上的内核代码读/写功能。
    * `kerneldump` - 与 UMTX 和/或 Byepervisor 结合，此有效负载通过网络发送内核转储。
    * `ftpserver`——有限的 FTP 服务器。
    * `samples`——各种简单的示例，用于展示 BD-J 平台的各种功能。

##  配置
在编译并将 JAR Loader 刻录到磁盘之前，可以调整 [xlet/pom.xml](xlet/pom.xml) 中的以下属性：
* `loader.port` – JAR 加载器将监听数据的端口。
* `loader.resolution.width`, `loader.resolution.height` - 在各种文件中设置的屏幕分辨率。不确定这会有什么影响，我对此没有进行足够的实验。
* `loader.logger.host` - 屏幕上显示消息的 IP 地址。如果为空，则不使用远程日志记录。此主机还可以接收二进制数据，请参阅 [RemoteLogger#sendBytes](xlet/src/main/java/org/ps5jb/loader/RemoteLogger.java)。
* `loader.logger.port` – 远程记录器将发送状态消息的端口。
* `loader.logger.timeout` - 放弃尝试连接远程日志记录主机之前等待的毫秒数。如果主机在第一次发送尝试后超时关闭，则不会再尝试进行远程日志记录。
* `loader.payload.root` - 可以将 JAR 有效负载包含到磁盘程序集中（见下文）。此配置参数指定相对于放置有效负载的磁盘根目录的路径。

可以直接修改 POM，也可以从命令行传递新值，例如：`mvn clean package -Dloader.port=9025 -Dloader.logger.host=192.168.1.100`。要在远程记录器激活时监听远程计算机上的消息，请使用`socat udp-recv:[remote.logger.port] stdout`。

即使远程记录器在刻录光盘的 Xlet 中默认未处于活动状态，也可以使用以下两种方法之一来更改远程服务器配置：
1. 编译 JAR 时指定 `xploit.logger.host` 和可选的 `xploit.logger.port` 属性。这些可以在 [xploit/pom.xml](xploit/pom.xml) 中或在命令行 `mvn clean package -Dxploit.logger.host=192.168.1.110` 中设置。
2. 通过在 JAR 有效负载中以编程方式调用 [Status#resetLogger](xlet/src/main/java/org/ps5jb/loader/Status.java)。

##  用法
1. 确保环境变量“JAVA_HOME”指向 JDK 11 的根目录。将“${JAVA_HOME}/bin”目录添加到“${PATH}”。
2. 还要确保 `MAVEN_HOME` 指向 Apache Maven 安装的根目录。将 `${MAVEN_HOME}/bin` 目录添加到 `${PATH}`。
3. 要创建有效载荷，请按照以下步骤操作：
   * 复制整个目录并将其放在 [xploit](xploit) 目录中，以制作其中一个示例有效负载的副本。
   * 在新有效载荷的 `pom.xml` 中，将父级的 `artifactId` 设置为“xploit”，将模块的 `groupId` 设置为“org.ps5jb.xploit”，并将模块的 `artifactId` 设置为有效载荷的名称。
   * 在新模块的“org.ps5jb.client.payloads”包中创建一个实现“Runnable”接口的类。“run”方法中的代码将成为有效负载的入口点。
   * 回到 `pom.xml`，将属性 `xploit.payload` 设置为上述类的名称。如果该类是在子包中创建的，则需要该类的完全限定名称。否则，只需指定类的名称而不指定包。
4. 从项目根目录执行“mvn clean package”。它应该会产生以下产物：
   * 目录“assembly/target/assembly-[version]”包含所有应刻录到 BD-R 光盘的文件。
   * 文件“xploit/[payload]/target/[payload]-[version].jar”包含加载器部署后可重复发送到 PS5 的代码。
   可以将生成的有效负载 JAR 包含到光盘程序集中，以便从菜单而不是远程加载。为此，请在编译时激活配置文件“xploitOnDisc”，例如“mvn clean package -P xploitOnDisc”。
5. 使用步骤 4a 中提到的目录中的内容刻录 BD-R（最好是 BD-RE）。请注意，仅当 [xlet](xlet) 或 [assembly](assembly) 模块的来源发生更改，或者有效载荷包含在上一步中的光盘组件中时，才需要重新刻录 JAR 加载器光盘。
6. 将光盘插入 PS5 并从媒体/光盘播放器启动“PS5 JAR Loader”。
7. 屏幕上应出现一条消息，告知加载器正在等待 JAR，或者如果在磁盘上找到有效负载，则会显示菜单。
8. 对于远程执行，使用以下命令发送 JAR：
   ```壳
   java -jar xploit/[payload]/target/[payload]-[版本].jar <ps5 ip 地址>`
   ```
   PS5 应该在屏幕上告知上传和执行的状态。
9. 远程执行完成后，加载程序将等待新的 JAR。在“xploit”项目中进行必要的修改，使用“mvn package”重新编译，然后重新执行步骤 8，根据需要重试多次。

注释
1. 要与 IntelliJ 一起使用，请将“文件 -> 打开”对话框指向项目的根目录。将发生 Maven 导入。然后按照 [IntelliJ 项目结构](#intellij-project-structure) 中的手动步骤调整依赖项，以便 IntelliJ 能够先于 JDK 类看到 BD-J 类。
2. 如果修改了任何 POM，则需要在 IntelliJ 中执行`Maven -> Reload Project` 来同步项目文件。
3. 要生成 Javadocs，请使用 `mvn verify`，而不是 `mvn package`。Javadocs 已为 [sdk](sdk)、[xlet](xlet) 和 [xploit](xploit) 模块启用，并在每个模块的 `target/site/apidocs` 目录中生成。
4. 要运行单元测试，请使用“mvn test”。每次编译时也会自动运行测试。要跳过它们，请在命令行上添加“-DskipTests”属性。请注意，由于许多功能依赖于 PS5，因此目前没有太多单元测试。
5. 如果 `xploit` JAR 没有 PS5 特定的依赖项，则可以在本地进行测试。重要的是将 `xlet`、`stubs` 和 `xploit` JAR 都放在同一个文件夹中。如果有效负载引用 GEM、BD-J 或 Java TV API，则在 [lib](lib) 目录中生成的相应 JAR 文件也应存在于同一文件夹中。Maven build 会自动在每个有效负载的 `target` 目录中创建此安排，因此在开发机器上运行有效负载的命令与将 JAR 发送到 PS5 的命令非常相似：
   ```壳
   java -jar xploit/[有效载荷]/目标/[有效载荷]-[版本].jar
   ```
   在本地运行时，“Status”类会打印到标准输出/错误，而不是“Screen”。
6. 项目当前使用两个独立的版本号：
   * `xlet` 版本是独立的，只有在需要使用更新的 JAR 加载器类刻录新光盘时才会增加。如果 PS5 显示的版本与此 repo 代码生成的版本不同，则不能保证有效负载兼容，因此最好刻录新的加载器光盘。由于加载器非常稳定，因此预计此版本不会经常增加。要增加此版本，请更改 [pom.xml](pom.xml) 中 `xlet.version` 属性的值。
   * 其余模块使用父 POM 中的版本。此版本将随着新版本的发布而增加，并反映 SDK 或有效负载已发生更改。如果加载程序版本保持不变，则这些新版本的有效负载仍可发送到 JAR 加载程序，而无需重新刻录光盘。可以通过执行“mvn 版本：set -DnewVersion=[version]”来增加此版本，然后刷新 IntelliJ Maven 项目，如要点 2 中所述。

IntelliJ 项目结构
IntelliJ Maven 项目文件位于 IntelliJ 的私有本地文件夹中。首次打开 Maven 项目并随后重新加载时会错误地导入某些设置。特别是，BD-J 堆栈 JAR 会被完全忽略或以错误范围导入。不幸的是，由于这个事实，每次重新加载 Maven 项目时都需要执行以下步骤：
* 同步 Maven 项目会修改 [.idea/compiler.xml](.idea/compiler.xml) 以包含绝对系统路径。只需再次用 `$PROJECT_DIR$` 宏替换它们即可。
* 进入“项目结构”窗口并切换到“模块”选项卡。检查每个模块并确保模块“bdj-api”、“javatv-api”和“gem-api”具有“提供”范围。
* 此外，对于所有具有上述依赖项的模块，单击 `+ (添加) -> 库` 按钮并添加 `bdjstack` 库依赖项。确保它被移动到 SDK 11 条目上方的顶部位置。此设置过去已提交到版本控制，可以简单地恢复，但在最近的更新中，每次都必须执行此操作。

## 致谢
很多人决定与社区分享知识，以使这个项目成为可能。
- [Andy "theflow" Nguyen](https://github.com/theofficialflow) 发现并分享了 BD-J 漏洞和本机执行技术，如果没有这些技术，本 repo 中的所有工作都不可能实现。
- Specter 的 PS5 内核访问 Webkit 实现可作为 Java 实现的基础：[IPV6](https://github.com/Cryptogenic/PS5-IPV6-Kernel-Exploit)、[UMTX](https://github.com/PS5Dev/PS5-UMTX-Jailbreak/) 和 [Byepervisor](https://github.com/PS5Dev/Byepervisor)。
- [Flat_z](https://github.com/flatz) 几乎涵盖了自 PS3 以来 PlayStation 领域发生的所有重要事件，包括此 repo 中包含的 UMTX 开发策略和基于 AGC 的内核 r/w。
- [Cheburek3000](https://github.com/cheburek3000) 贡献了 UMTX 开发的一种替代实现。
- [bigboss](https://github.com/psxdev) 和 [John Törnblom](https://github.com/john-tornblom) 在 BD-J 领域的工作。
- [iakdev](https://github.com/iakdev) 为菜单加载器做出贡献。
- CryoNumb 提供想法、代码贡献并帮助测试各种固件版本。
- Specter 的 Webkit 实现的所有其他贡献者：[ChendoChap](https://github.com/ChendoChap)、[Znullptr](https://twitter.com/Znullptr)、[sleirsgoevy](https://twitter.com/sleirsgoevy)、[zecoxao](https://twitter.com/notnotzecoxao)、[SocracticBliss](https://twitter.com/SocraticBliss)、SlidyBat、[idlesauce](https://github.com/idlesauce)、[fail0verflow](https://fail0verflow.com/blog/) [kiwidog](https://kiwidog.me/)、[EchoStretch](https://github.com/EchoStretch)、[LM](https://github.com/LightningMods)。
- 各个固件版本的测试人员：Jamdoogen、Twan322、MSZ_MGS、KingMaxLeo、RdSklld、Ishaan、Kirua、PLK、Benja44_、MisaAmane、Unai G、Leo。

此存储库中的示例 BD-J 有效负载是以下工作的改编版：
- 由 [pReya](https://github.com/pReya/ftpServer) 提供的 FTP 服务器。
- [Edu4Java](http://www.edu4java.com/en/game/game0-en.html) 的迷你网球。