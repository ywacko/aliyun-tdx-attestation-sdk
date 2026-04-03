# aliyun-tdx-attestation-sdk

`aliyun-tdx-attestation-sdk` 是一个面向 Java 服务的 SDK，用于在阿里云 `TDX VM` 内通过本地 helper bridge 生成 attestation `Quote`。

当前设计目标不是让 `TeeGateway` 直接处理 `libtdx_attest.so` 的 native 细节，而是提供一层稳定的 Java API：

- 根据 `TeeGateway` 部署级指纹生成 canonical JSON
- 计算 `SHA-256` 摘要
- 组装 64 字节 `report_data`
- 调用本地 helper 生成 `Quote`
- 把 `Quote`、`report_data` 和摘要信息以 Java 模型返回给业务代码

仓库同时包含两部分代码：

- `src/main/java`
  - Java SDK
- `native`
  - 可直接在阿里云 `TDX VM` 上编译的 `tdx-quote-helper`

## 当前边界

- SDK 对外暴露 Java API
- SDK 内部第一版通过本地 helper 可执行程序调用 TDX 能力
- helper 负责最终调用阿里云 `TDX` 本地接口，例如 `tdx_att_get_quote(...)`
- SDK 不直接内嵌 `JNI`

## 快速示例

```java
DeploymentFingerprint fingerprint = DeploymentFingerprint.builder()
        .service("tee-gateway")
        .containerName("tee-gateway")
        .imageRef("ywackoo/tee-gateway:20260402")
        .imageId("a6c80d35a995")
        .gitRev("021b2d7")
        .build();

AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .helperCommand(List.of("/usr/local/bin/tdx-quote-helper"))
        .timeout(Duration.ofSeconds(15))
        .build();

QuoteGenerationResult result = client.generateQuote(fingerprint);

String quoteBase64 = result.getQuoteBase64();
String digestHex = result.getDeploymentDigestHex();
String reportDataHex = result.getReportDataHex();
```

## helper 协议

SDK 默认通过 JSON over stdin/stdout 与 helper 交互。

输入示例：

```json
{
  "reportDataHex": "60ec27a1f310ff4203a1b5ed21f2661ac0c9617b7a0800f695f56d059fd8f5810000000000000000000000000000000000000000000000000000000000000000",
  "deploymentDigestHex": "60ec27a1f310ff4203a1b5ed21f2661ac0c9617b7a0800f695f56d059fd8f581"
}
```

输出示例：

```json
{
  "quoteBase64": "BASE64_ENCODED_QUOTE",
  "quoteSize": 5006,
  "reportDataHex": "60ec27a1f310ff4203a1b5ed21f2661ac0c9617b7a0800f695f56d059fd8f5810000000000000000000000000000000000000000000000000000000000000000",
  "provider": "aliyun-tdx-helper",
  "helperVersion": "0.1.0"
}
```

## 部署步骤

### 1. 阿里云 TDX VM 前置条件

目标机器需要先满足下面这些条件：

- 已确认机器运行在阿里云 `TDX VM` 中
- 已存在 `/dev/tdx_guest`
- 已安装 `libtdx-attest` 和 `libtdx-attest-devel`
- 若后续要容器化接入 `TeeGateway`，容器内也需要能访问 `/dev/tdx_guest`

当前我们已经验证过的最小前置包是：

```bash
dnf -y install libtdx-attest libtdx-attest-devel
```

### 2. 编译 Java SDK

在本地 SDK 项目根目录执行：

```bash
mvn test
```

如果只需要打包：

```bash
mvn package
```

输出产物位于：

```text
target/aliyun-tdx-attestation-sdk-0.1.0-SNAPSHOT.jar
```

### 3. 在阿里云 TDX VM 上编译 helper

进入 native 目录：

```bash
cd native
make
```

产物为：

```text
native/tdx-quote-helper
```

建议安装到固定路径，例如：

```bash
install -m 0755 tdx-quote-helper /usr/local/bin/tdx-quote-helper
```

### 4. helper 运行要求

helper 实际运行时依赖：

- `libtdx_attest.so`
- `/dev/tdx_guest`

如果 helper 运行在宿主机上，通常不需要额外处理。

如果 helper 运行在容器内，至少要保证：

- 挂载 `/dev/tdx_guest`
- 容器内可找到 `libtdx_attest.so`
- 容器镜像中包含 `tdx-quote-helper`

### 5. TeeGateway 接入方式

`TeeGateway` 不需要直接处理 C 接口，只需要引用 Java SDK，并配置 helper 路径：

```java
AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .helperCommand(List.of("/usr/local/bin/tdx-quote-helper"))
        .timeout(Duration.ofSeconds(15))
        .build();
```

接入流程建议固定为：

1. 从当前网关运行信息组装 `DeploymentFingerprint`
2. 调用 `client.generateQuote(fingerprint)`
3. 取出 `quoteBase64`
4. 封装到 `TeeGateway` 接口返回

### 6. 推荐部署形态

当前更推荐下面这条路径：

- Java SDK 跟随 `TeeGateway` 一起打包
- `tdx-quote-helper` 作为本机 helper 安装在宿主机或容器内固定路径
- `TeeGateway` 通过 `ProcessBuilder` 调 helper

这样做的优点是：

- Java 侧不需要 JNI
- native 依赖边界清楚
- helper 后续可以独立替换或升级

### 7. 当前验证状态

当前仓库内已经完成：

- Java SDK 代码骨架
- `DeploymentFingerprint -> digest -> report_data` 生成逻辑
- helper 协议定义
- native helper 实现
- 本地 `mvn test` 验证通过

当前还未完成的只有一项：

- 需要把 `native/tdx-quote-helper` 在真实阿里云 `TDX VM` 上编译并做一次端到端联调

native helper 的编译说明见 [native/README.md](native/README.md)。

详细设计见 [sdk-design.md](docs/sdk-design.md)。
