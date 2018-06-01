package com.alibaba.dubbo.performance.demo.agent.registry;

import java.net.InetAddress;

public class IpHelper {

    public static String getHostIp() throws Exception {

        String ip = InetAddress.getLocalHost().getHostAddress();
        return ip;
    }

    public static void main(String[] args) {
        try {
            System.out.println(getHostIp());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
