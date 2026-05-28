# aliyun-tdx-attestation-sdk 设计文档

## 1. 文档定位

`aliyun-tdx-attestation-sdk` 是面向 Java 服务的阿里云 TDX 远程证明 SDK 实现。当前版本为 `v3.0.0`，围绕阿里云 TDX 可信执行环境构建统一的 Quote 生成、Quote 验证与证明画像代码边界。

本 SDK 将以下能力沉淀为稳定、可复用的 Java API：

- 部署级指纹模型与 canonical JSON 表达
- 部署级指纹到 `SHA-256` 摘要的确定性计算
- 64 字节 `report_data` 的标准组装
- 基于 `JNA` 的阿里云 TDX native Quote 生成
- 面向 Quote 材料结构的 SDK 级验证
- 基于阿里云官方远程证明服务的 Quote 内容验证
- 验证结果的结构化表达与分项说明
- 远程证明 JWT 和 TDX Quote claims 的结构化解析
- 当前可信执行环境证明画像的组合输出

SDK 对调用侧暴露统一客户端 `AliyunTdxAttestationClient`。生成侧通过该客户端对接 TDX native 能力，验证侧通过该客户端复用 SDK 内置验证器与阿里云远程证明服务验证器。

## 2. 工程结构

SDK 核心代码位于：

```text
src/main/java/com/ywacko/aliyun/tdx/attestation
```

主要包结构如下：

```text
com.ywacko.aliyun.tdx.attestation
├── AliyunTdxAttestationClient.java
├── exception
│   ├── AttestationException.java
│   └── QuoteGenerationException.java
├── jna
│   ├── JnaQuoteProvider.java
│   ├── NativeTdxAttestationApi.java
│   └── TdxAttestLibrary.java
├── model
│   ├── DeploymentFingerprint.java
│   ├── DeploymentFingerprintReportDataFactory.java
│   ├── QuoteGenerationRequest.java
│   ├── QuoteGenerationResult.java
│   ├── ReportData.java
│   └── TdxEnvironmentProfile.java
├── util
│   ├── HexUtils.java
│   └── Sha256Utils.java
└── verify
    ├── AliyunRemoteAttestationConfig.java
    ├── AliyunRemoteQuoteEvidenceVerifier.java
    ├── DefaultQuoteVerifier.java
    ├── QuoteEvidenceVerificationResult.java
    ├── QuoteEvidenceVerifier.java
    ├── QuoteVerifier.java
    └── model
        ├── AttestationEvidence.java
        ├── AttestationTokenClaims.java
        ├── QuoteVerificationDetails.java
        ├── QuoteVerificationRequest.java
        ├── QuoteVerificationResult.java
        └── TdxQuoteClaims.java
```

工程依赖采用 Maven 管理，核心依赖包括：

- Java 11
- `net.java.dev.jna:jna:5.14.0`
- `com.nimbusds:nimbus-jose-jwt:9.37.3`

其中 `JNA` 负责 Java 到本地 TDX 运行时的调用映射，`nimbus-jose-jwt` 负责阿里云远程证明 JWT 与 JWKS 的验签处理。

## 3. 总体架构

SDK 代码架构分为四个核心层次：

1. 客户端门面层
   - `AliyunTdxAttestationClient`
   - 对外提供 Quote 生成、Quote 验证与当前环境证明画像统一入口
   - 通过 Builder 暴露生成侧与验证侧配置扩展点

2. 部署指纹与 `report_data` 层
   - `DeploymentFingerprint`
   - `DeploymentFingerprintReportDataFactory`
   - `ReportData`
   - 负责将 Java 服务部署身份稳定映射为 TDX Quote 中的 `report_data`

3. TDX Quote 生成层
   - `JnaQuoteProvider`
   - `NativeTdxAttestationApi`
   - `TdxAttestLibrary`
   - 负责加载 `libtdx_attest.so` 并调用 `tdx_att_get_quote(...)`

4. Quote 验证层
   - `DefaultQuoteVerifier`
   - `AliyunRemoteQuoteEvidenceVerifier`
   - `QuoteEvidenceVerifier`
   - `QuoteVerificationRequest`
   - `QuoteVerificationResult`
   - 负责完成字段自洽校验、远程证明服务校验与结构化结果输出

5. 证明载荷解析层
   - `AttestationEvidence`
   - `AttestationTokenClaims`
   - `TdxQuoteClaims`
   - 负责保留远程证明 JWT 原始 claims，并解析 TDX Quote 中可展示、可审计的核心字段

