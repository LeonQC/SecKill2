package com.jiuzhang.seckill.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisService redisService;

    @Resource
    SeckillActivityService seckillActivityService;

    @Test
    public void stockTest() {
        redisService.setValue("stock:19", 10L);
    }

    @Test
    public void  getStockTest() {
        String stock = redisService.getValue("stock:19");
        System.out.println(stock);
    }

    @Test
    public void stockDeductValidatorTest() {
        boolean result = redisService.stockDeductValidation("stock:19");
        System.out.println("result:" + result);
        String stock = redisService.getValue("stock:19");
        System.out.println("stock" + stock);
    }

    @Test
    public void revertStock() {
        String stock = redisService.getValue("stock:19");
        System.out.println("revert:" + stock);
        redisService.revertStock("stock:19");
        System.out.println("post revert:" + stock);
    }

    @Test
    public void removeLimitMember() {
        redisService.removeLimitMember(19L, 1234L);
    }

    @Test
    public void pushSeckillInfoToRedis() {
        seckillActivityService.pushSeckillInfoToRedis(19);
    }

    @Test
    public void getSeckillInfoFromRedis() {
        String seckillInfo = redisService.getValue("seckillActivity:" + 19);
        System.out.println(seckillInfo);
        String seckillCommodity = redisService.getValue("seckillCommodity:" + 1001);
        System.out.println(seckillCommodity);
    }
}
