package cyt.wuwadownloader;

import lombok.Data;

@Data
public class 配置 {
    private String IndexURL;
    private int 下载线程数;
    private boolean 打印日志;
    private String 错误日志路径;
    private String 保存目录;
    private int 最大重试次数;
}
