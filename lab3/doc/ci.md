# Git提交与自动检查

Java工程在`sehw3`中编辑，通过同步工具更新课程仓库`bluedemon66/sehw1`的`lab3/`。仓库根目录的`.github/workflows/sehw3-ci.yml`负责自动构建。

## 每次提交的流程

1. 在`sehw3`修改代码，运行本地测试和格式检查。
2. 在`sehw3`执行下面的同步命令。
3. 到`sehw1`检查`git status`和`git diff`，提交本次改动并推送。
4. 在[GitHub Actions](https://github.com/bluedemon66/sehw1/actions/workflows/sehw3-ci.yml)查看本次提交的`Java verify`结果。

```powershell
# Export the working project into the existing course repository.
powershell -NoProfile -ExecutionPolicy Bypass -File tools/sync-repository.ps1 -RepositoryPath ../sehw1
```

同步清单在源目录`sync/SYNC.json`与仓库`lab3/SYNC.json`各保存一份，记录每个文件的源指纹、仓库副本指纹和换行转换。`sync/SYNC.md`提供批次概览。

## CI执行内容

修改`lab3/`或工作流后，push和PR都会触发检查；也可以在Actions页面选择`Run workflow`手动运行。同一分支或PR的新运行会替代之前仍在执行的运行。

工作流使用Ubuntu 24.04、Temurin JDK 23.0.1和Wrapper固定的Maven 3.9.11，Java编译目标保持17。依赖缓存在云端runner的Maven目录。

检查顺序为：

1. `sh ./mvnw --batch-mode --no-transfer-progress clean verify`：编译、JUnit、ArchUnit、Google Java格式检查和Checkstyle。
2. 执行框架JAR，检查Core通信演示。
3. 执行`tools/javadoc.ps1`，校验并生成英文API文档。
4. 保存测试及风格报告，成功时保存JAR和API文档。

在对应运行的`Artifacts`区域下载`sehw3-reports-*`、`sehw3-jar-*`和`sehw3-apidocs-*`，保留时间为14天。报告中的Surefire XML给出用例、失败与异常明细；失败步骤的日志提供定位信息。

CI调用现有`verify`中的检查配置。修改代码格式后，先在本地执行`fmt:format`，再提交格式化结果。

## 工作流文件与维护

工作流源文件位于`sync/sehw3-ci.yml`，同步后放到课程仓库根目录。修改JDK、触发条件或artifact配置时，更新源文件并同步提交。使用的Actions版本固定到实际提交SHA。

- [GitHub Maven CI说明](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven)
- [setup-java](https://github.com/actions/setup-java)
- [upload-artifact](https://github.com/actions/upload-artifact)

## P.S. 当前阶段

本阶段启用自动验证和构建产物保存。合并门槛、tag发布及服务器部署在对应流程确定后接入。CI检查已有用例覆盖的行为；新增业务同时补充相应测试。同步工具新增和更新副本；删除源文件后，在课程仓库检查并执行对应`git rm`。本机`tools/maven.ps1`使用E盘缓存，云端使用标准Wrapper。
