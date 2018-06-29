consumer agent 以及 provider agent 都包括 server 以及 client 的功能

consumer agent server端 从 Consumer 接受 http 请求，client 端建立与 PA 的连接，并转发请求。


provider agent  server 端接受 CA 的请求，client 端转发请求给 provider，然后接收回应发回给 CA。
 
 
第一次写这种类型的代码，所以写的过程中每个部分的功能有点混杂在一起了。  

CA server handler 都在 `CAInitializer.java` 中，client 的代码在 `ConnectManager.java` 中

PA server 和 client 的代码都被混杂在了 `PAinitializer.java` 中



为追求速度，一些 handler 进行了硬编码，比如CA 的 HTTP 请求解码。


一个比较重要的优化点在 CA 的收、转发 request 最好在一个线程上，因此我记录下了 eventloop 到 PA 连接的映射，变量名为 `PAchannelFromEventloop`。
其它的优化包括 
* 尽量保证零拷贝
* CA 和  PA 之间自定了类似 dubbo 的协议，对方法名进行缓存，每次调用的方法名用一个 `methodID`表示，相关的代码是 `codec/CacheXXX.java`



`garage`文件下的代码请忽略，是废弃的代码，但是删除还要改 test，就集合在一起没删掉