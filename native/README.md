# native helper

这里提供 `aliyun-tdx-attestation-sdk` 的本地 helper 实现：

- 文件：`tdx-quote-helper.c`
- 目标：`tdx-quote-helper`

helper 运行在阿里云 `TDX VM` 内，直接调用：

- `tdx_att_get_quote(...)`
- `tdx_att_free_quote(...)`

## 编译

远程 `TDX VM` 已安装如下包时可直接编译：

- `libtdx-attest`
- `libtdx-attest-devel`

编译命令：

```bash
make
```

等价命令：

```bash
gcc -O2 -Wall -Wextra -o tdx-quote-helper tdx-quote-helper.c -ltdx_attest
```

## 输入

helper 从标准输入读取 JSON：

```json
{
  "reportDataHex": "<128 hex chars>",
  "deploymentDigestHex": "<64 hex chars>"
}
```

当前真正用于 `Quote` 生成的是 `reportDataHex`。

## 输出

成功时向标准输出返回 JSON：

```json
{
  "quoteBase64": "<base64>",
  "quoteSize": 5006,
  "reportDataHex": "<128 hex chars>",
  "provider": "aliyun-tdx-helper",
  "helperVersion": "0.1.0"
}
```

失败时：

- 标准错误输出错误信息
- 退出码非 `0`
