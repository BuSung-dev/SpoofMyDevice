package com.devicespooflab.hooks.utils;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Generates valid random device identifiers with proper checksums and formats.
 */
public class RandomGenerator {

    private static final SecureRandom random = new SecureRandom();

    public static String generateIMEI() {
        String tac = "35847631"; // Generic Google TAC

        StringBuilder serial = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            serial.append(random.nextInt(10));
        }

        String imeiWithoutCheck = tac + serial.toString();
        int checkDigit = calculateLuhnCheckDigit(imeiWithoutCheck);

        return imeiWithoutCheck + checkDigit;
    }

    public static String generateMEID() {
        StringBuilder meid = new StringBuilder();
        String hexChars = "0123456789ABCDEF";
        for (int i = 0; i < 14; i++) {
            meid.append(hexChars.charAt(random.nextInt(16)));
        }
        return meid.toString();
    }

    public static String generateIMSI() {
        String mcc = "310";
        String mnc = "260";

        StringBuilder msin = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            msin.append(random.nextInt(10));
        }

        return mcc + mnc + msin.toString();
    }

    public static String generateICCID() {
        String prefix = "8901";
        String issuer = "260";

        StringBuilder account = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            account.append(random.nextInt(10));
        }

        String iccidWithoutCheck = prefix + issuer + account.toString();
        int checkDigit = calculateLuhnCheckDigit(iccidWithoutCheck);

        return iccidWithoutCheck + checkDigit;
    }

    public static String generatePhoneNumber() {
        int areaCode = 200 + random.nextInt(800);
        int exchange = 200 + random.nextInt(800);
        int subscriber = random.nextInt(10000);

        return String.format("+1%03d%03d%04d", areaCode, exchange, subscriber);
    }

    public static String generateSerial() {
        String chars = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // No I or O to avoid confusion
        StringBuilder serial = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            serial.append(chars.charAt(random.nextInt(chars.length())));
        }
        return serial.toString();
    }

    public static String generateGAID() {
        return UUID.randomUUID().toString();
    }

    public static byte[] generateMediaDrmId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return bytes;
    }

    public static String generateGSFId() {
        StringBuilder gsf = new StringBuilder();
        String hexChars = "0123456789abcdef";
        for (int i = 0; i < 16; i++) {
            gsf.append(hexChars.charAt(random.nextInt(16)));
        }
        return gsf.toString();
    }

    public static String generateAndroidId() {
        StringBuilder androidId = new StringBuilder();
        String hexChars = "0123456789abcdef";
        for (int i = 0; i < 16; i++) {
            androidId.append(hexChars.charAt(random.nextInt(16)));
        }
        return androidId.toString();
    }

    /**
     * Generate Build.FINGERPRINT in format:
     * brand/product/device:version/build_id/incremental:type/keys
     */
    public static String generateFingerprint() {
        String buildId = generateBuildId();
        String incremental = generateIncremental();
        return String.format(
            "google/cheetah/cheetah:15/%s/%s:user/release-keys",
            buildId,
            incremental
        );
    }

    /**
     * Generate Build ID (e.g., AP4A.241205.013)
     * Format: [A-Z]{2}[0-9]{1,2}[A-Z]\.YYMMDD\.XXX
     */
    public static String generateBuildId() {
        String prefix = "AP4A"; // Android 15 prefix
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
        String date = dateFormat.format(new Date());
        int build = random.nextInt(999) + 1;
        return String.format("%s.%s.%03d", prefix, date, build);
    }

    /**
     * Generate incremental build number (e.g., 12621605)
     * 8-digit number
     */
    public static String generateIncremental() {
        return String.format("%08d", random.nextInt(100000000));
    }

    /**
     * Generate bootloader version (e.g., cheetah-1.2-A1B2C3D4)
     */
    public static String generateBootloader() {
        StringBuilder hex = new StringBuilder();
        String hexChars = "0123456789ABCDEF";
        for (int i = 0; i < 8; i++) {
            hex.append(hexChars.charAt(random.nextInt(16)));
        }
        return "cheetah-1.2-" + hex.toString();
    }

    /**
     * Generate security patch date (YYYY-MM-DD)
     * Returns a recent date within last 90 days
     */
    public static String generateSecurityPatch() {
        Calendar cal = Calendar.getInstance();
        // Random day within last 90 days
        int daysAgo = random.nextInt(90);
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return dateFormat.format(cal.getTime());
    }

    /**
     * Generate random hex string of specified length
     */
    public static String generateHex(int length) {
        StringBuilder hex = new StringBuilder();
        String hexChars = "0123456789abcdef";
        for (int i = 0; i < length; i++) {
            hex.append(hexChars.charAt(random.nextInt(16)));
        }
        return hex.toString();
    }

    private static int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }
}
