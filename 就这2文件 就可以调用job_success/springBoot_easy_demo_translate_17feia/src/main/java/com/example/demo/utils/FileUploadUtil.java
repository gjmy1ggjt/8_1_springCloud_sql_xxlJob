package com.example.demo.utils;

import com.example.demo.entity.StaticFileObj;
import com.example.demo.translate_work.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Administrator on 2020/3/7.
 */
@Slf4j
public class FileUploadUtil {
//    http://localhost:8769/demo/importExcel/2020/03/07/212f1747c134440b9ef31229afb14e1d/1583571796279.jpg
//    C:\Users\Administrator\Desktop\springCloud_221\idea_open\springBoot_easy_demo_translate_17feia\importExcel\2020\03\07\212f1747c134440b9ef31229afb14e1d\1583571796279.jpg
//    http://localhost:8769/demo/picture/1583571796279.jpg

    public static String div = "http://localhost:8769/demo/picture";

    public static String pictureDir = "/importExcel";
//    public static String pictureDir = "/src/main/resources/static/picture";

//    public static DataGrid<StaticFileObj> uploadFile(MultipartFile file) {
//1366642016026019
//        String companyFileName = UUID.randomUUID().toString().replace("-", "");
//        //改为上传到本地静态文件服务器
//        DataGrid dataGrid = new DataGrid<>();
//        try {
//            String dateTime = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
//            String saveDir = /importExcel/ + dateTime + "/" + companyFileName + "/";
//            String rootDir = canonicalPath() + "/" +  saveDir;
////            String rootDir = fileUploadPath + saveDir;
//            File root = new File(rootDir);
//            if (!root.exists()) {
//                root.mkdirs();
//            }
//            String realFileName = file.getOriginalFilename();
//            int index = realFileName.lastIndexOf(".");
//            String fileName = System.currentTimeMillis() + realFileName.substring(index);
//            String filePath = rootDir + "/" + fileName;
//            String savePath = "/" + saveDir + fileName;
//            File newFile = new File(filePath);
//            file.transferTo(newFile);
//            dataGrid.setFlag(true);
//
//            StaticFileObj staticFileObj = new StaticFileObj();
//            http://localhost:8769/demo/picture/1583571796279.jpg
//
//            staticFileObj.setDownloadPath(canonicalPath() + savePath);
//            staticFileObj.setFileName(file.getName());
//            staticFileObj.setSavePath(savePath);
////  C:\Users\Administrator\Desktop\springCloud_221\idea_open\springBoot_easy_demo_translate_17feia/importExcel/2020/03/07/5cb806e1b575478b9cacab62708a5152/1583570942270.jpg
//            dataGrid.setObj(staticFileObj);
//        } catch (Exception e) {
//            e.printStackTrace();
//            String errorMsg = String.format("查询失败！，error=%s", e.getMessage());
//            log.error(errorMsg, e);
//            dataGrid.setMsg(errorMsg);
//        }
//        return dataGrid;
//    }

    public static DataGrid<StaticFileObj> uploadFile(MultipartFile file) {

        String companyFileName = UUID.randomUUID().toString().replace("-", "");
        //改为上传到本地静态文件服务器
        DataGrid dataGrid = new DataGrid<>();
        try {
            String saveDir = pictureDir + "/";
            String rootDir = canonicalPath() + pictureDir + "/";
            File root = new File(rootDir);
            if (!root.exists()) {
                root.mkdirs();
            }
            String realFileName = file.getOriginalFilename();
            int index = realFileName.lastIndexOf(".");
            String fileName = System.currentTimeMillis() + realFileName.substring(index);
            String filePath = rootDir + "/" + fileName;
            String savePath = saveDir + fileName;
            File newFile = new File(filePath);
            file.transferTo(newFile);
            dataGrid.setFlag(true);

            StaticFileObj staticFileObj = new StaticFileObj();

            staticFileObj.setDownloadPath(div + "/" + fileName);
//            staticFileObj.setDownloadPath(canonicalPath() + savePath);
            staticFileObj.setFileName(file.getName());
            staticFileObj.setSavePath(savePath);
            dataGrid.setObj(staticFileObj);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = String.format("查询失败！，error=%s", e.getMessage());
            log.error(errorMsg, e);
            dataGrid.setMsg(errorMsg);
        }
        return dataGrid;
    }

    public static String canonicalPath() {

        String courseFile = "";

        try {
            File directory = new File("");//参数为空

            courseFile = directory.getCanonicalPath();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return courseFile.replaceAll("\\\\", "/");
    }
}
