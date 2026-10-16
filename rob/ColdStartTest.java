import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import com.mj.tvremote.MainActivity;

/** Process-death case: app starts cold with a saved, already-paired TV. It must reconnect without asking for a new code. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ColdStartTest {
    static final String DIR = "/tmp/tvui";
    static void collect(View v, List<View> out) {
        if (v.getVisibility() != View.VISIBLE) return;
        out.add(v);
        if (v instanceof ViewGroup) { ViewGroup g = (ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), out); }
    }
    static boolean has(Activity a, String s) {
        List<View> l = new ArrayList<View>(); collect(a.getWindow().getDecorView(), l);
        for (View v : l) if (v instanceof TextView && s.equals(((TextView) v).getText().toString())) return true;
        return false;
    }
    static boolean waitFor(Activity a, String s, long ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) { ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS); if (has(a, s)) return true; try { Thread.sleep(50); } catch (Exception e) {} }
        return false;
    }
    static boolean logHas(String frag) throws Exception { for (String l : Files.readAllLines(Paths.get(DIR, "tv_log.jsonl"))) if (l.contains(frag)) return true; return false; }
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) Assert.fail(name); }

    @Test public void coldStartReconnectsWithoutPairing() throws Exception {
        File fd = org.robolectric.RuntimeEnvironment.getApplication().getFilesDir();
        Files.copy(Paths.get(DIR, "client_key.der"), new File(fd, "client_key.der").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(DIR, "client_cert.der"), new File(fd, "client_cert.der").toPath(), StandardCopyOption.REPLACE_EXISTING);
        org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("tvremote", 0).edit().putString("host", "127.0.0.1").putString("tvname", "Mock TV").commit();
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = c.get();
        boolean ok = waitFor(a, "VOL", 40000);
        check("saved paired TV reconnects on cold start (even if the first connection attempts are dropped)", ok);
        check("no new pairing was started (no code prompt, TV never saw a pairing connection)", !logHas("pair_conn") && !has(a, "Pair"));
        check("TV saw the dropped attempts we configured", System.getenv("DROP_FIRST") == null || logHas("remote_dropped"));
    }
}
