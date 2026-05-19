# aliyun-tdx-attestation-sdk

`aliyun-tdx-attestation-sdk` 是面向 Java 服务的阿里云 TDX 远程证明 SDK。当前版本为 `v2.0.0`，提供 Quote 生成与 Quote 验证两类核心能力。

SDK 将 TDX native 运行时对接、部署级指纹计算、`report_data` 组装、Quote 生成和 Quote 验证统一封装为稳定 Java API。业务服务通过 Maven 依赖接入 SDK，生成侧对接阿里云 TDX 本地运行时，验证侧对接阿里云官方远程证明服务。

## Maven 坐标

```xml
<dependency>
    <groupId>com.ywacko</groupId>
    <artifactId>aliyun-tdx-attestation-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

## 核心能力

- 基于部署级指纹生成稳定 canonical JSON
- 对部署级指纹计算 `SHA-256` 摘要
- 将 32 字节部署摘要写入 64 字节 `report_data` 前 32 字节，后 32 字节补零
- 通过 `JNA` 调用阿里云 TDX native 运行时生成 Quote
- 输出 Quote、Quote hash、Quote size、deployment digest、`report_data`、provider 和 providerVersion
- 对 Quote 材料结构提供 SDK 级验证入口
- 校验 Quote hash、Quote size、deployment digest、`reportDataHex`、provider 和 providerVersion
- 调用阿里云官方远程证明服务验证 Quote 内容，并校验 JWT 与 attested `report_data`

## 运行边界

### Quote 生成

`generateQuote(...)` 运行在阿里云 `TDX VM` 生成侧，目标运行节点需要具备：

- `/dev/tdx_guest`
- `libtdx_attest.so`
- `libtdx-attest` / `libtdx-attest-devel`

已验证的最小系统包安装方式：

```bash
dnf -y install libtdx-attest libtdx-attest-devel
```

容器化部署时，容器运行环境需要具备访问 `/dev/tdx_guest` 和加载 `libtdx_attest.so` 的能力。

### Quote 验证

`verifyQuote(...)` 是验证侧能力，与本地 TDX 设备和 native 运行时解耦。验证方可运行在 Java 11 及以上标准 Java 环境中，通过阿里云官方远程证明服务完成 Quote 内容验证。

默认远程证明 endpoint：

```text
https://attest.cn-beijing.aliyuncs.com/v1/attestation
```

默认 JWKS 地址：

```text
https://attest.cn-beijing.aliyuncs.com/jwks.json
```

默认 issuer 与 audience：

```text
https://attest.cn-beijing.aliyuncs.com
```

## 生成 Quote

部署级指纹包含三个核心字段：

- `service`
- `imageDigest`
- `gitRev`

示例：

```java
DeploymentFingerprint fingerprint = DeploymentFingerprint.builder()
        .service("trusted-service")
        .imageDigest("registry.example.com/trusted-service@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
        .gitRev("021b2d7")
        .build();

AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .build();

QuoteGenerationResult result = client.generateQuote(fingerprint);

String quoteBase64 = result.getQuoteBase64();
String quoteSha256Hex = result.getQuoteSha256Hex();
Integer quoteSize = result.getQuoteSize();
String deploymentDigestHex = result.getDeploymentDigestHex();
String reportDataHex = result.getReportDataHex();
String provider = result.getProvider();
String providerVersion = result.getProviderVersion();
```

生成侧 provider 标准取值：

```text
provider=aliyun-tdx-jna
providerVersion=v2.0.0
```

## 验证 Quote

验证入参采用 SDK 定义的 Quote 材料字段：

- `service`
- `imageDigest`
- `gitRev`
- `deploymentDigestHex`
- `reportDataHex`
- `quoteBase64`
- `quoteSha256Hex`
- `quoteSize`
- `provider`
- `providerVersion`

示例：

```java
QuoteVerificationRequest request = QuoteVerificationRequest.builder()
        .service("trusted-service")
        .imageDigest("registry.example.com/trusted-service@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
        .gitRev("021b2d7")
        .deploymentDigestHex("<deploymentDigestHex>")
        .reportDataHex("<reportDataHex>")
        .quoteBase64("<quoteBase64>")
        .quoteSha256Hex("<quoteSha256Hex>")
        .quoteSize(12345)
        .provider("aliyun-tdx-jna")
        .providerVersion("v2.0.0")
        .build();

AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .build();

QuoteVerificationResult verification = client.verifyQuote(request);

boolean verified = verification.isVerified();
String resultCode = verification.getResultCode();
String message = verification.getMessage();
```

`verified=true` 表示本地字段结构、自洽关系和远程证明内容均已通过。`verified=false` 表示验证正常完成，但材料未通过某一项校验。

主要结果字段：

- `structureValid`：本地字段结构与派生字段自洽
- `contentValid`：远程证明内容通过，且 attested `report_data` 与本地期望一致
- `quoteValid`：阿里云远程证明和 JWT 校验通过
- `quoteHashMatched`：Quote hash 匹配
- `quoteSizeMatched`：Quote size 匹配
- `deploymentDigestMatched`：deployment digest 匹配
- `reportDataMatched`：`report_data` 综合匹配
- `attestedReportDataMatched`：远程证明返回的 `report_data` 匹配
- `providerMatched`：Quote 生成 provider 匹配
- `providerVersionMatched`：Quote 生成 providerVersion 匹配

## 远程证明配置

SDK 默认使用阿里云官方远程证明服务。需要自定义远程证明参数时，可以通过 `AliyunRemoteAttestationConfig` 调整 endpoint、JWKS、issuer、audience、policyIds、请求超时和时钟偏移。

```java
AliyunRemoteAttestationConfig config = AliyunRemoteAttestationConfig.builder()
        .attestationEndpoint(URI.create("https://attest.cn-beijing.aliyuncs.com/v1/attestation"))
        .jwksUri(URI.create("https://attest.cn-beijing.aliyuncs.com/jwks.json"))
        .issuer("https://attest.cn-beijing.aliyuncs.com")
        .audience("https://attest.cn-beijing.aliyuncs.com")
        .build();

AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .remoteAttestationConfig(config)
        .build();
```

也可以通过 `quoteEvidenceVerifier(...)` 注入自定义 Quote 证明验证器：

```java
AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .quoteEvidenceVerifier(customVerifier)
        .build();
```

## 构建

执行 Maven 测试生命周期与编译校验：

```bash
mvn test
```

执行打包：

```bash
mvn package
```

输出产物：

```text
target/aliyun-tdx-attestation-sdk-2.0.0.jar
```

## 设计文档

详细设计见 [sdk-design.md](docs/sdk-design.md)。
