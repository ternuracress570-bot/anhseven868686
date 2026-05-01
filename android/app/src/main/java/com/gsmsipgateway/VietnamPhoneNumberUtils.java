package com.gsmsipgateway;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for normalizing Vietnam mobile phone numbers before GSM dial.
 */
public final class VietnamPhoneNumberUtils {
    private static final Pattern PHONE_LIKE_PATTERN = Pattern.compile("(\\+?\\d[\\d\\s().-]{7,})");

    // 3-digit prefixes for current 10-digit Vietnam mobile numbers.
    private static final Set<String> MOBILE_PREFIXES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "032", "033", "034", "035", "036", "037", "038", "039", "086", "096", "097", "098",
        "070", "076", "077", "078", "079", "089", "090", "093",
        "081", "082", "083", "084", "085", "088", "091", "094",
        "052", "056", "058", "092",
        "059", "099", "087"
    )));

    // Legacy 11-digit prefixes -> current 10-digit prefix.
    private static final Map<String, String> LEGACY_PREFIX_MAP;
    static {
        Map<String, String> m = new HashMap<>();

        // Viettel
        m.put("0162", "032");
        m.put("0163", "033");
        m.put("0164", "034");
        m.put("0165", "035");
        m.put("0166", "036");
        m.put("0167", "037");
        m.put("0168", "038");
        m.put("0169", "039");

        // MobiFone
        m.put("0120", "070");
        m.put("0121", "079");
        m.put("0122", "077");
        m.put("0126", "076");
        m.put("0128", "078");

        // VinaPhone
        m.put("0123", "083");
        m.put("0124", "084");
        m.put("0125", "085");
        m.put("0127", "081");
        m.put("0129", "082");

        // Vietnamobile
        m.put("0186", "056");
        m.put("0188", "058");

        // Gmobile
        m.put("0199", "059");

        LEGACY_PREFIX_MAP = Collections.unmodifiableMap(m);
    }

    private VietnamPhoneNumberUtils() {
    }

    public static String normalizeVietnamMobile(String raw) {
        if (raw == null) return "";
        String input = raw.trim();
        if (input.isEmpty()) return "";

        Matcher m = PHONE_LIKE_PATTERN.matcher(input);
        if (m.find()) {
            input = m.group(1);
        }

        String clean = input.replaceAll("[^0-9+]", "");
        if (clean.isEmpty()) return "";

        if (clean.startsWith("+84") && clean.length() > 3) {
            clean = "0" + clean.substring(3);
        } else if (clean.startsWith("84") && clean.length() > 9) {
            clean = "0" + clean.substring(2);
        }

        if (!clean.startsWith("0") && clean.matches("[35789]\\d{8,9}")) {
            clean = "0" + clean;
        }

        // Convert legacy 11-digit mobile numbers (01xx...) to current 10-digit.
        if (clean.length() == 11 && clean.startsWith("01")) {
            String legacyPrefix = clean.substring(0, 4);
            String mapped = LEGACY_PREFIX_MAP.get(legacyPrefix);
            if (mapped != null) {
                clean = mapped + clean.substring(4);
            }
        }

        if (clean.length() != 10 || !clean.startsWith("0")) {
            return "";
        }

        String prefix = clean.substring(0, 3);
        return MOBILE_PREFIXES.contains(prefix) ? clean : "";
    }
}