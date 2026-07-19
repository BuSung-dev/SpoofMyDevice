package com.devicespooflab.hooks.data;

import android.content.Context;
import android.content.Intent;

import com.devicespooflab.hooks.utils.ConfigManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AppProfileStore {

    public static final String FILE_NAME = "app_profiles.json";
    private static final int FORMAT_VERSION = 1;

    private final ConfigFileManager configFileManager;

    public AppProfileStore(ConfigFileManager configFileManager) {
        this.configFileManager = configFileManager;
    }

    public State ensureLoaded(
        Context context,
        List<DevicePreset> presets,
        String defaultProfileName
    ) throws Exception {
        File storeFile = getStoreFile(context);
        if (storeFile.exists()) {
            return load(context);
        }

        Map<String, String> globalProperties = new LinkedHashMap<>();
        String profileContent;
        File legacyConfigFile = configFileManager.getConfigFile(context);
        if (legacyConfigFile.exists()) {
            String legacyContent = readFile(legacyConfigFile);
            ConfigFileManager.LoadedConfig legacyConfig = configFileManager.loadContent(
                legacyConfigFile,
                legacyContent,
                presets
            );
            Map<String, String> profileProperties = legacyConfig.getExtraProperties();
            moveGlobalProperty(profileProperties, globalProperties, ConfigManager.KEY_APPLY_SCREEN_METRICS);
            moveGlobalProperty(profileProperties, globalProperties, ConfigManager.KEY_SAFE_MODE_PACKAGES);
            profileContent = configFileManager.render(
                legacyConfig.getProfile(),
                profileProperties,
                legacyConfig.getSelectedPresetId(),
                legacyConfig.isCustomMode()
            );
        } else {
            if (presets.isEmpty()) {
                throw new IllegalStateException("No presets available.");
            }
            DevicePreset preset = presets.get(0);
            profileContent = configFileManager.render(
                preset.getProfile(),
                new LinkedHashMap<String, String>(),
                preset.getId(),
                false
            );
        }

        Profile defaultProfile = new Profile(
            UUID.randomUUID().toString(),
            defaultProfileName,
            profileContent
        );
        List<Profile> profiles = new ArrayList<>();
        profiles.add(defaultProfile);
        State state = new State(
            profiles,
            new LinkedHashMap<String, String>(),
            globalProperties
        );
        save(context, state);
        if (legacyConfigFile.exists() && !legacyConfigFile.delete()) {
            throw new IllegalStateException("Failed to remove the migrated legacy config.");
        }
        return state;
    }

    public State load(Context context) throws Exception {
        return parse(readFile(getStoreFile(context)));
    }

    public void save(Context context, State state) throws Exception {
        if (state.getProfiles().isEmpty()) {
            throw new IllegalArgumentException("At least one profile is required.");
        }

        JSONObject root = new JSONObject();
        root.put("version", FORMAT_VERSION);

        JSONArray profiles = new JSONArray();
        for (Profile profile : state.getProfiles()) {
            JSONObject profileJson = new JSONObject();
            profileJson.put("id", profile.getId());
            profileJson.put("name", profile.getName());
            profileJson.put("content", profile.getContent());
            profiles.put(profileJson);
        }
        root.put("profiles", profiles);
        root.put("assignments", toJsonObject(state.getAssignments()));
        root.put("globalProperties", toJsonObject(state.getGlobalProperties()));

        File storeFile = getStoreFile(context);
        try (FileOutputStream outputStream = new FileOutputStream(storeFile, false)) {
            outputStream.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        context.sendBroadcast(new Intent(ConfigManager.ACTION_CONFIG_CHANGED));
    }

    public File getStoreFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    public static String resolveConfig(Context context, String packageName) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) {
            return null;
        }
        try {
            State state = parse(readFile(new File(context.getFilesDir(), FILE_NAME)));
            String profileId = state.getAssignments().get(packageName);
            if (profileId == null) {
                return null;
            }
            Profile profile = state.findProfile(profileId);
            if (profile == null) {
                return null;
            }
            StringBuilder content = new StringBuilder(profile.getContent().trim());
            if (!state.getGlobalProperties().isEmpty()) {
                content.append("\n\n# Global runtime options\n");
                for (Map.Entry<String, String> entry : state.getGlobalProperties().entrySet()) {
                    content.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
                }
            }
            return content.toString();
        } catch (Exception exception) {
            return null;
        }
    }

    private static State parse(String content) throws Exception {
        JSONObject root = new JSONObject(content);
        if (root.getInt("version") != FORMAT_VERSION) {
            throw new IllegalStateException("Unsupported app profile format.");
        }

        List<Profile> profiles = new ArrayList<>();
        JSONArray profilesJson = root.getJSONArray("profiles");
        for (int index = 0; index < profilesJson.length(); index++) {
            JSONObject profileJson = profilesJson.getJSONObject(index);
            profiles.add(
                new Profile(
                    profileJson.getString("id"),
                    profileJson.getString("name"),
                    profileJson.getString("content")
                )
            );
        }
        if (profiles.isEmpty()) {
            throw new IllegalStateException("No profiles are stored.");
        }

        return new State(
            profiles,
            toStringMap(root.getJSONObject("assignments")),
            toStringMap(root.getJSONObject("globalProperties"))
        );
    }

    private static JSONObject toJsonObject(Map<String, String> values) throws Exception {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static Map<String, String> toStringMap(JSONObject object) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        java.util.Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            result.put(key, object.getString(key));
        }
        return result;
    }

    private static String readFile(File file) throws Exception {
        if (!file.exists()) {
            throw new IllegalStateException("App profile store does not exist.");
        }
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream inputStream = new FileInputStream(file)) {
            int offset = 0;
            while (offset < bytes.length) {
                int read = inputStream.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            return new String(bytes, 0, offset, StandardCharsets.UTF_8);
        }
    }

    private static void moveGlobalProperty(
        Map<String, String> profileProperties,
        Map<String, String> globalProperties,
        String key
    ) {
        String value = profileProperties.remove(key);
        if (value != null) {
            globalProperties.put(key, value);
        }
    }

    public static final class Profile {
        private final String id;
        private final String name;
        private final String content;

        public Profile(String id, String name, String content) {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Profile id is required.");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Profile name is required.");
            }
            this.id = id;
            this.name = name.trim();
            this.content = content;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getContent() {
            return content;
        }
    }

    public static final class State {
        private final List<Profile> profiles;
        private final Map<String, String> assignments;
        private final Map<String, String> globalProperties;

        public State(
            List<Profile> profiles,
            Map<String, String> assignments,
            Map<String, String> globalProperties
        ) {
            this.profiles = new ArrayList<>(profiles);
            this.assignments = new LinkedHashMap<>(assignments);
            this.globalProperties = new LinkedHashMap<>(globalProperties);
        }

        public List<Profile> getProfiles() {
            return new ArrayList<>(profiles);
        }

        public Map<String, String> getAssignments() {
            return new LinkedHashMap<>(assignments);
        }

        public Map<String, String> getGlobalProperties() {
            return new LinkedHashMap<>(globalProperties);
        }

        public Profile findProfile(String profileId) {
            for (Profile profile : profiles) {
                if (profile.getId().equals(profileId)) {
                    return profile;
                }
            }
            return null;
        }
    }
}
