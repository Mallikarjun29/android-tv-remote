package com.mj.tvremote;

/**
 * The seven apps on the grid. Each has a list of ways to open it: deep links (documented as working for Home Assistant's
 * Android TV Remote integration) and the Play Store style "market://launch?id=" package link. Tapping a tile twice within
 * a few seconds tries the next way.
 */
public final class BuiltinApps {
    private BuiltinApps() {}
    public static final String[] NAMES = {"Airtel Xstream", "ZEE5", "JioHotstar", "Netflix", "Prime Video", "Apple TV+", "YouTube"};
    public static final String[] LOGOS = {"logo_xstream", "logo_zee5", "logo_hotstar", "logo_netflix", "logo_prime", "logo_apple", "logo_youtube"};
    /** Android TV package names (checked against Play Store / APK listings). Editable in the app. */
    public static final String[] PACKAGES = {"tv.airtel.xstream.tvapp", "com.graymatrix.did", "in.startv.hotstartvonly",
            "com.netflix.ninja", "com.amazon.amazonvideo.livingroom", "com.apple.atve.androidtv.appletv", "com.google.android.youtube.tv"};
    private static final String[][] DEEP = {
            {},
            {},
            {"market://launch?id=in.startv.hotstar", "hotstar://"},
            {"netflix://", "https://www.netflix.com/title/?source=30"},
            {"https://app.primevideo.com"},
            {"https://tv.apple.com"},
            {"https://www.youtube.com", "vnd.youtube://"}};
    /** Deep links first where known to work, then the package link. If the user edited the package, only that is used. */
    public static String[] candidates(int i, String pkgOverride) {
        String pkg = pkgOverride == null ? PACKAGES[i] : pkgOverride;
        String market = "market://launch?id=" + pkg;
        if (pkgOverride != null) return new String[]{market};
        String[] d = DEEP[i];
        String[] out = new String[d.length + 1];
        // deep links that are safe to try first (the documented ones), then the package link, then the rest
        int n = 0;
        for (String s : d) if (!s.startsWith("market://")) out[n++] = s;
        out[n++] = market;
        for (String s : d) if (s.startsWith("market://")) out[n++] = s;
        return out;
    }
}
