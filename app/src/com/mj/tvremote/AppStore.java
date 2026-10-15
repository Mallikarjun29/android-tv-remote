package com.mj.tvremote;

import java.util.ArrayList;
import java.util.List;

/** Custom apps / links shown in the Apps grid, and their text serialization for SharedPreferences. */
public final class AppStore {
    public static final class Entry {
        public final String name, link;
        public final boolean isLink; // true: a web link; false: an Android package name
        public Entry(String name, String link, boolean isLink) { this.name = name; this.link = link; this.isLink = isLink; }
        /** What is sent to the TV. */
        public String launchLink() { return isLink ? link : "market://launch?id=" + link; }
    }

    /** Normalizes user input; returns null if it is not usable. */
    public static String normalizeLink(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.indexOf(' ') >= 0) return null;
        if (!s.matches("^[A-Za-z][A-Za-z0-9+.-]*://.+")) {
            if (!s.contains(".")) return null;
            s = "https://" + s;
        }
        return s;
    }

    public static boolean validPackage(String s) {
        return s != null && s.trim().matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+");
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    private static String unesc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                b.append(n == 't' ? '\t' : n == 'n' ? '\n' : n);
            } else b.append(c);
        }
        return b.toString();
    }

    public static String serialize(List<Entry> list) {
        StringBuilder b = new StringBuilder();
        for (Entry e : list) b.append(e.isLink ? "L" : "A").append('\t').append(esc(e.name)).append('\t').append(esc(e.link)).append('\n');
        return b.toString();
    }

    public static List<Entry> parse(String s) {
        List<Entry> out = new ArrayList<Entry>();
        if (s == null) return out;
        for (String line : s.split("\n")) {
            String[] p = line.split("\t", -1);
            if (p.length != 3 || p[1].isEmpty() || p[2].isEmpty()) continue;
            out.add(new Entry(unesc(p[1]), unesc(p[2]), p[0].equals("L")));
        }
        return out;
    }
}
