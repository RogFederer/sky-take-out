package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计指定时间区间内的营业额数量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放时间区间内的所有日期
        List<LocalDate> dateList=getDateList(begin, end);

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

    /**
     * 统计指定时间区间内的用户数量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=getDateList(begin, end);
        //存放每天的新增用户数量和总用户数量
        List<Integer> newUserList=new ArrayList<>();
        List<Integer> totalUserList=new ArrayList<>();

        for(LocalDate date:dateList){
            LocalDateTime beginTime=LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(date,LocalTime.MAX);
            Map map=new HashMap<>();
            map.put("end",endTime);
            Integer totalUser=userMapper.countByMap(map);
            map.put("begin",beginTime);
            Integer newUser=userMapper.countByMap(map);
            newUserList.add(newUser);
            totalUserList.add(totalUser);

        }

        String dateListStr=StringUtils.join(dateList,",");
        String newUserListStr=StringUtils.join(newUserList,",");
        String totalUserListStr=StringUtils.join(totalUserList,",");

        return UserReportVO.builder()
                .dateList(dateListStr)
                .newUserList(newUserListStr)
                .totalUserList(totalUserListStr)
                .build();
    }

    /**
     * 统计指定时间区间内的订单数量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=getDateList(begin, end);
        //存放每天的有效订单数量和订单总数
        List<Integer> validOrderList=new ArrayList<>();
        List<Integer> totalOrderList=new ArrayList<>();

        Integer validOrder=0;
        Integer totalOrder=0;

        for(LocalDate date:dateList){
            LocalDateTime beginTime=LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(date,LocalTime.MAX);

            Integer valid=getOrderCount(beginTime,endTime,Orders.COMPLETED);
            Integer total=getOrderCount(beginTime,endTime,null);
            validOrder+=valid;
            totalOrder+=total;

            validOrderList.add(valid);
            totalOrderList.add(total);
        }

        String dateListStr=StringUtils.join(dateList,",");
        String validOrderListStr=StringUtils.join(validOrderList,",");
        String totalOrderListStr=StringUtils.join(totalOrderList,",");

        Double orderCompletionRate=(validOrder*1.0)/(totalOrder*1.0);

        return OrderReportVO.builder()
                .dateList(dateListStr)
                .validOrderCountList(validOrderListStr)
                .orderCountList(totalOrderListStr)
                .validOrderCount(validOrder)
                .totalOrderCount(totalOrder)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量top10统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=getDateList(begin,end);

        LocalDateTime beginTime=LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime=LocalDateTime.of(end,LocalTime.MAX);
        List<GoodsSalesDTO> salesTop = orderMapper.getSalesTop(beginTime, endTime);

        List<String> nameList = salesTop.stream().map(GoodsSalesDTO::getName).toList();
        String nameListStr = StringUtils.join(nameList, ",");
        List<Integer> numberList = salesTop.stream().map(GoodsSalesDTO::getNumber).toList();
        String numberListStr = StringUtils.join(numberList, ",");


        return SalesTop10ReportVO.builder()
                .nameList(nameListStr)
                .numberList(numberListStr)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //查询数据库，获取营业数据
        LocalDate dateBegin=LocalDate.now().minusDays(30);
        LocalDate dateEnd=LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //通过POI将数据写入文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            XSSFWorkbook excel=new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //填充数据
            sheet.getRow(1).getCell(1).setCellValue("时间："+dateBegin+"至"+dateEnd);

            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            row=sheet.getRow(5);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date=dateBegin.plusDays(i);
                BusinessDataVO dataVO = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                row= sheet.getRow(7+i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(dataVO.getTurnover());
                row.getCell(3).setCellValue(dataVO.getValidOrderCount());
                row.getCell(4).setCellValue(dataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(dataVO.getUnitPrice());
                row.getCell(6).setCellValue(dataVO.getNewUsers());
            }


            //通过输出流，将excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 根据开始结束日期生成日期列表
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> getDateList(LocalDate begin, LocalDate end){
        //当前集合用于存放时间区间内的所有日期
        List<LocalDate> dateList=new ArrayList<>();
        //日期计算
        dateList.add(begin);
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        return dateList;
    }

    /**
     * 根据动态条件查询订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin,LocalDateTime end,Integer status){
        Map map=new HashMap<>();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);
        return orderMapper.countByMap(map);
    }
}
