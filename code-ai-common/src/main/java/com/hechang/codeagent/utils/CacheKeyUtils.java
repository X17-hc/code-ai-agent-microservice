package com.hechang.codeagent.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

/**
 * 缓存key 生成工具类
 *  为了生成一致且唯一的缓存键，在utiis包下创建个专门的缓存工具类。
 *  缓存键的生成思路是将复杂的对象转换为固定长度的哈希值，这样既保证了不同查询请求的 key唯一，又避免了key过长的问题：
 */
public class CacheKeyUtils {

    /**
     * 生成缓存键
     * @param obj 要生成缓存键的对象
     * @return MD5哈希后的缓存key
     */
    public static String generateCacheKey(Object obj) {
        if (obj == null) {
            // 处理 null 对象,避免缓存穿透
            return DigestUtil.md5Hex("null");
        }
        // 先转 JSON，再转 MD5
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }
}
