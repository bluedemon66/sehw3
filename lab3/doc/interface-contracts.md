# 接口规约

## Core接入与消息快照

`CoreEndpoint`提供`send(int senderId, int targetId, Core core, Message message)`和`parse(Message message)`。前者返回本地分发结果`DispatchResult`；后者接收目标消息并进入类内策略。`MessageParticipant`持有private Core引用、private消息模板及草稿锁，提供protected `prepareMessage`和private `maker`。

`prepareMessage`接收请求编号、目标编号、操作编号与载荷；含`correlationId`的重载支持一个操作分解为多个子请求。方法在锁内创建模板并调用maker生成独立快照，返回后由send交给Core。`Core.register`绑定唯一实例与编号，`Core.allow`登记`RouteRule`。

`Message`保存`requestId`、`correlationId`、发送者、接收者、操作号和`MessagePayload`。一个远端操作使用一个`requestId`，重试沿用该编号；多个子请求以共同的`correlationId`关联到UI操作。`ReplyRoute`保存二者，页面回传使用根操作的编号匹配UI当前请求。

事件分发方法接收`callId`和`requestId`，页面分发方法接收`pageId`和`requestId`。具体private策略按请求编号查上下文，取得各自参数。`getTargetRef`按实例身份及独立的UI引用权限返回`UiDataPort`。

## UI和资源

`UiThreadDispatcher.dispatch(Runnable)`描述UI线程派发；`UiInteractionController.render()`读取类内页面状态，`receivePage(UiPage)`接收完整页面。页面字段使用`TextValue`、`DecimalValue`和`BooleanValue`，页面与资源引用使用不可变值对象。

`LocalUiLibrary.findPage`返回`Optional<PageResourceManifest>`。清单第零项为UI布局，每个元素编号唯一。`LocalResourceStore`提供`contains`、`resolve`和`save`，将本地引用解析和存储过程集中到资源适配器。

## 异步网络和认证

`HttpsTransport.submit`与`upload`均返回`CompletionStage<SecureResponse>`。成功响应携带业务状态与载荷，传输失败通过异步阶段的异常完成交给查询层处理。`RequestSecurity.prepare`组织会话和报文；`MessageCodec`提供双方共用的确定性编码与解码形式。

服务器的`ServerRequestVerifier.verify`从凭据查出真实主体及权限，生成`ServerRequestContext`。`VerifiedTask`要求该上下文与消息的请求编号一致；上传任务还要求`VerifiedUpload`持有相同的授权上下文。

## 任务与事务

`TaskPool.offer`返回入队是否成功；队列满时由接收端形成BUSY响应。`take()`以可中断方式等待任务，取出后释放队列内部锁。Worker在队列外执行任务，响应通过`ReplyChannel`回到发起连接。

`TransactionRunner.inTransaction(TransactionWork<T>)`拥有提交、回滚和连接生命周期。回调接收的`TransactionContext`描述当前事务，`DatabaseAdapter.executeWithinTransaction`、`IdempotencyStore.claim/complete`、评论仓储、订单状态与异常工单写入都接收同一上下文。业务处理器继续向内部private方法传递该参数。

`ServerOperationCatalog`保存唯一正整数编号与`PreparedOperation`。操作元数据包含所需权限、字段规约和事务策略；`ParameterSchema`要求每个必填字段具有类型定义。`OrderRepository.transition`同时接收预期版本、预期状态与目标状态，作为后续条件更新的接口形式。

## P.S. 实现约束

- 网络、认证、SQL、资源适配器和业务策略保留存根。当前会话、摘要和事务类型表达接入规约；真实凭据校验、编码互通、数据库原子性与异常补偿在对应实现及集成测试中验证。
- `TransactionContext.connection()`是当前事务拥有的连接，生命周期由`TransactionRunner`管理；仓储参与当前事务。文件上传的暂存、落盘及回收还需与数据库资源登记协调。
- `MessagePayload.deepCopy()`由每种载荷实现。新增可变数据时继续提供防御性复制，编号与字段目录随新业务同步扩展。
- `requestId`服务于单项操作的去重；`correlationId`服务于多步关联。接收端的身份、对象权限和幂等结果以服务器校验与持久化为准。
