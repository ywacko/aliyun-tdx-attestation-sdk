# aliyun-tdx-attestation-sdk

`aliyun-tdx-attestation-sdk` 是一个面向 Java 服务的 SDK，用于在阿里云 `TDX VM` 内通过 `JNA` 直接调用本机 `TDX` 运行时生成 attestation `Quote`。

这套 SDK 的运行前提是：

- 当前服务必须运行在阿里云 `TDX VM` 中
- 当前机器必须存在 `/dev/tdx_guest`
- 当前机器必须具备 `TDX` 本地运行时能力，例如 `libtdx_attest.so`

这不是一个“任意 Linux 机器都能直接使用”的通用 SDK。脱离阿里云 `TDX VM` 前提后，`Quote` 生成能力本身就不成立。

当前设计目标不是让 `TeeGateway` 直接处理 `libtdx_attest.so` 的 native 细节，而是提供一层稳定的 Java API：

- 根据 `TeeGateway` 部署级指纹生成 canonical JSON
- 计算 `SHA-256` 摘要
- 组装 64 字节 `report_data`
- 通过 `JNA` 直接调用 `libtdx_attest.so` 生成 `Quote`
- 把 `Quote`、`report_data` 和摘要信息以 Java 模型返回给业务代码

仓库当前核心代码全部位于：

- `src/main/java`
  - Java SDK

## 当前边界

- SDK 对外暴露 Java API
- SDK 内部当前通过 `JNA` 直接调用 `libtdx_attest.so`
- SDK 不依赖额外 helper 部署
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
        .build();

QuoteGenerationResult result = client.generateQuote(fingerprint);

String quoteBase64 = result.getQuoteBase64();
String digestHex = result.getDeploymentDigestHex();
String reportDataHex = result.getReportDataHex();
```

## 运行机制

SDK 当前的主路径是：

- Java 代码生成 64 字节 `report_data`
- SDK 通过 `JNA` 加载 `libtdx_attest.so`
- SDK 直接调用 `tdx_att_get_quote(...)`
- SDK 返回 `Quote` 字节、Base64 和 `report_data` 相关元数据

这意味着：

- `TeeGateway` 只需要 Maven 引入 SDK
- 不需要额外部署 helper 服务
- 但运行机器必须是阿里云 `TDX VM`
- 运行机器必须存在 `/dev/tdx_guest` 和 `libtdx_attest.so`

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

### 3. 运行时要求

SDK 实际运行时依赖：

- `libtdx_attest.so`
- `/dev/tdx_guest`

如果 `TeeGateway` 运行在宿主机上，通常只需要保证上述两个条件成立。

如果 `TeeGateway` 运行在容器内，至少要保证：

- 容器内可访问 `/dev/tdx_guest`
- 容器内可找到 `libtdx_attest.so`
- JVM 有权限加载本地动态库
### 4. TeeGateway 接入方式

`TeeGateway` 不需要直接处理 C 接口，只需要引用 Java SDK：

```java
AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .build();
```

接入流程建议固定为：

1. 从当前网关运行信息组装 `DeploymentFingerprint`
2. 调用 `client.generateQuote(fingerprint)`
3. 取出 `quoteBase64`
4. 封装到 `TeeGateway` 接口返回

### 5. 推荐集成形态

当前更推荐下面这条路径：

- `TeeGateway` 通过 Maven 依赖引入 `aliyun-tdx-attestation-sdk`
- SDK 通过 `JNA` 直接调用阿里云 `TDX` 本地运行时
- `TeeGateway` 不需要额外部署 helper 或宿主机 attestation service

这样做的优点是：

- `TeeGateway` 只需要 Maven 引入 SDK 即可使用
- Java 侧不需要 JNI
- 不需要额外部署 helper
- native 调用边界仍然集中在 SDK 内部

当前推荐的版本边界是：

- Java 层通过 Maven 坐标管理 SDK 版本
- 运行机器通过阿里云 `TDX VM` 和本地运行时提供底层能力
- SDK 版本与业务服务版本按正常依赖关系管理

### 6. 当前验证状态

当前仓库内已经完成：

- Java SDK 代码骨架
- `DeploymentFingerprint -> digest -> report_data` 生成逻辑
- `JNA` 直连 `libtdx_attest.so` 的主调用链
- 本地 `mvn test` 验证通过
- 已在真实阿里云 `TDX VM` 上跑通端到端联调
- 已在阿里云 `TDX VM` 上通过纯 `JNA` 路径成功生成 `Quote`
- 已对 `SDK` 生成出的 `Quote` 完成正式验证，`overall_appraisal_result = 1`

详细设计见 [sdk-design.md](docs/sdk-design.md)。
