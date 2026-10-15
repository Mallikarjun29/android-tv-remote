package com.mj.tvremote;

import java.util.ArrayList;
import java.util.List;

/** Favourite channels (name + channel number) shown on the keypad page. */
public final class FavStore {
    public static final class Fav {
        public final String name, number;
        public Fav(String name, String number) { this.name = name; this.number = number; }
    }

    public static final int MAX_NAME = 16, MAX_DIGITS = 5, MAX_FAVS = 30;

    public static String cleanName(String s) {
        if (s == null) return "";
        String t = s.replace('\t', ' ').replace('\n', ' ').trim();
        return t.length() > MAX_NAME ? t.substring(0, MAX_NAME).trim() : t;
    }

    public static boolean validNumber(String s) { return s != null && s.matches("[0-9]{1," + MAX_DIGITS + "}"); }

    public static String serialize(List<Fav> list) {
        StringBuilder b = new StringBuilder();
        for (Fav f : list) b.append(cleanName(f.name)).append('\t').append(f.number).append('\n');
        return b.toString();
    }

    public static List<Fav> parse(String s) {
        List<Fav> out = new ArrayList<Fav>();
        if (s == null) return out;
        for (String line : s.split("\n")) {
            String[] p = line.split("\t", -1);
            if (p.length != 2 || cleanName(p[0]).isEmpty() || !validNumber(p[1])) continue;
            out.add(new Fav(cleanName(p[0]), p[1]));
            if (out.size() >= MAX_FAVS) break;
        }
        return out;
    }

    /** Key codes sent when a favourite is tapped: its digits, then OK (centre). */
    public static int[] keySequence(String number) {
        int[] k = new int[number.length() + 1];
        for (int i = 0; i < number.length(); i++) k[i] = Keys.digit(number.charAt(i) - '0');
        k[number.length()] = Keys.CENTER;
        return k;
    }
}
