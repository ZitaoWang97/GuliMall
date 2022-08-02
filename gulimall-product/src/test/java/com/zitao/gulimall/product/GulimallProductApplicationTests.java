package com.zitao.gulimall.product;

//import com.aliyun.oss.*;

import java.io.FileInputStream;
import java.io.InputStream;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.netflix.client.ClientException;
import com.zitao.gulimall.product.entity.BrandEntity;
import com.zitao.gulimall.product.service.BrandService;
import com.zitao.gulimall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest()
public class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    /*
    测试查找所属分类的完整路径
    [2, 34, 225]
     */
    @Test
    public void testFindPath() {
        Long[] categoryPath = categoryService.findCategoryPath(225L);
        log.info("完整路径:{}", Arrays.asList(categoryPath));
    }

//    @Autowired
//    OSSClient ossClient;

//    @Test
//    /*
//    1. 引入oss的starter
//    2. 配置key endpoint
//    3. 使用对象存储client进行相关操作
//    * */
//    public void testUploadStarter()  throws Exception{
//        // 填写Bucket名称，例如examplebucket。
//        String bucketName = "gulimall-zitao";
//        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
//        String objectName = "头像.jpg";
//        // 填写本地文件的完整路径，例如D:\\localpath\\examplefile.txt。
//        // 如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
//        String filePath= "C:\\Users\\Wang Zitao\\Pictures\\Saved Pictures\\头像.jpg";
//        try {
//            InputStream inputStream = new FileInputStream(filePath);
//            // 创建PutObject请求。
//            ossClient.putObject(bucketName, objectName, inputStream);
//            System.out.println("上传成功...");
//        } catch (OSSException oe) {
//            System.out.println("Caught an OSSException, which means your request made it to OSS, "
//                    + "but was rejected with an error response for some reason.");
//            System.out.println("Error Message:" + oe.getErrorMessage());
//            System.out.println("Error Code:" + oe.getErrorCode());
//            System.out.println("Request ID:" + oe.getRequestId());
//            System.out.println("Host ID:" + oe.getHostId());
//        } catch (ClientException ce) {
//            System.out.println("Caught an ClientException, which means the client encountered "
//                    + "a serious internal problem while trying to communicate with OSS, "
//                    + "such as not being able to access the network.");
//            System.out.println("Error Message:" + ce.getMessage());
//        } finally {
//            if (ossClient != null) {
//                ossClient.shutdown();
//            }
//        }
//    }
//
//    @Test
//    /*
//    使用原生的SDK
//    * */
//    public void testUploadSDK() throws Exception {
//        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
//        String endpoint = "oss-eu-central-1.aliyuncs.com";
//        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
//        String accessKeyId = "LTAI5tEmjWJmidi9c3YTseRf";
//        String accessKeySecret = "ZmoZ8IDnA4ObqWkwfk7QIR0ewaAQMs";
//        // 填写Bucket名称，例如examplebucket。
//        String bucketName = "gulimall-zitao";
//        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
//        String objectName = "头像.jpg";
//        // 填写本地文件的完整路径，例如D:\\localpath\\examplefile.txt。
//        // 如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
//        String filePath= "C:\\Users\\Wang Zitao\\Pictures\\Saved Pictures\\头像.jpg";
//
//        // 创建OSSClient实例。
//        OSS ossClient1 = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//
//        try {
//            InputStream inputStream = new FileInputStream(filePath);
//            // 创建PutObject请求。
//            ossClient1.putObject(bucketName, objectName, inputStream);
//            System.out.println("上传成功...");
//        } catch (OSSException oe) {
//            System.out.println("Caught an OSSException, which means your request made it to OSS, "
//                    + "but was rejected with an error response for some reason.");
//            System.out.println("Error Message:" + oe.getErrorMessage());
//            System.out.println("Error Code:" + oe.getErrorCode());
//            System.out.println("Request ID:" + oe.getRequestId());
//            System.out.println("Host ID:" + oe.getHostId());
//        } catch (ClientException ce) {
//            System.out.println("Caught an ClientException, which means the client encountered "
//                    + "a serious internal problem while trying to communicate with OSS, "
//                    + "such as not being able to access the network.");
//            System.out.println("Error Message:" + ce.getMessage());
//        } finally {
//            if (ossClient1 != null) {
//                ossClient1.shutdown();
//            }
//        }
//
//    }

    @Test
    public void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
        // 测试增添功能
        brandEntity.setName("xiaomi");
        brandService.save(brandEntity);
        System.out.println("保存成功...");
        // 测试修改功能
        brandEntity.setBrandId(2l);
        brandEntity.setName("huawei");
        brandService.updateById(brandEntity);
        // 测试查找功能
        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 2));
        list.forEach((item) -> {
            System.out.println(item);
        });
    }

}
