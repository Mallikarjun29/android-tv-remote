import android.content.Context;
import java.io.File;
import java.nio.file.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import com.mj.tvremote.TvSession;

/** TV identity check: the first successful connection pins the TV's certificate; a different TV at the same address is refused,
 *  and tapping Connect afterwards goes to pairing (code on the real TV's screen). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PinTest {
    static final String DIR = "/tmp/tvui";
    static boolean waitState(TvSession s, TvSession.State st, long ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) { ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS); if (s.state == st) return true; try { Thread.sleep(50); } catch (Exception e) {} }
        return false;
    }
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) Assert.fail(name); }
    static void sh(String c) throws Exception { new ProcessBuilder("bash", "-c", c).inheritIO().start().waitFor(); }

    @Test public void differentTvIsRefused() throws Exception {
        Context ctx = org.robolectric.RuntimeEnvironment.getApplication();
        File fd = ctx.getFilesDir();
        Files.copy(Paths.get(DIR, "client_key.der"), new File(fd, "client_key.der").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(DIR, "client_cert.der"), new File(fd, "client_cert.der").toPath(), StandardCopyOption.REPLACE_EXISTING);
        ctx.getSharedPreferences("tvremote", 0).edit().putString("client_name", "TV Remote - Test abcd").putString("host", "127.0.0.1").putString("tvname", "Mock TV").commit();
        TvSession s = new TvSession(ctx);
        s.connect("127.0.0.1", "Mock TV");
        check("first connection works", waitState(s, TvSession.State.CONNECTED, 20000));
        check("TV certificate remembered after first connection", ctx.getSharedPreferences("tvremote", 0).getString("tv_pin", "").length() == 64);
        s.disconnect();
        s.connect("127.0.0.1", "Mock TV");
        check("same TV reconnects with the pin in place", waitState(s, TvSession.State.CONNECTED, 20000));
        s.disconnect();

        // a different TV (new certificate) now answers at the same address, and it would accept our client certificate
        sh("pkill -f '[m]ock_tv'; sleep 1; rm -f " + DIR + "/tv_ready " + DIR + "/tv_log.jsonl");
        new ProcessBuilder("bash", "-c", "cd /home/sandbox/android && MOCK_PAIRED=1 exec python3 test/mock/mock_tv.py " + DIR + " real >/dev/null 2>&1").start();
        for (int i = 0; i < 50; i++) { if (new File(DIR, "tv_ready").exists()) break; Thread.sleep(200); }
        Thread.sleep(1500);
        s.connect("127.0.0.1", "Mock TV");
        boolean w = waitState(s, TvSession.State.IDLE, 20000); System.out.println("state=" + s.state + " err=" + s.error);
        check("different TV is refused with the warning", w && s.error != null && s.error.startsWith("This TV's identity changed"));
        check("nothing was sent to the other TV", !logHas("remote_conn"));
        s.connect("127.0.0.1", "Mock TV");
        check("tapping Connect after the warning starts pairing", waitState(s, TvSession.State.NEED_CODE, 20000) && logHas("pair_conn"));
    }
    static boolean logHas(String frag) throws Exception { for (String l : Files.readAllLines(Paths.get(DIR, "tv_log.jsonl"))) if (l.contains(frag)) return true; return false; }
}
