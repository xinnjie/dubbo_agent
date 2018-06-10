package com.alibaba.dubbo.performance.demo.nettyagent.model;

import io.netty.buffer.ByteBuf;

/**
 * Created by gexinjie on 2018/6/10.
 */
public class Request {
    FuncType methodType;

    ByteBuf argument;

}
