package com.example.demo.service.impl;

import com.example.demo.entity.StaticFileObj;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.utils.*;
import com.example.demo.vo.UserListRequestVo;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Administrator on 2020/1/23.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Value("${static.file.upload}")
    private String staticFileUpload;

    @Value("${static.file.server}")
    private String staticFileServer;

    @Override
    public DataGrid<User> list(UserListRequestVo requestVo) {

        List<User> listGetUser = new ArrayList<>();

        PagingDto pagingDto = requestVo.getPagingDto();

        List<User> newListUser = CreateDatas.list;

        int sizeUse = newListUser.size();

        if (ObjectUtils.isEmpty(pagingDto)) {


        } else {

            Integer pageSize = pagingDto.getPageSize();

            Integer pageNo = pagingDto.getPageNo();

            Integer start = (pageNo - 1) * pageSize;

            Integer end = pageNo * pageSize;

            Integer size = newListUser.size();

            if (start > size) {

                return new DataGrid<User>(true, new ArrayList<>());
            }

            if (end > size) {

                listGetUser = newListUser.subList(start, size);

            } else {

                listGetUser = newListUser.subList(start, end);
            }
        }

        String userId = requestVo.getUserId();

        if (StringUtils.isNotBlank(userId)) {

            listGetUser = new ArrayList<>();

            List<String> listUserId = newListUser.stream().map(user -> user.getId()).collect(Collectors.toList());

            if (listUserId.contains(userId)) {

                User user = newListUser.stream().filter(user1 -> userId.equals(user1.getId())).findFirst().get();

                if (!ObjectUtils.isEmpty(user)) {

                    listGetUser.add(user);
                }

            } else {

                return new DataGrid<>(false, "当前数据不存在这个userId:" + userId);

            }

        } else {

//            listGetUser.addAll(newListUser);
        }

        if (listGetUser.size() == 10) {

            sizeUse = CreateDatas.list.size();

        } else if (listGetUser.size() == 1){

            sizeUse = listGetUser.size();

        } else {

            sizeUse = CreateDatas.list.size();
        }

        return new DataGrid<>(true, listGetUser, sizeUse);
//        return new DataGrid<>(true, listGetUser, CreateDatas.list.size());
    }


    @Override
    public DataGrid<User> getOne(String userId) {

        if (StringUtils.isBlank(userId)) {

            return new DataGrid<>(false, "查询一条数据时，用户id不能为空");

        }
        User user = null;
//        先判断 userId 是否存在于list\的数据里
            if (CreateDatas.checkListToId(userId)) {

                user = CreateDatas.list.stream().filter(user1 -> userId.equals(user1.getId())).findFirst().get();

            } else {

                return new DataGrid<>(false, "查询一条数据时，用户id不存在");

        }

        if (ObjectUtils.isEmpty(user)) {

            return new DataGrid<>(false, "查询一条数据时，用户id不存在");

        }

        return new DataGrid<>(true, user);
    }

    @Override
    public DataGrid dropUser() {

        Map<String, String> map = new LinkedHashMap<>();

        for (User user : CreateDatas.list) {

            map.put(user.getId(), user.getName());
        }
        return new DataGrid(true, map);
    }

    @Override
    public DataGrid<String> update(User user) {

        if (StringUtils.isBlank(user.getId())) {

            return new DataGrid(false, "修改时，用户id不能为空");

        }

        if (StringUtils.isBlank(user.getCode())) {

            return new DataGrid(false, "用户角色不能为空！");

        }

        if (StringUtils.isBlank(user.getName())) {

            return new DataGrid(false, "用户名称不能为空！");

        }

        if (StringUtils.isBlank(user.getPassword())) {

            return new DataGrid(false, "用户密码不能为空！");

        }

        String photoUrl =  user.getPhotoUrl();

        if (StringUtils.isBlank(user.getPhotoUrl())) {

            return new DataGrid(false, "用户照片不能为空！");

        }

        if (photoUrl.indexOf(":") == -1) {

            photoUrl = FileUploadUtil.div + photoUrl;

            user.setPhotoUrl(photoUrl);
        }

        String userId = user.getId();
        List<User> list = CreateDatas.list;
        for(Iterator<User> it=list.iterator();it.hasNext();){

            User  user1 =it.next();

            String user1Id = user1.getId();
            if (userId.equals(user1Id)) {
                it.remove();
                list.add(user);
                break;
            }
        }

        return new DataGrid(true, "用户修改成功！");

    }

    @Override
    public DataGrid<String> save(User user) {

        if (StringUtils.isBlank(user.getCode())) {

            return new DataGrid(false, "用户角色不能为空！");

        }

        if (StringUtils.isBlank(user.getName())) {

            return new DataGrid(false, "用户名称不能为空！");

        }

        if (StringUtils.isBlank(user.getPassword())) {

            return new DataGrid(false, "用户密码不能为空！");

        }

        user.setId(UUID.randomUUID().toString());

        String photoUrl =  user.getPhotoUrl();

        if (StringUtils.isBlank(user.getPhotoUrl())) {

            return new DataGrid(false, "用户图片不能为空！");

        }

        if (photoUrl.indexOf(":") == -1) {

            photoUrl = FileUploadUtil.div + photoUrl;

            user.setPhotoUrl(photoUrl);
        }

        CreateDatas.list.add(user);

        return new DataGrid(true, "用户保存成功");
    }

    @Override
    public DataGrid<String> deleteOne(String userId) {

        if (StringUtils.isBlank(userId)) {

            return new DataGrid(false, "用户id不能为空");
        }

        if (CreateDatas.checkListToId(userId)) {

            for(Iterator<User> it=CreateDatas.list.iterator();it.hasNext();){

                User  user1 =it.next();

                String user1Id = user1.getId();
                if (userId.equals(user1Id)) {
                    it.remove();
                    break;
                }
            }
            return new DataGrid(true, "用户删除成功");

        } else {

            return new DataGrid(true, "当前用户id不存在");
        }

    }

    @Override
    public DataGrid<String> deleteBatch(List<String> listId) {

        if (ListUtil.isEmpty(listId)) {

            return new DataGrid(false, "用户id不能为空");

        }

        List<String> listNewId = CreateDatas.list.stream().map(user -> user.getId()).collect(Collectors.toList());

        if (listNewId.containsAll(listId)) {

            for(Iterator<User> it=CreateDatas.list.iterator();it.hasNext();){
                User  user1 =it.next();
                String user1Id = user1.getId();
                if (listId.contains(user1Id)) {
                    it.remove();
                }
            }
            return new DataGrid(true, "用户批量删除成功");

        } else {

            return new DataGrid(false, "有不存在的id，无法批量删除");
        }


    }

    @Override
    public DataGrid<String> resetDatas() {
        CreateDatas createDatas = new CreateDatas();
        CreateDatas.list = new ArrayList<>();
        createDatas.init();
        return new DataGrid(true, "重置数据成功");
    }

    @Override
    public DataGrid<StaticFileObj> uploadImg(MultipartFile file) {
        return FileUploadUtil.uploadFile(file);
    }
}
