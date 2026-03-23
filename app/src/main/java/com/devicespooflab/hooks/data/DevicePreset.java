package com.devicespooflab.hooks.data;

public class DevicePreset {

    private final String id;
    private final String brandLabel;
    private final String modelLabel;
    private final String summary;
    private final DeviceProfile profile;

    public DevicePreset(String id, String brandLabel, String modelLabel, String summary, DeviceProfile profile) {
        this.id = id;
        this.brandLabel = brandLabel;
        this.modelLabel = modelLabel;
        this.summary = summary;
        this.profile = profile;
    }

    public String getId() {
        return id;
    }

    public String getBrandLabel() {
        return brandLabel;
    }

    public String getModelLabel() {
        return modelLabel;
    }

    public String getSummary() {
        return summary;
    }

    public DeviceProfile getProfile() {
        return profile.copy();
    }

    public String getDisplayName() {
        return brandLabel + " " + modelLabel;
    }
}
