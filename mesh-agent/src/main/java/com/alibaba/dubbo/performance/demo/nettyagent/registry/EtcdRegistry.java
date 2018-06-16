package com.alibaba.dubbo.performance.demo.nettyagent.registry;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import org.apache.logging.log4j.LogManager;


import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class EtcdRegistry implements IRegistry {
org.apache.logging.log4j.Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);
    // 该EtcdRegistry没有使用etcd的Watch机制来监听etcd的事件
    // 添加watch，在本地内存缓存地址列表，可减少网络调用的次数

    private final String rootPath = "dubbomesh";
    private Lease lease;
    private KV kv;
    private long leaseId;

    public EtcdRegistry(String registryAddress, int portion) {
        Client client = Client.builder().endpoints(registryAddress).build();
        this.lease   = client.getLeaseClient();
        this.kv      = client.getKVClient();
        try {
            this.leaseId = lease.grant(30).get().getID();
        } catch (Exception e) {
            e.printStackTrace();
        }

        keepAlive();

        String type = System.getProperty("type");   // 获取type参数
        if ("provider".equals(type)){
            // 如果是provider，去etcd注册服务
            try {
                int port = Integer.valueOf(System.getProperty("server.port"));
                register("com.alibaba.dubbo.performance.demo.provider.IHelloService",port, portion);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 向ETCD中注册服务
    public void register(String serviceName,int port, int maxConnectionNum) throws Exception {
        // 服务注册的key为:    /dubbomesh/com.some.package.IHelloService/192.168.100.100:2000
        // /dubbomesh/com.alibaba.dubbo.performance.demo.provider.IHelloService/10.10.10.3:30000
        String strKey = MessageFormat.format("/{0}/{1}/{2}:{3}",rootPath,serviceName,IpHelper.getHostIp(),String.valueOf(port));
        ByteSequence key = ByteSequence.fromString(strKey);
        ByteSequence val = ByteSequence.fromString(String.valueOf(maxConnectionNum));     // 目前只需要创建这个key,对应的value暂不使用,先留空
        kv.put(key,val, PutOption.newBuilder().withLeaseId(leaseId).build()).get();
        logger.debug("Register a new service at:" + strKey);
    }

    // 发送心跳到ETCD,表明该host是活着的
    public void keepAlive(){
        Executors.newSingleThreadExecutor().submit(
                () -> {
                    try {
                        Lease.KeepAliveListener listener = lease.keepAlive(leaseId);
                        listener.listen();
                        logger.debug("KeepAlive lease:" + leaseId + "; Hex format:" + Long.toHexString(leaseId));
                    } catch (Exception e) { e.printStackTrace(); }
                }
        );
    }

    public Map<Endpoint, Integer> find(String serviceName) throws Exception {

        String strKey = MessageFormat.format("/{0}/{1}",rootPath,serviceName);
        ByteSequence key  = ByteSequence.fromString(strKey);
        GetResponse response = kv.get(key, GetOption.newBuilder().withPrefix(key).build()).get();

        Map<Endpoint, Integer> endpoints = new HashMap<>();

        for (com.coreos.jetcd.data.KeyValue kv : response.getKvs()){
            String serviceAndAddress = kv.getKey().toStringUtf8();

            int index = serviceAndAddress.lastIndexOf("/");
            String endpointStr = serviceAndAddress.substring(index + 1,serviceAndAddress.length());

            String host = endpointStr.split(":")[0];
            int port = Integer.valueOf(endpointStr.split(":")[1]);
            String maxConnections = kv.getValue().toStringUtf8();
            endpoints.put(new Endpoint(host,port), Integer.valueOf(maxConnections));
        }
        return endpoints;
    }
}
