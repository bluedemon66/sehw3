# 实验二到Java框架的设计调整

基线为实验二03至06、11、14号类图，以及订单、评论状态和服务端时序。本轮调整已体现在Java的类型、参数和包依赖中。

## 包与端口

通信值对象、UI回传接口和业务DTO归入`model`，Core归入`client.core`。客户端与服务器各自使用共享模型。数据库、网络、安全与存储职责提取为接口，由`JdbcDatabaseAdapter`、`JdkHttpsTransport`等具名适配器承接实现，便于测试时替换依赖。

Core增加`register`和`allow`，先创建Core及参与者，再绑定实例和关系表，补齐类图中对象组装的过程。注册校验参与者所属Core、编号与实例唯一性。

## 请求关联与内部策略

原分发方法只接收调用号，Java中增加`requestId`参数：`eventHandler(callId, requestId)`、`pageHandler(callId, requestId)`、`handlePage(pageId, requestId)`。各个private策略继续按请求编号读取上下文，支持同时处理多个事件。

消息新增`correlationId`，将多步骤操作关联起来。上传每张附件与提交评论可以使用各自的`requestId`，并共享UI根操作的`correlationId`；重试沿用对应子请求编号。`RequestContext`、`CommentRequestContext`和`ReplyRoute`同步保存关联编号。

## 数据快照

消息模板由基类独占，maker保持private。record构造器对集合和数组取快照，消息载荷在构造与读取时复制。UI值采用明确的文本、金额和布尔类型，查询缺失项使用`Optional`。

页面清单增加首项布局和元素编号唯一约束。评论图片标志由`CommentView.images`计算；正文长度交给`TextLengthPolicy`，正文、图片与计数各自保持明确的数据来源。

## 异步与线程

网络端口以`CompletionStage`统一表达返回结果和传输失败。UI线程派发独立为`UiThreadDispatcher`，页面接收与实际绘制保持各自入口。任务池使用JDK有界阻塞队列，`take()`显式声明中断，Worker取得任务后进入队列外的处理阶段。

## 事务与异常订单

原图分别列出数据库执行及幂等claim、complete。Java增加`TransactionRunner`和`TransactionContext`，将业务写入、幂等终态与仓储调用接入同一个事务入口。评论处理器的内部方法继续传递事务上下文。

订单条件更新接口加入预期状态与版本号。未收到场景补充`CommunicationCase`和`ExceptionCaseRepository`，把沟通工单进度与订单的`EXCEPTION`状态分别保存；工单结案记录处理结果，订单继续保留异常标志。

## P.S. 回填与待定参数

实验二图稿保持现有版本，后续可按本页回填参数及接口。评论4101至4106沿用原图，订单1101至1107、页面2101和资源3101为本轮目录编号，可在`OperationCode`统一调整。字数、分页、上传限额、超时与业务费率通过策略或配置确定；数据库、认证、网络与存储的实现仍保留存根。源码行数与最终实验要求的实现规模，在业务完成后分别核算。
