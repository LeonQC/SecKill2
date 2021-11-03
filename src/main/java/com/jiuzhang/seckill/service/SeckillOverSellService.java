package com.jiuzhang.seckill.service;

import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillOverSellService {

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    public String processSeckill(long activityId) {
        SeckillActivity activity = seckillActivityDao.querySeckillActivityById(activityId);
        int availableStock = activity.getAvailableStock();
        String result;

        if (availableStock > 0) {
            result = "Congrats, Success";
            System.out.println(result);
            availableStock -= 1;
            activity.setAvailableStock(availableStock);
            seckillActivityDao.updateSeckillActivity(activity);
        } else {
            result = "Sorry, Fail";
            System.out.println(result);
        }
        return result;
    }
}
