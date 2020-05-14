package com.example.demo.controller;

import com.example.demo.entity.StaticFileObj;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.translate_work.FileUtils;
import com.example.demo.translate_work.ListUtils;
import com.example.demo.translate_work.TranslateExcel;
import com.example.demo.translate_work.excel.ExcelData;
import com.example.demo.translate_work.excel.ExportExcelUtils;
import com.example.demo.utils.CreateDatas;
import com.example.demo.utils.DataGrid;
import com.example.demo.vo.UserDeleteBatchRequestVo;
import com.example.demo.vo.UserListRequestVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by Administrator on 2020/1/23.
 */
@RestController
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/path/{id}", method = RequestMethod.GET)
    public DataGrid<User> getPath(@PathVariable("id") String id) {

        return userService.getOne(id);
    }

    @RequestMapping(value = "/one", method = RequestMethod.GET)
    public DataGrid<User> getParam(@RequestParam("id") String id) {

        return userService.getOne(id);
    }

    @RequestMapping(value = "/one/formdate", method = RequestMethod.GET)
    public DataGrid<User> getFormdate(String id) {

        return userService.getOne(id);
    }

    @RequestMapping(value = "/save_json", method = RequestMethod.POST)
    public DataGrid<String> saveJson(@RequestBody User user) {

        return userService.save(user);
    }

    @RequestMapping(value = "/save_formdata", method = RequestMethod.POST)
    public DataGrid<String> saveFormdata(User user) {

        return userService.save(user);
    }


    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    public DataGrid<String> delete(@PathVariable("id") String id) {

        return userService.deleteOne(id);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.GET)
    public DataGrid<String> deleteParam(@RequestParam("id") String id) {

        return userService.deleteOne(id);
    }

    @RequestMapping(value = "/deleteBatch", method = RequestMethod.POST)
    public DataGrid<String> deleteBatch(@RequestParam("listId") List<String> listId) {

        return userService.deleteBatch(listId);
    }

    @RequestMapping(value = "/deleteBatch_body", method = RequestMethod.POST)
    public DataGrid<String> deleteBatch_body(@RequestBody UserDeleteBatchRequestVo requestVo) {

        return userService.deleteBatch(requestVo.getListUserId());
    }

    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    public DataGrid<String> update(@RequestBody User user) {

        return userService.update(user);
    }

    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public DataGrid<User> list(@RequestBody UserListRequestVo requestVo) {

        return userService.list(requestVo);
    }

    @RequestMapping(value = "/drop", method = RequestMethod.GET)
    public DataGrid<User> drop() {

        return userService.dropUser();
    }

    @RequestMapping(value = "/data/reset", method = RequestMethod.GET)
    public DataGrid<String> reset() {

        return userService.resetDatas();
    }

    //    @ApiOperation(value = "上传图片通用接口", notes = "by Hukai")
//    @PostMapping(value = "/uploadImg")
    @RequestMapping(value = "/data/uploadImg", method = RequestMethod.POST)
    public DataGrid<StaticFileObj> uploadImg(@RequestParam("file") MultipartFile file) {

        return userService.uploadImg(file);
    }


    @RequestMapping(value = "/repect/translate", method = RequestMethod.GET)
    public DataGrid<String> translate() {

        FileUtils.getFiles(FileUtils.canonicalPath());

        List<String> listString = new ArrayList<>();

        for (File file : FileUtils.listFile) {

            List<String> list = FileUtils.readListFile(file);

            if (ListUtils.isNotEmpty(list)) {

                listString.addAll(list);
            }
        }

        FileUtils.writeListString(listString);

        return new DataGrid<String>(true, new ArrayList<>());
    }

    @RequestMapping(value = "/excel/export", method = RequestMethod.POST)
    public void exportExcel(HttpServletResponse response, @RequestBody UserListRequestVo requestVo) {

        requestVo.setPagingDto(null);
        List<String> titleList = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        List<User> listUser = list(requestVo).getRows();

        for (User user : listUser) {
            List<Object> dataList = new ArrayList<>();
            if (titleList.size() == 0) {
                titleList.add("用户id");
                titleList.add("用户角色");
                titleList.add("用户名称");
                titleList.add("用户密码");
                titleList.add("用户照片");
              }
            dataList.add(user.getId());
            dataList.add(user.getCode());
            dataList.add(user.getName());
            dataList.add(user.getPassword());
            dataList.add(user.getPhotoUrl());
            rows.add(dataList);
        }
        String fileName = "用户" + System.currentTimeMillis() + ".xlsx";
        ExcelData excelData = new ExcelData();
        excelData.setName("用户");
        excelData.setRows(rows);
        excelData.setTitles(titleList);

        try {
            ExportExcelUtils.exportExcel(response, fileName, excelData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    @RequestMapping(value = "/repect/export", method = RequestMethod.GET)
//    public void export(HttpServletResponse response) {
//
//        Map<String, Map<String, List<String>>> datas = FileUtils.writeListDatas();
//
//        List<String> titleList = new ArrayList<>();
//
//        List<List<Object>> rows = new ArrayList<>();
//
//        for (TranslateExcel excel : ListUtils.createEntityByMap(datas)) {
//
//            List<Object> dataList = new ArrayList<>();
//
//            if (titleList.size() == 0) {
//
//                titleList.add("序号");
//                titleList.add("类名位置");
//                titleList.add("方法名");
//                titleList.add("意思描述");
//                titleList.add("原始提示文字");
//                titleList.add("最终提示中文");
//                titleList.add("最终提示英文");
//            }
//
//            dataList.add(excel.getNum());
//            dataList.add(excel.getClassName());
//            dataList.add(excel.getMethodName());
//            dataList.add(excel.getMean());
//            dataList.add(String.join("\r\n", excel.getChinese()));
//            dataList.add(null);
//            dataList.add(null);
//            rows.add(dataList);
//        }
//        String fileName = "保险规则" + System.currentTimeMillis() + ".xlsx";
//        ExcelData excelData = new ExcelData();
//        excelData.setName("保险规则");
//        excelData.setRows(rows);
//        excelData.setTitles(titleList);
//
//        try {
//            ExportExcelUtils.exportExcel(response, "问题订单.xlsx", excelData);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
