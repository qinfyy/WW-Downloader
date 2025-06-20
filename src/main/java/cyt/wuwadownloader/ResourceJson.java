package cyt.wuwadownloader;

import java.util.List;
import lombok.Data;

@Data
public class ResourceJson {
    private List<ResourceItem> resource;
    private SampleHashInfo sampleHashInfo;

    @Data
    public static class ResourceItem {
        private String dest;
        private String md5;
        private String sampleHash;
        private long size;
    }

    @Data
    public static class SampleHashInfo {
        private int sampleNum;
        private int sampleBlockMaxSize;
    }
}
