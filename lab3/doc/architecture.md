# 包结构与UML映射

主包为`edu.nju.sehw.takeout`。跨进程交换的数据集中在`model`，客户端与服务器分别组织自己的控制流程和端口实现。Maven使用标准源码与测试目录，主代码运行依赖为JDK。

| 包 | 职责与主要类型 | 实验二来源 |
| --- | --- | --- |
| `model.common`、`model.message` | 用户标识、权限、消息、载荷、操作编号、编码端口 | 04通信契约、06网络报文 |
| `model.ui`、`model.resource` | UI输入、页面树、字段值、资源清单、本地与远端资源引用 | 05 UI与资源模型 |
| `model.network` | 请求、响应、凭据和回复通道 | 06网络与服务器 |
| `model.comment`、`model.order` | 评论主体、正文、回复、分页、订单状态、沟通工单与对应载荷 | 08订单状态、11与14评论模型 |
| `client.core` | Core、关系表、路由规则、CoreEndpoint和MessageParticipant | 03、04 |
| `client.ui`、`client.processing` | UI状态、资源森林、事件策略和页面装配策略 | 05 |
| `client.logic`、`client.comment` | 页面目录、资源收集、CRUD计划、独立评论流程 | 05、11 |
| `client.query`、`client.security`、`client.resource` | 网络与安全端口、本地目录及资源存储端口、查询层参与者 | 05、06 |
| `server.network`、`server.task` | 接收、暂存上传、任务队列、Worker、响应发送 | 06 |
| `server.security`、`server.operation` | 会话验证、对象授权、操作目录与服务器分发 | 06 |
| `server.persistence` | 事务入口、事务上下文、数据库与幂等端口 | 06、10服务端时序 |
| `server.comment`、`server.order`、`server.resource` | 评论与订单处理器、仓储端口、资源与上传处理 | 08、11、14 |
| `scaffold`、`app` | 待实现异常和可执行框架演示 | 本轮工程组织 |

## 两条主线

客户端按`UiInteractionController → Core → EventProcessor或PageProcessor → 逻辑服务 → 查询层`组织调用。所有Core参与者继承`MessageParticipant`，业务入口为`parse(Message)`；11个事件策略和7个页面策略保留为各处理器的private方法。Core持有注册关系，按实例、编号和操作授权分发。

服务器按`ServerReceiver → ServerRequestVerifier → TaskPool → Worker → OperationExecutor`组织。`ServerOperationExecutor`持有授权、目录、事务、幂等和业务处理端口。评论、订单及异常工单写操作接收同一个`TransactionContext`。

`model`提供双方共用的数据类型，客户端和服务器通过这些类型及网络交互。`UiDataPort`也位于`model.ui`，Core返回该回传端口，`PageProcessor`据此把装配完成的`UiPage`送回UI。

## 主要文件定位

Java文件位置为`src/main/java/edu/nju/sehw/takeout/`加包路径，例如`client/core/Core.java`、`client/processing/EventProcessor.java`、`server/task/TaskPool.java`。同名接口与实现通过名称区分：`HttpsTransport`对应`JdkHttpsTransport`，`DatabaseAdapter`对应`JdbcDatabaseAdapter`，`CommentRepository`对应`JdbcCommentRepository`。

## P.S. 对照与后续实现

实验二源图保存在相邻项目`../sehw2/design/drafts`，本轮修改记录见[设计调整](design-changes.md)。文件内的存根表示待实现入口；具体页面目录、数据库结构、HTTP编码和参数配置随业务实现落地。
