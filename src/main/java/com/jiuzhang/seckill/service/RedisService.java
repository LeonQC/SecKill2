package com.jiuzhang.seckill.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.util.Collections;

@Slf4j
@Service
public class RedisService {

    @Resource
    private JedisPool jedisPool;

    public RedisService setValue(String key, Long value) {
        Jedis client = jedisPool.getResource();
        client.set(key, value.toString());
        client.close();
        return this;
    }

    public String getValue(String key) {
        Jedis client = jedisPool.getResource();
        String value = client.get(key);
        client.close();
        return value;
    }

    public boolean stockDeductValidation(String key) {
        try (Jedis client = jedisPool.getResource()) {
            String script = "if redis.call('exists', KEYS[1]) == 1 then\n" +
                    "    local stock = tonumber(redis.call('get', KEYS[1]))\n" +
                    "    if (stock <= 0) then\n" +
                    "        return -1 \n" +
                    "    end;\n" +
                    "    \n" +
                    "    redis.call('decr', KEYS[1]);\n" +
                    "    return stock -1\n" +
                    "end;\n" +
                    "\n" +
                    "return -1;";

            Long stock = (Long) client.eval(
                    script,
                    Collections.singletonList(key),
                    Collections.emptyList()
            );

            if (stock < 0) {
                System.out.println("Redis Service: Out of Stock");
                return false;
            }

            System.out.println("Redis Service: Congrats! Got it!");
            return true;
        } catch (Throwable throwable) {
            System.out.println("Fail to deduct stock number: " + throwable.toString());
            return false;
        }
    }

    public void addLimitMember(long activityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.sadd("seckillActivity_users:" + activityId, String.valueOf(userId));
        jedisClient.close();
    }

    public boolean isInLimiteMemeber(long activityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        boolean sismember = jedisClient.sismember("seckillActivity_users:" + activityId, String.valueOf(userId));
        jedisClient.close();
        log.info("userId:{} activityId:{} in limited list: {}", activityId, userId, sismember);
        return sismember;
    }

    public void removeLimitMember(Long activityId, Long userId) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.srem("seckillActivity_users:" + activityId, String.valueOf(userId));
        jedisClient.close();
    }

    public void revertStock(String key) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.incr(key);
        jedisClient.close();
    }

}
