import com.mj.tvremote.*;
import java.util.*;

public class UnitTests {
    static int fails = 0;
    static void eq(String n, Object a, Object b) { boolean ok = Objects.equals(a, b); System.out.println((ok ? "PASS " : "FAIL ") + n + (ok ? "" : "  got=" + a + " want=" + b)); if (!ok) fails++; }
    public static void main(String[] x) {
        {
            java.util.Random r = new java.util.Random(5);
            java.util.Set<String> seen = new java.util.HashSet<String>();
            for (int i = 0; i < 200; i++) seen.add(com.mj.tvremote.ClientName.make("Pixel 7", r));
            eq("client names differ per install", seen.size() > 195 ? "ok" : "dupes " + seen.size(), "ok");
            eq("name has model", com.mj.tvremote.ClientName.make("Pixel 7", new java.util.Random(1)).startsWith("TV Remote - Pixel 7 ") ? "ok" : "bad", "ok");
            eq("name strips junk", com.mj.tvremote.ClientName.make("A\"B<>", new java.util.Random(1)).matches("TV Remote - AB [0-9a-f]{4}") ? "ok" : "bad", "ok");
            eq("empty model", com.mj.tvremote.ClientName.make(null, new java.util.Random(1)).matches("TV Remote [0-9a-f]{4}") ? "ok" : "bad", "ok");
        }
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
        eq("builtin count consistent", BuiltinApps.NAMES.length + "," + BuiltinApps.LOGOS.length + "," + BuiltinApps.PACKAGES.length, "7,7,7");
        eq("netflix tries deep link first", BuiltinApps.candidates(3, null)[0] + "|" + BuiltinApps.candidates(3, null).length, "netflix://|4");
        eq("netflix package link is in the list", Arrays.asList(BuiltinApps.candidates(3, null)).contains("market://launch?id=com.netflix.ninja"), true);
        eq("youtube first link", BuiltinApps.candidates(6, null)[0], "https://www.youtube.com");
        eq("xstream tries both package names then store page", Arrays.toString(BuiltinApps.candidates(0, null)), "[market://launch?id=com.airtel.tv, market://launch?id=tv.airtel.xstream.tvapp, market://details?id=com.airtel.tv]");
        eq("edited package overrides all", Arrays.toString(BuiltinApps.candidates(3, "com.x.y")), "[market://launch?id=com.x.y, market://details?id=com.x.y]");
        eq("hotstar has package fallbacks", BuiltinApps.candidates(2, null).length, 4);
        eq("fav roundtrip", FavStore.serialize(FavStore.parse("Star Plus\t101\nZee TV\t102\n")), "Star Plus\t101\nZee TV\t102\n");
        eq("fav parse drops bad rows", FavStore.parse("\t101\nColors\tabc\nSony\t104\nbroken\n").size(), 1);
        eq("fav name cleaned and capped", FavStore.cleanName("  A very long channel name indeed\t"), "A very long chan");
        eq("fav number validation", FavStore.validNumber("101") + "," + FavStore.validNumber("") + "," + FavStore.validNumber("12a") + "," + FavStore.validNumber("123456"), "true,false,false,false");
        eq("fav tap sends digits then OK", Arrays.toString(FavStore.keySequence("101")), "[8, 7, 8, 23]");
        eq("digit keys", Keys.digit(0) + "," + Keys.digit(9), "7,16");
        System.out.println(fails == 0 ? "ALL PASS" : "FAILURES " + fails);
        System.exit(fails == 0 ? 0 : 1);
    }
}
