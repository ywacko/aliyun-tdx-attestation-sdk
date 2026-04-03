# aliyun-tdx-attestation-sdk 设计文档

## 1. 目标

`aliyun-tdx-attestation-sdk` 面向 `TeeGateway` 这类 Java 服务，提供阿里云 `TDX VM` 环境下的 attestation `Quote` 生成能力。

当前设计目标有三点：

- 对业务代码暴露稳定的 Java SDK API，而不是让业务代码直接处理 native 库
- 把 `TeeGateway` 部署级指纹收敛成标准 `report_data` 生成流程
- 为后续把 `Quote` 封装到网关接口返回中预留稳定边界

## 2. 当前架构

当前 SDK 分成两层：

- Java SDK 层
  - 负责部署级指纹模型
  - 负责 canonical JSON 和 `SHA-256` 摘要
  - 负责生成 64 字节 `report_data`
  - 负责调用 helper 并返回 `Quote`
- helper bridge 层
  - 运行在阿里云 `TDX VM` 内
  - 负责调用 `libtdx_attest.so`
  - 负责最终执行 `tdx_att_get_quote(...)`

当前仓库已经同时放入两部分实现：

- Java SDK 实现：`src/main/java`
- native helper 实现：`native/tdx-quote-helper.c`

这样做的理由是：

- `TeeGateway` 当前是 Java 服务
- 阿里云现成能力是 C 库接口
- 第一版用 helper bridge 比直接上 `JNI` 更稳
- Java API 可以先固定，helper 内部实现后续仍可替换为 `JNA/JNI`

## 3. 部署级指纹与 report_data

当前绑定对象采用 `TeeGateway` 部署级指纹：

```json
{
  "service": "tee-gateway",
  "container_name": "tee-gateway",
  "image_ref": "ywackoo/tee-gateway:20260402",
  "image_id": "a6c80d35a995",
  "git_rev": "021b2d7"
}
```

SDK 处理流程如下：

1. 生成字段顺序固定的 canonical JSON
2. 对 canonical JSON 计算 `SHA-256`
3. 将 32 字节摘要写入 64 字节 `report_data` 的前 32 字节
4. 剩余 32 字节补零
5. 将完整 64 字节 `report_data` 交给 helper

这样做的好处是：

- 输入稳定
- 长度固定
- 可以直接对齐当前已验证通过的阿里云 `TDX` 实验链路

## 4. SDK API

当前建议对外暴露以下主接口：

- `AliyunTdxAttestationClient`
  - `generateQuote(DeploymentFingerprint fingerprint)`
  - `generateQuote(QuoteGenerationRequest request)`
- `DeploymentFingerprint`
  - 表示待绑定的部署级身份对象
- `QuoteGenerationRequest`
  - 表示显式传入的生成请求
- `QuoteGenerationResult`
  - 返回 `Quote`、摘要、`report_data` 和 helper 元信息

## 5. helper 协议

### 5.1 输入

SDK 通过 helper 的标准输入发送 JSON：

```json
{
  "reportDataHex": "<128 hex chars>",
  "deploymentDigestHex": "<64 hex chars>"
}
```

字段说明：

- `reportDataHex`
  - 完整 64 字节 `report_data`
  - 128 个十六进制字符
- `deploymentDigestHex`
  - 原始部署级指纹 `SHA-256`
  - 64 个十六进制字符

### 5.2 输出

helper 标准输出返回 JSON：

```json
{
  "quoteBase64": "<base64>",
  "quoteSize": 5006,
  "reportDataHex": "<128 hex chars>",
  "provider": "aliyun-tdx-helper",
  "helperVersion": "0.1.0"
}
```

字段说明：

- `quoteBase64`
  - 必填，Base64 编码后的 `Quote`
- `quoteSize`
  - 可选，返回 `Quote` 原始字节长度
- `reportDataHex`
  - 可选，helper 回显最终使用的 `report_data`
- `provider`
  - 可选，helper 提供方标识
- `helperVersion`
  - 可选，helper 版本

## 6. 容器与运行要求

如果后续把 `TeeGateway` 放进 Docker 容器，运行要求至少包括：

- 容器内可访问 `/dev/tdx_guest`
- 容器内可调用 helper 可执行文件
- helper 运行环境可访问 `libtdx_attest.so`
- helper 若依赖 verifier 或其他本地能力，需同步挂载相关配置和库

## 7. 后续演进

当前第一版先固定 SDK API 和 helper 协议。

后续可演进方向：

- 提供 JNI/JNA 实现，替换 process bridge
- 增加 `Quote` 验证结果模型
- 增加 `MRTD`、`RTMR` 等字段的解析模型
- 增加 `TeeGateway` 直接集成示例
