package com.devicespooflab.hooks.data;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import com.devicespooflab.hooks.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DevicePresetCatalog {

    private static final String CACHE_NAME = "remote_device_presets.json";
    private static final String CACHE_KEY_SOURCE_URL = "sourceUrl";
    private static final String CACHE_KEY_UPDATED_AT = "updatedAt";
    private static final String CACHE_KEY_PRESETS = "presets";
    private static final String CURRENT_DEVICE_PRESET_ID = "current_device";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 12000;

    public List<DevicePreset> load(Context context) {
        List<DevicePreset> cachedPresets = loadFromCache(context);
        if (!cachedPresets.isEmpty()) {
            return withCurrentDevicePreset(context, cachedPresets);
        }
        return withCurrentDevicePreset(context, refreshRemote(context, AppSettingsStore.getPresetSourceUrl(context)));
    }

    public List<DevicePreset> refreshRemote(Context context, String sourceUrl) {
        try {
            JSONArray normalizedArray = fetchRemotePresetArray(sourceUrl);
            List<DevicePreset> presets = parsePresetArray(normalizedArray);
            if (!presets.isEmpty()) {
                writeCache(context, sourceUrl, normalizedArray);
                return withCurrentDevicePreset(context, presets);
            }
        } catch (Exception ignored) {
        }
        return withCurrentDevicePreset(context, loadFromCache(context));
    }

    private List<DevicePreset> loadFromCache(Context context) {
        File cacheFile = getCacheFile(context);
        if (!cacheFile.exists()) {
            return Collections.emptyList();
        }
        try (InputStream inputStream = new FileInputStream(cacheFile)) {
            JSONArray array = extractPresetArray(readFully(inputStream));
            return parsePresetArray(array);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private void writeCache(Context context, String sourceUrl, JSONArray array) {
        try (FileOutputStream outputStream = new FileOutputStream(getCacheFile(context), false)) {
            JSONObject payload = new JSONObject();
            payload.put(CACHE_KEY_SOURCE_URL, sourceUrl == null ? "" : sourceUrl);
            payload.put(CACHE_KEY_UPDATED_AT, System.currentTimeMillis());
            payload.put(CACHE_KEY_PRESETS, array);
            outputStream.write(payload.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private File getCacheFile(Context context) {
        return new File(context.getFilesDir(), CACHE_NAME);
    }

    private List<DevicePreset> withCurrentDevicePreset(Context context, List<DevicePreset> source) {
        ArrayList<DevicePreset> merged = new ArrayList<>();
        merged.add(createCurrentDevicePreset(context));
        if (source != null) {
            for (DevicePreset preset : source) {
                if (preset == null || CURRENT_DEVICE_PRESET_ID.equals(preset.getId())) {
                    continue;
                }
                merged.add(preset);
            }
        }
        return merged;
    }

    private DevicePreset createCurrentDevicePreset(Context context) {
        DeviceProfile profile = new DeviceProfile();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        profile.setBrand(Build.BRAND);
        profile.setManufacturer(Build.MANUFACTURER);
        profile.setModel(Build.MODEL);
        profile.setProductName(Build.PRODUCT);
        profile.setDeviceCode(Build.DEVICE);
        profile.setBoard(Build.BOARD);
        profile.setHardware(Build.HARDWARE);
        profile.setBuildFingerprint(Build.FINGERPRINT);
        profile.setBuildId(Build.ID);
        profile.setBuildDisplayId(Build.DISPLAY);
        profile.setBuildIncremental(Build.VERSION.INCREMENTAL);
        profile.setBuildRelease(Build.VERSION.RELEASE);
        profile.setBuildSdk(Build.VERSION.SDK_INT);
        profile.setBuildDescription(Build.TYPE);
        profile.setBuildFlavor(Build.TYPE);
        profile.setBuildProduct(Build.PRODUCT);
        profile.setBuildCharacteristics(isTablet(metrics) ? "tablet" : "nosdcard");
        profile.setScreenWidth(metrics.widthPixels);
        profile.setScreenHeight(metrics.heightPixels);
        profile.setScreenDensity(metrics.densityDpi);
        profile.setTimezone(TimeZone.getDefault().getID());
        profile.setUserAgent(buildDefaultUserAgent(profile));
        profile.setBootloader(Build.BOOTLOADER);
        profile.setCpuAbi(Build.CPU_ABI);
        profile.setCpuAbiList(joinAbis(Build.SUPPORTED_ABIS));
        profile.setCpuAbiList64(joinAbis(Build.SUPPORTED_64_BIT_ABIS));
        profile.setCpuAbiList32(joinAbis(Build.SUPPORTED_32_BIT_ABIS));
        profile.setSocModel(readBuildField("SOC_MODEL"));
        profile.setSocManufacturer(readBuildField("SOC_MANUFACTURER"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            profile.setSecurityPatch(Build.VERSION.SECURITY_PATCH);
        }

        try {
            TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
            if (telephonyManager != null) {
                profile.setOperatorAlpha(telephonyManager.getNetworkOperatorName());
                profile.setOperatorNumeric(telephonyManager.getNetworkOperator());
                profile.setSimOperatorAlpha(telephonyManager.getSimOperatorName());
                profile.setSimOperatorNumeric(telephonyManager.getSimOperator());
                profile.setSimCountryIso(telephonyManager.getSimCountryIso());
            }
        } catch (Throwable ignored) {
        }

        profile.applyFallbacks();
        return new DevicePreset(
            CURRENT_DEVICE_PRESET_ID,
            context.getString(R.string.preset_current_device_brand),
            firstNonBlank(profile.getModel(), context.getString(R.string.preset_current_device_model)),
            context.getString(R.string.preset_current_device_summary),
            profile
        );
    }

    private JSONArray extractPresetArray(String rawJson) throws Exception {
        String trimmed = rawJson == null ? "" : rawJson.trim();
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        JSONObject payload = new JSONObject(trimmed);
        JSONArray presets = payload.optJSONArray(CACHE_KEY_PRESETS);
        return presets == null ? new JSONArray() : presets;
    }

    private JSONArray fetchRemotePresetArray(String sourceUrl) throws Exception {
        String apiUrl = toGithubContentsApiUrl(sourceUrl);
        JSONArray contents = new JSONArray(readUrl(apiUrl));
        JSONArray normalizedArray = new JSONArray();

        for (int index = 0; index < contents.length(); index++) {
            JSONObject item = contents.getJSONObject(index);
            if (!"file".equalsIgnoreCase(item.optString("type"))) {
                continue;
            }
            String name = item.optString("name");
            if (!name.toLowerCase(Locale.US).endsWith(".json")) {
                continue;
            }

            String downloadUrl = item.optString("download_url");
            if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
                continue;
            }

            JSONObject sourceJson = new JSONObject(readUrl(downloadUrl));
            normalizedArray.put(normalizePresetJson(name, sourceJson));
        }

        return normalizedArray;
    }

    private JSONObject normalizePresetJson(String fileName, JSONObject sourceJson) throws Exception {
        String fileBaseName = fileName.replaceFirst("(?i)\\.json$", "").trim();
        JSONObject profileJson = sourceJson.optJSONObject("profile");
        if (profileJson == null) {
            profileJson = sourceJson;
        }

        String manufacturer = firstNonBlank(
            profileJson.optString("manufacturer"),
            profileJson.optString("brand")
        );
        String model = firstNonBlank(
            profileJson.optString("model"),
            fileBaseName
        );
        String brandLabel = firstNonBlank(sourceJson.optString("brandLabel"), manufacturer);
        String modelLabel = firstNonBlank(sourceJson.optString("modelLabel"), model);

        JSONObject normalized = new JSONObject();
        normalized.put("id", firstNonBlank(sourceJson.optString("id"), slugify(fileBaseName)));
        normalized.put("brandLabel", brandLabel);
        normalized.put("modelLabel", modelLabel);
        normalized.put("summary", firstNonBlank(sourceJson.optString("summary"), buildSummary(profileJson)));
        normalized.put("profile", profileJson);
        return normalized;
    }

    private List<DevicePreset> parsePresetArray(JSONArray array) {
        List<DevicePreset> presets = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item == null) {
                continue;
            }
            JSONObject profileJson = item.optJSONObject("profile");
            if (profileJson == null) {
                continue;
            }
            DeviceProfile profile = readProfile(profileJson);
            profile.applyFallbacks();
            presets.add(new DevicePreset(
                item.optString("id", slugify(item.optString("modelLabel"))),
                item.optString("brandLabel", firstNonBlank(profile.getManufacturer(), profile.getBrand())),
                item.optString("modelLabel", profile.getModel()),
                item.optString("summary"),
                profile
            ));
        }
        return presets;
    }

    private String toGithubContentsApiUrl(String sourceUrl) throws Exception {
        String trimmed = AppSettingsStore.DEFAULT_PRESET_SOURCE_URL.equals(sourceUrl)
            ? sourceUrl
            : sourceUrl.trim();
        if (trimmed.startsWith("https://api.github.com/repos/")) {
            if (trimmed.endsWith("/contents")) {
                return trimmed;
            }
            if (trimmed.contains("/contents/")) {
                return trimmed;
            }
        }

        String normalized = trimmed
            .replace("git@github.com:", "https://github.com/")
            .replace(".git", "");
        if (!normalized.startsWith("https://github.com/")) {
            throw new IllegalArgumentException("Only GitHub repository URLs are supported.");
        }

        String[] parts = normalized.substring("https://github.com/".length()).split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub repository URL.");
        }

        String owner = parts[0];
        String repo = parts[1];
        StringBuilder builder = new StringBuilder("https://api.github.com/repos/")
            .append(owner)
            .append("/")
            .append(repo)
            .append("/contents");

        if (parts.length > 4 && "tree".equals(parts[2])) {
            String branch = parts[3];
            StringBuilder pathBuilder = new StringBuilder();
            for (int i = 4; i < parts.length; i++) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append("/");
                }
                pathBuilder.append(parts[i]);
            }
            builder.append("/").append(pathBuilder);
            builder.append("?ref=").append(URLEncoder.encode(branch, StandardCharsets.UTF_8.name()));
        }

        return builder.toString();
    }

    private String readUrl(String urlValue) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlValue).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "SpoofMyDevice");
        connection.setInstanceFollowRedirects(true);
        try (InputStream inputStream = connection.getInputStream()) {
            return readFully(inputStream);
        } finally {
            connection.disconnect();
        }
    }

    private String readFully(InputStream inputStream) throws Exception {
        try (InputStream stream = inputStream;
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    private String buildSummary(JSONObject profileJson) {
        String soc = firstNonBlank(
            profileJson.optString("socModel"),
            profileJson.optString("boardPlatform")
        );
        String release = firstNonBlank(profileJson.optString("buildRelease"), "Android");
        if (soc.isEmpty()) {
            return "Android " + release;
        }
        return soc + " - Android " + release;
    }

    private String buildDefaultUserAgent(DeviceProfile profile) {
        return String.format(
            Locale.US,
            "Mozilla/5.0 (Linux; Android %s; %s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            firstNonBlank(profile.getBuildRelease(), String.valueOf(Build.VERSION.SDK_INT)),
            firstNonBlank(profile.getModel(), "Android")
        );
    }

    private boolean isTablet(DisplayMetrics metrics) {
        if (metrics == null || metrics.densityDpi <= 0) {
            return false;
        }
        int smallestWidthDp = Math.round((Math.min(metrics.widthPixels, metrics.heightPixels) * 160f) / metrics.densityDpi);
        return smallestWidthDp >= 600;
    }

    private String joinAbis(String[] abis) {
        if (abis == null || abis.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String abi : abis) {
            if (abi == null || abi.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(abi.trim());
        }
        return builder.toString();
    }

    private String readBuildField(String fieldName) {
        try {
            Object value = Build.class.getField(fieldName).get(null);
            return value instanceof String ? (String) value : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String slugify(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.US);
        return normalized.replace(" ", "_").replace("-", "_");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
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
