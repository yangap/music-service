package com.anping.music.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public final class FFmpeg {

    @Value("${ffmpeg.path}")
    private String FFMPEG_PATH;

    private static final Map<String, Process> processMap = new ConcurrentHashMap<>();

    /**
     * 执行FFmpeg命令
     *
     * @param isWait  是否等待ffmpeg命令执行完
     * @return FFmpeg程序在执行命令过程中产生的各信息，执行出错时返回null
     */
    public String executeCommand(String cmd, String businessId, boolean isWait) {
        cmd = cmd.replaceFirst("ffmpeg", FFMPEG_PATH);
        if (StringUtils.isEmpty(cmd)) {
            log.error("{} cmd is null", businessId);
            return null;
        }
        log.info("--- will execute command：---{}", cmd);
        Runtime runtime = Runtime.getRuntime();
        Process ffmpeg = null;
        try {
            // 执行ffmpeg指令
            ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", cmd);
            ffmpeg = builder.start();
            log.info("--- 开始执行FFmpeg指令：--- 执行线程名：" + builder.toString());
            //如果关联业务id
            if (StringUtils.isNotEmpty(businessId)) {
                processMap.put(businessId, ffmpeg);
            }
            cleanProcess();
            ProcessKiller ffmpegKiller = new ProcessKiller(ffmpeg);
            // JVM退出时，先通过钩子关闭FFmepg进程
            runtime.addShutdownHook(ffmpegKiller);
            // 取出输出流和错误流的信息
            // 注意：必须要取出ffmpeg在执行命令过程中产生的输出信息，如果不取的话当输出流信息填满jvm存储输出留信息的缓冲区时，线程就会阻塞住
            PrintStream errorStream = new PrintStream(ffmpeg.getErrorStream());
            PrintStream inputStream = new PrintStream(ffmpeg.getInputStream());
            errorStream.start();
            inputStream.start();
            // 等待ffmpeg命令执行完
            if (isWait) {
                ffmpeg.waitFor();
            }
            // 获取执行结果字符串
            String result = errorStream.stringBuffer.append(inputStream.stringBuffer).toString();
            // 输出执行的命令信息
            String resultStr = StringUtils.isBlank(result) ? "[异常]" : "【正常】";
            log.info("--- 已执行的FFmepg命令： ---" + cmd + " 已执行完毕,执行结果： " + resultStr);
            return result;
        } catch (Exception e) {
            log.error("--- FFmpeg命令执行出错！ --- 出错信息： " + e.getMessage());
            if (null != ffmpeg) {
                ffmpeg.destroy();
            }
            return null;
        }
    }

    /**
     * 清理不存在的进程
     */
    private void cleanProcess() {
        Map<String, Process> pMap = processMap;
        for (Map.Entry<String, Process> p : pMap.entrySet()) {
            if (!p.getValue().isAlive()) {
                pMap.remove(p.getKey());
            }
        }
    }

    public Process getProcess(String businessId){
        return processMap.get(businessId);
    }


    /**
     * 在程序退出前结束已有的FFmpeg进程
     */
    private static class ProcessKiller extends Thread {
        private final Process process;

        public ProcessKiller(Process process) {
            this.process = process;
        }

        @Override
        public void run() {
            this.process.destroy();
            log.info("--- 已销毁FFmpeg进程 --- 进程名： " + process.toString());
        }
    }

    /**
     * 用于取出ffmpeg线程执行过程中产生的各种输出和错误流的信息
     */
    static class PrintStream extends Thread {
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        StringBuffer stringBuffer = new StringBuffer();

        public PrintStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                if (null == inputStream) {
                    log.error("--- 读取输出流出错！因为当前输出流为空！---");
                }
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
//                    log.info(line);
                    stringBuffer.append(line).append("\n");
                }
            } catch (Exception e) {
                log.error("--- 读取输入流出错了！--- 错误信息：" + e.getMessage());
            } finally {
                try {
                    if (null != bufferedReader) {
                        bufferedReader.close();
                    }
                    if (null != inputStream) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    log.error("--- 调用PrintStream读取输出流后，关闭流时出错！---");
                }
            }
        }
    }

}
