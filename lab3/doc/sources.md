# 工程参考

设计依据为相邻sehw2项目的03至06、08至14号工程图及模块说明。Java文件的包、接口与参数调整见[设计调整](design-changes.md)。

- [Maven Surefire的JUnit Platform配置](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html)：测试执行入口。
- [JUnit 5.13.4用户指南](https://docs.junit.org/5.13.4/user-guide/)：断言、测试生命周期和参数化测试。
- [ArchUnit用户指南](https://www.archunit.org/userguide/html/000_Index.html)：基于字节码的包依赖和架构约束。
- [fmt-maven-plugin](https://github.com/spotify/fmt-maven-plugin)：调用google-java-format，显式格式化与构建时检查分开执行。
- [Javadoc注释规范](https://docs.oracle.com/en/java/javase/23/docs/specs/javadoc/doc-comment-spec.html)：类型、方法、record参数和返回值标签的写法。
- [Javadoc命令说明](https://docs.oracle.com/en/java/javase/23/docs/specs/man/javadoc.html)：文档生成、编码和DocLint校验选项。

项目版本固定在pom.xml及Wrapper配置中。Checkstyle启用命名、显式import、花括号、行宽等规则，完整排版由固定版本的Google Java格式器检查。

## P.S. 报告引用

本页记录工程使用的资料入口；实验报告由本人按课程要求撰写，并按实际使用情况自行引用。
