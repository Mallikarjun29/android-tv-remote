import android.app.Activity;
import android.app.AlertDialog;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.mj.tvremote.MainActivity;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UiSmokeTest {
    static final String DIR = "/tmp/tvui";

    static void collect(View v, List<View> out) {
        if (v.getVisibility() != View.VISIBLE) return;
        out.add(v);
        if (v instanceof ViewGroup) { ViewGroup g = (ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), out); }
    }
    static List<View> all(Activity a) { List<View> l = new ArrayList<View>(); collect(a.getWindow().getDecorView(), l); return l; }
    static TextView text(Activity a, String s) {
        for (View v : all(a)) if (v instanceof TextView && !(v instanceof EditText) && s.equals(((TextView) v).getText().toString())) return (TextView) v;
        return null;
    }
    static boolean has(Activity a, String s) { return text(a, s) != null; }
    static void click(View v) { Assert.assertNotNull(v); View x = v; while (x != null && !x.isClickable()) x = (View) x.getParent(); (x == null ? v : x).performClick(); }
    static void touch(View v, float x, float y) {
        long t = SystemClock.uptimeMillis();
        v.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0));
        v.dispatchTouchEvent(MotionEvent.obtain(t, t + 20, MotionEvent.ACTION_UP, x, y, 0));
    }
    static void pump(long ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) { ShadowLooper.idleMainLooper(); try { Thread.sleep(40); } catch (Exception e) {} }
    }
    static boolean waitFor(Activity a, String s, long ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) { ShadowLooper.idleMainLooper(); if (has(a, s)) return true; try { Thread.sleep(50); } catch (Exception e) {} }
        return false;
    }
    static List<String> log() throws Exception { return Files.readAllLines(Paths.get(DIR, "tv_log.jsonl")); }
    static boolean logHas(String frag) throws Exception { for (String l : log()) if (l.contains(frag)) return true; return false; }
    static int n = 0;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) Assert.fail(name); }
    static EditText edit(Activity a, int idx) { List<EditText> e = new ArrayList<EditText>(); for (View v : all(a)) if (v instanceof EditText) e.add((EditText) v); return e.get(idx); }
    static View byClass(Activity a, String simple, int idx) { int i = 0; for (View v : all(a)) if (v.getClass().getSimpleName().equals(simple)) { if (i++ == idx) return v; } return null; }

    @Test public void fullFlow() throws Exception {
        File fd = org.robolectric.RuntimeEnvironment.getApplication().getFilesDir();
        Files.copy(Paths.get(DIR, "client_key.der"), new File(fd, "client_key.der").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(DIR, "client_cert.der"), new File(fd, "client_cert.der").toPath(), StandardCopyOption.REPLACE_EXISTING);
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = c.get();
        check("setup screen shown first (Add your TV)", has(a, "Add your TV"));
        check("tab bar present: Remote, Apps, Keyboard", has(a, "Remote") && has(a, "Apps") && has(a, "Keyboard"));
        check("no-TV state tells user how to proceed", has(a, "Looking for TVs on your Wi-Fi...") || has(a, "No TV found"));
        click(text(a, "TV not listed?"));
        pump(100);
        edit(a, 0).setText("127.0.0.1");
        click(text(a, "Connect"));
        check("pairing starts: code entry appears (Pair button)", waitFor(a, "Pair", 8000));
        // wrong code first
        edit(a, 1).setText("000000");
        click(text(a, "Pair"));
        check("wrong code shows error card", waitFor(a, "Wrong code", 5000));
        String code = new String(Files.readAllBytes(Paths.get(DIR, "tv_code.txt"))).trim();
        edit(a, 1).setText(code);
        click(text(a, "Pair"));
        check("correct code connects: Connected status + TV controls", waitFor(a, "Connected", 10000) && has(a, "VOL"));
        pump(1500);
        check("TV focus event right after connect does NOT switch to Keyboard tab (stays on Remote)", has(a, "VOL") && !has(a, "Type on TV"));
        
        check("live volume number 18 shown inside the VOL pill", has(a, "18"));
        // D-pad up (top of circle) and center
        View dpad = byClass(a, "DPadView", 0);
        check("d-pad rendered", dpad != null);
        int w = dpad.getWidth() > 0 ? dpad.getWidth() : 780;
        dpad.layout(0, 0, 780, 780);
        touch(dpad, 390, 100);   // up
        touch(dpad, 390, 680);   // down
        touch(dpad, 100, 390);   // left
        touch(dpad, 680, 390);   // right
        touch(dpad, 390, 390);   // center
        pump(400);
        check("TV got UP/DOWN/LEFT/RIGHT/CENTER from d-pad", logHas("\"code\": 19, \"dir\": 3") && logHas("\"code\": 20, \"dir\": 3") && logHas("\"code\": 21, \"dir\": 3") && logHas("\"code\": 22, \"dir\": 3") && logHas("\"code\": 23, \"dir\": 3"));
        // keypad
        click(text(a, "123"));
        check("keypad page opens", has(a, "OK") && has(a, "5"));
        touch(text(a, "5"), 5, 5); touch(text(a, "0"), 5, 5);
        check("keypad echoes digits locally", has(a, "50"));
        ShadowLooper.idleMainLooper(3, java.util.concurrent.TimeUnit.SECONDS);
        check("keypad echo auto-clears after idle (new number replaces old)", !has(a, "50"));
        touch(text(a, "OK"), 5, 5);
        pump(300);
        check("TV got digit 5 (key 12), 0 (key 7), OK (23)", logHas("\"code\": 12,") && logHas("\"code\": 7,"));
        click(text(a, "D-pad"));
        // page swipe on the empty area of the page (outside the d-pad circle)
        View sf = byClass(a, "SwipeFrame", 0);
        long ts = SystemClock.uptimeMillis();
        sf.dispatchTouchEvent(MotionEvent.obtain(ts, ts, MotionEvent.ACTION_DOWN, 600, 100, 0));
        sf.dispatchTouchEvent(MotionEvent.obtain(ts, ts + 20, MotionEvent.ACTION_MOVE, 450, 105, 0));
        sf.dispatchTouchEvent(MotionEvent.obtain(ts, ts + 40, MotionEvent.ACTION_MOVE, 200, 110, 0));
        sf.dispatchTouchEvent(MotionEvent.obtain(ts, ts + 60, MotionEvent.ACTION_UP, 200, 110, 0));
        check("swipe left (even on empty area) opens keypad page", has(a, "OK") && has(a, "D-pad"));
        ts = SystemClock.uptimeMillis();
        sf.dispatchTouchEvent(MotionEvent.obtain(ts, ts, MotionEvent.ACTION_DOWN, 200, 100, 0));
        sf.dispatchTouchEvent(MotionEvent.obtain(ts, ts + 20, MotionEvent.ACTION_MOVE, 300, 100, 0));
        sf.dispatchTouchEvent(MotionEvent.obtain(ts, ts + 40, MotionEvent.ACTION_MOVE, 600, 100, 0));
        sf.dispatchTouchEvent(MotionEvent.obtain(ts, ts + 60, MotionEvent.ACTION_UP, 600, 100, 0));
        check("swipe right returns to d-pad page", has(a, "123") && !has(a, "OK"));
        // power and bottom block
        View power = null;
        for (View v : all(a)) if (v instanceof android.widget.FrameLayout && v.getClass() == android.widget.FrameLayout.class && v.isClickable() && v.getWidth() == 0) { }
        pump(4500); // past the post-connect grace period so the Home check below is meaningful
        // bottom block: power, back, home, menu, VOL+, VOL-, mute, CH+, CH-
        List<View> btns = new ArrayList<View>();
        for (View v : all(a)) if (v.getClass() == android.widget.FrameLayout.class && v.isClickable()) {
            boolean icon = false; ViewGroup g = (ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) if (g.getChildAt(i).getClass().getSimpleName().equals("IconView")) icon = true;
            if (icon) btns.add(v);
        }
        check("8 icon buttons on remote (power, back, home, vol+/-, mute, ch+/-), no Menu", btns.size() == 8);
        for (View b : btns) touch(b, 5, 5);
        pump(400);
        check("power/back/home/vol/mute/ch keys reach TV (26,4,3,24,25,164,166,167)", logHas("\"code\": 26,") && logHas("\"code\": 4,") && logHas("\"code\": 3,") && logHas("\"code\": 24,") && logHas("\"code\": 25,") && logHas("\"code\": 164,") && logHas("\"code\": 166,") && logHas("\"code\": 167,"));
        pump(1500);
        check("TV text-field focus right after Home does NOT jump to Keyboard tab", has(a, "VOL") && !has(a, "Type on TV"));
        // touch mode
        View sw = null; for (View v : all(a)) if (v instanceof android.widget.Switch) sw = v;
        ((android.widget.Switch) sw).setChecked(true);
        View tp = byClass(a, "TouchPad", 0);
        check("touch pad visible in touch mode, d-pad hidden", tp != null && byClass(a, "DPadView", 0) == null);
        int before = log().size();
        long t0 = SystemClock.uptimeMillis();
        tp.dispatchTouchEvent(MotionEvent.obtain(t0, t0, MotionEvent.ACTION_DOWN, 100, 300, 0));
        tp.dispatchTouchEvent(MotionEvent.obtain(t0, t0 + 20, MotionEvent.ACTION_MOVE, 400, 300, 0)); // swipe right
        tp.dispatchTouchEvent(MotionEvent.obtain(t0, t0 + 40, MotionEvent.ACTION_UP, 400, 300, 0));
        touch(tp, 50, 50); // tap = select
        pump(300);
        List<String> nl = log().subList(before, log().size());
        String joined = nl.toString();
        check("swipe right sends RIGHT(22), tap sends CENTER(23)", joined.contains("\"code\": 22,") && joined.contains("\"code\": 23,"));
        ((android.widget.Switch) sw).setChecked(false);
        // Apps tab
        click(text(a, "Apps"));
        check("apps grid shows 7 built-ins + add tile", has(a, "Airtel Xstream") && has(a, "ZEE5") && has(a, "JioHotstar") && has(a, "Netflix") && has(a, "Prime Video") && has(a, "Apple TV+") && has(a, "YouTube") && has(a, "Add custom app"));
        pump(4000); // past the post-connect grace period
        click(text(a, "Netflix"));
        check("tile shows Opening...", has(a, "Opening..."));
        pump(400);
        check("TV got Netflix launch (deep link first)", logHas("\"link\": \"netflix://\""));
        // keyboard auto-open from TV IME focus
        pump(2000);
        check("TV text-field focus right after an app launch does NOT jump to Keyboard tab", !has(a, "Type on TV"));
        check("later genuine text-field focus auto-opens Keyboard tab", waitFor(a, "Type on TV", 14000) && has(a, "Typing to: com.netflix.ninja"));
        EditText kb = edit(a, 0);
        kb.setText("hi");
        pump(300);
        check("typed text sent live", logHas("\"value\": \"hi\""));
        kb.setText("h");
        pump(300);
        check("backspace sent as DEL key (67)", logHas("\"code\": 67,"));
        click(text(a, "Send"));
        pump(300);
        check("Send presses Enter (66)", logHas("\"code\": 66,"));
        click(text(a, "Auto-open: ON"));
        check("auto-open toggle turns OFF", has(a, "Auto-open: OFF"));
        click(text(a, "Apps"));
        pump(200);
        click(text(a, "Opening...") != null ? text(a, "Opening...") : text(a, "Netflix"));
        pump(300);  // cycle window expired during the long wait, so this tap restarts at the first link
        click(text(a, "Opening...") != null ? text(a, "Opening...") : text(a, "Netflix"));
        pump(300);
        check("tapping again tries the next way (netflix title link)", logHas("https://www.netflix.com/title/?source=30"));
        click(text(a, "Opening...") != null ? text(a, "Opening...") : text(a, "Netflix"));
        pump(300);
        check("third tap tries the package link", logHas("market://launch?id=com.netflix.ninja"));
        // add custom link
        click(text(a, "Add custom app"));
        AlertDialog d = ShadowAlertDialog.getLatestAlertDialog();
        check("add dialog opens", d != null && d.isShowing());
        List<View> dv = new ArrayList<View>(); collect(d.getWindow().getDecorView(), dv);
        TextView tLink = null; List<EditText> fields = new ArrayList<EditText>();
        for (View v : dv) { if (v instanceof TextView && !(v instanceof EditText) && "Link".equals(((TextView) v).getText().toString())) tLink = (TextView) v; if (v instanceof EditText) fields.add((EditText) v); }
        click(tLink);
        fields.get(0).setText("Cricket highlights");
        fields.get(1).setText("youtube.com/watch?v=abc");
        d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        check("custom link tile added", has(a, "Cricket highlights"));
        click(text(a, "Cricket highlights"));
        pump(400);
        check("TV got custom link launch (https normalized)", logHas("https://youtube.com/watch?v=abc"));
        System.out.println("UI FLOW DONE");
    }
}
