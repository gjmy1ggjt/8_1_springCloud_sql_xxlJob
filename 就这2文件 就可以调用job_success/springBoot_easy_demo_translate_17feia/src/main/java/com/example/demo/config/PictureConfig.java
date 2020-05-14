package com.example.demo.config;

import com.example.demo.utils.FileUploadUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by Administrator on 2020/3/8.
 */

@Configuration
public class PictureConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//映射图片保存地址

        String pathUrl = "file:" + FileUploadUtil.canonicalPath() + FileUploadUtil.pictureDir + "/";
        registry.addResourceHandler("/picture/importExcel/**").addResourceLocations(pathUrl);
    }

}
