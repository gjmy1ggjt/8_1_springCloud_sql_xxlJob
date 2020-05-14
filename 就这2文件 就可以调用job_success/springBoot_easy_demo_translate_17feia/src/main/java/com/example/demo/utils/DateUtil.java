package com.example.demo.utils;


import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class DateUtil {
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private final static String SECOND_DF = "yyyy-MM-dd HH:mm:ss";
    private final static String FIRST_DF = "yyyy.MM.dd";
    private final static String THIRD_DF = "yyyy-MM-dd";
    private final static String FOURTH_DF = "yyyy-MM-dd'T'HH:mm:ss";
    private final static String FIFTH_DF = "yyyy-MM-dd'T'HH:mm:ss.S'Z'";
    private final static String SIXTH_DF = "dd/MM/yyyy";
    private final static String SEVENTH_DF = "yyyyMMddHHmmss";
    /**
     * 锁对象
     */
    private static final Object lockObj = new Object();
    /**
     * 存放不同的日期模板格式的sdf的Map
     */
    private static Map<String, ThreadLocal<SimpleDateFormat>> sdfMap = new HashMap<>();

    /**
     * @Author: LJ
     * @Date: 2018/12/7 17:52
     * @Desc: 返回一个ThreadLocal的sdf, 每个线程只会new一次sdf
     */
    private static SimpleDateFormat getSdf(final String pattern) {
        ThreadLocal<SimpleDateFormat> localSdf = sdfMap.get(pattern);

        // 此处的双重判断和同步是为了防止sdfMap这个单例被多次put重复的sdf
        if (localSdf == null) {
            synchronized (lockObj) {
                localSdf = sdfMap.get(pattern);
                if (localSdf == null) {

                    // 这里是关键,使用ThreadLocal<SimpleDateFormat>替代原来直接new SimpleDateFormat
                    log.info("Thread: {} init pattern: {}", Thread.currentThread(), pattern);
                    localSdf = ThreadLocal.withInitial(() -> new SimpleDateFormat(pattern));

                    sdfMap.put(pattern, localSdf);
                }
            }
        }

        return localSdf.get();
    }

    /**
     * @Author: LJ
     * @Date: 2018/12/7 17:29
     * @Desc: 使用传入的格式化字符串格式化时间
     */
    public static String format(String pattern, Date date) {
        return getSdf(pattern).format(date);
    }


    /**
     * @Author: LJ
     * @Date: 2018/12/7 17:30
     * @Desc: 使用传入的格式化字符串格式化时间
     */
    public static String format(Date date, String pattern) {
        return format(pattern, date);
    }

    /**
     * @Author: LJ
     * @Date: 2018/5/8 15:37
     * @Description: 将随意字符串转成时间
     */
    public static Date parse(String pattern, String dateStr) throws ParseException {
        return getSdf(pattern).parse(dateStr);
    }


    /**
     * @Author: LJ
     * @Date: 2018/5/8 15:37
     * @Description: 将 dd/MM/yyyy 字符串转成时间
     */
    public static Date parseSixth(String dateStr) {

        try {
            return parse(SIXTH_DF, dateStr);
        } catch (ParseException e) {
            log.error("字符串转成时间出错：" + dateStr, e);

            return null;
        }
    }


    public static String formatFirst(Date date) {
        if (date == null) {
            return "";
        }
        return format(FIRST_DF, date);
    }

    public static String formatSecond(Date date) {
        if (date == null) {
            return "";
        }

        return format(SECOND_DF, date);
    }

    public static String formatThird(Date date) {
        if (date == null) {
            return "";
        }

        return format(THIRD_DF, date);
    }

    public static String formatThirdToday() {
        Date date = new Date();

        return format(THIRD_DF, date);
    }

    public static String formatFive(Date date) {
        if (date == null) {
            return "";
        }
        return format(FIFTH_DF, date);
    }

    /**
     * @Author: Kevin
     * @Date: 2018-12-28 10:08
     * @Description: eg: 2018-12-10 17:10:00 ---> 2018-12-10T17:10:00.000Z
     */
    public static String formatTzTime(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return "";
        }

        if (dateStr.indexOf("T") != -1) {
            //如果传的本来是2018-12-10T17:10:00.000Z,则直接返回
            return dateStr;
        }
        dateStr = dateStr.replace(" ", "T");
        dateStr += ".000Z";

        return dateStr;
    }

    /**
     * 返回格式：yyyyMMddHHmmss
     *
     * @return
     */
    public static String formatSeventhToday() {
        Date date = new Date();

        return format(SEVENTH_DF, date);
    }

    /**
     * @Author: LJ
     * @Date: 2018/5/8 11:50
     * @Description: yyyy-MM-dd'T'HH:mm:ss
     */
    public static String formatFourthToday() {
        Date date = new Date();

        return format(FOURTH_DF, date);
    }


    /**
     * 得到某年某月的第一天
     * 若传入参数为空则返回当月第一天
     *
     * @param dateStr yy-MM
     * @return 返回 yyyy-MM-dd 00:00:00
     * @throws ParseException
     */
    public static Date getFirstDayOfMonth(String dateStr) {
        try {
            int year, month;
            Calendar cal = Calendar.getInstance();

            if (StringUtils.isNotBlank(dateStr)) {

                String[] dateArr = dateStr.split("-");
                year = Integer.parseInt(dateArr[0]);
                month = Integer.parseInt(dateArr[1]);

            } else {

                year = cal.get(Calendar.YEAR);
                month = cal.get(Calendar.MONTH) + 1;
            }

            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, cal.getMinimum(Calendar.DATE));

            return parse(SECOND_DF, format("yyyy-MM-dd 00:00:00", cal.getTime()));

        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }


    /**
     * 得到某年某月的最后一天
     * 若传入参数为空则返回当月最后一天
     *
     * @param dateStr yy-MM
     * @return yyyy-MM-dd HH:mm:ss
     * @throws ParseException
     */
    public static Date getLastDayOfMonth(String dateStr) throws ParseException {
        int year, month;
        Calendar cal = Calendar.getInstance();

        if (StringUtils.isNotBlank(dateStr)) {

            String[] dateArr = dateStr.split("-");
            year = Integer.parseInt(dateArr[0]);
            month = Integer.parseInt(dateArr[1]);

        } else {

            year = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH) + 1;
        }

        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));

        return parse(SECOND_DF, format("yyyy-MM-dd 23:59:59", cal.getTime()));
    }

    /**
     * 执行当前时间加n天
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/19 16:37
     */
    public static Date plusDays(int days) {
        Date today = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(today);
        c.add(Calendar.DAY_OF_MONTH, days);
        Date targetDate = c.getTime();
        return targetDate;
    }

    /**
     * 获取传入时间加一个月
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/29 17:00
     */
    public static Date getNextMonthDate(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 1);

        return calendar.getTime();
    }

    /**
     * 获取传入时间加一周
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/29 17:04
     */
    public static Date getNextWeekDate(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.WEDNESDAY, 1);

        return calendar.getTime();
    }

    /**
     * 获取传入时间的下个半月
     *
     * @param
     * @return Date : 返回当前月月底最后一天23:59:59或者返回下月的15号23:59:59
     * @author Kee.Li
     * @date 2018/1/29 17:04
     */
    public static Date getNextHalfMonthDate(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        if (dayOfMonth < 16) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            Date lastDayOfMonth = calendar.getTime();
            return lastDayOfMonth;
        } else {
            calendar.add(Calendar.MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 15);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            Date halfDayOfMonth = calendar.getTime();
            return halfDayOfMonth;
        }
    }

    /**
     * 获取传入时间的下一天开始时间
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/29 18:37
     */
    public static Date getNextDayStartTime(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    /**
     * 获取传入时间的前一天开始时间
     *
     * @author LinYun
     * @date 11:10 2018/6/8
     * @description
     */
    public static Date getBeforeDayStartTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    /**
     * 获取传入时间的前一天结束时间
     *
     * @author LinYun
     * @date 11:10 2018/6/8
     * @description
     */
    public static Date getBeforeDayEndTime(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);

        return calendar.getTime();
    }

    /**
     * 获取传入时间加plusDays天
     *
     * @param
     * @author Kee.Li
     * @date 2018/1/29 18:44
     */
    public static Date getPlusDays(Date date, int plusDays) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, plusDays);

        return calendar.getTime();
    }

    /**
     * 获取传入时间加plusHour小时
     *
     * @author LinYun
     * @date 15:44 2018/6/27
     * @description
     */
    public static Date getPlusHour(Date date, int plusHour) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, plusHour);

        return calendar.getTime();
    }

    public static void main(String[] args) throws ParseException {
        log.debug(parse("dd/MM/yyyy HH:mm", "05/05/2018 23:22").toString());
    }


    /**
     * @Author: chenyi
     * @Date: 2018/6/25 11:18
     * @Description:根据传入的节假日和日期格式化，返回非节假日的最前面的日期
     */
    public static Date getHolidays(List<String> holidays, SimpleDateFormat dateFormat, Date selectDate) {
        try {

            if (selectDate == null) {
                selectDate = new Date();
            }

            if (holidays != null && holidays.size() > 0 && dateFormat != null) {
                String selectDateStr = dateFormat.format(selectDate);
                //如果在里面，那么开始进行下一个检查，知道检查合格的

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(selectDate);

                //如果是星期六或者星期日或者节假日列表，那么直接跳过，取下一个
                if (holidays.contains(selectDateStr) || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {

                    calendar.add(Calendar.DAY_OF_MONTH, 1);

                    selectDate = calendar.getTime();

                    selectDate = getHolidays(holidays, dateFormat, selectDate);
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }

        return selectDate;
    }


    /**
     * @author LinYun
     * @date 10:02 2018/8/9
     * @description 获取时间的日期部分, 去掉时分秒
     */
    public static Date getDateFromTime(Date date) throws ParseException {
        return parse(DATE_FORMAT, format(DATE_FORMAT, date));
    }

    /**
     * @author LinYun
     * @date 9:46 2018/8/9
     * @description 比较两个日期的大小，仅比较日期
     */
    public static int compareDate(Date DATE1, Date DATE2) {
        try {
            Date dt1 = getDateFromTime(DATE1);
            Date dt2 = getDateFromTime(DATE2);
            if (dt1.getTime() > dt2.getTime()) {
                //dt1 大于dt2
                return 1;
            } else if (dt1.getTime() < dt2.getTime()) {
                //dt1 小于dt2
                return -1;
            } else {
                //dt1 等于dt2
                return 0;
            }
        } catch (Exception e) {

            log.error(e.getLocalizedMessage(), e);
        }

        return 0;
    }

    /**
     * @Author: HuKai
     * @Date: 2018-09-14 0014 下午 5:01
     * @Description: 计算两个时间相差的天数
     */
    public static String betweenDays(String startDate, String endDate) {
        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
            String result = "";

            try {
                Date date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startDate);
                Date date2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(endDate);

                float days = (date2.getTime() - date1.getTime()) * 1.0f / (1000 * 3600 * 24);

                BigDecimal decimal = new BigDecimal(days);
                decimal = decimal.setScale(1, RoundingMode.HALF_UP);

                DecimalFormat df = new DecimalFormat("#,##0.0");

                result = df.format(decimal);
            } catch (ParseException e) {
                log.error(e.getMessage(), e);
            }

            return result;
        } else {
            return "";
        }
    }
}
