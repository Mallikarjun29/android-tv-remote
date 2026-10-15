package com.mj.tvremote;

/** The six apps on the grid. Package names are best guesses and can be wrong for a given TV. */
public final class BuiltinApps {
    private BuiltinApps() {}
    public static final String[] NAMES = {"Airtel Xstream", "ZEE5", "JioHotstar", "Netflix", "Prime Video", "Apple TV+"};
    public static final String[] LOGOS = {"logo_xstream", "logo_zee5", "logo_hotstar", "logo_netflix", "logo_prime", "logo_apple"};
    public static final String[] PACKAGES = {"tv.accedo.airtel.wynk", "com.graymatrix.did", "in.startv.hotstar.dplus.tv",
            "com.netflix.ninja", "com.amazon.amazonvideo.livingroom", "com.apple.atve.androidtv.appletv"};
    public static String link(int i) { return "market://launch?id=" + PACKAGES[i]; }
}
