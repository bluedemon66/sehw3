# 文件层 API 运行练习

从`res/pages/1001.properties`读取真实页面清单，使用项目现有值对象解释编号，再读取布局、中文文字及缓存内容。教程见[实现手册第2、3节](../../implementation-guide.md)，代码为[FileLayerWalkthrough.java](FileLayerWalkthrough.java)。

## 编译与运行

在`sehw3`根目录执行：

```powershell
# Compile the existing project model with the configured local Maven entry point.
powershell -NoProfile -ExecutionPolicy Bypass -File tools/maven.ps1 -B -ntp -DskipTests compile

# Compile the standalone exercise with the project's model classes.
New-Item -ItemType Directory -Force -Path target/file-layer-demo | Out-Null
javac --release 17 -encoding UTF-8 -cp target/classes -d target/file-layer-demo doc/examples/file-layer/FileLayerWalkthrough.java

# Run the exercise; all generated files stay under target.
java '-Dstdout.encoding=UTF-8' -cp 'target/file-layer-demo;target/classes' edu.nju.sehw.takeout.examples.FileLayerWalkthrough doc/examples/file-layer/res target/file-layer-demo/cache
```

成功时最后一行是`checks=14`。检查覆盖清单转换、数字地址映射、布局读取、UTF-8、缺失页面、路径构造、缓存字节、索引重载，以及错误数字、空清单、目录读取和未知资源区。

运行后打开`target/file-layer-demo/cache/index.properties`及`target/file-layer-demo/cache/0/1`，观察索引和资源内容。重复运行得到相同的业务输出。

## P.S. 运行范围

上述命令面向当前Windows环境；其它系统的classpath分隔符使用`:`。练习使用现有项目模型，读写演示输出，不修改`src/main/java`中的存根。缓存样例会覆盖自己生成的固定示例文件，生产缓存的编号分配、并发和恢复策略见教程末尾。
