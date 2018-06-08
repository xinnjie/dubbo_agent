package com.alibaba.dubbo.performance.demo.nettyagent.util;

import com.alibaba.dubbo.performance.demo.nettyagent.model.FuncType;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gexinjie on 2018/6/8.
 */
public class CacheContext {
    private final ConcurrentHashMap<FuncType, Integer> methodIDs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, FuncType> methods = new ConcurrentHashMap<>();
    public void put(FuncType type, Integer methodID) {
        methodIDs.put(type, methodID);
        methods.put(methodID, type);
    }
    public void put(Integer methodID, FuncType type) {
        methodIDs.put(type, methodID);
        methods.put(methodID, type);
    }
    public FuncType get(Integer methodID) {
        return methods.get(methodID);
    }
    public Integer get(FuncType type) {
        return methodIDs.get(type);
    }

    public boolean contains(Integer methodID) {
        return methods.containsKey(methodID);
    }
    public boolean contains(FuncType type) {
        return methodIDs.containsKey(type);
    }

    public int size() {
        return methods.size();
    }
}
