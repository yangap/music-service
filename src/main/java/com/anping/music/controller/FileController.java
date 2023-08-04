package com.anping.music.controller;

import com.anping.music.config.SpringContextHolder;
import com.anping.music.entity.DownloadInfo;
import com.anping.music.entity.MusicSource;
import com.anping.music.service.CatchService;
import com.anping.music.utils.DownloadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

/**
 * @author Anping Sec
 * @date 2023/03/06
 * description:
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${static.path}")
    private String tmp;

    @GetMapping("/download")
    public void download(@RequestParam String source,MusicSource musicSource, HttpServletResponse response) {
        CatchService catchService = ((CatchService) SpringContextHolder.getBean(source));
        String localPath = catchService.getByMV(musicSource);
        downLocalFile(localPath, response);
    }

    @GetMapping("/downloadSource")
    public void downloadSource(@RequestParam String mid, String musicName,@RequestParam String source, HttpServletResponse response) {
        log.info("download music {}",musicName);
        DownloadInfo.totalDownload.incrementAndGet();
        CatchService catchService = ((CatchService) SpringContextHolder.getBean(source));
        String listenUrl = catchService.getListenDetail(mid,null).getResourceUrl();
        if(StringUtils.isEmpty(listenUrl)){
            log.info("找不到歌词源!");
            return;
        }
        int end = listenUrl.indexOf("?");
        int start = 0;
        for (int i = end; i >= 0; i--) {
            if (listenUrl.charAt(i) == '.') {
                start = i;
                break;
            }
        }
        String type = listenUrl.substring(start + 1, end);
        DownloadUtils.downNetFile(listenUrl, musicName + "." + type, response);
    }


    public void downLocalFile(String path, HttpServletResponse response) {
        InputStream inputStream = null;
        ServletOutputStream outputStream = null;
        // 读到流中
        //从输入流中读取一定数量的字节，并将其存储在缓冲区字节数组中，读到末尾返回-1
        try {
            inputStream = new FileInputStream(path);// 文件的存放路径
            response.setContentType("application/octet-stream");
            String filename = new File(path).getName();
            response.addHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));
            outputStream = response.getOutputStream();
            byte[] b = new byte[1024];
            int len;
            while ((len = inputStream.read(b)) > 0) {
                outputStream.write(b, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                File file = new File(path);
                file.delete();
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
