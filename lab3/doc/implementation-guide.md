# 实验三实现手册：从页面读取到完整业务流程

本手册保留从框架开始的实现顺序。当前工程的运行入口和构建命令见 [README](../README.md)，各节示例也可以用来对照已实现的代码。

## 1 作业导览

在本次作业中，你将在现有Java框架上完成一个外卖点餐与配送系统。你已经设计了UI、Core消息中转、事件与页面策略、查询层和服务器任务池。现在要做的是把这些对象接起来：先让一个页面显示本地内容，再让它取得服务器数据，随后完成订单状态变化、评论与图片。

这份手册按实现任务排列。每个任务先说明最终要得到什么，再介绍它用到的对象和API，接着解释待实现方法、处理步骤和验收结果。完成一节后，将它的结果交给下一节使用。

本次实现沿下面的过程推进：

```text
页面号 → 页面清单 → 本地资源 → 完整页面
                            ↓
UI操作 → Core → 事件／页面处理器 → 业务服务
                                     ↓
                              请求编码与身份验证
                                     ↓
                            数据库查询、事务与业务规则
                                     ↓
                       Worker、HTTPS、图片与实际UI
```

项目根目录包含`pom.xml`和`src/`，下文命令均在这个目录执行。Java文件的统一包前缀是`edu.nju.sehw.takeout`；例如`client.resource.FileUiLibrary`位于`src/main/java/edu/nju/sehw/takeout/client/resource/FileUiLibrary.java`。

现有的`Core`、`RelationTable`、`MessageParticipant`、`TaskPool`、`ServerOperationCatalog`以及共享值对象已经提供基础实现。你将在它们之上完成各业务类中的存根。

## 2 任务一：根据页面号取得页面清单

第一次调用可以很简单：输入页面号`1001`，返回这个页面使用的布局和文字资源。清单的内容进入Java对象后，后续页面处理器就可以使用它。

### 2.1 需要了解的对象和API

从返回结果开始看。`PageResourceManifest`代表一张页面的资源清单，它的`entries()`返回`List<ResourceDescriptor>`，`uiLayout()`返回第一条布局描述。构造它时传入资源列表，现有构造器会检查首项类别及元素编号唯一性。

列表中的`ResourceDescriptor`描述一个元素需要的资源。构造参数依次是`elementId`、可选本地引用、可选服务器资源编号和`ResourceKind`。例如布局元素的编号取`0`，文字元素取`11`。

本地引用由`LocalResourceRef`保存：`folderIndices()`返回一组整数，`entryIndex()`返回文件编号，`kind()`返回资源类别。资源类别使用已有枚举`UI_LAYOUT`、`IMAGE`、`TEXT`、`BINARY`。

这些值对象描述“页面需要什么”。`FileUiLibrary`负责从磁盘取得它们，它已经保存根路径`root`，对外提供：

```text
Optional<PageResourceManifest> findPage(int pageId)
```

下面是本任务实际使用的Java工具：

| API | 输入和结果 | 在本任务中的作用 |
| --- | --- | --- |
| `Path.resolve(String)` | 接收子路径，返回新的`Path` | 根据页面号组成清单地址 |
| `Files.newBufferedReader(Path, Charset)` | 打开文本文件，返回Reader | 按UTF-8读取清单 |
| `Properties.load(Reader)` | 把键值文本读进对象 | 解析`key=value`文件 |
| `Properties.getProperty(String)` | 按键取得字符串 | 读取每个字段 |
| `Integer.parseInt(String)` | 字符串转整数 | 解析数量和编号 |
| `ResourceKind.valueOf(String)` | 枚举名称转枚举值 | 解析资源类别 |

`Path`保存地址，`Files`执行实际读写；`Properties`负责理解键值格式。它们与项目值对象分别承担定位、读取、解析和结果表示的工作。[Path与文件API](https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/nio/file/Path.html)

### 2.2 本任务的输入格式

先使用一套明确的文件约定。`root`下有`pages`目录，页面1001对应`pages/1001.properties`：

```properties
entry.count=2
entry.0.elementId=0
entry.0.kind=UI_LAYOUT
entry.0.folders=0,2
entry.0.entryIndex=7
entry.1.elementId=11
entry.1.kind=TEXT
entry.1.folders=0,3
entry.1.entryIndex=2
```

`entry.count`给出记录数量。`entry.0`和`entry.1`分别描述布局与文字；`folders`保存引用中的整数序列，`entryIndex`保存文件编号。第二个任务会解释这些编号怎样找到文件。

以第一条为例，字段转换后形成：

```java
LocalResourceRef local =
    new LocalResourceRef(List.of(0, 2), 7, ResourceKind.UI_LAYOUT);
ResourceDescriptor descriptor =
    new ResourceDescriptor(
        0, Optional.of(local), Optional.empty(), ResourceKind.UI_LAYOUT);
```

另一条以相同方式构造，最后把二者放进列表，调用`new PageResourceManifest(entries)`。

### 2.3 你的任务：完成 findPage

现在打开`FileUiLibrary`。一次调用的处理步骤是：