该架构将生成侧可信根能力和验证侧远程证明能力收敛到同一个 SDK 内，同时通过懒加载与接口抽象保持清晰边界。

## 4. 客户端门面

`AliyunTdxAttestationClient` 是 SDK 对外的统一入口，提供以下核心方法：

```java
public QuoteGenerationResult generateQuote(DeploymentFingerprint fingerprint)

public QuoteGenerationResult generateQuote(QuoteGenerationRequest request)

public QuoteVerificationResult verifyQuote(QuoteVerificationRequest request)

public TdxEnvironmentProfile attestCurrentEnvironment(DeploymentFingerprint fingerprint)
```

客户端通过 Builder 创建：

```java
AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .build();
```

Builder 提供以下扩展能力：

- `libraryName(String libraryName)`：配置 native 库加载名称
- `tdxDevicePath(Path tdxDevicePath)`：配置 TDX 设备路径
- `nativeApi(NativeTdxAttestationApi nativeApi)`：替换 native 调用封装，便于测试或专用运行环境接入
- `quoteEvidenceVerifier(QuoteEvidenceVerifier quoteEvidenceVerifier)`：替换 Quote 远程证明验证器
- `remoteAttestationConfig(AliyunRemoteAttestationConfig config)`：配置阿里云远程证明服务参数

客户端内部对 native provider 采用懒加载机制。Quote 生成链路按需初始化 `JnaQuoteProvider`，Quote 验证链路直接使用默认验证器，使生成侧和验证侧能力在同一客户端中保持解耦。`verifyQuote(...)` 输出 `QuoteVerificationResult`；`attestCurrentEnvironment(...)` 在生成和验证之上组合出完整证明画像。

## 5. 部署级指纹模型

`DeploymentFingerprint` 表示需要绑定到 TDX Quote 的部署级身份。当前采用三个核心字段：

```java
private final String service;
private final String imageDigest;
private final String gitRev;
```

字段含义如下：

- `service`：服务标识，用于表达被证明对象所属 Java 服务
- `imageDigest`：镜像内容标识，用于绑定不可变镜像内容
- `gitRev`：代码版本标识，用于绑定源码提交版本

`DeploymentFingerprint` 提供稳定的 canonical JSON 表达。参与摘要计算的是无空白字符、固定字段顺序的一行字符串：

```json
{"service":"trusted-service","image_digest":"registry.example.com/trusted-service@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef","git_rev":"021b2d7"}
```

代码中通过固定字段顺序生成 canonical JSON，避免序列化器差异影响摘要稳定性。字段名、字段顺序、字符串转义规则和无空白字符表达共同构成摘要口径，该设计使同一组部署身份字段在不同运行节点、不同调用时刻得到一致的摘要结果。

## 6. report_data 生成机制

部署级指纹到 `report_data` 的转换由 `DeploymentFingerprintReportDataFactory` 和 `ReportData` 完成。

核心流程如下：

1. 将 `DeploymentFingerprint` 转换为 canonical JSON
2. 对 canonical JSON 计算 `SHA-256`
3. 将 32 字节摘要写入 64 字节 `report_data` 的前 32 字节
4. 将剩余 32 字节补零
5. 将完整 64 字节 `report_data` 交给 TDX native Quote 生成链路

核心 API 包括：

```java
DeploymentFingerprintReportDataFactory.canonicalJson(fingerprint)

DeploymentFingerprintReportDataFactory.digestHex(fingerprint)

DeploymentFingerprintReportDataFactory.reportData(fingerprint)

ReportData.fromDeploymentDigest(digest)
```

该机制把服务部署身份稳定投射到 TDX Quote 的 `report_data` 字段中。验证阶段，SDK 使用相同算法重新计算期望 `report_data`，并与 Quote 中的远程证明结果进行比对。

## 7. Quote 生成链路

Quote 生成链路由 `JnaQuoteProvider` 承接。该类定义生成侧标准 provider 信息：

```java
public static final String PROVIDER = "aliyun-tdx-jna";
public static final String PROVIDER_VERSION = "v3.0.0";
```

生成流程如下：

1. 调用方构造 `DeploymentFingerprint`
2. SDK 将其转换为 `QuoteGenerationRequest`
3. SDK 生成部署级摘要与 64 字节 `report_data`
4. `JnaQuoteProvider` 检查 TDX 设备路径
5. `NativeTdxAttestationApi` 调用 native `tdx_att_get_quote(...)`
6. SDK 返回 `QuoteGenerationResult`

