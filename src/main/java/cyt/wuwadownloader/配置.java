package cyt.wuwadownloader;

import lombok.Data;

@Data
public class 配置 {
    private String OSPROD;
    private String CNPROD;
    private int 下载线程数;
    private boolean 打印日志;
    private String 错误日志路径;
    private String 默认保存目录;
    private int 最大重试次数;
}
