# 外卖点餐与配送系统 Java 框架

项目沿用实验二的分层、Core消息中转和服务器任务池设计。当前交付包括package与import组织、接口与数据契约、主要类和private策略框架，以及可执行的基础契约测试。

[![sehw3 CI](https://github.com/bluedemon66/sehw1/actions/workflows/sehw3-ci.yml/badge.svg)](https://github.com/bluedemon66/sehw1/actions/workflows/sehw3-ci.yml)

课程仓库副本位于`sehw1/lab3/`。push与PR会自动运行构建、测试、风格和Javadoc检查，并保存报告、JAR与API文档；日常同步和查看结果见[CI说明](doc/ci.md)。

使用Maven单模块工程，Java语言目标为17；本机使用已有的`D:\jdk` 23编译。Maven Wrapper固定3.9.11，测试采用JUnit 5，包依赖通过ArchUnit校验。

## 编译与测试

在本目录执行：

```powershell
# Local entry point; dependency and temporary files stay on drive E.
powershell -NoProfile -ExecutionPolicy Bypass -File tools/maven.ps1 verify

# Run the Core messaging demonstration.
java -jar target/takeout-system-0.1.0-SNAPSHOT.jar

# Apply Google Java formatting after editing source files.
powershell -NoProfile -ExecutionPolicy Bypass -File tools/maven.ps1 fmt:format

# Generate and strictly validate the English API documentation.
powershell -NoProfile -ExecutionPolicy Bypass -File tools/javadoc.ps1
```

IDE直接打开`pom.xml`导入工程，源码位于`src/main/java`，JUnit测试位于`src/test/java`。通用Maven入口为`mvnw.cmd`或`sh ./mvnw`；本机脚本复用已有Maven发行版，将新依赖缓存定位到`E:\softwares\sehw3-tools\maven-repository`。

源码注释和Javadoc使用英文。类说明职责，构造器与方法说明参数、返回值和异常；private策略也提供说明。生成后的API入口为`target/apidocs/index.html`。

## 阅读顺序

1. [包结构与UML映射](doc/architecture.md)：从实验二类名找到Java文件。
2. [接口规约](doc/interface-contracts.md)：参数、返回、请求关联及事务责任。
3. [实现指南](doc/implementation-guide.md)：按类依赖安排实现顺序、阶段用例和验收。
4. [设计调整](doc/design-changes.md)：本轮对实验二设计的具体修改与原因。
5. [测试说明](doc/testing.md)：测试入口、校验内容和后续业务用例。
6. [验证结果](doc/verification.md)：本轮构建、测试、格式及运行结果。
7. [Git与CI](doc/ci.md)：同步到课程仓库、自动检查和构建产物下载。

实现业务时，按实现指南先完成本地资源和页面流程，再接入评论查询、事务写入、网络和图片。`EventProcessor`与`PageProcessor`的private策略随用例逐项补充。`app.FrameworkDemo`演示Core注册、授权路由、消息快照和接收。

## P.S. 当前阶段与扩展

- 业务处理器、认证、HTTP、JDBC和文件存储的执行方法保留`NotImplementedYetException`存根；已实现的内容集中于消息值对象、Core、编号目录和有界任务队列。
- 当前测试验证已实现的基础契约。数据库竞争、业务权限、真实网络及UI交互随对应实现补充；源文件数量与存根行数分别统计，最终实现规模在完成业务后核算。
- 新增业务时，登记操作编号、实现`MessagePayload.deepCopy()`、添加处理器策略和Core路由，再补服务器目录与测试。端口允许替换HTTP、数据库、存储和UI线程适配器。
- 工具脚本中的E盘路径服务于当前电脑；其他环境可使用标准Wrapper和自己的`-Dmaven.repo.local`。现有JDK与Maven安装保持原配置。
- `doc`保存工程设计与验证说明；实验三报告按课程要求由本人撰写。
