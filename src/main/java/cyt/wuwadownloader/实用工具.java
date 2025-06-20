package cyt.wuwadownloader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class 实用工具 {

    private static final OkHttpClient client = new OkHttpClient();

    public static String GET_获取字符串(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] GET_获取二进制(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body().bytes();
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String 确保以斜杠结尾(String input) {
        return input.endsWith("/") ? input : input + "/";
    }

    public static String 确保不以斜杠开头(String input) {
        return input.startsWith("/") ? input.substring(1) : input;
    }

    public static String 计算MD5(byte[] 要计算的数据) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return 字节集到十六进制字符串(md.digest(要计算的数据));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("计算MD5失败：" + e);
        }
    }

    public static String 计算文件MD5(File 文件) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] 缓冲区 = new byte[524288];

            try (FileInputStream fis = new FileInputStream(文件)) {
                int 读入字节数;
                while ((读入字节数 = fis.read(缓冲区)) != -1) {
                    md.update(缓冲区, 0, 读入字节数);
                }
            }

            return 字节集到十六进制字符串(md.digest());

        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("计算文件MD5失败：" + e);
        }
    }

    public static String 字节集到十六进制字符串(byte[] 字节集) {
        StringBuilder sb = new StringBuilder();
        for (byte 字节 : 字节集) {
            sb.append(String.format("%02x", 字节 & 0xff));
        }
        return sb.toString();
    }

    public static String 格式化文件大小(long 字节数) {
        if (字节数 < 1024) {
            return 字节数 + " B";
        } else if (字节数 < 1024 * 1024) {
            return String.format("%.2f KB", 字节数 / 1024.0);
        } else if (字节数 < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", 字节数 / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", 字节数 / (1024.0 * 1024 * 1024));
        }
    }
}