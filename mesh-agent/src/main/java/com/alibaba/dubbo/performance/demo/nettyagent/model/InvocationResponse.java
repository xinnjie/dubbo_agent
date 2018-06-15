package com.alibaba.dubbo.performance.demo.nettyagent.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

/**
 * Created by gexinjie on 2018/6/10.
 */
public class InvocationResponse {
    FuncType funcType;
    ByteBuf result;
    long requestID = -1;

    public InvocationResponse(ByteBuf result) {
        this.result = result;
    }

    public InvocationResponse() {
    }

    public void setResult(ByteBuf result) {
        this.result = result;
    }

    public FuncType getFuncType() {
        return funcType;
    }

    public void setFuncType(FuncType funcType) {
        this.funcType = funcType;
    }

    public ByteBuf getResult() {
        return result;
    }

    public long getRequestID() {
        return requestID;
    }

    public void setRequestID(long requestID) {
        this.requestID = requestID;
    }

    @Override
    public String toString() {
        return "InvocationResponse{" +
                "funcType=" + funcType +
                ", result='" + result + '\'' +
                ", requestID=" + requestID +
                '}';
    }
}
