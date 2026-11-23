# 外卖点餐与配送系统

本项目把实验二的 UML 设计实现为 Java 17 Maven 工程。客户端通过 Core 完成界面事件、页面装配和异步请求回送。服务端通过 HTTPS 接收请求，由有界队列和 Worker 调度，再由 H2 事务处理订单、工单、评论和图片。

## 构建与验证

在本目录执行以下命令。本机可使用 `D:\jdk`，其它环境可通过 Maven Wrapper 使用自己的 JDK。

```powershell
$env:JAVA_HOME = 'D:\jdk'
powershell -NoProfile -ExecutionPolicy Bypass -File tools/maven.ps1 verify
powershell -NoProfile -ExecutionPolicy Bypass -File tools/javadoc.ps1
```

`verify` 运行单元测试、HTTPS 和 H2 集成测试、架构规则、格式检查及 Checkstyle，并将 H2 打入 `target/takeout-system-0.1.0-SNAPSHOT.jar`。[API 文档](target/apidocs/index.html) 由 Javadoc 命令生成。[测试报告](target/surefire-reports/) 由 Maven 生成。

`fmt:format` 可统一主源码、测试和文件层示例的 Java 格式。`verify` 对这些文件执行 Checkstyle 的 Google Java 规则和项目附加规则。两套规则报告分别位于 `target/checkstyle-result.xml` 和 `target/checkstyle-project-result.xml`。

## 在本机运行

在项目目录创建一组 localhost 测试证书与四个角色会话。以下命令在首次运行时执行。

```powershell
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
$keytool = Join-Path $env:JAVA_HOME 'bin/keytool.exe'
$env:SEHW3_TLS_PASSWORD = [guid]::NewGuid().ToString('N')
foreach ($role in 'CUSTOMER', 'MERCHANT', 'RIDER', 'MODERATOR') {
    Set-Item -Path "Env:SEHW3_${role}_TOKEN" -Value ([guid]::NewGuid().ToString('N'))
}
New-Item -ItemType Directory -Force target/demo-tls | Out-Null
& $keytool -genkeypair -alias localhost -keyalg EC -storetype PKCS12 -keystore target/demo-tls/server.p12 -storepass:env SEHW3_TLS_PASSWORD -dname CN=localhost -ext SAN=DNS:localhost,IP:127.0.0.1 -validity 7 -noprompt
& $keytool -exportcert -alias localhost -keystore target/demo-tls/server.p12 -storepass:env SEHW3_TLS_PASSWORD -file target/demo-tls/localhost.cer
& $keytool -importcert -alias localhost -storetype PKCS12 -keystore target/demo-tls/trust.p12 -storepass:env SEHW3_TLS_PASSWORD -file target/demo-tls/localhost.cer -noprompt
```

在同一个 PowerShell 会话启动服务端与客户端。服务端初始化示例门店、菜单和骑手分配，随后在 localhost 的 8443 端口监听。

```powershell
$jar = 'target/takeout-system-0.1.0-SNAPSHOT.jar'
$server = Start-Job -ArgumentList (Get-Location).Path, $java, $jar -ScriptBlock {
    param($workspace, $java, $jar)
    Set-Location -LiteralPath $workspace
    & $java -cp $jar edu.nju.sehw.takeout.app.ServerApplication target/demo-tls/server.p12
}
Receive-Job $server -Keep
& $java -cp $jar edu.nju.sehw.takeout.app.ClientConsole https://localhost:8443 target/demo-tls/trust.p12 CUSTOMER
```

客户端打开首页后，使用 `help` 查看命令。输入 `quit` 后，可以用 `MERCHANT`、`RIDER` 或 `MODERATOR` 重启客户端。全部结束后运行 `Stop-Job $server` 和 `Remove-Job $server`。

- 客户用 `place 2` 下单。返回元素 21 是订单 ID，元素 23 是版本。商家依次执行 `accept <订单ID> 0`、`prepare <订单ID> 1`、`ready <订单ID> 2`。骑手执行 `pickup <订单ID> 3` 和 `deliver <订单ID> 4`。客户在版本 5 执行 `confirm` 或 `missing`。选择 `missing` 后会返回供平台结案的工单 ID。
- 客户用 `publish <正文>` 发布门店评论，也可以输入 `publish <正文> | <图片路径>` 带一张图片。用 `list LONG_FIRST` 或 `list IMAGE_FIRST` 查看排序，用 `next` 翻页。商家可以回复，作者可以撤回，平台可以隐藏。
- `detail`、`turnover` 和 `tasks` 从服务器加载订单详情、商家营业额和骑手任务。原有 Core 演示仍可用 `java -jar` 运行。

[实现手册](doc/implementation-guide.md) 按八阶段讲解各层算法。[接口规约](doc/interface-contracts.md) 描述消息、路由与事务边界。

## P.S.

- 控制台客户端使用单独的 UI 线程。骑手由服务端按门店配置分配，营业额累计已确认送达的订单。结案记录保留在本地工单中。
- 数据、下载缓存和本地 TLS 文件位于 `target`。角色会话及证书用于 localhost 演示。图片接受 PNG、JPEG 或 GIF，单文件上限 5 MiB。
- [课程仓库同步说明](doc/ci.md) 适用于 `sehw1/lab3/`。这里的本地实现和测试对应当前目录。
