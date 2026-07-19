package com.devicespooflab.hooks.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RandomGeneratorTest {

    @Test
    public void advancedIdentifiersUseValidFormats() {
        String imei = RandomGenerator.generateIMEI();
        String iccid = RandomGenerator.generateICCID();

        assertTrue(imei.matches("\\d{15}"));
        assertTrue(isValidLuhn(imei));
        assertTrue(iccid.matches("\\d{19}"));
        assertTrue(isValidLuhn(iccid));
        assertTrue(RandomGenerator.generateMEID().matches("[0-9A-F]{14}"));
        assertTrue(RandomGenerator.generateIMSI().matches("\\d{15}"));
        assertTrue(RandomGenerator.generatePhoneNumber().matches("\\+1\\d{10}"));
        assertTrue(RandomGenerator.generateGAID().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertTrue(RandomGenerator.generateGSFId().matches("[0-9a-f]{16}"));
        assertTrue(RandomGenerator.generateAndroidId().matches("[0-9a-f]{16}"));
        assertEquals(32, RandomGenerator.generateMediaDrmId().length);
    }

    private boolean isValidLuhn(String value) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int index = value.length() - 1; index >= 0; index--) {
            int digit = value.charAt(index) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
