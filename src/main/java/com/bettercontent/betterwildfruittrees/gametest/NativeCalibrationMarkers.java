package com.bettercontent.betterwildfruittrees.gametest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Nonshipping fixture markers only; preserve original raw stdout AND use the native file appenders. */
final class NativeCalibrationMarkers {
    // Original all32 inventory: binding1 +profile1 +initial32 +sample32 +palm8 +means4 +completion1.
    private static final int MAX_RECORDS = 1 + 1 + 32 + 32 + 8 + 4 + 1;
    private static final Logger LOGGER = LogManager.getLogger("bc.fixture.better_wild_fruit_calibration");
    private static int records;
    private NativeCalibrationMarkers() {}
    static synchronized void emit(String marker) {
        if (marker == null || marker.length() > 2048 || marker.indexOf('\n') >= 0 || marker.indexOf('\r') >= 0
                || !(marker.startsWith("OPTIONAL_NATIVE_REDWOOD_BINDING ") || marker.startsWith("OPTIONAL_GROWTH_PROFILE ")
                || marker.startsWith("OPTIONAL_GROWTH_INITIAL ") || marker.startsWith("OPTIONAL_GROWTH_SAMPLE ")
                || marker.startsWith("OPTIONAL_PALM_NATIVE_BOUND ") || marker.startsWith("OPTIONAL_GROWTH_MEAN ")
                || marker.startsWith("OPTIONAL_GROWTH_DIAGNOSTIC_ONLY ")))
            throw new IllegalArgumentException("Invalid bounded calibration fixture marker");
        if (records >= MAX_RECORDS) throw new IllegalStateException("Original all32 fixture marker inventory exceeded");
        records++;
        System.out.println(marker);
        LOGGER.info(marker);
    }
}
