# 测试入口与覆盖内容

运行`tools/maven.ps1 verify`会编译主代码及测试，执行JUnit，再校验Google Java格式和Checkstyle规则，最后保留JAR与Surefire结果。结果文件位于`target/surefire-reports`，最新构建记录见[验证结果](verification.md)。

| 测试类 | 校验内容 |
| --- | --- |
| `CoreRoutingTest` | 实例身份、发送者及目标编号、操作路由、Core归属、目标失败、UI回传端口授权 |
| `MessageSnapshotTest` | 集合及自定义可变载荷隔离、独立消息对象、连续草稿、并发草稿、请求与关联编号 |
| `ProtocolContractsTest` | 字节数组快照、凭据日志隐藏、验证上下文绑定、页面清单、必填字段规约和编号唯一性 |
| `TaskPoolTest` | 正容量、队列满、FIFO、处理期间释放队列槽、阻塞等待的中断 |
| `ArchitectureTest` | 共享模型独立、客户端与服务器通过模型交互、Core职责以及参与者公共基类 |

并发用例使用Future、CountDownLatch和有界等待协调执行。测试通过内存端点观察消息投递，通过任务队列观察并发交接，不需要数据库或网络服务。

修改接口后先运行`tools/maven.ps1 test`，再运行`tools/maven.ps1 verify`检查完整构建。实现某个业务方法时，为该方法补充成功、拒绝、重复请求和异常回送用例。

修改注释后运行`tools/javadoc.ps1`生成英文API文档，并校验Javadoc语法、引用和标签。输出入口为`target/apidocs/index.html`；注释统一使用英文，方法用`@param`、`@return`和`@throws`说明调用契约。

## P.S. 业务测试接续

业务存根完成后，补充订单取消与开始制作竞争、结算只生效一次、未收到创建工单、评论作者与商家权限、附件所有权、真实HTTP失败回送，以及数据库回滚和幂等恢复测试。当前用例的通过结果对应框架及值契约，业务存根的执行结果继续由后续用例验证。
