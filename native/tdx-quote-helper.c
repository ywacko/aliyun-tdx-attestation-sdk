#include <ctype.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "tdx_attest.h"

#define HELPER_VERSION "0.1.0"
#define PROVIDER_NAME "aliyun-tdx-helper"
#define REPORT_DATA_HEX_LEN 128

static char *read_all_stdin(void) {
    size_t capacity = 4096;
    size_t length = 0;
    char *buffer = (char *) malloc(capacity);
    if (buffer == NULL) {
        return NULL;
    }

    int ch = 0;
    while ((ch = getchar()) != EOF) {
        if (length + 1 >= capacity) {
            capacity *= 2;
            char *next = (char *) realloc(buffer, capacity);
            if (next == NULL) {
                free(buffer);
                return NULL;
            }
            buffer = next;
        }
        buffer[length++] = (char) ch;
    }
    buffer[length] = '\0';
    return buffer;
}

static int json_extract_string(const char *json, const char *key, char *out, size_t out_len) {
    char pattern[128];
    snprintf(pattern, sizeof(pattern), "\"%s\"", key);
    const char *pos = strstr(json, pattern);
    if (pos == NULL) {
        return -1;
    }

    pos += strlen(pattern);
    while (*pos != '\0' && isspace((unsigned char) *pos)) {
        pos++;
    }
    if (*pos != ':') {
        return -1;
    }
    pos++;
    while (*pos != '\0' && isspace((unsigned char) *pos)) {
        pos++;
    }
    if (*pos != '"') {
        return -1;
    }
    pos++;

    size_t i = 0;
    while (*pos != '\0' && *pos != '"') {
        if (*pos == '\\' && pos[1] != '\0') {
            pos++;
        }
        if (i + 1 >= out_len) {
            return -1;
        }
        out[i++] = *pos++;
    }
    if (*pos != '"') {
        return -1;
    }
    out[i] = '\0';
    return 0;
}

static int hex_char_to_int(char c) {
    if (c >= '0' && c <= '9') {
        return c - '0';
    }
    if (c >= 'a' && c <= 'f') {
        return c - 'a' + 10;
    }
    if (c >= 'A' && c <= 'F') {
        return c - 'A' + 10;
    }
    return -1;
}

static int hex_to_bytes(const char *hex, uint8_t *out, size_t out_len) {
    size_t hex_len = strlen(hex);
    if (hex_len != out_len * 2) {
        return -1;
    }
    for (size_t i = 0; i < out_len; ++i) {
        int hi = hex_char_to_int(hex[i * 2]);
        int lo = hex_char_to_int(hex[i * 2 + 1]);
        if (hi < 0 || lo < 0) {
            return -1;
        }
        out[i] = (uint8_t) ((hi << 4) | lo);
    }
    return 0;
}

static char *base64_encode(const uint8_t *data, size_t len) {
    static const char table[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    size_t out_len = ((len + 2) / 3) * 4;
    char *out = (char *) malloc(out_len + 1);
    if (out == NULL) {
        return NULL;
    }

    size_t i = 0;
    size_t j = 0;
    while (i < len) {
        uint32_t octet_a = i < len ? data[i++] : 0;
        uint32_t octet_b = i < len ? data[i++] : 0;
        uint32_t octet_c = i < len ? data[i++] : 0;
        uint32_t triple = (octet_a << 16) | (octet_b << 8) | octet_c;

        out[j++] = table[(triple >> 18) & 0x3f];
        out[j++] = table[(triple >> 12) & 0x3f];
        out[j++] = table[(triple >> 6) & 0x3f];
        out[j++] = table[triple & 0x3f];
    }

    size_t mod = len % 3;
    if (mod != 0) {
        out[out_len - 1] = '=';
        if (mod == 1) {
            out[out_len - 2] = '=';
        }
    }

    out[out_len] = '\0';
    return out;
}

int main(void) {
    char *json = read_all_stdin();
    if (json == NULL) {
        fprintf(stderr, "failed to read stdin\n");
        return 2;
    }

    char report_data_hex[REPORT_DATA_HEX_LEN + 1];
    if (json_extract_string(json, "reportDataHex", report_data_hex, sizeof(report_data_hex)) != 0) {
        fprintf(stderr, "missing or invalid reportDataHex\n");
        free(json);
        return 3;
    }
    free(json);

    tdx_report_data_t report_data;
    memset(&report_data, 0, sizeof(report_data));
    if (hex_to_bytes(report_data_hex, report_data.d, sizeof(report_data.d)) != 0) {
        fprintf(stderr, "reportDataHex must be exactly 128 hex chars\n");
        return 4;
    }

    uint8_t *quote_buf = NULL;
    uint32_t quote_size = 0;
    tdx_attest_error_t ret = tdx_att_get_quote(&report_data, NULL, 0, NULL, &quote_buf, &quote_size, 0);
    if (ret != TDX_ATTEST_SUCCESS) {
        fprintf(stderr, "tdx_att_get_quote failed: 0x%04x\n", ret);
        return 5;
    }

    char *quote_base64 = base64_encode(quote_buf, quote_size);
    if (quote_base64 == NULL) {
        fprintf(stderr, "failed to base64 encode quote\n");
        tdx_att_free_quote(quote_buf);
        return 6;
    }

    printf("{\"quoteBase64\":\"%s\",\"quoteSize\":%u,\"reportDataHex\":\"%s\",\"provider\":\"%s\",\"helperVersion\":\"%s\"}\n",
           quote_base64, quote_size, report_data_hex, PROVIDER_NAME, HELPER_VERSION);

    free(quote_base64);
    tdx_att_free_quote(quote_buf);
    return 0;
}
