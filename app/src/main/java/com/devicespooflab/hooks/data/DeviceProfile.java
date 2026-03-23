package com.devicespooflab.hooks.data;

import java.util.Locale;
import java.util.Objects;

public class DeviceProfile {

    private String brand;
    private String manufacturer;
    private String model;
    private String productName;
    private String deviceCode;
    private String board;
    private String hardware;
    private String boardPlatform;
    private String buildFingerprint;
    private String buildId;
    private String buildDisplayId;
    private String buildIncremental;
    private String buildRelease;
    private int buildSdk;
    private String securityPatch;
    private String buildDescription;
    private String buildFlavor;
    private String buildProduct;
    private String buildCharacteristics;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    private String operatorAlpha;
    private String operatorNumeric;
    private String simOperatorAlpha;
    private String simOperatorNumeric;
    private String simCountryIso;
    private String timezone;
    private String userAgent;
    private String serialNumber;
    private String bootloader;
    private String androidId;
    private String cpuAbi;
    private String cpuAbiList;
    private String cpuAbiList64;
    private String cpuAbiList32;
    private String socModel;
    private String socManufacturer;

    public DeviceProfile() {
    }

    public DeviceProfile(DeviceProfile other) {
        this.brand = other.brand;
        this.manufacturer = other.manufacturer;
        this.model = other.model;
        this.productName = other.productName;
        this.deviceCode = other.deviceCode;
        this.board = other.board;
        this.hardware = other.hardware;
        this.boardPlatform = other.boardPlatform;
        this.buildFingerprint = other.buildFingerprint;
        this.buildId = other.buildId;
        this.buildDisplayId = other.buildDisplayId;
        this.buildIncremental = other.buildIncremental;
        this.buildRelease = other.buildRelease;
        this.buildSdk = other.buildSdk;
        this.securityPatch = other.securityPatch;
        this.buildDescription = other.buildDescription;
        this.buildFlavor = other.buildFlavor;
        this.buildProduct = other.buildProduct;
        this.buildCharacteristics = other.buildCharacteristics;
        this.screenWidth = other.screenWidth;
        this.screenHeight = other.screenHeight;
        this.screenDensity = other.screenDensity;
        this.operatorAlpha = other.operatorAlpha;
        this.operatorNumeric = other.operatorNumeric;
        this.simOperatorAlpha = other.simOperatorAlpha;
        this.simOperatorNumeric = other.simOperatorNumeric;
        this.simCountryIso = other.simCountryIso;
        this.timezone = other.timezone;
        this.userAgent = other.userAgent;
        this.serialNumber = other.serialNumber;
        this.bootloader = other.bootloader;
        this.androidId = other.androidId;
        this.cpuAbi = other.cpuAbi;
        this.cpuAbiList = other.cpuAbiList;
        this.cpuAbiList64 = other.cpuAbiList64;
        this.cpuAbiList32 = other.cpuAbiList32;
        this.socModel = other.socModel;
        this.socManufacturer = other.socManufacturer;
    }

    public DeviceProfile copy() {
        return new DeviceProfile(this);
    }