1. 用`root.resolve("pages").resolve(pageId + ".properties")`组成文件地址。
2. 创建`Properties`，打开UTF-8 Reader并执行`load`。
3. 读取`entry.count`，根据数量循环处理每一条记录。
4. 在第`i`次循环中组成前缀`"entry." + i + "."`，读取该项字段。
5. 把编号转成整数，把`folders`按逗号拆分，再把每一段转成整数列表。
6. 构造`LocalResourceRef`和`ResourceDescriptor`，加入结果列表。
7. 构造`PageResourceManifest`并返回`Optional.of(manifest)`。

打开和读取文件的核心写法如下。`try (...)`会在这段处理结束时关闭Reader：

```java
Properties properties = new Properties();
try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
  properties.load(reader);
}
```

你可以在同一个类中增加`manifestPath(pageId)`、`readProperties(path)`、`parseEntry(properties, index)`等private辅助方法。公开入口组织整个过程，辅助方法分别完成一个具体步骤。

结果按照以下规则交给调用者：目标清单缺失时返回`Optional.empty()`；格式错误时指出字段并抛出`IllegalArgumentException`；I/O故障用`UncheckedIOException`保留路径和原异常。先捕获`NoSuchFileException`处理缺失，再处理其它`IOException`。

### 2.4 跟踪一个例子并验收

当`pageId=1001`时，路径规则找到清单；第一次循环得到布局引用`[0,2],7`，第二次得到文字引用`[0,3],2`。最终`entries().size()`是`2`，`uiLayout().kind()`是`UI_LAYOUT`。

为正式类补充这些用例：能读取两条记录；不存在的页面得到空结果；字段缺失能够指出键名；错误枚举和数字有明确结果；布局首项及元素编号唯一性检查生效。

## 3 任务二：根据引用读取和保存资源

第一步得到的清单告诉我们资源的编号。现在要把编号转成路径，取得真正的布局和文字，并学会把下载内容留下来。

### 3.1 需要了解的对象和API

`FileLocalResourceStore`保存两个根：`resRoot`存内置资源，`cacheRoot`存下载缓存。它实现三个方法：

| 方法 | 参数含义 | 返回结果 |
| --- | --- | --- |
| `Path resolve(LocalResourceRef ref)` | 本地编号引用 | 对应文件路径 |
| `boolean contains(ResourceDescriptor descriptor)` | 元素需要的资源及定位信息 | 是否已有可用本地文件 |
| `LocalResourceRef save(ResourceBlob resource)` | 下载资源的元数据与字节 | 保存后的本地引用 |

`ResourceBlob.reference()`取得`RemoteResourceRef`，其中有服务器资源编号、媒体类型和长度；`content()`取得字节数组。保存后上层使用的是`LocalResourceRef`，所以文件和定位信息都要留在本地。

| API | 结果 | 用途 |
| --- | --- | --- |
| `Files.isRegularFile(path)` | 布尔值 | 检查普通文件 |
| `Files.readString(path, UTF_8)` | `String` | 取得布局或文字内容 |
| `Files.readAllBytes(path)` | `byte[]` | 取得二进制内容 |
| `Files.createDirectories(parent)` | 建立所需目录 | 准备保存位置 |
| `Files.write(path, bytes)` | 写入字节 | 保存下载结果 |
| `Properties.setProperty/store/load` | 设置、保存和恢复键值 | 持久化缓存索引 |

### 3.2 你的任务：完成 resolve 和 contains

本手册使用下面的地址约定：引用的第一个目录编号选择资源区，`0`选择`resRoot`，`1`选择`cacheRoot`；余下数字组成目录；`entryIndex`作为文件名。

```text
[0,2], entryIndex=7  → resRoot/2/7
[0,3], entryIndex=2  → resRoot/3/2
[1,0], entryIndex=1  → cacheRoot/0/1
```

`resolve`先读取第一个编号，用`switch`选择根；接着遍历剩余编号，逐个`resolve`；最后接上文件编号。这个方法返回组合后的路径。

先让`contains`处理有`localRef`的情况：取出引用，调用`resolve`，再用`isRegularFile`生成结果。有服务器编号时，再查本地缓存索引取得对应引用，复用同样的检查。

准备`resRoot/2/7`作为HTML布局、`resRoot/3/2`作为UTF-8文字。输入第一条清单后，应当能读到布局内容和“欢迎来到首页”。

### 3.3 你的任务：完成 save

在`cacheRoot/index.properties`保存“服务器资源编号→本地引用”和下一可用编号。启动时读取索引，在内存中建立查找表；每次保存按下面的步骤处理：

1. 用`resource.reference().resourceId()`查已有映射；已有资源按约定的更新策略使用原编号，新资源分配新编号。
2. 根据类型表确定`ResourceKind`。本任务先处理`text/plain → TEXT`，然后扩展图片及其它二进制类型。
3. 构造指向缓存区的引用，例如`[1,0],1,TEXT`，通过`resolve`得到目标路径。
4. 创建父目录，写入`resource.content()`。
5. 更新并保存资源映射及下一编号，再返回引用。

写入动作本身是：

```java
Files.createDirectories(target.getParent());
Files.write(target, resource.content());
```

索引中的一个值可以采用`1,0|1|TEXT`：分别表示目录序列、文件编号和类别。这个文本表示与`LocalResourceRef`之间写一对编码／解码方法，启动后便能恢复相同引用。

### 3.4 运行与验收

前两项任务已经配有一个独立练习程序和实际文件。在项目根目录执行以下命令，便能先观察整个过程：

