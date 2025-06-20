package cyt.wuwadownloader;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class IndexJson {
    private int chunkDownloadSwitch;

    @SerializedName("default")
    private DefaultConfig defaultConfig;

    private int keyFileCheckSwitch;
    private int RHIOptionSwitch;
    private int predownloadSwitch;
    private List<RHIOption> RHIOptionList;
    private ResourcesLogin resourcesLogin;
    private Experiment experiment;
    private int checkExeIsRunning;
    private int hashCacheCheckAccSwitch;
    private List<String> keyFileCheckList;
    private List<String> fingerprints;

    @Data
    public static class DefaultConfig {
        private int sampleHashSwitch;
        private List<CdnItem> cdnList;
        private String resourcesBasePath;
        private Map<String, Object> changelog;
        private String resources;
        private List<String> resourcesExcludePathNeedUpdate;
        private List<String> resourcesExcludePath;
        private Config config;
        private ResourcesDiff resourcesDiff;
        private String version;
        private int changelogVisible;
    }

    @Data
    public static class CdnItem {
        private int P;
        private int K1;
        private int K2;
        private String url;
    }

    @Data
    public static class Config {
        private String indexFileMd5;
        private long unCompressSize;
        private String baseUrl;
        private long size;
        private String patchType;
        private String indexFile;
        private String version;
        private List<PatchConfig> patchConfig;
    }

    @Data
    public static class PatchConfig {
        private String indexFileMd5;
        private long unCompressSize;
        private Ext ext;
        private String baseUrl;
        private long size;
        private String indexFile;
        private String version;
    }

    @Data
    public static class Ext {
        private long maxFileSize;
    }

    @Data
    public static class ResourcesDiff {
        private GameInfo currentGameInfo;
        private GameInfo previousGameInfo;
    }

    @Data
    public static class GameInfo {
        private String fileName;
        private String version;
        private String md5;
    }

    @Data
    public static class RHIOption {
        private String cmdOption;
        private Map<String, String> text;
        private int isShow;
    }

    @Data
    public static class ResourcesLogin {
        private String host;
        private int loginSwitch;
    }

    @Data
    public static class Experiment {
        private Download download;
        private ResCheck res_check;

        @Data
        public static class Download {
            private String downloadCdnSelectTestDuration;
            private String downloadReadBlockTimeout;
        }

        @Data
        public static class ResCheck {
            private String fileChunkCheckSwitch;
            private String fileSizeCheckSwitch;
            private String resValidCheckTimeOut;
            private String fileCheckWhiteListConfig;
        }
    }
}
