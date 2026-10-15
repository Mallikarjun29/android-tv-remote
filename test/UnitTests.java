import com.mj.tvremote.*;
import java.util.*;

public class UnitTests {
    static int fails = 0;
    static void eq(String n, Object a, Object b) { boolean ok = Objects.equals(a, b); System.out.println((ok ? "PASS " : "FAIL ") + n + (ok ? "" : "  got=" + a + " want=" + b)); if (!ok) fails++; }
    public static void main(String[] x) {
        TextDiff d = TextDiff.between("", "a"); eq("append first char", d.deletions + "|" + d.insertion, "0|a");
        d = TextDiff.between("abc", "abcd"); eq("append", d.deletions + "|" + d.insertion, "0|d");
        d = TextDiff.between("abc", "ab"); eq("backspace", d.deletions + "|" + d.insertion, "1|");
        d = TextDiff.between("abc", "axyz"); eq("replace tail", d.deletions + "|" + d.insertion, "2|xyz");
        d = TextDiff.between("hello", ""); eq("clear all", d.deletions + "|" + d.insertion, "5|");
        d = TextDiff.between("abc", "abc"); eq("no change", d.deletions + "|" + d.insertion, "0|");
        d = TextDiff.between("a\uD83D\uDE00", "a"); eq("emoji deletes as one key", d.deletions + "|" + d.insertion, "1|");
        d = TextDiff.between("a\uD83D\uDE00", "a\uD83D\uDE01"); eq("emoji swap", d.deletions + "|" + d.insertion, "1|\uD83D\uDE01");
        d = TextDiff.between("ab", "a\uD83D\uDE00"); eq("insert emoji", d.deletions + "|" + d.insertion, "1|\uD83D\uDE00");

        eq("link https kept", AppStore.normalizeLink(" https://youtu.be/x "), "https://youtu.be/x");
        eq("link bare domain gets https", AppStore.normalizeLink("youtube.com/tv"), "https://youtube.com/tv");
        eq("link custom scheme kept", AppStore.normalizeLink("vnd.youtube://abc"), "vnd.youtube://abc");
        eq("link with space rejected", AppStore.normalizeLink("not a link"), null);
        eq("link word rejected", AppStore.normalizeLink("hello"), null);
        eq("link empty rejected", AppStore.normalizeLink("  "), null);
        eq("pkg ok", AppStore.validPackage("com.netflix.ninja"), true);
        eq("pkg single word bad", AppStore.validPackage("netflix"), false);
        eq("pkg with space bad", AppStore.validPackage("com.a b"), false);

        List<AppStore.Entry> l = new ArrayList<AppStore.Entry>();
        l.add(new AppStore.Entry("Tab\tName\nX", "com.x.y", false));
        l.add(new AppStore.Entry("Back\\slash", "https://a.b/c?d=1", true));
        List<AppStore.Entry> r = AppStore.parse(AppStore.serialize(l));
        eq("roundtrip size", r.size(), 2);
        eq("roundtrip name escapes", r.get(0).name, "Tab\tName\nX");
        eq("roundtrip pkg", r.get(0).link + r.get(0).isLink, "com.x.yfalse");
        eq("roundtrip backslash", r.get(1).name, "Back\\slash");
        eq("package launch link", r.get(0).launchLink(), "market://launch?id=com.x.y");
        eq("url launch link", r.get(1).launchLink(), "https://a.b/c?d=1");
        eq("parse garbage ignored", AppStore.parse("junk\n\nA\t\t\n").size(), 0);
        eq("parse null", AppStore.parse(null).size(), 0);
        eq("builtin count consistent", BuiltinApps.NAMES.length + "," + BuiltinApps.LOGOS.length + "," + BuiltinApps.PACKAGES.length, "6,6,6");
        eq("digit keys", Keys.digit(0) + "," + Keys.digit(9), "7,16");
        System.out.println(fails == 0 ? "ALL PASS" : "FAILURES " + fails);
        System.exit(fails == 0 ? 0 : 1);
    }
}