```powershell
# Compile the existing project models with the configured E-drive cache.
powershell -NoProfile -ExecutionPolicy Bypass -File tools/maven.ps1 -B -ntp -DskipTests compile

# Compile the file-layer exercise.
New-Item -ItemType Directory -Force -Path target/file-layer-demo | Out-Null
javac --release 17 -encoding UTF-8 -cp target/classes -d target/file-layer-demo doc/examples/file-layer/FileLayerWalkthrough.java

# Run the exercise and write its generated data under target.
java '-Dstdout.encoding=UTF-8' -cp 'target/file-layer-demo;target/classes' edu.nju.sehw.takeout.examples.FileLayerWalkthrough doc/examples/file-layer/res target/file-layer-demo/cache
```

其中`doc/examples/file-layer/res`就保存着前面展示的清单、布局与文字。成功输出为：

```text
manifest.entries=2
layout.file=2/7
text=欢迎来到首页
missing.page=true
cached.text=下载评论正文
checks=14
```

正式实现的验收再覆盖：两个服务器资源分配不同编号；同一资源按更新策略处理；重建存储对象后索引仍能恢复；文件缺失时`contains`产生正确结果。

## 4 任务三：把本地资源装成一张页面并交给UI

现在已经能取得清单和文件，接下来把这些结果变成一棵页面树，沿你设计的Core流程返回UI。第一次先打开页面1001。

### 4.1 需要了解的对象和API

从最终结果开始看。`UiPage`保存`pageId`、根操作编号`requestId`和`UiResourceNode root`。一个`UiResourceNode`保存元素编号、可选资源、可选字段值和子节点列表。`UiResourceForest`用`Map<Integer, UiResourceNode>`保存页面号到根节点的对应关系。

页面字段用`TextValue`、`DecimalValue`、`BooleanValue`表示；`ResourceBundle`用`Map<Integer, LocalResourceRef>`保存元素编号到资源的绑定。清单、资源和字段齐全后，可以构造`UiPageAssembly`，再生成`UiPage`。

Core相关的现有入口为：

- `register(id, endpoint)`：登记参与者实例。
- `allow(RouteRule)`：登记允许的发送者、目标、操作号及UI引用权限。
- `prepareMessage(...)`：参与者创建带编号和载荷的独立消息快照。
- `send(senderId, targetId, core, message)`：把消息交给Core分发。
- `getTargetRef(caller, targetId)`：取得授权的`UiDataPort`，调用`receivePage(page)`。

`Message`中的`requestId`标识一次请求，`correlationId`连接同一个UI操作的多个子请求。`RequestContext`保存根操作、来源UI、页面及参数。处理器收到子结果后，就根据这些编号回到对应的页面装配过程。

### 4.2 先组装对象与路由

启动代码先创建Core，再创建参与者，随后注册实例和路由。例如可以给UI、事件、页面、页面目录和资源服务分别分配编号`1`至`5`。

```text
1 UI → 2 EventProcessor → 3 PageProcessor
                            ↓
                   4 PageCatalogService
                   5 ResourceAcquisitionService
                            ↓
                   3 PageProcessor → 1 UI
```

请求和结果回传的方向都登记相应路由；页面处理器取得UI回传端口的路由打开`allowUiReturnRef`。具体编号集中放到启动配置中，各类由构造器取得自己的编号和Core。

为统一接收UI输入，在`OperationCode`中补一个内部操作`UI_INPUT(1000)`，用于UI到事件处理器的消息。已有的`OPEN_PAGE(2101)`用于事件处理器发起页面请求。

### 4.3 你的任务：完成目录、资源与页面处理

**`SessionPermissionChecker.check`**根据`ClientSession`和页面访问规则完成本地预检查。先让公共首页通过，受限页面按所需业务权限检查；后面远端业务继续使用服务器确认的主体。

**`PageCatalogService.parse/lookupPage`**接收`PageRequestPayload(1001)`，做本地检查，调用`FileUiLibrary.findPage`，将结果装成已有的`PageManifestPayload`交回页面处理器。

**`ResourceAcquisitionService.parse/acquire/collect`**接收资源需求，按每个描述符调用本地`contains`和`resolve`。把得到的引用按元素号放进映射，形成`ResourceBundlePayload`返回。先完成全部资源都在本地的分支。

**`PageProcessor`**为每个根操作保存装配进度。增加一个进度类型，记录上下文、已收到的清单、资源与字段，以及尚在等待的子请求编号；`pendingPages`使用这个类型保存等待中的状态。到齐后再构造现有的`UiPageAssembly`。

在`parse`中，按载荷类型区分页面请求、清单结果、资源结果和字段结果。`handlePage`根据页面号选private策略，先实现`handleHomePage`；`assemblePage`创建布局根节点和元素子节点；`returnToUi`取得对应的`UiDataPort`并交付页面。

页面树的第一版可以用一个布局根节点承载平铺子元素。`UiResourceNode`是不可变值对象，`UiResourceForest.bind`可使用递归辅助方法：找到目标元素后创建带新资源的节点，再逐层创建包含新子节点的父节点，最后替换该页面的根。`install`保存根，`findPage`通过`Map.get`返回可选结果。

### 4.4 你的任务：完成UI输入与事件策略

