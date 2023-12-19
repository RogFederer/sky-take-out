package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 统计指定时间区间内的营业额数量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放时间区间内的所有日期
        List<LocalDate> dateList=new ArrayList<>();
        //日期计算
        dateList.add(begin);
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }
        //将日期集合拼接成字符串
        String dateJoin = StringUtils.join(dateList, ",");

        //存放每天的营业额
        List<Double> turnoverList=new ArrayList<>();
        for(LocalDate date:dateList){
            //查询当天的营业额，订单的状态为已完成
            LocalDateTime beginTime=LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(date,LocalTime.MAX);

            Map map=new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover=orderMapper.sumByMap(map);
            turnover = turnover==null? 0.0:turnover;//如果当天营业额为0，数据库查询会返回空，因此需要加一个判断
            turnoverList.add(turnover);
        }
        //将营业额拼接成字符串
        String turnoverJoin=StringUtils.join(turnoverList,",");

        //生成返回给前端的VO
        TurnoverReportVO turnoverReportVO=TurnoverReportVO.builder()
                .turnoverList(turnoverJoin)
                .dateList(dateJoin)
                .build();

        return turnoverReportVO;
    }
}