    public void applyFallbacks() {
        brand = normalized(brand);
        manufacturer = firstNonBlank(manufacturer, brand);
        model = firstNonBlank(model, "Unknown device");
        deviceCode = firstNonBlank(deviceCode, slugify(model));
        productName = firstNonBlank(productName, deviceCode);
        board = firstNonBlank(board, deviceCode);
        hardware = firstNonBlank(hardware, board);
        boardPlatform = firstNonBlank(boardPlatform, board);
        buildRelease = firstNonBlank(buildRelease, "15");
        buildSdk = buildSdk > 0 ? buildSdk : 35;
        buildId = firstNonBlank(buildId, "AP4A.241205.013");
        buildDisplayId = firstNonBlank(buildDisplayId, buildId);
        buildIncremental = firstNonBlank(buildIncremental, buildId.replace(".", ""));
        securityPatch = firstNonBlank(securityPatch, "2024-12-05");
        buildProduct = firstNonBlank(buildProduct, productName);
        buildFlavor = firstNonBlank(buildFlavor, productName + "-user");
        buildCharacteristics = firstNonBlank(buildCharacteristics, "nosdcard");
        buildDescription = firstNonBlank(
            buildDescription,
            String.format(
                Locale.US,
                "%s-user %s %s %s release-keys",
                deviceCode,
                buildRelease,
                buildId,
                buildIncremental
            )
        );
        buildFingerprint = firstNonBlank(
            buildFingerprint,
            String.format(
                Locale.US,
                "%s/%s/%s:%s/%s/%s:user/release-keys",
                firstNonBlank(brand, manufacturer).toLowerCase(Locale.US),
                productName,
                deviceCode,
                buildRelease,
                buildId,
                buildIncremental
            )
        );
        screenWidth = screenWidth > 0 ? screenWidth : 1440;
        screenHeight = screenHeight > 0 ? screenHeight : 3120;
        screenDensity = screenDensity > 0 ? screenDensity : 480;
        operatorAlpha = firstNonBlank(operatorAlpha, "T-Mobile");
        operatorNumeric = firstNonBlank(operatorNumeric, "310260");
        simOperatorAlpha = firstNonBlank(simOperatorAlpha, operatorAlpha);
        simOperatorNumeric = firstNonBlank(simOperatorNumeric, operatorNumeric);
        simCountryIso = firstNonBlank(simCountryIso, "us");
        timezone = firstNonBlank(timezone, "America/Los_Angeles");
        userAgent = firstNonBlank(
            userAgent,
            String.format(
                Locale.US,
                "Mozilla/5.0 (Linux; Android %s; %s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                buildRelease,
                model
            )
        );
        cpuAbi = firstNonBlank(cpuAbi, "arm64-v8a");
        cpuAbiList = firstNonBlank(cpuAbiList, "arm64-v8a,armeabi-v7a,armeabi");
        cpuAbiList64 = firstNonBlank(cpuAbiList64, "arm64-v8a");
        cpuAbiList32 = firstNonBlank(cpuAbiList32, "armeabi-v7a,armeabi");
        socModel = firstNonBlank(socModel, boardPlatform);
        socManufacturer = firstNonBlank(socManufacturer, manufacturer);
    }

    public boolean matchesPreset(DeviceProfile other) {
        if (other == null) {
            return false;
        }
        DeviceProfile left = copy();
        DeviceProfile right = other.copy();
        left.applyFallbacks();
        right.applyFallbacks();
        return Objects.equals(left.brand, right.brand)
            && Objects.equals(left.manufacturer, right.manufacturer)
            && Objects.equals(left.model, right.model)
            && Objects.equals(left.productName, right.productName)
            && Objects.equals(left.deviceCode, right.deviceCode)
            && Objects.equals(left.boardPlatform, right.boardPlatform)
            && Objects.equals(left.buildFingerprint, right.buildFingerprint)
            && Objects.equals(left.buildRelease, right.buildRelease)
            && left.buildSdk == right.buildSdk
            && left.screenWidth == right.screenWidth
            && left.screenHeight == right.screenHeight
            && left.screenDensity == right.screenDensity;
    }