`UiInteractionController.receiveUiInput`建立根操作编号，保存为当前活动请求，将`UiInput`装进`UiInputPayload`发送。按钮与触控事件由页面、元素和动作编号组成，`EventProcessor`维护其到目标页面或业务事件的映射。

在`EventProcessor.parse`中先保存`RequestContext`，再进入`pageHandler`或`eventHandler`。打开首页时，`pageHandler`生成`PageRequestPayload(1001)`；取消订单时，后续任务会由`handleCancelOrder`生成订单请求。private事件与页面策略分别留在这两个处理器中。

`receivePage`按页面中的根请求编号匹配活动请求，安装树与资源绑定，然后通过`UiThreadDispatcher.dispatch`调度`render()`。先用记录任务的dispatcher和记录页面的接收端观察结果，再接实际UI控件。

### 4.5 跟踪与验收

给一次根操作取名`R`。UI发出打开1001的输入后，事件处理器把页面请求交给页面处理器；目录和资源结果都关联到`R`；页面装配成功后返回`UiPage(1001,R,root)`；UI匹配`R`并显示布局与文字。

验收这条完整链路，再测试连续打开页面A、B但A最后返回的情况：UI最终显示B；每个处理器都能找到并结束正确的上下文。一个子步骤失败时，用`ErrorPayload`通过Core交给UI的`parse`，显示失败并清理活动请求和等待记录。

## 5 任务四：建立请求编码和身份验证

本地页面流程已经成立。为了让评论查询到达服务器，需要让双方理解同一份报文，并从服务器会话中确定请求者身份。

### 5.1 需要了解的对象和API

`MessageCodec`提供`byte[] encode(Message)`和`Message decode(byte[])`。`SecureRequest`在消息外增加凭据、发送时间、请求编号和摘要；`SecureResponse`返回请求编号、`ResultStatus`和类型化结果载荷。

`DefaultRequestSecurity`使用`ClientSession`、编码器和`Clock`。服务器的`DefaultServerRequestVerifier`使用`ServerSessionStore`、编码器和Clock，返回`ServerRequestContext`，其中是服务器确认的主体、权限和请求编号。

先新增一个实现`ServerSessionStore`的内存会话表，以测试凭据查出`SessionPrincipal`。时间通过`clock.instant()`取得，摘要通过`MessageDigest.getInstance("SHA-256").digest(bytes)`计算。

### 5.2 你的任务：实现一份双方一致的编码

可以先用JDK的`ByteArrayOutputStream/DataOutputStream`写二进制字段，使用`DataInputStream`按同样顺序读回。先覆盖第一条评论列表请求涉及的载荷，再逐步增加类型。

编码格式依次写版本号、两个UUID、发送与接收编号、操作号、载荷类型号及载荷字段。UUID可以写成两个long；字符串先转UTF-8字节，再写长度与内容；列表先写数量再写各项。Map按固定键顺序写入。

为载荷类型建立显式编号表。比如`ListCommentPayload`包含`CommentSubject`、`CommentSort`、可选cursor和limit，就把这些字段逐项写出；解码分支读回后调用现有构造器。为报文外壳再增加一个编码组件，保存`SecureRequest`和`SecureResponse`各自的字段。

先验证同一消息的编码可重复，解码保留请求编号和参数，然后让双方用同一套消息字节计算摘要。

### 5.3 你的任务：实现请求准备与验证

`DefaultRequestSecurity.prepare`的过程是：编码消息，计算摘要，读取当前会话凭据与时间，构造`SecureRequest`。

`DefaultServerRequestVerifier.verify/validateEnvelope`依次检查外壳与消息的请求编号、查找会话、判断时间有效性、重新计算并比较摘要，然后根据会话中的主体和权限构造`ServerRequestContext`。

`DefaultServerPermissionChecker.check`先根据这个上下文检查操作所需权限与目标类别；具体对象的归属由后续业务处理器在事务中查询并检查。

### 5.4 验收

准备一个有效会话和固定Clock。正常报文得到预期主体；更换消息字节、使用未知凭据、设置过期时间和错误编号时得到对应失败结果。异常测试用同一格式的有效报文改变一个字段，便于定位检查步骤。

## 6 任务五：完成无图片评论列表的查询闭环

现在选择`COMMENT_LIST(4101)`完成第一条服务端业务。一次请求提供评价目标、排序、cursor和limit，服务器查询数据，再把评论列表装回页面。

### 6.1 需要了解的对象和API

`ListCommentPayload`保存`subject`、`sort`、可选cursor和limit。`CommentSubject`保存目标种类、目标ID、可选门店和订单关联。`CommentSort`有`LONG_FIRST`与`IMAGE_FIRST`。

`CommentRepository.listPage(TransactionContext, CommentQuery)`返回`CommentPage`，其中是`List<CommentView>`和可选下一页cursor。`CommentView`包含正文、购买标记、状态、图片及商家回复等显示信息。第一次先用空图片和回复列表构造它。

数据库相关的入口是：`DataSource.getConnection()`取得连接；`Connection.prepareStatement(sql)`准备语句；`setString/setInt`绑定参数；`executeQuery()`返回`ResultSet`；`next()`逐行移动，再用`getString/getInt`取得列。

### 6.2 准备一个可连接的数据库

