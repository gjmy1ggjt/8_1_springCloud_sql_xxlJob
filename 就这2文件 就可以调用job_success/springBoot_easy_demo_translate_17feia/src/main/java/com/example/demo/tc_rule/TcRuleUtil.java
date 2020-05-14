package com.example.demo.tc_rule;

import com.example.demo.utils.ListUtil;
import org.thymeleaf.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2020/4/28.
 */
public class TcRuleUtil {

    private static String step = "|";

    private static String nullStr = " ";

    /**
     * 1, timeType ， personType 都为0,；
     * 2， timeType 为0 ， personType不为0；
     * 3，timeType不为0，personType为0
     * 4, timeType不为0，personType不为0
     */
    public static List<TcRule> checkTcRule(TcRule tcRule, List<TcRule> listTcRule) {

        int timeType = tcRule.getTimeType();

        int personType = tcRule.getPersonType();

        if (timeType == 0) {

            if (personType == 0) {

                return checkTcRuleByTwoZero(tcRule, listTcRule);

            } else {

                return checkTcRuleByTimeTypeZero(tcRule, listTcRule);
            }

        } else {

            if (personType == 0) {

                return checkTcRuleByPersonTypeZero(tcRule, listTcRule);

            } else {

                return checkTcRuleByNoZero(tcRule, listTcRule);
            }
        }
    }

    //   1, timeType ， personType 都为0
    private static List<TcRule> checkTcRuleByTwoZero(TcRule tcRule, List<TcRule> listTcRule) {

        return listTcRule;
    }

    //   2, timeType 为0 ， personType不为0
    private static List<TcRule> checkTcRuleByTimeTypeZero(TcRule tcRule, List<TcRule> listTcRule) {

        List<TcRule> returnListTcRule = new ArrayList<>();

        String personList = tcRule.getPersonList();

        for (TcRule checkTcRule : listTcRule) {

            String checkPersonList = checkTcRule.getPersonList();

            if (checkPersonId(personList, checkPersonList)) {

                returnListTcRule.add(checkTcRule);
            }
        }

        return returnListTcRule;
    }

    //    3，timeType不为0，personType为0
    private static List<TcRule> checkTcRuleByPersonTypeZero(TcRule tcRule, List<TcRule> listTcRule) {

        int count = 0;

        List<TcRule> returnListTcRule = new ArrayList<>();

        String startDate = tcRule.getStartDate();

        String endDate = tcRule.getEndDate();

        String week = tcRule.getWeek();

        String startTime = tcRule.getStartTime();

        String endTime = tcRule.getEndTime();

        List<String> listDate = WeekDayUtil.getDates(startDate, endDate, week);

        for (TcRule checkTcRule : listTcRule) {

            String checkStartDate = checkTcRule.getStartDate();

            String checkEndDate = checkTcRule.getEndDate();

            String checkWeek = checkTcRule.getWeek();

            String checkStartTime = checkTcRule.getStartTime();

            String checkEndTime = checkTcRule.getEndTime();

            List<String> listCheckDate = WeekDayUtil.getDates(checkStartDate, checkEndDate, checkWeek);

            for (String date : listDate) {

                String dateStart = date.concat(nullStr).concat(startTime);

                String dateEnd = date.concat(nullStr).concat(endTime);

                for (String checkDate : listCheckDate) {

                    String checkDateStart = checkDate.concat(nullStr).concat(checkStartTime);

                    String checkDateEnd = checkDate.concat(nullStr).concat(checkEndTime);

                    boolean dateFlag = WeekDayUtil.isOverlap(dateStart, dateEnd, checkDateStart, checkDateEnd);

                    if (dateFlag) {

                        count++;

                        returnListTcRule.add(checkTcRule);

                        break;
                    }
                }
                if (count > 0) {

                    count = 0;

                    break;
                }
            }
        }

        return returnListTcRule;

    }

    //    4, timeType不为0，personType不为0
    private static List<TcRule> checkTcRuleByNoZero(TcRule tcRule, List<TcRule> listTcRule) {

        List<TcRule> returnListTcRule = new ArrayList<>();

//        personId 有交集的list
        List<TcRule> listPersonTcRule = checkTcRuleByTimeTypeZero(tcRule, listTcRule);

        if (ListUtil.isNotEmpty(listPersonTcRule)) {

            List<TcRule> listTimeTcRule = checkTcRuleByPersonTypeZero(tcRule, listPersonTcRule);

            returnListTcRule.addAll(listTimeTcRule);
        }

        return returnListTcRule;
    }

    //    判断id 是否有交集
    private static boolean checkPersonId(String personId, String personIdList) {

        String[] personIds = personId.split(step);

        String[] personIdLists = personIdList.split(step);

        for (String personIdOne : personIds) {

            if (ArrayUtils.contains(personIdLists, personIdOne)) {

                return true;
            }
        }

        return false;
    }

    public static void main(String[] args) {

        TcRule tcRule = new TcRule(1, "2020-04-01", "2020-05-01", "1|2|0", "01:00", "21:00", 1, "100|101|102");

        TcRule tcRule_list_1 = new TcRule(1, "2020-04-01", "2020-05-01", "1|2|0", "01:00", "21:00", 1, "100|101|102");
        TcRule tcRule_list_2 = new TcRule(1, "2020-04-02", "2020-05-02", "1|3|0", "02:00", "22:00", 1, "100|101|202");
        TcRule tcRule_list_3 = new TcRule(1, "2020-06-01", "2020-07-01", "1|2|0", "01:00", "21:00", 1, "100|401|402");
        TcRule tcRule_list_4 = new TcRule(1, "2020-04-01", "2020-05-01", "3|4|5", "01:00", "21:00", 1, "100|501|502");
        TcRule tcRule_list_5 = new TcRule(1, "2020-04-01", "2020-05-01", "1|2|0", "21:00", "22:00", 1, "600|601|602");
        List<TcRule> list = new ArrayList<>();

        list.add(tcRule_list_1);
        list.add(tcRule_list_2);
        list.add(tcRule_list_3);
        list.add(tcRule_list_4);
        list.add(tcRule_list_5);

        System.out.println(checkTcRule(tcRule, list).toString());

    }
}
