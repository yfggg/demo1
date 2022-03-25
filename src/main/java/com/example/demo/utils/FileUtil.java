package com.example.demo.utils;

import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import cn.afterturn.easypoi.handler.inter.IExcelExportServer;
import cn.hutool.core.util.NumberUtil;
import com.example.demo.entity.TestData;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import org.elasticsearch.search.SearchHit;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Component
public class FileUtil {

    @Resource
    private RedisUtil redisUtils;

    /**
     * excel上传，开启验证传入的参数 需要在pom中加入hibernate-validator，validation-api依赖
     *
     * @param filePath
     * @param contentClass
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> List<T> readExcel(String filePath, Class<T> contentClass){
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(filePath);
            ImportParams importParams = new ImportParams();
            // 设置表格坐标
            importParams.setStartSheetIndex(0);
            // 验证传入的参数
            importParams.isNeedVerify();
            // 读取数据
            return ExcelImportUtil.importExcel(fileInputStream, contentClass, importParams);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fileInputStream) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * excel上传，开启验证传入的参数 需要在pom中加入hibernate-validator，validation-api依赖
     *
     * @param fileName excel.xls
     * @param contentClass
     * @return
     */
    public static <T> ExcelImportResult<T> readExcelMore(String fileName, Class<T> contentClass) {
        ImportParams importParams = new ImportParams();
        // 设置表格坐标
        importParams.setStartSheetIndex(0);
        // 验证传入的参数
        importParams.isNeedVerify();
        // 在线程中运行的代码可以通过该类加载器来加载类与资源
        URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        log.info("文件保存路径:{}", url);
        if(null == url) {
            return null;
        }
        return ExcelImportUtil.importExcelMore(new File(url.getFile()), contentClass, importParams);
    }

    /**
     * 5W以内
     * excel客户端下载，需要在contentClass中需要加上@Excel
     *
     * @param fileName
     * @param contents
     * @param contentClass
     * @param response
     * @throws IOException
     */
    public static <T> void writeExcel(String fileName,
                                      List<T> contents,
                                      Class<T> contentClass,
                                      HttpServletResponse response) {
        OutputStream outputStream = null;
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");

            //因为游览器不能直接使用中文，需要进行编码
            String splicingName = new StringBuffer(fileName).append(".xlsx").toString();
            String fianlName = new String(splicingName.getBytes(), "iso8859-1");

            //添加Content-disposition响应头，在用户请求下载文件的时候，设置文件的文件名
            response.addHeader("content-disposition", "attachment;filename=" + fianlName);

            Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), contentClass, contents);
            outputStream = response.getOutputStream();
            workbook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(null != outputStream) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 数据量大超过5W,还在100W以内
     * excel客户端下载，需要在contentClass中需要加上@Excel
     *
     * @param fileName
     * @param contents
     * @param contentClass
     * @param response
     * @throws IOException
     */
    public static <T> void writeBigExcel(String fileName,
                                         List<T> contents,
                                         Class<T> contentClass,
                                         HttpServletResponse response) {
        OutputStream outputStream = null;
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");

            //因为游览器不能直接使用中文，需要进行编码
            String splicingName = new StringBuffer(fileName).append(".xlsx").toString();
            String fianlName = new String(splicingName.getBytes(), "iso8859-1");

            //添加Content-disposition响应头，在用户请求下载文件的时候，设置文件的文件名
            response.addHeader("content-disposition", "attachment;filename=" + fianlName);

            int pageSize = 10000;
            int totalPage = (contents.size() / pageSize) + 1;
            Workbook workbook = ExcelExportUtil.exportBigExcel(new ExportParams(), contentClass, new IExcelExportServer() {

                @Override
                public List<Object> selectListForExcelExport(Object obj, int page) {
                    if (page > totalPage) {
                        return null;
                    }

                    // fromIndex开始索引，toIndex结束索引
                    int fromIndex = (page - 1) * pageSize;
                    int toIndex = page != totalPage ? fromIndex + pageSize :contents.size();

                    List<Object> list = new ArrayList<>();
                    list.addAll(contents.subList(fromIndex, toIndex));

                    return list;
                }
            }, totalPage);

            outputStream = response.getOutputStream();
            workbook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(null != outputStream) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 异步显示进度条
     *
     * @param key
     * @param total
     * @param consumer
     */
//    @Async
//    public void progressBar(String key, Integer total, Consumer<Integer> consumer) {
//        int j = 1;
//        if(100 < total) {
//            j = NumberUtil.div(total, new Double(100)).intValue();
//        }
//        for (int i = j; i <= total; i+=j) {
//            Double scale = NumberUtil.div((float)i, (float)total);
//            Double progress = NumberUtil.mul(scale, new Double(100));
//            consumer.accept(total);
//            redisUtils.set(key, progress.intValue());
//        }
//    }

}
