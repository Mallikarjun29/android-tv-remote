package com.mj.tvremote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.mj.tvremote.proto.Identity;
import com.mj.tvremote.proto.PairingClient;
import com.mj.tvremote.proto.RemoteClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Connection state and the connect / pair / reconnect flow. All callbacks run on the main thread. */
public final class TvSession {
    public enum State { IDLE, CONNECTING, NEED_CODE, PAIRING, CONNECTED }

    public interface Listener {
        void onStateChanged();
        void onVolume();
        void onImeFocus(String pkg);
        void onTvError(String message);
    }

    public static final String CLIENT_NAME = "TV Remote";
    private final Context ctx;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newCachedThreadPool();
    private Listener listener;
    private Identity id;
    private RemoteClient remote;
    private PairingClient pairing;
    private int gen; // bumps on every new attempt so stale callbacks are ignored

    /** The TV can report a focused text field right after connecting; do not jump to the keyboard for that. */
    public static final long IME_GRACE_MS = 4000;
    public volatile long connectedAt;
    /** Home/Back make the TV launcher focus a text field; do not treat that as the user wanting the keyboard. */
    public static final long KEY_QUIET_MS = 6000, LAUNCH_QUIET_MS = 10000;
    public volatile long quietUntil;
    public volatile State state = State.IDLE;
    public String host, tvName, error;
    public int volume = -1, volumeMax = 100;
    public boolean muted;
    public boolean imeActive;
    public String imePkg;

    public TvSession(Context c) {
        ctx = c.getApplicationContext();
        prefs = ctx.getSharedPreferences("tvremote", Context.MODE_PRIVATE);
        host = prefs.getString("host", null);
        tvName = prefs.getString("tvname", null);
    }

    public void setListener(Listener l) { listener = l; }
    public SharedPreferences prefs() { return prefs; }
    public boolean hasSavedTv() { return host != null; }

    private Identity identity() throws Exception {
        if (id != null) return id;
        File k = new File(ctx.getFilesDir(), "client_key.der"), c = new File(ctx.getFilesDir(), "client_cert.der");
        if (k.exists() && c.exists()) {
            try { id = Identity.load(read(k), read(c)); return id; } catch (Exception ignored) {}
        }
        id = Identity.generate("tvremote");
        write(k, id.keyBytes());
        write(c, id.certBytes());
        return id;
    }

