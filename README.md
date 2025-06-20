# Wuthering Waves Downloader
该工具用于解析并下载 Wuthering Waves 启动器 Api 的内容。<br>

## 运行环境
- JDK17

## 编译:
```bash
./gradlew jar
```

## 下载
你需要把Api链接填写到 配置.json 的 IndexURL 中，是以index.json结尾的Api链接。

如:
```json
"IndexURL": "https://prod-cn-alicdn-gamestarter.kurogame.com/launcher/game/G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json",
```
然后运行 jar 文件进行下载。

下载后默认存储在Wuthering Waves Game文件夹中。