package cyt.wuwadownloader;

import java.io.FileWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import lombok.AllArgsConstructor;
import org.fusesource.jansi.AnsiConsole;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class Main {
    private static 配置 字段_配置;
    private static String 输出路径;
    private static final OkHttpClient 客户端 = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(64, 1, TimeUnit.MINUTES))
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build();

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        System.out.print("\033]0; Wuthering Waves Downloader \007");

        try {
            String 初始URL;
            try {
                字段_配置 = Json工具.到对象(Files.readString(Paths.get("配置.json")), 配置.class);
                初始URL = 字段_配置.getIndexURL();
                输出路径 = 字段_配置.get保存目录();
            } catch (Exception e) {
                System.err.println("无法读取配置文件：" + e);
                AnsiConsole.systemUninstall();
                return;
            }

            // 下载并解析index.json
            String indexJson = 实用工具.GET_获取字符串(初始URL);
            IndexJson index = Json工具.到对象(indexJson, IndexJson.class);
            if (index == null) {
                System.err.println("无法获取 index.json" + 初始URL);
                AnsiConsole.systemUninstall();
                return;
            }

            String 资源路径 = index.getDefaultConfig().getResources();
            System.out.println("版本: " + index.getDefaultConfig().getVersion());

            String CDN地址 = 选择最快的CDN(index.getDefaultConfig().getCdnList());
            if (CDN地址 == null) {
                System.err.println("没有可用的CDN");
                AnsiConsole.systemUninstall();
                return;
            }
            System.out.println("选择CDN: " + CDN地址);

            String resourcesJson = 实用工具.GET_获取字符串(CDN地址 + 资源路径);
            ResourceJson resourcesObj = Json工具.到对象(resourcesJson, ResourceJson.class);
            if (resourcesObj == null) {
                System.err.println("无法获取资源列表");
                AnsiConsole.systemUninstall();
                return;
            }

            String 资源基础路径 = 实用工具.确保以斜杠结尾(index.getDefaultConfig().getResourcesBasePath());
            System.out.println("文件数量: " + resourcesObj.getResource().size());

            long 总大小 = resourcesObj.getResource().stream()
                    .mapToLong(ResourceJson.ResourceItem::getSize)
                    .sum();
            System.out.println("总大小: " + 实用工具.格式化文件大小(总大小));

            Files.createDirectories(Paths.get(字段_配置.get保存目录()));
            Files.deleteIfExists(Paths.get(字段_配置.get错误日志路径()));

            AtomicLong 成功数量 = new AtomicLong(0);
            AtomicLong 失败数量 = new AtomicLong(0);

            // 创建进度条
            try (ProgressBar 进度条 = new ProgressBarBuilder()
                    .setTaskName("下载")
                    .setInitialMax(总大小)
                    .setUnit("MB", 1024 * 1024)
                    .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                    .showSpeed()
                    .build())
            {
                ExecutorService 线程池 = Executors.newFixedThreadPool(字段_配置.get下载线程数());
                CountDownLatch 下载计数器 = new CountDownLatch(resourcesObj.getResource().size());

                for (ResourceJson.ResourceItem 原始资源 : resourcesObj.getResource()) {
                    final ResourceJson.ResourceItem 资源 = 原始资源;
                    final String 游戏文件 = 实用工具.确保不以斜杠开头(资源.getDest());
                    final Path 本地路径 = Paths.get(字段_配置.get保存目录(), 游戏文件);
                    final String 下载地址 = CDN地址 + 资源基础路径 + 游戏文件;

                    线程池.submit(() -> {
                        try {
                            boolean 成功 = 下载文件(资源, CDN地址 + 资源基础路径, 输出路径, 进度条);
                            if (成功) {
                                成功数量.incrementAndGet();
                            } else {
                                失败数量.incrementAndGet();
                                String 错误信息 = String.format(
                                        "下载失败：文件: %s\n预期MD5: %s | 预期大小: %d\n下载地址: %s",
                                        本地路径, 资源.getMd5(), 资源.getSize(), 下载地址);
                                记录错误(错误信息);
                            }
                        } catch (Exception e) {
                            失败数量.incrementAndGet();
                            记录错误(String.format(String.format(
                                    "下载失败：文件: %s\n预期MD5: %s | 预期大小: %d\n下载地址: %s\n原因：%s",
                                    本地路径, 资源.getMd5(), 资源.getSize(), 下载地址, e)));
                        } finally {
                            下载计数器.countDown();
                        }
                    });
                }

                下载计数器.await();
                线程池.shutdown();
            }

            System.out.println("文件下载完成");
            System.out.printf("成功: %d 个, 失败: %d 个\n", 成功数量.get(), 失败数量.get());

            // 输出校验结果
            if (失败数量.get() == 0) {
                System.out.println("所有文件校验通过");
            } else {
                System.out.println("有 " + 失败数量.get() + " 个文件校验失败");
            }
        } catch (Exception e) {
            System.err.println("程序发生错误：" + e);
        } finally {
            AnsiConsole.systemUninstall();
        }
    }

    private static boolean 下载文件(ResourceJson.ResourceItem 资源, String 基础URL, String 输出路径, ProgressBar 进度条) throws IOException {
        var 游戏文件 = 实用工具.确保不以斜杠开头(资源.getDest());
        Path 本地文件 = Paths.get(输出路径, 游戏文件);
        String 下载地址 = 基础URL + 游戏文件;

        if (Files.exists(本地文件)) {
            if (校验文件(资源, 本地文件, false)) {
                if (字段_配置.is打印日志()) {
                    进度条时打印消息("文件已存在且MD5匹配，跳过下载: " + 游戏文件);
                }
                进度条.stepBy(资源.getSize()); // 更新进度条
                return true;
            }
            if (字段_配置.is打印日志()) {
                进度条时打印消息("文件已存在但校验失败，准备重新下载: " + 游戏文件);
            }
            Files.delete(本地文件);
        }

        Files.createDirectories(本地文件.getParent());

        for (int i = 0; i < 字段_配置.get最大重试次数(); i++) {
            try {
                Request 请求 = new Request.Builder()
                        .url(下载地址)
                        .header("User-Agent", "Mozilla/5.0")
                        .build();

                try (Response 响应 = 客户端.newCall(请求).execute()) {
                    if (!响应.isSuccessful()) throw new IOException("HTTP错误: " + 响应.code());

                    try (InputStream 输入流 = 响应.body().byteStream();
                         OutputStream 输出流 = new FileOutputStream(本地文件.toFile(), false)) {
                        byte[] 缓冲区 = new byte[524288];
                        int 字节数;
                        while ((字节数 = 输入流.read(缓冲区)) != -1) {
                            输出流.write(缓冲区, 0, 字节数);
                        }
                    }

                    if (校验文件(资源, 本地文件, true)) {
                        if (字段_配置.is打印日志()) {
                            进度条时打印消息("文件下载完成: " + 游戏文件);
                        }
                        进度条.stepBy(资源.getSize());
                        return true;
                    } else {
                        进度条时打印消息("下载文件校验失败: " + 游戏文件 + " 下载URL：" + 下载地址);
                        Files.deleteIfExists(本地文件);
                    }
                }

                if (字段_配置.is打印日志()) {
                    进度条时打印消息("下载失败 (" + (i+1) + "/" + 字段_配置.get最大重试次数() + "): " + 游戏文件 + " 下载URL：" + 下载地址);
                }
                Thread.sleep(3000);
            } catch (Exception e) {
                if (字段_配置.is打印日志()) {
                    进度条时打印消息("下载异常 (" + (i + 1) + "/" + 字段_配置.get最大重试次数() + "): " + 游戏文件 + " 下载URL：" + 下载地址 + " 原因：" + e);
                }
                if (i < 字段_配置.get最大重试次数() - 1) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) {}
                }
            }
        }

        进度条时打印消息("超过最大重试次数: " + 游戏文件 + " 下载URL：" + 下载地址);
        return false;
    }

    private static boolean 校验文件(ResourceJson.ResourceItem 文件, Path 文件路径, Boolean 记录错误信息) {
        try {
            // 检查文件大小
            long 实际大小 = Files.size(文件路径);
            if (实际大小 != 文件.getSize()) {
                if (记录错误信息 && 字段_配置.is打印日志()) {
                    进度条时打印消息(String.format("大小不匹配: %s (预期: %d, 实际: %d)",
                            文件.getDest(), 文件.getSize(), 实际大小));
                }
                return false;
            }

            // 检查MD5
            String 实际MD5 = 实用工具.计算文件MD5(文件路径.toFile());
            if (!实际MD5.equalsIgnoreCase(文件.getMd5())) {
                if (记录错误信息 && 字段_配置.is打印日志()) {
                    进度条时打印消息(String.format("MD5不匹配: %s (预期: %s, 实际: %s)",
                            文件.getDest(), 文件.getMd5(), 实际MD5));
                }
                return false;
            }

            return true;
        } catch (Exception e) {
            if (记录错误信息 && 字段_配置.is打印日志()) {
                进度条时打印消息("校验文件失败: " + 文件.getDest() + "\n原因：" + e);
            }
            return false;
        }
    }

    private static String 选择最快的CDN(List<IndexJson.CdnItem> cdn列表) {
        ExecutorService 线程池 = Executors.newFixedThreadPool(Math.min(cdn列表.size(), 字段_配置.get下载线程数()));
        List<Future<速度测试结果>> futures = new ArrayList<>();

        for (IndexJson.CdnItem cdn : cdn列表) {
            String 测试Url = cdn.getUrl();
            futures.add(线程池.submit(new 速度测试任务(测试Url, cdn.getUrl())));
        }

        速度测试结果 最好的结果 = null;

        for (Future<速度测试结果> future : futures) {
            try {
                速度测试结果 result = future.get(10, TimeUnit.SECONDS);
                if (result.成功 && (最好的结果 == null || result.响应时间 < 最好的结果.响应时间)) {
                    最好的结果 = result;
                }
            } catch (Exception e) {
                System.out.println("测速任务超时或异常: " + e.getMessage());
            }
        }

        线程池.shutdown();

        if (最好的结果 != null) {
            return 最好的结果.cdn;
        } else {
            return null;
        }
    }

    @AllArgsConstructor
    private static class 速度测试任务 implements Callable<速度测试结果> {
        private String 测试Url;
        private String cdnUrl;

        @Override
        public 速度测试结果 call() {
            long 开始时间 = System.currentTimeMillis();
            try {
                Request 请求 = new Request.Builder()
                        .url(测试Url)
                        .head()
                        .build();

                客户端.newCall(请求).execute();
                long 响应时间 = System.currentTimeMillis() - 开始时间;
                return new 速度测试结果(cdnUrl, 响应时间, true);
            } catch (Exception e) {
                long 响应时间 = System.currentTimeMillis() - 开始时间;
                System.out.println("测试CDN异常: " + cdnUrl + " 异常信息: " + e.getMessage());
                return new 速度测试结果(cdnUrl, 响应时间, false);
            }
        }
    }

    @AllArgsConstructor
    private static class 速度测试结果 {
        private String cdn;
        private long 响应时间;
        private boolean 成功;
    }

    private static synchronized void 进度条时打印消息(String 消息) {
        System.out.print("\u001B[1L");
        System.out.println("\r\033[2K" + 消息);
    }

    private static synchronized void 记录错误(String 错误信息) {
        String 带时间戳的错误 = "[" + new Date() + "] " + 错误信息 + "\n";
        try (FileWriter fw = new FileWriter(字段_配置.get错误日志路径(), true)) {
            fw.write(带时间戳的错误);
            fw.flush();
        } catch (IOException ex) {
            System.err.println("无法写入错误日志：");
            ex.printStackTrace();
        }
    }
}