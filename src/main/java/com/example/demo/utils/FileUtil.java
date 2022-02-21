package com.example.demo.utils;

import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

@Slf4j
@Component
public class FileUtil {

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
                if (fileInputStream != null) {
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
            Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), contentClass, contents);
            // 设置浏览器用分段(part)请求
            response.setContentType("multipart/form-data");
            // 设置消息头，告诉浏览器，我要下载
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");
            outputStream = response.getOutputStream();
            workbook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