`QuoteGenerationResult` 包含以下输出：

- `quoteBytes`：Quote 原始字节
- `quoteBase64`：Quote Base64 表达
- `quoteSha256Hex`：Quote 原始字节摘要
- `quoteSize`：Quote 原始字节长度
- `deploymentDigestHex`：部署级指纹摘要
- `reportDataHex`：实际写入 Quote 的 `report_data`
- `provider`：生成提供方标识
- `providerVersion`：生成提供方版本

该输出模型用于表达生成侧可信材料，使 Quote 原始字节、摘要、长度、`report_data` 和 provider 信息能够完整进入验证链路。

## 8. Native 对接层

Native 对接层包含三个核心类：

- `TdxAttestLibrary`
  - 负责通过 JNA 加载 `libtdx_attest.so`
  - 映射 TDX native 函数

- `NativeTdxAttestationApi`
  - 封装 native 函数调用
  - 承接 Java 字节数组与 native 内存结构转换
  - 对 native 返回结果进行 Java 异常化表达

- `JnaQuoteProvider`
  - 作为 SDK Quote 生成 provider
  - 负责生成前运行环境检查
  - 负责组装 `QuoteGenerationResult`

SDK 默认 native 库名称为：

```text
tdx_attest
```

默认 TDX 设备路径为：

```text
/dev/tdx_guest
```

该层将阿里云 TDX 本地运行时能力收敛在 SDK 内部，使业务服务仅面向 Java API 完成 Quote 生成。

## 9. Quote 验证模型

Quote 验证入口为：

```java
QuoteVerificationResult verifyQuote(QuoteVerificationRequest request)
```

`QuoteVerificationRequest` 采用 SDK 定义的 Quote 材料字段，字段包括：

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

该模型保留 Quote 生成结果与验证所需的完整字段。SDK 会校验 Quote hash、Quote size、deployment digest、`reportDataHex`、`provider` 和 `providerVersion`；本地结构与自洽校验通过后，再调用阿里云官方远程证明服务验证 Quote 内容，并比对远程证明返回的 attested `report_data`。这样可以保证生成结果和 SDK 验证入参的字段口径一致。

`QuoteVerificationResult` 是结构化验证结果，包含总结果和分项结果：

- `verified`
- `resultCode`
- `message`
- `structureValid`
- `contentValid`
- `quoteValid`
- `quoteHashMatched`
- `quoteSizeMatched`
- `deploymentDigestMatched`
- `reportDataMatched`
- `attestedReportDataMatched`
- `providerMatched`
- `providerVersionMatched`
- `expectedDeploymentDigestHex`
- `actualDeploymentDigestHex`
- `expectedReportDataHex`
- `actualReportDataHex`
- `attestedReportDataHex`
- `verifierProvider`
- `verifierVersion`

其中 `verified` 是总验证结果；其余字段用于呈现结构校验、远程证明校验和 `report_data` 匹配细节。

v3.0.0 中，`verifyQuote(...)` 输出 `QuoteVerificationResult`；远程证明 JWT 解析出的完整载荷通过 `attestCurrentEnvironment(...)` 输出的证明画像提供。

## 10. 默认验证器

`DefaultQuoteVerifier` 是 SDK 默认 Quote 验证器，提供本地字段自洽校验与远程证明内容校验的二合一实现。

默认验证器标识为：

```java
public static final String VERIFIER_PROVIDER = "aliyun-tdx-sdk-local-verifier";
public static final String VERIFIER_VERSION = "v3.0.0";
```

验证流程如下：

1. 从 `QuoteVerificationRequest` 重建 `DeploymentFingerprint`
2. 基于部署级指纹重新计算 `expectedDeploymentDigestHex`
3. 基于部署级摘要重新组装 `expectedReportDataHex`
4. 解码 `quoteBase64` 得到 Quote 原始字节
5. 校验 `quoteSha256Hex` 与 Quote 原始字节是否一致
6. 校验 `quoteSize` 与 Quote 原始字节长度是否一致
7. 校验 `deploymentDigestHex` 与重新计算结果是否一致
8. 校验 `reportDataHex` 与重新组装结果是否一致
9. 校验 `provider` 与 `providerVersion` 是否符合 SDK 生成侧标准
10. 本地结构校验通过后，调用 `QuoteEvidenceVerifier` 执行远程证明校验
11. 比对远程证明返回的 attested `report_data` 与本地期望值
12. 汇总生成 `QuoteVerificationResult`

默认验证器输出的主要结果码包括：

