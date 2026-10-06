package com.fgsqw.httpserver.utils;

import java.util.HashMap;

/**
 * 不区分大小写的Map
 * 键名统一转换为小写存储，实现大小写不敏感的访问
 *
 * @param <V> 值类型
 * @author fgsq
 */
public class CaseInsensitiveMap<V> extends HashMap<String, V> {

    /**
     * 放入键值对
     *
     * @param key   键
     * @param value 值
     * @return 旧值
     */
    @Override
    public V put(String key, V value) {
        return super.put(key.toLowerCase(), value);
    }

    /**
     * 获取值
     *
     * @param key 键
     * @return 值
     */
    @Override
    public V get(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        return super.get(((String) key).toLowerCase());
    }

    /**
     * 是否包含键
     *
     * @param key 键
     * @return 是否包含
     */
    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof String)) {
            return false;
        }
        return super.containsKey(((String) key).toLowerCase());
    }
}
