/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.logging;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内部接口工厂抽象类
 * 通过继承此工厂，将具体的日志工厂注入到此工厂中。通过参数获取对应的日志工厂
 */
public abstract class InternalLoggerFactory {

    public static final String LOGGER_SLF4J = "slf4j";

    public static final String LOGGER_INNER = "inner";

    // 默认Slf4j日志工厂
    public static final String DEFAULT_LOGGER = LOGGER_SLF4J;

    private static String loggerType = null;

    // 存储日志类型-日志工厂
    private static ConcurrentHashMap<String, InternalLoggerFactory> loggerFactoryCache = new ConcurrentHashMap<String, InternalLoggerFactory>();

    /**
     * 通过对应的日志工厂获取该类的日志操作实例
     * @param clazz 需要日志操作的实例类
     * @return 日志实例
     */
    public static InternalLogger getLogger(Class clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * 通过对应的日志工厂获取该类名的日志操作实例
     * @param name 需要日志操作的实例类名
     * @return 日志实例
     */
    public static InternalLogger getLogger(String name) {
        return getLoggerFactory().getLoggerInstance(name);
    }

    /**
     * 获取当前系统设置的日志工厂
     * @return 日志工厂
     */
    private static InternalLoggerFactory getLoggerFactory() {
        InternalLoggerFactory internalLoggerFactory = null;
        if (loggerType != null) {
            internalLoggerFactory = loggerFactoryCache.get(loggerType);
        }
        if (internalLoggerFactory == null) {
            internalLoggerFactory = loggerFactoryCache.get(DEFAULT_LOGGER);
        }
        if (internalLoggerFactory == null) {
            internalLoggerFactory = loggerFactoryCache.get(LOGGER_INNER);
        }
        if (internalLoggerFactory == null) {
            throw new RuntimeException("[RocketMQ] Logger init failed, please check logger");
        }
        return internalLoggerFactory;
    }

    /**
     * 设置当前系统全局的日志工厂类型
     * @param type 工厂类型
     */
    public static void setCurrentLoggerType(String type) {
        loggerType = type;
    }

    /**
     * 默认执行Slf4j日志工厂和Inner日志工厂的构造器
     * 这两个构造器内部有反向注入当前工厂的操作
     */
    static {
        try {
            new Slf4jLoggerFactory();
        } catch (Throwable e) {
            //ignore
        }
        try {
            new InnerLoggerFactory();
        } catch (Throwable e) {
            //ignore
        }
    }

    /**
     * 公共方法
     * 让子工厂执行此方法，注入到当前工厂中
     */
    protected void doRegister() {
        String loggerType = getLoggerType();
        if (loggerFactoryCache.get(loggerType) != null) {
            return;
        }
        loggerFactoryCache.put(loggerType, this);
    }

    /**
     * 杀死工厂，由子工厂各自实现
     */
    protected abstract void shutdown();

    /**
     * 通过类名获取日志实例
     * @param name 类名称
     * @return 日志实例
     */
    protected abstract InternalLogger getLoggerInstance(String name);

    /**
     * 由子工厂各自实现，表明子工厂的工厂类型
     * @return 工厂类型名称
     */
    protected abstract String getLoggerType();
}
