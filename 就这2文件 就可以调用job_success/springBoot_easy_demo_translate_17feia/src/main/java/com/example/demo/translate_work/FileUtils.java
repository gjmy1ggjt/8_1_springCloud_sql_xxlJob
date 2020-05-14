package com.example.demo.translate_work;

import java.io.*;
import java.util.*;

/**
 * Created by Administrator on 2020/2/28.
 */
public class FileUtils {

    public static List<File> listFile = new ArrayList<>();

    public static List<Map<String, List<Map<String, List<String>>>>> listDatas = new ArrayList<>();

    public static String canonicalPath() {

        String courseFile = "";

        try {
            File directory = new File("");//参数为空

            courseFile = directory.getCanonicalPath();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return courseFile;
    }

    public static String thisFileClassPath(Class clazz) {

        return clazz.getResource("").toString();
    }

    public static String getClassName(String fileName) {
//  UserController
        return fileName.substring(fileName.lastIndexOf(Constant.step_right_two) + 1, fileName.lastIndexOf("."));

    }

    public static String getClassDirName(String fileName) {
//        src
        return fileName.substring(fileName.lastIndexOf(Constant.step_right_two) + 1, fileName.length());

    }

    public static String getClassFileName(String fileName) {
//  UserController.class
        return fileName.substring(fileName.lastIndexOf(Constant.step_right_two) + 1, fileName.length());

    }

    public static String getClassFileLastName(String fileName) {
//UserController.class => class

        String[] arr = fileName.split(Constant.point);

        if (arr != null && arr.length == 2) {

            return arr[1];

        } else {

            return fileName;
        }
    }

    public static List<File> getFiles(String filePath) {
//        用全局变量是因为 递归算法，避免结果丢失
        listFile = new ArrayList<>();

        File root = new File(filePath);

        File[] files = root.listFiles();

        for (File file : files) {

            if (file.isDirectory()) {

                if (Constant.getDirListName().contains(file.getName())) {
//                  controller 里面也存在文件夹不过只有一层
                    if (file.isDirectory()) {

                        getListFile(file.getAbsolutePath());

                    }
                }
            } else {

                String className = getClassFileName(file.getName());

                if (Constant.javaType.equals(getClassFileLastName(className))) {

                    listFile.add(file);
                }
//                System.out.println("显示" + filePath + "下所有子目录" + file.getAbsolutePath());
            }
        }

        return listFile;
    }

    public static List<File> getListFile(String filePath) {

        File rootClild = new File(filePath);

        File[] filesClild = rootClild.listFiles();

        for (File fileClild : filesClild) {

            String fileClildName = fileClild.getName();

            if (fileClild.isDirectory()) {

                getListFile(fileClild.getAbsolutePath());

            } else {

                String className = getClassFileName(fileClildName);

                if (Constant.javaType.equals(getClassFileLastName(className))) {

                    listFile.add(fileClild);
                }
            }
        }
        return listFile;
    }

    public static Map<String, List<Map<String, List<String>>>> readDatasFile(File file) {

        String className = file.getName();

        Map<String, List<Map<String, List<String>>>> map = new HashMap<>();

        List<Map<String, List<String>>> list = new ArrayList<>();

        List<String> listString = new ArrayList<>();

        BufferedReader br = null;

        try {

            br = new BufferedReader(new FileReader(file));

            String line = null;

            while ((line = br.readLine()) != null) {

                String useLine = IgnoreStringUtils.noIgnoreStringTwo(line);

                if (StringUtils.isNotBlank(useLine)) {

                    if (StringUtils.hasChinese(useLine) && !IgnoreStringUtils.ignoreStringLine(useLine)) {

                        listString.add(useLine);
                    }
                }
            }
        } catch (
                Exception e)

        {
            e.printStackTrace();

        } finally

        {
            try {

                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    public static List<String> readListFile(File file) {

        List<String> listString = new ArrayList<>();

        BufferedReader br = null;

        try {
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), "utf-8");
            br = new BufferedReader(read);

            String line = null;

            while ((line = br.readLine()) != null) {

                String useLine = IgnoreStringUtils.noIgnoreStringTwo(line);

                if (StringUtils.isNotBlank(useLine)) {

                    if (StringUtils.hasChinese(useLine) && !IgnoreStringUtils.ignoreStringLine(useLine)) {

                        listString.add(useLine.trim());
                    }
                }
            }
        } catch (
                Exception e)

        {
            e.printStackTrace();

        } finally

        {
            try {

                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return listString;
    }

    public static Map<String, Map<String, List<String>>> writeListDatas() {

        FileUtils.getFiles(FileUtils.canonicalPath());

        Map<String, Map<String, List<String>>> mapMap = new LinkedHashMap();

        for (File file : listFile) {

            String className = getClassName(file.getName());

            Map<String, List<String>> mapFileMethodAndChinese = MethodStringUtils.getMapFileMethodAndChinese(file);

            if (mapFileMethodAndChinese != null && mapFileMethodAndChinese.size() > 0) {

                mapMap.put(className, mapFileMethodAndChinese);
            }
        }

        return mapMap;

    }

    public static void writeListString(List<String> list) {

        BufferedWriter bi = null;
        try {
            File file = new File(canonicalPath() + "\\test.txt");

            bi = new BufferedWriter(new FileWriter(file));

            for (String str : list) {

                bi.write(str.trim());

                bi.write("\r\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bi.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //    java -classpath ****.jar com.example.demo.translate_work.FileUtils [args]
    public static void main(String[] args) {

        System.out.println(writeListDatas().size());

        System.out.println(writeListDatas().toString());

    }


}
