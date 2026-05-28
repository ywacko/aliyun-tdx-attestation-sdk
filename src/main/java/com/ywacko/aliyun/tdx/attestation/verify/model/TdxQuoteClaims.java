package com.ywacko.aliyun.tdx.attestation.verify.model;

/**
 * 阿里云远程证明 JWT 中可解析的 TDX Quote 字段。
 */
public final class TdxQuoteClaims {

    private final String type;
    private final String size;
    private final String initData;
    private final String reportData;
    private final Header header;
    private final Body body;
    private final TdAttributes tdAttributes;

    private TdxQuoteClaims(Builder builder) {
        this.type = builder.type;
        this.size = builder.size;
        this.initData = builder.initData;
        this.reportData = builder.reportData;
        this.header = builder.header;
        this.body = builder.body;
        this.tdAttributes = builder.tdAttributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getType() {
        return type;
    }

    public String getSize() {
        return size;
    }

    public String getInitData() {
        return initData;
    }

    public String getReportData() {
        return reportData;
    }

    public Header getHeader() {
        return header;
    }

    public Body getBody() {
        return body;
    }

    public TdAttributes getTdAttributes() {
        return tdAttributes;
    }

    public static final class Header {
        private final String version;
        private final String attKeyType;
        private final String teeType;
        private final String reserved;
        private final String vendorId;
        private final String userData;

        private Header(HeaderBuilder builder) {
            this.version = builder.version;
            this.attKeyType = builder.attKeyType;
            this.teeType = builder.teeType;
            this.reserved = builder.reserved;
            this.vendorId = builder.vendorId;
            this.userData = builder.userData;
        }

        public static HeaderBuilder builder() {
            return new HeaderBuilder();
        }

        public String getVersion() {
            return version;
        }

        public String getAttKeyType() {
            return attKeyType;
        }

        public String getTeeType() {
            return teeType;
        }

        public String getReserved() {
            return reserved;
        }

        public String getVendorId() {
            return vendorId;
        }

        public String getUserData() {
            return userData;
        }
    }

    public static final class Body {
        private final String tcbSvn;
        private final String mrSeam;
        private final String mrSignerSeam;
        private final String seamAttributes;
        private final String tdAttributes;
        private final String xfam;
        private final String mrTd;
        private final String mrConfigId;
        private final String mrOwner;
        private final String mrOwnerConfig;
        private final String rtmr0;
        private final String rtmr1;
        private final String rtmr2;
        private final String rtmr3;
        private final String reportData;
        private final String teeTcbSvn2;
        private final String mrServiceTd;

        private Body(BodyBuilder builder) {
            this.tcbSvn = builder.tcbSvn;
            this.mrSeam = builder.mrSeam;
            this.mrSignerSeam = builder.mrSignerSeam;
            this.seamAttributes = builder.seamAttributes;
            this.tdAttributes = builder.tdAttributes;
            this.xfam = builder.xfam;
            this.mrTd = builder.mrTd;
            this.mrConfigId = builder.mrConfigId;
            this.mrOwner = builder.mrOwner;
            this.mrOwnerConfig = builder.mrOwnerConfig;
            this.rtmr0 = builder.rtmr0;
            this.rtmr1 = builder.rtmr1;
            this.rtmr2 = builder.rtmr2;
            this.rtmr3 = builder.rtmr3;
            this.reportData = builder.reportData;
            this.teeTcbSvn2 = builder.teeTcbSvn2;
            this.mrServiceTd = builder.mrServiceTd;
        }

        public static BodyBuilder builder() {
            return new BodyBuilder();
        }

        public String getTcbSvn() {
            return tcbSvn;
        }

        public String getMrSeam() {
            return mrSeam;
        }

        public String getMrSignerSeam() {
            return mrSignerSeam;
        }

        public String getSeamAttributes() {
            return seamAttributes;
        }

        public String getTdAttributes() {
            return tdAttributes;
        }

        public String getXfam() {
            return xfam;
        }

        public String getMrTd() {
            return mrTd;
        }

        public String getMrConfigId() {
            return mrConfigId;
        }

        public String getMrOwner() {
            return mrOwner;
        }

        public String getMrOwnerConfig() {
            return mrOwnerConfig;
        }

        public String getRtmr0() {
            return rtmr0;
        }

        public String getRtmr1() {
            return rtmr1;
        }

        public String getRtmr2() {
            return rtmr2;
        }

        public String getRtmr3() {
            return rtmr3;
        }

        public String getReportData() {
            return reportData;
        }

        public String getTeeTcbSvn2() {
            return teeTcbSvn2;
        }

        public String getMrServiceTd() {
            return mrServiceTd;
        }
    }

    public static final class TdAttributes {
        private final Boolean debug;
        private final Boolean keyLocker;
        private final Boolean perfmon;
        private final Boolean protectionKeys;
        private final Boolean septveDisable;

        private TdAttributes(TdAttributesBuilder builder) {
            this.debug = builder.debug;
            this.keyLocker = builder.keyLocker;
            this.perfmon = builder.perfmon;
            this.protectionKeys = builder.protectionKeys;
            this.septveDisable = builder.septveDisable;
        }

        public static TdAttributesBuilder builder() {
            return new TdAttributesBuilder();
        }

        public Boolean getDebug() {
            return debug;
        }

        public Boolean getKeyLocker() {
            return keyLocker;
        }

        public Boolean getPerfmon() {
            return perfmon;
        }

        public Boolean getProtectionKeys() {
            return protectionKeys;
        }

        public Boolean getSeptveDisable() {
            return septveDisable;
        }
    }

    public static final class Builder {
        private String type;
        private String size;
        private String initData;
        private String reportData;
        private Header header;
        private Body body;
        private TdAttributes tdAttributes;

        private Builder() {
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder initData(String initData) {
            this.initData = initData;
            return this;
        }

        public Builder reportData(String reportData) {
            this.reportData = reportData;
            return this;
        }

        public Builder header(Header header) {
            this.header = header;
            return this;
        }

        public Builder body(Body body) {
            this.body = body;
            return this;
        }

        public Builder tdAttributes(TdAttributes tdAttributes) {
            this.tdAttributes = tdAttributes;
            return this;
        }

        public TdxQuoteClaims build() {
            return new TdxQuoteClaims(this);
        }
    }

    public static final class HeaderBuilder {
        private String version;
        private String attKeyType;
        private String teeType;
        private String reserved;
        private String vendorId;
        private String userData;

        private HeaderBuilder() {
        }

        public HeaderBuilder version(String version) {
            this.version = version;
            return this;
        }

        public HeaderBuilder attKeyType(String attKeyType) {
            this.attKeyType = attKeyType;
            return this;
        }

        public HeaderBuilder teeType(String teeType) {
            this.teeType = teeType;
            return this;
        }

        public HeaderBuilder reserved(String reserved) {
            this.reserved = reserved;
            return this;
        }

        public HeaderBuilder vendorId(String vendorId) {
            this.vendorId = vendorId;
            return this;
        }

        public HeaderBuilder userData(String userData) {
            this.userData = userData;
            return this;
        }

        public Header build() {
            return new Header(this);
        }
    }

    public static final class BodyBuilder {
        private String tcbSvn;
        private String mrSeam;
        private String mrSignerSeam;
        private String seamAttributes;
        private String tdAttributes;
        private String xfam;
        private String mrTd;
        private String mrConfigId;
        private String mrOwner;
        private String mrOwnerConfig;
        private String rtmr0;
        private String rtmr1;
        private String rtmr2;
        private String rtmr3;
        private String reportData;
        private String teeTcbSvn2;
        private String mrServiceTd;

        private BodyBuilder() {
        }

        public BodyBuilder tcbSvn(String tcbSvn) {
            this.tcbSvn = tcbSvn;
            return this;
        }

        public BodyBuilder mrSeam(String mrSeam) {
            this.mrSeam = mrSeam;
            return this;
        }

        public BodyBuilder mrSignerSeam(String mrSignerSeam) {
            this.mrSignerSeam = mrSignerSeam;
            return this;
        }

        public BodyBuilder seamAttributes(String seamAttributes) {
            this.seamAttributes = seamAttributes;
            return this;
        }

        public BodyBuilder tdAttributes(String tdAttributes) {
            this.tdAttributes = tdAttributes;
            return this;
        }

        public BodyBuilder xfam(String xfam) {
            this.xfam = xfam;
            return this;
        }

        public BodyBuilder mrTd(String mrTd) {
            this.mrTd = mrTd;
            return this;
        }

        public BodyBuilder mrConfigId(String mrConfigId) {
            this.mrConfigId = mrConfigId;
            return this;
        }

        public BodyBuilder mrOwner(String mrOwner) {
            this.mrOwner = mrOwner;
            return this;
        }

        public BodyBuilder mrOwnerConfig(String mrOwnerConfig) {
            this.mrOwnerConfig = mrOwnerConfig;
            return this;
        }

        public BodyBuilder rtmr0(String rtmr0) {
            this.rtmr0 = rtmr0;
            return this;
        }

        public BodyBuilder rtmr1(String rtmr1) {
            this.rtmr1 = rtmr1;
            return this;
        }

        public BodyBuilder rtmr2(String rtmr2) {
            this.rtmr2 = rtmr2;
            return this;
        }

        public BodyBuilder rtmr3(String rtmr3) {
            this.rtmr3 = rtmr3;
            return this;
        }

        public BodyBuilder reportData(String reportData) {
            this.reportData = reportData;
            return this;
        }

        public BodyBuilder teeTcbSvn2(String teeTcbSvn2) {
            this.teeTcbSvn2 = teeTcbSvn2;
            return this;
        }

        public BodyBuilder mrServiceTd(String mrServiceTd) {
            this.mrServiceTd = mrServiceTd;
            return this;
        }

        public Body build() {
            return new Body(this);
        }
    }

    public static final class TdAttributesBuilder {
        private Boolean debug;
        private Boolean keyLocker;
        private Boolean perfmon;
        private Boolean protectionKeys;
        private Boolean septveDisable;

        private TdAttributesBuilder() {
        }

        public TdAttributesBuilder debug(Boolean debug) {
            this.debug = debug;
            return this;
        }

        public TdAttributesBuilder keyLocker(Boolean keyLocker) {
            this.keyLocker = keyLocker;
            return this;
        }

        public TdAttributesBuilder perfmon(Boolean perfmon) {
            this.perfmon = perfmon;
            return this;
        }

        public TdAttributesBuilder protectionKeys(Boolean protectionKeys) {
            this.protectionKeys = protectionKeys;
            return this;
        }

        public TdAttributesBuilder septveDisable(Boolean septveDisable) {
            this.septveDisable = septveDisable;
            return this;
        }

        public TdAttributes build() {
            return new TdAttributes(this);
        }
    }
}