- `PASSED`
- `INPUT_INVALID`
- `QUOTE_BASE64_INVALID`
- `QUOTE_HASH_MISMATCH`
- `QUOTE_SIZE_MISMATCH`
- `DEPLOYMENT_DIGEST_MISMATCH`
- `REPORT_DATA_MISMATCH`
- `PROVIDER_MISMATCH`
- `PROVIDER_VERSION_MISMATCH`
- `QUOTE_EVIDENCE_VERIFIER_NOT_CONFIGURED`
- `QUOTE_EVIDENCE_INVALID`
- `ATTESTED_REPORT_DATA_MISMATCH`
- `FAILED`

该设计使验证失败原因能够结构化呈现，便于调用方区分字段结构问题、摘要自洽问题、provider 版本问题和远程证明内容问题。

## 11. 阿里云远程证明验证器

`AliyunRemoteQuoteEvidenceVerifier` 是 SDK 内置的阿里云官方远程证明服务验证器。

验证器标识为：

```java
public static final String PROVIDER = "aliyun-remote-attestation";
public static final String PROVIDER_VERSION = "v3.0.0";
```

默认配置由 `AliyunRemoteAttestationConfig` 提供：

```text
DEFAULT_ATTESTATION_ENDPOINT = https://attest.cn-beijing.aliyuncs.com/v1/attestation
DEFAULT_JWKS_URI = https://attest.cn-beijing.aliyuncs.com/jwks.json
DEFAULT_ISSUER = https://attest.cn-beijing.aliyuncs.com
DEFAULT_AUDIENCE = https://attest.cn-beijing.aliyuncs.com
```

默认请求超时时间为 10 秒，JWT 时钟偏移容忍时间为 2 分钟。

远程证明验证流程如下：

1. 将 Quote 原始字节转换为 Base64
2. 组装阿里云远程证明服务要求的 evidence JSON
3. 将 evidence JSON 转换为 Base64URL 表达
4. 以 `tee=tdx` 调用阿里云远程证明 endpoint
5. 获取远程证明服务返回的 JWT
6. 通过 JWKS 加载与 JWT `kid` 匹配的 RSA 公钥
7. 校验 JWT 签名算法与签名有效性
8. 校验 issuer、audience、expiration、notBefore、issueTime
9. 校验 JWT 中 `tee=tdx`
10. 按阿里云实际返回格式解析 `tcb-status` 字符串中的扁平 JSON，并构造 `TdxQuoteClaims`
11. 从 `TdxQuoteClaims.body.reportData` 获取 attested `report_data`
12. 构造 `AttestationEvidence`，保留 JWT 注册字段、核心 EAT 字段、原始 claims、`tcb-status`、`evaluation-reports` 与 `customized_claims`
13. 将 attested `report_data` 返回给默认验证器进行最终比对

该验证器将阿里云远程证明协议细节集中封装在 SDK 内部。调用侧通过调用 `verifyQuote(...)` 获得结构化验证结果。

远程证明载荷中可解析的主要 TDX 字段包括：

- 顶层字段：`init_data`、`report_data`
- Quote 元信息：`tdx.quote.type`、`tdx.quote.size`
- Quote header：`tdx.quote.header.version`、`tdx.quote.header.att_key_type`、`tdx.quote.header.tee_type`、`tdx.quote.header.reserved`、`tdx.quote.header.vendor_id`、`tdx.quote.header.user_data`
- Quote body：`tdx.quote.body.tcb_svn`、`tdx.quote.body.mr_seam`、`tdx.quote.body.mrsigner_seam`、`tdx.quote.body.seam_attributes`、`tdx.quote.body.td_attributes`、`tdx.quote.body.xfam`、`tdx.quote.body.mr_td`、`tdx.quote.body.mr_config_id`、`tdx.quote.body.mr_owner`、`tdx.quote.body.mr_owner_config`、`tdx.quote.body.rtmr_0` 到 `tdx.quote.body.rtmr_3`、`tdx.quote.body.report_data`、`tdx.quote.body.tee_tcb_svn2`、`tdx.quote.body.mr_servicetd`
- TD attributes：`debug`、`key_locker`、`perfmon`、`protection_keys`、`septve_disable`

SDK 按上述完整 key 解析。`tdx.quote.type`、`tdx.quote.size` 等字段按远程证明载荷中的原始 hex 字符串保留。

## 12. 可扩展验证接口

SDK 定义了 `QuoteEvidenceVerifier` 接口，用于承接可替换的 Quote 证明验证实现：

