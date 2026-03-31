package com.devicespooflab.hooks.data;

import android.content.Context;

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

public class DevicePresetCatalog {

    private static final String CACHE_NAME = "remote_device_presets.json";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 12000;

    public List<DevicePreset> load(Context context) {
        List<DevicePreset> cachedPresets = loadFromCache(context);
        if (!cachedPresets.isEmpty()) {
            return cachedPresets;
        }
        return refreshRemote(context, AppSettingsStore.getPresetSourceUrl(context));
    }

    public List<DevicePreset> refreshRemote(Context context, String sourceUrl) {
        try {
            JSONArray normalizedArray = fetchRemotePresetArray(sourceUrl);
            List<DevicePreset> presets = parsePresetArray(normalizedArray);
            if (!presets.isEmpty()) {
                writeCache(context, normalizedArray);
                return presets;
            }
        } catch (Exception ignored) {
        }
        return loadFromCache(context);
    }

    private List<DevicePreset> loadFromCache(Context context) {
        File cacheFile = getCacheFile(context);
        if (!cacheFile.exists()) {
            return Collections.emptyList();
        }
        try (InputStream inputStream = new FileInputStream(cacheFile)) {
            JSONArray array = new JSONArray(readFully(inputStream));
            return parsePresetArray(array);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private void writeCache(Context context, JSONArray array) {
        try (FileOutputStream outputStream = new FileOutputStream(getCacheFile(context), false)) {
            outputStream.write(array.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private File getCacheFile(Context context) {
        return new File(context.getFilesDir(), CACHE_NAME);
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
