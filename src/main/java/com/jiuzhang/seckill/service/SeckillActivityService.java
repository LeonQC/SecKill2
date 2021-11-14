package com.jiuzhang.seckill.service;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.dao.SeckillCommodityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.db.po.SeckillCommodity;
import com.jiuzhang.seckill.mq.RocketMQService;
import com.jiuzhang.seckill.util.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Slf4j
@Service
public class SeckillActivityService {

    @Resource
    private RedisService service;

    @Resource
    private SeckillActivityDao seckillActivityDao;

    @Resource
    private SeckillCommodityDao seckillCommodityDao;

    @Resource
    private RocketMQService rocketMQService;

    @Resource
    private OrderDao orderDao;

    @Resource
    private SnowFlake snowFlake = new SnowFlake(1,1);

    public boolean seckillStockValidator(long activityId) {
        String key = "stock:" + activityId;
        return service.stockDeductValidation(key);
    }

    public Order createOrder(long seckillActivityId, long userId) throws Exception {
        SeckillActivity activity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        Order order = new Order();

        order.setOrderNo(String.valueOf(snowFlake.nextId()));
        order.setSeckillActivityId(activity.getId());
        order.setUserId(userId);
        order.setOrderAmount(activity.getSeckillPrice().longValue());
        log.info("Created Order #: " + order.getOrderNo());

        rocketMQService.sendMessage("seckill_order", JSON.toJSONString(order));

        rocketMQService.sendDelayMessage("pay_check", JSON.toJSONString(order), 5);
        return order;
    }

    public void payOrderProcess(String orderNo) throws Exception {
        Order order = orderDao.queryOrder(orderNo);
        if (order == null) {
            log.error("Non-exist Order: " + orderNo);
            return;
        }
        log.info("Completed Order #: " + orderNo);
        order.setPayTime(new Date());
        order.setOrderStatus(2);
        orderDao.updateOrder(order);

        rocketMQService.sendMessage("pay_done", JSON.toJSONString(order));
    }

    public void pushSeckillInfoToRedis(long seckillActivityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        service.setValue("seckillActivity:" + seckillActivityId, JSON.toJSONString(seckillActivity));

        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        service.setValue("seckillCommodity:" + seckillActivity.getCommodityId(), JSON.toJSONString(seckillCommodity));

    }
}
