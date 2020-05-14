package com.example.demo.translate_work;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2020/2/29.
 */
public class ListUtils {

    public static boolean isEmpty(List list) {

        if (list != null && list.size() > 0) {

            return false;

        } else {

            return true;
        }
    }

    public static boolean isNotEmpty(List list) {
        return !isEmpty(list);
    }

    public static boolean hasString(String str, List<String> list) {

        for (String l : list) {

            if (str.indexOf(l) != -1) {

                return true;

            }
        }

        return false;
    }

    public static List<TranslateExcel> createEntityByMap(Map<String, Map<String, List<String>>> datas) {

        List<TranslateExcel> list = new ArrayList<>();

        int count = 0;

        Iterator it = datas.entrySet().iterator();

        while (it.hasNext()) {

            Map.Entry entry = (Map.Entry) it.next();

            String className = (String) entry.getKey();

            Map<String, List<String>> classData = (Map<String, List<String>>) entry.getValue();

            Iterator itClassData = classData.entrySet().iterator();

            while (itClassData.hasNext()) {

                count++;

                Map.Entry entryClassData = (Map.Entry) itClassData.next();

                String methodName = (String) entryClassData.getKey();

                List<String> listChinese = (List<String>) entryClassData.getValue();

                TranslateExcel translateExcel = new TranslateExcel();

                translateExcel.setNum(count);

                translateExcel.setClassName(className);

                translateExcel.setMethodName(methodName);

                translateExcel.setChinese(listChinese);

                list.add(translateExcel);
            }
        }
        return list;
    }
}
