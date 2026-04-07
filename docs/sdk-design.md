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
  - 负责通过 `JNA` 调用 native `TDX` 库并返回 `Quote`
- Native `TDX` 运行时层
  - 运行在阿里云 `TDX VM` 内
  - 提供 `libtdx_attest.so`
  - 最终执行 `tdx_att_get_quote(...)`

当前仓库当前的核心实现位于：

- Java SDK 实现：`src/main/java`

这样做的理由是：

- `TeeGateway` 当前是 Java 服务
- 阿里云现成能力是 C 库接口
- 使用 `JNA` 可以直接从 Java 映射到本地 C 库
- 这样可以避免额外部署 helper 进程
- Java API 可以固定在 SDK 层，后续如有必要再替换成 `JNI`
- `TeeGateway` 后续应通过 Maven 依赖引入 SDK，而不是直接复制 SDK 源码

## 3. 部署级指纹与 report_data

当前绑定对象采用 `TeeGateway` 部署级指纹：

```json
{
  "service": "tee-gateway",
  "image_digest": "ywackoo/tee-gateway@sha256:3872a935ba90b46925684a818401a682fb1aefd70b397e9c110bbbd2781aef46",
  "git_rev": "021b2d7"
}
```

当前最终方案将下面三个字段纳入核心部署级指纹：

1. `service`
2. `image_digest`
3. `git_rev`

选择这个方案的原因是：

1. `service` 用于表达当前绑定的是哪个服务
2. `image_digest` 用于表达当前绑定的是哪份不可变镜像内容
3. `git_rev` 用于表达当前绑定的是哪版源码

当前不再把以下字段纳入核心部署级指纹：

1. `container_name`
2. `image_ref`
3. `image_id`

原因如下：

1. `container_name` 在扩容或更换编排方式后容易变化，不适合作为长期身份锚点
2. `image_ref` 更适合做展示和排查字段，但 tag 口径不够稳定
3. `image_id` 更偏本地 Docker 运行时视角，不如 `image_digest` 适合作为跨环境一致的镜像内容标识

SDK 处理流程如下：

1. 生成字段顺序固定的 canonical JSON
2. 对 canonical JSON 计算 `SHA-256`
3. 将 32 字节摘要写入 64 字节 `report_data` 的前 32 字节
4. 剩余 32 字节补零
5. 将完整 64 字节 `report_data` 交给本地 `TDX` native 调用

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
  - 返回 `Quote`、摘要、`report_data` 和 provider 元信息

## 5. 运行要求

SDK 的运行前提必须明确：

- 当前服务运行在阿里云 `TDX VM`
- 当前机器存在 `/dev/tdx_guest`
- 当前机器可加载 `libtdx_attest.so`

如果上述前提不成立，`Quote` 生成能力本身就不成立。

如果后续把 `TeeGateway` 放进 Docker 容器，运行要求至少包括：

- 容器内可访问 `/dev/tdx_guest`
- 容器内可加载 `libtdx_attest.so`
- JVM 进程有权限访问本地 `TDX` 运行时能力

## 6. 后续演进

当前第一版先固定 SDK API 和 `JNA` 直连路径，不再保留多实现切换。

当前推荐的集成方式是：

- `TeeGateway` 通过 Maven 依赖接入 `aliyun-tdx-attestation-sdk`
- SDK 直接通过 `JNA` 调本地 `TDX` 运行时
- 不额外部署 helper 或宿主机 attestation service

后续可演进方向：

- 提供 `JNI` 实现，替换当前 `JNA` 实现
- 增加 `Quote` 验证结果模型
- 增加 `MRTD`、`RTMR` 等字段的解析模型
- 增加 `TeeGateway` 直接集成示例
