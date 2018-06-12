package com.alibaba.dubbo.performance.demo.nettyagent.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gexinjie on 2018/6/10.
 */
public class InvocationRequest {
    FuncType funcType;
    String argument;
    long requestID = -1;

    public InvocationRequest(String argument, String interfaceName, String methodName, String parameterTypes) {
        this.argument = argument;
        this.funcType = new FuncType(interfaceName, methodName, parameterTypes);
    }

    public InvocationRequest() {
    }

    public void setFuncType(FuncType funcType) {
        this.funcType = funcType;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }

    public FuncType getFuncType() {
        return funcType;
    }

    public String getArgument() {
        return argument;
    }

    public long getRequestID() {
        return requestID;
    }

    public void setRequestID(long requestID) {
        this.requestID = requestID;
    }

    public Map<String, String> getAttachments() {
        Map<String, String> result = new HashMap<>();
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