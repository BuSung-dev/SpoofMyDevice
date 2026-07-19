package com.devicespooflab.hooks.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeviceProfileTest {

    @Test
    public void presetMatchingIncludesDisplayAndRegionFields() {
        DeviceProfile preset = new DeviceProfile();
        preset.setBrand("google");
        preset.setManufacturer("Google");
        preset.setModel("Pixel 9 Pro");
        preset.setBuildId("AP4A.250205.002");
        preset.setBuildDisplayId("AP4A.250205.002 release-keys");
        preset.setTimezone("America/Los_Angeles");
        preset.setLocale("en-US");
        preset.applyFallbacks();

        DeviceProfile edited = preset.copy();
        assertTrue(edited.matchesPreset(preset));

        edited.setBuildDisplayId("AP4A.250205.002.custom");
        assertFalse(edited.matchesPreset(preset));

        edited = preset.copy();
        edited.setTimezone("Asia/Seoul");
        assertFalse(edited.matchesPreset(preset));

        edited = preset.copy();
        edited.setLocale("ko-KR");
        assertFalse(edited.matchesPreset(preset));
    }
}