    private static byte[] read(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[4096]; int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
            return o.toByteArray();
        } finally { in.close(); }
    }

    private static void write(File f, byte[] d) throws IOException {
        OutputStream o = new FileOutputStream(f);
        try { o.write(d); } finally { o.close(); }
    }

    private void set(final State s, final String err) {
        main.post(new Runnable() { public void run() {
            state = s; error = err;
            if (s != State.CONNECTED) imeActive = false;
            if (listener != null) listener.onStateChanged();
        } });
    }

    /** Connect to a TV: use the existing pairing if the TV accepts us, otherwise start pairing. */
    public void connect(final String ip, final String name) {
        close();
        final int g = ++gen;
        host = ip; tvName = name;
        set(State.CONNECTING, null);
        bg.execute(new Runnable() { public void run() {
            try {
                Identity i = identity();
                if (tryRemote(i, ip, g, 3000)) return;
                if (g != gen) return;
                PairingClient p = new PairingClient(i, ip, 6467);
                p.start(CLIENT_NAME);
                if (g != gen) { p.close(); return; }
                pairing = p;
                set(State.NEED_CODE, null);
            } catch (Exception e) {
                if (g == gen) set(State.IDLE, "Couldn't reach the TV at " + ip + ". Check it is on, and on the same Wi-Fi as this phone.");
            }
        } });
    }

    /** Finish pairing with the 6-character code from the TV, then open the remote channel. */
    public void submitCode(final String code) {
        final PairingClient p = pairing;
        if (p == null) return;
        final int g = gen;
        set(State.PAIRING, null);
        bg.execute(new Runnable() { public void run() {
            try {
                p.finish(code);
                p.close(); pairing = null;
                Identity i = identity();
                for (int n = 0; n < 4 && g == gen; n++) {
                    if (tryRemote(i, host, g, 4000)) return;
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                }
                if (g == gen) set(State.IDLE, "Paired, but the TV didn't open the remote channel. Try again.");
            } catch (Identity.BadCodeException e) {
                if (g == gen) set(State.NEED_CODE, "Wrong code. Check the code on the TV and try again.");
            } catch (Exception e) {
                p.close(); pairing = null;
                if (g == gen) set(State.IDLE, "Pairing didn't finish. Start again to get a new code on the TV.");
            }
        } });
    }

    private boolean tryRemote(Identity i, final String ip, final int g, int timeoutMs) {
        RemoteClient rc = new RemoteClient(i, new RemoteClient.Listener() {
            public void onReady() {}
            public void onVolume(final int level, final int max, final boolean mu) {
                main.post(new Runnable() { public void run() {
                    if (g != gen) return;
                    volume = level; volumeMax = max; muted = mu;
                    if (listener != null) listener.onVolume();
                } });
            }
            public void onImeFocus(final String pkg) {
                main.post(new Runnable() { public void run() {
                    if (g != gen) return;
                    imeActive = true; imePkg = pkg;
                    if (listener != null) listener.onImeFocus(pkg);
                } });
            }
            public void onTvError(final String m) {
                main.post(new Runnable() { public void run() { if (g == gen && listener != null) listener.onTvError(m); } });
            }
            public void onClosed(Exception e) { lost(g); }
        });
        try {
            rc.connect(ip, 6466, timeoutMs);
        } catch (Exception e) {
            rc.close();
            return false;
        }
        if (g != gen) { rc.close(); return true; }
        remote = rc;
        prefs.edit().putString("host", ip).putString("tvname", tvName == null ? ip : tvName).apply();
        connectedAt = System.currentTimeMillis();
        set(State.CONNECTED, null);
        return true;
    }

    private void lost(final int g) {
        main.post(new Runnable() { public void run() {
            if (g != gen || state != State.CONNECTED) return;
            reconnect(0);
        } });
    }

    private void reconnect(final int attempt) {
        final int g = gen;
        set(State.CONNECTING, null);
        bg.execute(new Runnable() { public void run() {
            try {
                if (g != gen) return;
                if (tryRemote(identity(), host, g, 4000)) return;
            } catch (Exception ignored) {}
            if (g != gen) return;
            if (attempt >= 4) { set(State.IDLE, "Lost connection to the TV. Tap Connect to reconnect."); return; }
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            if (g == gen) reconnect(attempt + 1);
        } });
    }

    /** Called when the app returns to the foreground. */
    public void resume() {
        if (state == State.CONNECTED && remote != null && remote.isOpen()) return;
        if (state == State.IDLE && host != null && error == null) connect(host, tvName);
        else if (state == State.CONNECTED) reconnect(0);
    }

    public void close() {
        gen++;
        if (remote != null) { remote.close(); remote = null; }
        if (pairing != null) { pairing.close(); pairing = null; }
    }

    public void disconnect() { close(); state = State.IDLE; error = null; if (listener != null) listener.onStateChanged(); }

    public volatile int cmdSeq;
    private void quiet(long ms) { cmdSeq++; long u = System.currentTimeMillis() + ms; if (u > quietUntil) quietUntil = u; }
    public void key(int code) {
        cmdSeq++;
        if (code != Keys.DEL) quiet(KEY_QUIET_MS); // navigating with D-pad/OK must never pull the app to the Keyboard tab
        RemoteClient r = remote; if (r != null && state == State.CONNECTED) r.sendKey(code); }
    /** True if a text-field focus event right now should switch to the Keyboard tab. */
    public boolean imeAutoOpenAllowed(String pkg) {
        long now = System.currentTimeMillis();
        if (now - connectedAt < IME_GRACE_MS || now < quietUntil) return false;
        return pkg == null || !pkg.toLowerCase().contains("launcher");
    }

    public void keyDir(int code, int dir) { if (code != Keys.DEL) quiet(KEY_QUIET_MS); RemoteClient r = remote; if (r != null && state == State.CONNECTED) r.sendKey(code, dir); }
    public void text(String t) { RemoteClient r = remote; if (r != null && state == State.CONNECTED) r.sendText(t); }
    public void launch(String link) { quiet(LAUNCH_QUIET_MS); RemoteClient r = remote; if (r != null && state == State.CONNECTED) r.launch(link); }
}
