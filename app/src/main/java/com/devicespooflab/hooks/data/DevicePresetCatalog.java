package com.devicespooflab.hooks.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DevicePresetCatalog {

    private static final String ASSET_NAME = "device_presets.json";

    public List<DevicePreset> load(Context context) {
        try (InputStream inputStream = context.getAssets().open(ASSET_NAME);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            JSONArray array = new JSONArray(outputStream.toString(StandardCharsets.UTF_8.name()));
            List<DevicePreset> presets = new ArrayList<>();
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.getJSONObject(index);
                DeviceProfile profile = readProfile(item.getJSONObject("profile"));
                profile.applyFallbacks();
                presets.add(new DevicePreset(
                    item.getString("id"),
                    item.getString("brandLabel"),
                    item.getString("modelLabel"),
                    item.optString("summary"),
                    profile
                ));
            }
            if (!presets.isEmpty()) {
                return presets;
            }
        } catch (Exception ignored) {
        }
        return Collections.singletonList(createFallbackPreset());
    }

    public DevicePreset createFallbackPreset() {
        DeviceProfile profile = new DeviceProfile();
        profile.setBrand("google");
        profile.setManufacturer("Google");
        profile.setModel("Pixel 7 Pro");
        profile.setProductName("cheetah");
        profile.setDeviceCode("cheetah");
        profile.setBoard("cheetah");
        profile.setHardware("cheetah");
        profile.setBoardPlatform("gs201");
        profile.setBuildRelease("15");
        profile.setBuildSdk(35);
        profile.setBuildId("AP4A.241205.013");
        profile.setBuildDisplayId("AP4A.241205.013");
        profile.setBuildIncremental("12621605");
        profile.setBuildFingerprint("google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        profile.setSecurityPatch("2024-12-05");
        profile.setScreenWidth(1440);
        profile.setScreenHeight(3120);
        profile.setScreenDensity(512);
        profile.setOperatorAlpha("T-Mobile");
        profile.setOperatorNumeric("310260");
        profile.setSimOperatorAlpha("T-Mobile");
        profile.setSimOperatorNumeric("310260");
        profile.setSimCountryIso("us");
        profile.setTimezone("America/Los_Angeles");
        profile.applyFallbacks();
        return new DevicePreset(
            "pixel_7_pro",
            "Google",
            "Pixel 7 Pro",
            "Tensor G2 - Android 15",
            profile
        );
    }

    private DeviceProfile readProfile(JSONObject jsonObject) {
        DeviceProfile profile = new DeviceProfile();
        profile.setBrand(jsonObject.optString("brand"));
        profile.setManufacturer(jsonObject.optString("manufacturer"));
        profile.setModel(jsonObject.optString("model"));
        profile.setProductName(jsonObject.optString("productName"));
        profile.setDeviceCode(jsonObject.optString("deviceCode"));
        profile.setBoard(jsonObject.optString("board"));
        profile.setHardware(jsonObject.optString("hardware"));
        profile.setBoardPlatform(jsonObject.optString("boardPlatform"));
        profile.setBuildFingerprint(jsonObject.optString("buildFingerprint"));
        profile.setBuildId(jsonObject.optString("buildId"));
        profile.setBuildDisplayId(jsonObject.optString("buildDisplayId"));
        profile.setBuildIncremental(jsonObject.optString("buildIncremental"));
        profile.setBuildRelease(jsonObject.optString("buildRelease"));
        profile.setBuildSdk(jsonObject.optInt("buildSdk"));
        profile.setSecurityPatch(jsonObject.optString("securityPatch"));
        profile.setBuildDescription(jsonObject.optString("buildDescription"));
        profile.setBuildFlavor(jsonObject.optString("buildFlavor"));
        profile.setBuildProduct(jsonObject.optString("buildProduct"));
        profile.setBuildCharacteristics(jsonObject.optString("buildCharacteristics"));
        profile.setScreenWidth(jsonObject.optInt("screenWidth"));
        profile.setScreenHeight(jsonObject.optInt("screenHeight"));
        profile.setScreenDensity(jsonObject.optInt("screenDensity"));
        profile.setOperatorAlpha(jsonObject.optString("operatorAlpha"));
        profile.setOperatorNumeric(jsonObject.optString("operatorNumeric"));
        profile.setSimOperatorAlpha(jsonObject.optString("simOperatorAlpha"));
        profile.setSimOperatorNumeric(jsonObject.optString("simOperatorNumeric"));
        profile.setSimCountryIso(jsonObject.optString("simCountryIso"));
        profile.setTimezone(jsonObject.optString("timezone"));
        profile.setUserAgent(jsonObject.optString("userAgent"));
        profile.setSerialNumber(jsonObject.optString("serialNumber"));
        profile.setBootloader(jsonObject.optString("bootloader"));
        profile.setAndroidId(jsonObject.optString("androidId"));
        profile.setCpuAbi(jsonObject.optString("cpuAbi"));
        profile.setCpuAbiList(jsonObject.optString("cpuAbiList"));
        profile.setCpuAbiList64(jsonObject.optString("cpuAbiList64"));
        profile.setCpuAbiList32(jsonObject.optString("cpuAbiList32"));
        profile.setSocModel(jsonObject.optString("socModel"));
        profile.setSocManufacturer(jsonObject.optString("socManufacturer"));
        return profile;
    }
}
