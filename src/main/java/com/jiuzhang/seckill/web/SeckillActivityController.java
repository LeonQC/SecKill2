package com.jiuzhang.seckill.web;

import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.dao.SeckillCommodityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.db.po.SeckillCommodity;
import com.jiuzhang.seckill.service.RedisService;
import com.jiuzhang.seckill.service.SeckillActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class SeckillActivityController {

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;

    @Autowired
    private SeckillActivityService seckillActivityService;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private RedisService redisService;

    @RequestMapping("/addSeckillActivity")
    public String addSeckillActivity() {
        return "add_activity";
    }

    @RequestMapping("/addSeckillActivityAction")
    public String addSeckillActivityAction(
            @RequestParam("name") String name,
            @RequestParam("commodityId") long commodityId,
            @RequestParam("seckillPrice") BigDecimal seckillPrice,
            @RequestParam("oldPrice") BigDecimal oldPrice,
            @RequestParam("seckillNumber") long seckillNumber,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            Map<String, Object> resultMap
    ) throws ParseException {
        startTime = startTime.substring(0, 10) + startTime.substring(11);
        endTime = endTime.substring(0, 10) + endTime.substring(11);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddhh:mm");
        SeckillActivity seckillActivity = new SeckillActivity();
        seckillActivity.setName(name);
        seckillActivity.setCommodityId(commodityId);
        seckillActivity.setSeckillPrice(seckillPrice);
        seckillActivity.setOldPrice(oldPrice);
        seckillActivity.setTotalStock(seckillNumber);
        seckillActivity.setAvailableStock(new Integer("" + seckillNumber));
        seckillActivity.setLockStock(0L);
        seckillActivity.setActivityStatus(1);
        seckillActivity.setStartTime(format.parse(startTime));
        seckillActivity.setEndTime(format.parse(endTime));
        seckillActivityDao.inertSeckillActivity(seckillActivity);
        resultMap.put("seckillActivity", seckillActivity);
        return "add_success";
    }

    @RequestMapping("/seckills")
    public String activityList(
            Map<String, Object> resultMap
    ) {
        List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitysByStatus(1);
        resultMap.put("seckillActivities", seckillActivities);
        return "seckill_activity";
    }

    @RequestMapping("/item/{seckillActivityId}")
    public String itemPage(
            @PathVariable("seckillActivityId") long seckillActivityId,
            Map<String, Object> resultMap
    ) {
        SeckillActivity seckillActivity =
                seckillActivityDao.querySeckillActivityById(seckillActivityId);
        SeckillCommodity seckillCommodity =
                seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());

        resultMap.put("seckillActivity",seckillActivity);
        resultMap.put("seckillCommodity",seckillCommodity);
        resultMap.put("seckillPrice",seckillActivity.getSeckillPrice());
        resultMap.put("oldPrice",seckillActivity.getOldPrice());
        resultMap.put("commodityId",seckillActivity.getCommodityId());
        resultMap.put("commodityName",seckillCommodity.getCommodityName());
        resultMap.put("commodityDesc",seckillCommodity.getCommodityDesc());

        return "seckill_item";
    }

    @RequestMapping("/addSeckillAcitivity")
    public String addSeckillAcitivty() {
        return "add_activity";
    }

    @RequestMapping("/seckill/buy/{userId}/{seckillActivityId}")
    public ModelAndView seckillCommodity(
            @PathVariable long userId,
            @PathVariable long seckillActivityId
    ) {
        boolean stockValidateResult = false;
        ModelAndView modelAndView = new ModelAndView();

        try {
            if (redisService.isInLimiteMemeber(seckillActivityId, userId)) {
                modelAndView.addObject("resultInfo", "Sorry, You've purchased before");
                modelAndView.setViewName("seckill_result");
                return modelAndView;
            }

            stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId);
            if (stockValidateResult) {
                Order order = seckillActivityService.createOrder(seckillActivityId, userId);
                modelAndView.addObject(
                        "resultInfo",
                        "Success! Processing Your Order... Order#: " + order.getOrderNo()
                );
                modelAndView.addObject(
                        "orderNo",
                        order.getOrderNo()
                );
                redisService.addLimitMember(seckillActivityId, userId);
            } else {
                modelAndView.addObject("resultInfo", "Sorry. Out of Stock");
            }
        } catch (Exception exception) {
            log.error("Seckill Exception: " + exception.toString());
            modelAndView.addObject("resultInfo", "Fail to process");
        }

        modelAndView.setViewName("seckill_result");
        return modelAndView;
    }

    @RequestMapping("/seckill/orderQuery/{orderNo}")
    public ModelAndView orderQuery(
            @PathVariable String orderNo
    ) {
        log.info("Order Status, Order #: " + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        ModelAndView modelAndView = new ModelAndView();

        if (order != null) {
            modelAndView.setViewName("order");
            modelAndView.addObject("order", order);
            SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById((order.getSeckillActivityId()));
            modelAndView.addObject("seckillActivity", seckillActivity);
        } else {
            modelAndView.setViewName("order");
        }
        return modelAndView;
    }

    @RequestMapping("/seckill/payOrder/{orderNo}")
    public String payOrder(
            @PathVariable String orderNo
    ) throws Exception {
        seckillActivityService.payOrderProcess(orderNo);
        return "redirect:/seckill/orderQuery/" + orderNo;
    }
}
