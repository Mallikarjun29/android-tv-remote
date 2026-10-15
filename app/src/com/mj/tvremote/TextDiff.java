package com.mj.tvremote;

/** Turns edits in the phone's text field into what must be sent to the TV: some DEL presses, then new text. */
public final class TextDiff {
    public final int deletions;
    public final String insertion;
    private TextDiff(int d, String s) { deletions = d; insertion = s; }

    public static TextDiff between(String before, String after) {
        int p = 0, max = Math.min(before.length(), after.length());
        while (p < max && before.charAt(p) == after.charAt(p)) p++;
        // do not split a surrogate pair
        if (p > 0 && p < before.length() && Character.isHighSurrogate(before.charAt(p - 1))) p--;
        return new TextDiff(before.codePointCount(p, before.length()), after.substring(p));
    }
}