本手册用嵌入式H2作为JDBC练习环境，数据库由服务器进程使用。到本任务时，在Maven依赖中加入下面的已发布版本：

```xml
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <version>2.5.252</version>
</dependency>
```

启动代码创建数据源并注入`JdbcDatabaseAdapter`：

```java
JdbcDataSource dataSource = new JdbcDataSource();
dataSource.setURL("jdbc:h2:./target/takeout-demo");
dataSource.setUser("sa");
```

`JdbcDataSource`来自`org.h2.jdbcx`。这个URL把练习数据库文件放在当前项目的`target`中。在IDE中以Maven工程启动相应入口，运行classpath包含H2驱动；交付程序包时一并纳入运行依赖。[H2版本与下载信息](https://h2database.com/html/download.html)、[数据源API](https://github.com/h2database/h2database/blob/master/h2/src/main/org/h2/jdbcx/JdbcDataSource.java)

先建立`comments`表，保存`comment_id`、`target_kind`、`target_id`、`author_id`、`author_label`、`body`、`body_length`、`image_count`、`purchase_mark`、`status`和`created_at`等列。插入同一目标的三条测试评论，正文长度分别为8、3、5，初始状态均为`PUBLISHED`。

### 6.3 你的任务：实现事务与仓储查询

`JdbcDatabaseAdapter.inTransaction`取得连接，调用`setAutoCommit(false)`，把连接包装成`TransactionContext`交给回调。回调成功时`commit`，失败时`rollback`并继续报告异常，最后释放连接。[JDBC连接与事务API](https://docs.oracle.com/en/java/javase/23/docs/api/java.sql/java/sql/Connection.html)

评论仓储使用传入的`transaction.connection()`执行领域SQL。`JdbcDatabaseAdapter.executeWithinTransaction`承担目录中登记的通用标量查询及CRUD操作，按操作编号选择预定义语句和参数绑定器。

`JdbcCommentRepository.listPage`先按目标和`PUBLISHED`筛选。`LONG_FIRST`按正文长度、时间、ID建立稳定顺序；`IMAGE_FIRST`先按有图标记，再按正文长度、时间、ID排序。用显式分支选择固定的排序SQL。

第一轮查询第一页，随后为cursor保存上次最后一项的排序键；下一页使用相同顺序及键值继续查询。逐行构造`CommentView`，再形成`CommentPage`。

一个参数化查询的写法如下，`connection`、`sql`、`targetId`和`limit`来自当前方法：

```java
try (PreparedStatement statement = connection.prepareStatement(sql)) {
  statement.setString(1, targetId.toString());
  statement.setInt(2, limit);
  try (ResultSet rows = statement.executeQuery()) {
    while (rows.next()) {
      String body = rows.getString("body");
      // Build the corresponding domain value from the current row.
    }
  }
}
```

### 6.4 你的任务：实现服务器分发

`CommentOperationHandler.handle`根据操作号进入`list`，把已验证上下文与`ListCommentPayload`组成`CommentQuery`，调用仓储并返回`CommentResult`，结果体使用`CommentPagePayload`。

在启动配置中，为`ServerOperationCatalog`提供4101的`PreparedOperation`，登记`READ_COMMENT`、参数规约和只读事务策略。`ServerOperationExecutor.execute`接收`VerifiedTask`后按以下顺序处理：

```text
查操作目录 → 验证载荷与字段 → 检查主体权限
  → 开启当前事务 → 调用评论处理器 → 构造SecureResponse
```

为4101登记`targetId: UUID`、`subjectKind: STRING`、`sort: STRING`、`limit: INTEGER`及可选`cursor: STRING`。从`ListCommentPayload`提取这些字段，按`ParameterSchema`的类型与必填集合校验；嵌套结构继续由对应载荷检查。对评论、订单、上传的领域操作分别进入对应处理器，其余已登记标量操作进入数据库适配器。

### 6.5 你的任务：把结果送回页面

`DatabaseGateway.parse`保存原始消息及`ReplyRoute`，再调用`executeOperation`。为这个辅助方法增加`requestId`参数，使它按本次请求读取原始参数。发送前先记录pending，完成时进入`onReply`，取回来源与根关联编号。

在`model.message`补一个实现`MessagePayload`的`OperationReplyPayload`，保存`ResultStatus`与结果载荷，并按现有消息约定实现深拷贝。查询层将服务器响应装进它，后续服务就能在`parse`中明确识别请求与响应。

`CommentService`先完成列表分支：检查本地权限，保存`CommentRequestContext`，通过`submitOperation`发给网关；响应到达后`returnResult`将列表交给页面处理器，结束上下文。为`handle`也增加`requestId`参数，按编号取得对应上下文。`PageProcessor.handleCommentListPage`把评论字段与既有页面资源装配起来。

第一次联调时，实现一个测试用的`HttpsTransport`：`submit`将请求交给真实验证器，根据返回上下文构造`VerifiedTask`，直接调用执行器，再返回完成的`CompletionStage<SecureResponse>`。这样可以沿同一条请求检查全部Java对象。

### 6.6 跟踪与验收

UI根操作为`R`，列表请求为`Q`且`correlationId=R`。服务器返回三条评论，按`LONG_FIRST`的长度顺序为8、5、3；网关根据`Q`找到回送位置；页面处理器按`R`更新列表并回到UI。

验收空列表、目标筛选、两种排序、下一页、权限拒绝和响应交错。事务回调中的访问使用同一连接；主动让第二次写入失败时，验证第一次写入一并回滚。

## 7 任务六：订单状态变化、幂等与异常处理

查询流程打通以后，用“取消订单”和“开始制作”完成第一次有并发意义的写入。两个操作都可能遇到同一张排队中的订单，数据库负责决定哪一个状态变化成功。

### 7.1 需要了解的对象和API

`OrderSnapshot`包含订单ID、客户、门店、状态、金额、更新时间和版本。`OrderCommandPayload`携带订单ID和预期版本。`OrderRepository.find`读取快照，`transition`接收预期版本、预期状态及目标状态，返回是否更新成功。

`IdempotencyStore.claim`接收同一事务、主体、请求编号和摘要，返回`CLAIMED`、`REPLAY`、`IN_PROGRESS`或`DIGEST_MISMATCH`。`complete`保存最终响应。`ExceptionCaseRepository`提供`open/find/resolve`管理未收到订单的沟通工单。

### 7.2 你的任务：实现条件更新与幂等存储

先新增订单仓储和工单仓储的具体实现，建立`orders`、`exception_cases`和幂等记录表。幂等表以主体与请求编号为唯一键，保存摘要、状态和已编码响应。

订单状态更新使用这样的SQL结构：

```sql
UPDATE orders
SET status = ?, version = version + 1, updated_at = ?
WHERE order_id = ? AND version = ? AND status = ?
```

绑定参数后调用`executeUpdate()`。影响一行表示转移成功，影响零行表示预期条件没有命中，再根据订单是否存在形成`NOT_FOUND`或`CONFLICT`结果。

`JdbcIdempotencyStore.claim`尝试取得该请求的处理资格。已有完成记录返回原响应；同编号不同摘要返回冲突类别；正在处理的记录按数据库事务和等待策略形成结果。

### 7.3 你的任务：完成写操作执行链

`ServerOperationExecutor`的写分支在一个事务回调中组织下面的动作：

```text
claim
  → CLAIMED：检查对象归属与状态 → 业务修改 → complete → 提交
  → REPLAY：返回已保存响应
  → IN_PROGRESS：形成处理中响应
  → DIGEST_MISMATCH：形成请求不匹配响应
```

`OrderOperationHandler.handle`读取订单，并检查当前主体与客户、门店或分配骑手的关系，然后依据操作选择状态转移。对象级授权和状态更新使用同一事务上下文。

客户端的`CrudOperationService.parse/planOperations`将事件参数转成`OperationRequest`。把`planOperations(int callId)`调整为同时接收`UUID requestId`，据此从上下文取得订单编号和版本。一个事件产生多项请求时，每项使用自己的`requestId`，共同保留根`correlationId`。`EventProcessor`先完成`handleCancelOrder`和`handleStartPreparation`，再逐个增加其它事件。

### 7.4 补齐订单生命周期

先用预置的`QUEUED`订单验证竞争，再补下单所需的菜品、数量等载荷及仓储创建方法。按现有状态设计把操作注册齐全：

| 操作 | 状态结果 |
| --- | --- |
| 下单1101 | 创建`PENDING_ACCEPTANCE`订单 |
| 新增接单1108／拒单1109 | 进入`QUEUED`／`REJECTED` |
| 取消1102 | 在允许取消的状态进入`CANCELLED` |
| 开始制作1105 | `QUEUED → PREPARING` |
| 新增制作完成1110 | `PREPARING → READY_FOR_PICKUP` |
| 取餐1106 | `READY_FOR_PICKUP → DELIVERING` |
| 送达1107 | `DELIVERING → AWAITING_CUSTOMER_CONFIRMATION` |
| 客户确认1103 | 进入`COMPLETED` |
| 未收到1104 | 进入`EXCEPTION`并创建沟通工单 |

新增操作号登记到`OperationCode`、服务器目录和客户端策略。为工单结案提供相应入口，调用`ExceptionCaseRepository.resolve`保存处理结果；订单保留异常标志。

### 7.5 跟踪与验收

预置订单为`QUEUED, version=3`。客户取消和商家开始制作各用不同请求编号，同时以版本3更新；一方将版本改成4，另一方取得冲突结果。

重复提交同一请求，业务效果只有一次。注入业务写入后的失败，订单、工单及幂等终态一并回滚。用两个独立连接验证竞争，并覆盖客户确认与未收到分支。

## 8 任务七：完成文字评论与页面策略

本任务复用前面的查询、事务和请求回送机制，增加发布、本人撤回、商家回复及平台隐藏。先完整处理文字，再在下一任务接图片。

### 8.1 需要了解的对象和API

`PublishCommentPayload`包含评价目标、正文和附件编号。`CommentRequestContext`保留本次操作及来源信息。`CommentRepository`提供`publish/withdraw/reply/hide`，分别产生评论视图、状态或回复视图。

`TextLengthPolicy.measure`提供统一长度计算。练习可选Unicode代码点作为长度单位，通过`text.codePointCount(0, text.length())`计算；验证、保存的排序长度和列表排序共用这一策略。

`PurchaseEvidenceResolver.resolve`接收当前事务、主体与评价目标，查询购买记录并返回`PURCHASED`或`NOT_PURCHASED`。`DefaultPurchaseEvidenceResolver`在服务器端组织这项查询。

### 8.2 你的任务：逐个完成评论操作

先固定有效购买记录的查询条件并准备一条命中、一条未命中的数据。目标为门店时按客户和门店关联订单；目标为菜品时继续关联订单项；骑手服务评价检查完成订单与该单骑手的关联。

`CommentOperationHandler.publish`检查主体、目标与正文，调用购买证据查询，再保存正文、排序长度、标记和状态，形成`CommentViewPayload`。`withdraw`检查作者，`reply`检查对应门店的商家身份，`hide`检查平台权限。

查询公开列表时筛选`PUBLISHED`评论及其可见回复。撤回或隐藏父评论后，这些内容按可见性规则从公开列表移除。写操作继续经过第七节的幂等与事务入口。

每完成一个服务器策略，就接通`CommentService.handle/submitOperation/returnResult`和`EventProcessor`中对应的`handlePublishComment`、`handleWithdrawComment`、`handleMerchantReply`、`handleHideComment`。

### 8.3 完成其余页面

`PageProcessor`中的排序和翻页方法修改查询条件，发起新的列表请求，再装配返回数据。订单详情、商家营业额、骑手任务页面按同样方式取得布局、字段和资源。

把需要的新查询登记成明确操作及载荷；调用已有的网关和执行器。UI字段按元素号绑定到`TextValue`、`DecimalValue`和`BooleanValue`。private方法各自负责自己的页面或事件，公共的请求组织和结果处理可以提取为辅助方法。

### 8.4 验收

发布后重新查询能看到评论和服务器生成的购买标记；作者能够撤回；对应商家能够回复；平台隐藏后列表刷新。跨用户和跨门店操作得到授权结果。重复请求、正文长度、两种排序及连续翻页均有回归用例。

## 9 任务八：接入任务池、HTTPS和图片

业务执行器已经能处理请求，现在把进程内调用换成实际网络和多个Worker，最后让评论使用上传的图片。

### 9.1 需要了解的对象和API

服务器入口`ServerReceiver`持有验证器、`TaskPool`和`ResponseSender`。`TaskPool.offer`尝试入队，`take`阻塞等待任务。`VerifiedTask`保存已验证上下文、消息、回复通道及可选上传信息。

`Worker`取得任务后调用`OperationExecutor.execute`，`ResponseSender.reply`通过`ReplyChannel`发送响应。JDK的`ExecutorService`可管理Worker任务，线程中断用于结束阻塞等待。

客户端`JdkHttpsTransport`使用`HttpClient`、`HttpRequest`和`HttpResponse`。`sendAsync`返回异步结果，`whenComplete`同时接收完成值与异常；`DatabaseGateway`在完成分支中归还结果并清理pending。[异步完成阶段API](https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/util/concurrent/CompletionStage.html)

服务器第一版可使用JDK的`com.sun.net.httpserver.HttpsServer`。`create`建立监听器，`setHttpsConfigurator`配置`SSLContext`，`createContext`登记请求路径，`start`开始监听。处理器通过`HttpExchange.getRequestBody`读取字节，回复通道通过`sendResponseHeaders`和`getResponseBody`写回，结束时关闭交换对象。[JDK HTTPS服务端API](https://docs.oracle.com/en/java/javase/23/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpsServer.html)

### 9.2 你的任务：实现普通请求的网络链路

先完成`ResponseSender.reply`，再完成`Worker.parseTask/run`：循环等待任务，在队列外执行，形成并发送响应，处理好一次任务的异常及中断退出。

`ServerReceiver.receive`验证报文并入队，队列满时返回`BUSY`。增加服务器HTTPS监听入口及`ReplyChannel`适配器，负责外壳解码、调用接收者和响应编码。

`JdkHttpsTransport.submit`使用URI和编码器构造请求，异步发送后解码为`SecureResponse`。连接故障交给异步异常分支，业务处理状态保存在响应中。把前面的测试transport替换为这个实现，再运行相同业务用例。

具体调用顺序为`HttpRequest.newBuilder(uri)`，用`BodyPublishers.ofByteArray`把编码后的外壳设置为POST请求体，再通过`sendAsync`与`BodyHandlers.ofByteArray`取得响应字节。在`thenApply`中调用外壳解码器得到`SecureResponse`。服务端路径可以先固定为`/api/operations`，上传使用独立路径。[HTTP客户端API](https://docs.oracle.com/en/java/javase/23/docs/api/java.net.http/java/net/http/HttpClient.html)

本地HTTPS联调时，用已有JDK的`keytool`准备localhost测试证书。服务器从密钥库建立`SSLContext`；客户端从测试信任库建立自己的SSLContext，再交给`HttpClient`，随后进行真实连接验收。

### 9.3 你的任务：完成资源下载和上传

`FileServerResourceStore.read/write`处理服务器资源字节，数据库资源表保存资源编号、所有者、类型、长度和版本等元数据。资源编号由服务器分配，本地文件路径由存储实现管理。

先完成下载：服务器执行`GET_RESOURCE`读取资源；`ResourceGetTool.download`发起请求，`onResource`调用本地`save`并返回引用；`ResourceAcquisitionService`保存等待状态，收齐资源后`collect`；页面处理器安装图片引用。为按请求协调的`acquire`、`download`和上传辅助方法传入请求上下文或编号，使异步结果找到各自的收集记录。

再完成上传：`ServerReceiver.receiveUpload/stageUpload`验证并暂存字节，`ServerUploadHandler.handle`调用存储并登记资源，返回服务器资源编号。给`JdkHttpsTransport.upload`补上本地引用到文件的读取依赖，再接`ResourceUploadTool.upload/onUploaded`。

最后实现`CommentService.uploadAttachments`：保存附件子请求集合，逐个接收上传结果，全部成功后把资源编号放进`PublishCommentPayload`提交。`FileServerResourceStore.checkOwned`在评论事务中核对这些资源与当前主体的关联。

### 9.4 组装与验收

分别提供服务器与客户端入口：服务器创建数据源、仓储、会话、操作目录、执行器、队列及Worker；客户端创建Core、参与者、根目录、会话及HTTP适配器，登记路由。实例依赖通过构造器连接。

实际UI为`UiThreadDispatcher`提供工具包对应的实现，在UI线程绘制页面，把按钮及触控动作转换成`UiInput`。从首页运行查看评论、下单、状态推进、未收到和带图评论。

第一版`render`可以先遍历页面树，向控制台打印元素编号、字段值和资源引用。制作桌面UI时，可以用JDK Swing：`JFrame/JPanel`放置页面，`JLabel`显示字段，`JButton`的`ActionListener`生成输入，`SwingUtilities.invokeLater`实现UI线程派发，再把相同业务流程接到按钮。

额外覆盖队列满、执行器异常、连接中断、重复请求、上传失败、缓存命中、附件归属和Worker停止。成功或失败完成的操作都能显示结果并结束相应等待状态。

## 10 运行与完成检查

每次实现一个方法，先运行它所属的测试。完成一项任务后，在项目根目录运行：

```powershell
# Run the project tests.
powershell -NoProfile -ExecutionPolicy Bypass -File tools/maven.ps1 test

# Format source and verify the completed stage.
powershell -NoProfile -ExecutionPolicy Bypass -File tools/maven.ps1 fmt:format verify

# Validate API documentation after changing method contracts.
powershell -NoProfile -ExecutionPolicy Bypass -File tools/javadoc.ps1
```

已经提供的框架演示可以这样运行：

```powershell
# Run the existing Core communication demonstration.
java -jar target/takeout-system-0.1.0-SNAPSHOT.jar
```

当你完成业务实现时，再让自己的客户端／服务器入口覆盖本手册中的完整流程。为每个任务保留成功、业务拒绝及异常路径的证据。代码规模、UML对应、构建结果和Git材料按课程要求整理。

## P.S. 实现约定、验证范围和资料

1. 正文保留最初的任务说明和示例。现有源码采用 `ClientApplication`、`ServerApplication` 和 `ClientConsole` 组装入口，`schema.sql` 建立 H2 表，集成测试通过真实 localhost HTTPS 调用服务。
2. Properties清单、资源区0／1、数字文件名、缓存索引表示和H2练习数据库是本手册的教学约定。采用时同步到工程接口与配置；实际购买状态条件、计数单位、分页大小、订单费用与超时规则需形成明确业务约定。
3. 文中的二进制编码需要校验版本、长度、数量和允许的载荷类型；完整网络协议还要定义外壳及上传格式。SHA-256摘要用于内容检查，可信身份由会话和HTTPS验证；对象授权以服务器业务数据为依据。测试证书、私钥和信任库按本地配置管理，真实凭据留在秘密存储中；UI布局文件的格式与选定渲染器统一。
4. 页面装配为每个等待中的子请求保存进度。资源服务先查本地索引，随后依次下载缺失的关联资源。UI 在派发线程中按根请求编号接收页面。
5. `Properties.load(Reader)`使用这里指定的UTF-8字符流；`load(InputStream)`另有编码语义。目录遍历顺序不提供稳定编号；`normalize`不完成符号链接的真实路径检查。缓存保存还需考虑版本、并发分配、临时文件及失败恢复，大资源采用大小限制或流式处理。
6. JDBC驱动、DataSource实现及SQL规约随所选数据库验证。仓储使用当前事务连接，事务入口管理提交、回滚和释放；条件更新与幂等需要真实数据库竞争用例。文件写入与数据库登记的跨资源一致性、退款等外部副作用分别设计恢复流程。
7. `TakeoutIntegrationTest` 覆盖 H2 和 HTTPS、订单状态竞争、评论排序与权限、图片上传和下载、页面回送与异常清理；测试报告由 `verify` 生成。
8. 编排方式参考你在浏览器打开的[Tai-e作业四](https://tai-e.pascal-lab.net/pa4.html)：按任务介绍相关类和API、明确方法工作、用例子连接算法与输出。技术资料可查[JDK文件接口](https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/nio/file/Files.html)、[Properties](https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/util/Properties.html)、[JDBC事务连接](https://docs.oracle.com/en/java/javase/23/docs/api/java.sql/java/sql/Connection.html)、[异步完成阶段](https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/util/concurrent/CompletionStage.html)及[H2教程](https://h2database.com/html/tutorial.html)。本手册用于实现学习，实验报告按课程要求由本人撰写。
