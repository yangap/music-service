package com.anping.music.utils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * @author Anping Sec
 * @date 2023/06/27
 * description:
 */
public class DownloadUtils {

    public static void downNetFile(String path, String fileName, HttpServletResponse response) {
        InputStream inStream = null, inputStream = null;
        ServletOutputStream outputStream = null;
        ByteArrayOutputStream copy = null;
        int total = 0;
        try {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            String encodeFile = URLEncoder.encode(fileName, "UTF-8");
            response.setContentType("application/octet-stream");
            response.addHeader("Content-Disposition", "attachment; filename=" + encodeFile);
            response.addHeader("filename", encodeFile);
            response.setHeader("Access-Control-Expose-Headers", "filename,Content-Disposition");
            inStream = conn.getInputStream();
            copy = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 2];
            int len;
            while ((len = inStream.read(buffer)) != -1) {
                total += len;
                copy.write(buffer, 0, len);
            }
            response.addHeader("Content-Length", Integer.toString(total));
            inputStream = new ByteArrayInputStream(copy.toByteArray());
            outputStream = response.getOutputStream();
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inStream != null) inStream.close();
                if (copy != null) copy.close();
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void downloadNetFile(String path, String outPath, Map<String,Object> headers) {
        FileOutputStream fileOutputStream = null;
        InputStream inputStream = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
            if(headers!=null){
                for(Map.Entry<String,Object> entry : headers.entrySet()){
                    conn.setRequestProperty(entry.getKey(),entry.getValue().toString());
                }
            }
            conn.connect();
            fileOutputStream = new FileOutputStream(outPath);
            inputStream = conn.getInputStream();
            int len;
            byte[] buffer = new byte[2 * 1024];
            while ((len = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (fileOutputStream != null) fileOutputStream.close();
                if (conn != null) conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