    public String getDisplayName() {
        return firstNonBlank((manufacturer + " " + model).trim(), model);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String slugify(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "device";
        }
        return value.toLowerCase(Locale.US)
            .replace(" ", "_")
            .replace("-", "_");
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public String getBoard() { return board; }
    public void setBoard(String board) { this.board = board; }
    public String getHardware() { return hardware; }
    public void setHardware(String hardware) { this.hardware = hardware; }
    public String getBoardPlatform() { return boardPlatform; }
    public void setBoardPlatform(String boardPlatform) { this.boardPlatform = boardPlatform; }
    public String getBuildFingerprint() { return buildFingerprint; }
    public void setBuildFingerprint(String buildFingerprint) { this.buildFingerprint = buildFingerprint; }
    public String getBuildId() { return buildId; }
    public void setBuildId(String buildId) { this.buildId = buildId; }
    public String getBuildDisplayId() { return buildDisplayId; }
    public void setBuildDisplayId(String buildDisplayId) { this.buildDisplayId = buildDisplayId; }
    public String getBuildIncremental() { return buildIncremental; }
    public void setBuildIncremental(String buildIncremental) { this.buildIncremental = buildIncremental; }
    public String getBuildRelease() { return buildRelease; }
    public void setBuildRelease(String buildRelease) { this.buildRelease = buildRelease; }
    public int getBuildSdk() { return buildSdk; }
    public void setBuildSdk(int buildSdk) { this.buildSdk = buildSdk; }
    public String getSecurityPatch() { return securityPatch; }
    public void setSecurityPatch(String securityPatch) { this.securityPatch = securityPatch; }
    public String getBuildDescription() { return buildDescription; }
    public void setBuildDescription(String buildDescription) { this.buildDescription = buildDescription; }
    public String getBuildFlavor() { return buildFlavor; }
    public void setBuildFlavor(String buildFlavor) { this.buildFlavor = buildFlavor; }
    public String getBuildProduct() { return buildProduct; }
    public void setBuildProduct(String buildProduct) { this.buildProduct = buildProduct; }
    public String getBuildCharacteristics() { return buildCharacteristics; }
    public void setBuildCharacteristics(String buildCharacteristics) { this.buildCharacteristics = buildCharacteristics; }
    public int getScreenWidth() { return screenWidth; }
    public void setScreenWidth(int screenWidth) { this.screenWidth = screenWidth; }
    public int getScreenHeight() { return screenHeight; }
    public void setScreenHeight(int screenHeight) { this.screenHeight = screenHeight; }
    public int getScreenDensity() { return screenDensity; }
    public void setScreenDensity(int screenDensity) { this.screenDensity = screenDensity; }
    public String getOperatorAlpha() { return operatorAlpha; }
    public void setOperatorAlpha(String operatorAlpha) { this.operatorAlpha = operatorAlpha; }
    public String getOperatorNumeric() { return operatorNumeric; }
    public void setOperatorNumeric(String operatorNumeric) { this.operatorNumeric = operatorNumeric; }
    public String getSimOperatorAlpha() { return simOperatorAlpha; }
    public void setSimOperatorAlpha(String simOperatorAlpha) { this.simOperatorAlpha = simOperatorAlpha; }
    public String getSimOperatorNumeric() { return simOperatorNumeric; }
    public void setSimOperatorNumeric(String simOperatorNumeric) { this.simOperatorNumeric = simOperatorNumeric; }
    public String getSimCountryIso() { return simCountryIso; }
    public void setSimCountryIso(String simCountryIso) { this.simCountryIso = simCountryIso; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getBootloader() { return bootloader; }
    public void setBootloader(String bootloader) { this.bootloader = bootloader; }
    public String getAndroidId() { return androidId; }
    public void setAndroidId(String androidId) { this.androidId = androidId; }
    public String getCpuAbi() { return cpuAbi; }
    public void setCpuAbi(String cpuAbi) { this.cpuAbi = cpuAbi; }
    public String getCpuAbiList() { return cpuAbiList; }
    public void setCpuAbiList(String cpuAbiList) { this.cpuAbiList = cpuAbiList; }
    public String getCpuAbiList64() { return cpuAbiList64; }
    public void setCpuAbiList64(String cpuAbiList64) { this.cpuAbiList64 = cpuAbiList64; }
    public String getCpuAbiList32() { return cpuAbiList32; }
    public void setCpuAbiList32(String cpuAbiList32) { this.cpuAbiList32 = cpuAbiList32; }
    public String getSocModel() { return socModel; }
    public void setSocModel(String socModel) { this.socModel = socModel; }
    public String getSocManufacturer() { return socManufacturer; }
    public void setSocManufacturer(String socManufacturer) { this.socManufacturer = socManufacturer; }
}
