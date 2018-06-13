package com.alibaba.dubbo.performance.demo.nettyagent.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gexinjie on 2018/6/10.
 */
public class InvocationRequest {
    FuncType funcType;
    ByteBuf argument;
    long requestID = -1;

    public InvocationRequest(ByteBuf argument, String interfaceName, String methodName, String parameterTypes) {
        this.argument = argument;
        this.funcType = new FuncType(interfaceName, methodName, parameterTypes);
    }

    public InvocationRequest() {
    }

    public void setFuncType(FuncType funcType) {
        this.funcType = funcType;
    }

    public void setArgument(ByteBuf argument) {
        this.argument = argument;
    }

    public FuncType getFuncType() {
        return funcType;
    }

    public ByteBuf getArgument() {
        return argument;
    }

    public long getRequestID() {
        return requestID;
    }

    public void setRequestID(long requestID) {
        this.requestID = requestID;
    }

    public Map<String, String> getAttachments() {
        Map<String, String> result = new HashMap<>(1);
        result.put("path", getFuncType().getInterfaceName());
        return result;
    }

    @Override
    public String toString() {
        return "InvocationRequest{" +
                "funcType=" + funcType +
                ", argument='" + argument + '\'' +
                ", requestID=" + requestID +
                '}';
    }
}