```java
QuoteEvidenceVerificationResult verify(byte[] quoteBytes, String expectedReportDataHex)
```

默认实现为 `AliyunRemoteQuoteEvidenceVerifier`。自定义策略校验或测试场景可以通过客户端 Builder 注入自定义实现：

```java
AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .quoteEvidenceVerifier(customVerifier)
        .build();
```

也可以通过 `remoteAttestationConfig(...)` 调整阿里云远程证明服务配置：

```java
AliyunTdxAttestationClient client = AliyunTdxAttestationClient.builder()
        .remoteAttestationConfig(config)
        .build();
```

该扩展点使 SDK 在保持默认阿里云官方远程证明实现的同时，也能承接策略扩展和环境适配。

## 13. 当前环境证明画像

`TdxEnvironmentProfile` 是 v3.0.0 新增的组合输出模型，用于表达一次完整的当前可信执行环境证明结果。该模型由 `attestCurrentEnvironment(...)` 返回，内部串联 Quote 生成、Quote 验证和远程证明载荷解析。

模型字段包括：

- `fingerprint`：调用侧传入的部署级指纹
- `quote`：`QuoteGenerationResult`，保留 Quote 原始材料与生成侧元信息
- `verification`：`QuoteVerificationResult`，保留结构化验证结论
- `evidence`：`AttestationEvidence`，保留远程证明 JWT 和 TDX claims 解析结果
- `attestedAt`：SDK 完成证明画像组装的时间

其中 `AttestationEvidence` 包含：

- `rawJwt`：阿里云远程证明服务返回的 JWT 原文
- `tokenClaims`：JWT header、注册字段与核心 EAT 字段
- `tdxQuote`：从 `tcb-status` 字符串扁平 JSON 中解析出的 TDX Quote claims
- `rawClaims`：JWT claims 原始 Map
- `tcbStatusClaims`：`tcb-status` 原始 Map
- `evaluationReports`：远程证明服务返回的策略评估信息
- `customizedClaims`：远程证明服务返回的自定义 claims

该设计通过基础验证模型表达验证结论，通过证明画像模型表达完整 TDX 实例状态。

## 14. 工具类与基础能力

SDK 提供两个基础工具类：

- `Sha256Utils`
  - 提供 `SHA-256` 字节摘要与 hex 摘要能力
  - 服务于部署级指纹摘要和 Quote 摘要生成

- `HexUtils`
  - 提供 hex 编码、解码与规范化能力
  - 服务于 `deploymentDigestHex`、`reportDataHex`、Quote hash 和远程证明 claims 解析

异常模型包括：

- `AttestationException`
- `QuoteGenerationException`

其中 Quote 生成链路通过 `QuoteGenerationException` 表达生成侧运行环境或 native 调用异常；验证链路通过 `QuoteVerificationResult` 表达验证结论与失败原因。

## 15. Maven 构建与产物

SDK Maven 坐标如下：

```xml
<groupId>com.ywacko</groupId>
<artifactId>aliyun-tdx-attestation-sdk</artifactId>
<version>3.0.0</version>
```

执行 Maven 测试生命周期与编译校验：

```bash
mvn test
```

执行打包：

```bash
mvn package
```

生成产物：

```text
target/aliyun-tdx-attestation-sdk-3.0.0.jar
```

## 16. 当前版本代码能力总结

当前版本 SDK 形成了完整的远程证明能力闭环：

- 以部署级指纹作为可信身份输入
- 以 canonical JSON 和 `SHA-256` 保证摘要稳定性
- 以 64 字节 `report_data` 承接 TDX Quote 绑定
- 以 `JNA` 直连阿里云 TDX native 运行时生成 Quote
- 以 `QuoteGenerationResult` 输出完整 Quote 材料
- 以 `QuoteVerificationRequest` 对齐 Quote 材料结构
- 以 `DefaultQuoteVerifier` 完成本地结构与自洽校验
- 以 `AliyunRemoteQuoteEvidenceVerifier` 对接阿里云官方远程证明服务
- 以 `QuoteVerificationResult` 输出结构化验证结论
- 以 `AttestationEvidence` 和 `TdxQuoteClaims` 输出远程证明载荷解析结果
- 以 `TdxEnvironmentProfile` 输出当前可信执行环境证明画像
- 以 Builder 与接口抽象保留运行环境配置和验证策略扩展能力

通过上述代码结构，SDK 将可信执行环境证明材料的生成、表达、传递和验证统一封装为面向 Java 服务的标准能力。
