# aliyun-tdx-attestation-sdk

`aliyun-tdx-attestation-sdk` 是面向 Java 服务的阿里云 TDX 远程证明 SDK。当前版本为 `v3.0.0`，提供 Quote 生成、Quote 验证与当前可信执行环境证明画像三类核心能力。

SDK 将 TDX native 运行时对接、部署级指纹计算、`report_data` 组装、Quote 生成、Quote 验证和远程证明 JWT 解析统一封装为稳定 Java API。业务服务通过 Maven 依赖接入 SDK，生成侧对接阿里云 TDX 本地运行时，验证侧对接阿里云官方远程证明服务。

## Maven 坐标

```xml
<dependency>
    <groupId>com.ywacko</groupId>
    <artifactId>aliyun-tdx-attestation-sdk</artifactId>
    <version>3.0.0</version>
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
- 解析远程证明 JWT 注册字段、核心 EAT 字段、`tcb-status`、TDX Quote header/body、RTMR 与 TD attributes
- 新增当前可信执行环境证明画像接口，输出 Quote、验证结论与远程证明载荷解析结果

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
providerVersion=v3.0.0
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
        .providerVersion("v3.0.0")
        .build();

AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .build();

QuoteVerificationResult verification = client.verifyQuote(request);

boolean verified = verification.isVerified();
String resultCode = verification.getResultCode();
String message = verification.getMessage();
```

`verified=true` 表示本地字段结构、自洽关系和远程证明内容均已通过。`verified=false` 表示验证正常完成，`resultCode` 和分项结果字段用于定位具体校验项。

`generateQuote(...)` 输出 Quote 材料，`verifyQuote(...)` 输出 `QuoteVerificationResult`，`attestCurrentEnvironment(...)` 输出包含 Quote、验证结果和远程证明载荷的 `TdxEnvironmentProfile`。

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

## 当前可信执行环境证明画像

`attestCurrentEnvironment(...)` 是 v3.0.0 新增的高层接口。它在当前 TDX 运行环境中生成 Quote，随后调用远程证明服务验证 Quote，并返回 `TdxEnvironmentProfile`。

示例：

```java
DeploymentFingerprint fingerprint = DeploymentFingerprint.builder()
        .service("trusted-service")
        .imageDigest("registry.example.com/trusted-service@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
        .gitRev("021b2d7")
        .build();

AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .build();

TdxEnvironmentProfile profile = client.attestCurrentEnvironment(fingerprint);

boolean verified = profile.isVerified();
QuoteGenerationResult quote = profile.getQuote();
QuoteVerificationResult verification = profile.getVerification();
AttestationEvidence evidence = profile.getEvidence();
```

`AttestationEvidence` 会保留验签后的远程证明 JWT 载荷：

- `rawJwt`：远程证明 JWT 原文
- `tokenClaims`：`kid`、`alg`、`iss`、`aud`、`iat`、`nbf`、`exp`、`jti`、`eat_profile`、`intuse`、`tee`、`x-acs-ver`
- `tdxQuote`：从 `tcb-status` 扁平 JSON 中解析 `init_data`、`report_data`、`tdx.quote.type`、`tdx.quote.size`、Quote header、Quote body、TD attributes
- `tdxQuote.header`：解析 `tdx.quote.header.version`、`tdx.quote.header.att_key_type`、`tdx.quote.header.tee_type`、`tdx.quote.header.reserved`、`tdx.quote.header.vendor_id`、`tdx.quote.header.user_data`
- `tdxQuote.body`：解析 `tdx.quote.body.mr_td`、`tdx.quote.body.mr_seam`、`tdx.quote.body.mrsigner_seam`、`tdx.quote.body.mr_config_id`、`tdx.quote.body.mr_owner`、`tdx.quote.body.mr_owner_config`、`tdx.quote.body.rtmr_0` 到 `tdx.quote.body.rtmr_3`、`tdx.quote.body.report_data` 等
- `rawClaims`、`tcbStatusClaims`、`evaluationReports`、`customizedClaims`：保留远程证明服务返回的原始信息，便于调用侧按需使用

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
target/aliyun-tdx-attestation-sdk-3.0.0.jar
```

## 设计文档

详细设计见 [sdk-design.md](docs/sdk-design.md)。
