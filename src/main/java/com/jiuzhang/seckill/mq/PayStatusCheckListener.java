package com.jiuzhang.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RocketMQMessageListener(topic = "pay_check", consumerGroup = "pay_check_group")
public class PayStatusCheckListener implements RocketMQListener<MessageExt> {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private RedisService redisService;

    @Override
    public void onMessage(MessageExt messageExt) {
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("Received Pay Check Message: " + message);
        Order order = JSON.parseObject(message, Order.class);
        Order orderInfo = orderDao.queryOrder(order.getOrderNo());
        if (orderInfo.getOrderStatus() != 2) {
            log.info("Closing unpaid order #: " + orderInfo.getOrderNo());
            orderInfo.setOrderStatus(99);
            orderDao.updateOrder(orderInfo);
            seckillActivityDao.revertStock(order.getSeckillActivityId());
            redisService.revertStock("stock:" + order.getSeckillActivityId());
            redisService.removeLimitMember(order.getSeckillActivityId(), order.getUserId());
        }
    }
}
