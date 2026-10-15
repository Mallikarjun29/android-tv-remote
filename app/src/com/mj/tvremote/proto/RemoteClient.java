package com.mj.tvremote.proto;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;

/** Remote control channel to an Android TV (port 6466). */
public final class RemoteClient implements Closeable {
    public interface Listener {
        void onReady();
        void onVolume(int level, int max, boolean muted);
        void onImeFocus(String appPackage);
        void onTvError(String message);
        void onClosed(Exception reason);
    }

    public static final int DIR_START = 1, DIR_END = 2, DIR_SHORT = 3;
    // PING | KEY | IME | POWER | VOLUME | APP_LINK
    private static final int OUR_FEATURES = 1 | 2 | 4 | 32 | 64 | 512;

    private final Identity id;
    private final Listener l;
    private SSLSocket sock;
    private InputStream in;
    private OutputStream out;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private volatile boolean closed;
    private volatile int imeCounter, fieldCounter;
    private final CountDownLatch ready = new CountDownLatch(1);
    private long features = OUR_FEATURES;

    public RemoteClient(Identity id, Listener l) { this.id = id; this.l = l; }

    /** Connects, completes the handshake and waits (up to timeoutMs) until the TV says the remote is started. */
    public void connect(String host, int port, int timeoutMs) throws Exception {
        sock = (SSLSocket) id.socketFactory().createSocket();
        sock.connect(new InetSocketAddress(host, port), 5000);
        sock.setSoTimeout(30000); // the TV pings every few seconds; silence for 30s means a dead link
        sock.startHandshake();
        in = sock.getInputStream();
        out = sock.getOutputStream();
        Thread t = new Thread(new Runnable() { public void run() { loop(); } }, "tv-reader");
        t.setDaemon(true);
        t.start();
        if (!ready.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            close();
            throw new IOException("TV did not start the remote session (not paired?)");
        }
    }

    private void loop() {
        Exception reason = null;
        try {
            while (!closed) handle(Proto.readFrame(in));
        } catch (Exception e) {
            reason = e;
        }
        boolean wasClosed = closed;
        closed = true;
        try { sock.close(); } catch (Exception ignored) {}
        writer.shutdown();
        if (!wasClosed || ready.getCount() > 0) l.onClosed(reason);
    }

    private void send(final byte[] msg) {
        if (closed) return;
        try {
            writer.execute(new Runnable() { public void run() {
                try { synchronized (RemoteClient.this) { Proto.writeFrame(out, msg); } }
                catch (IOException e) { try { sock.close(); } catch (IOException ignored) {} }
            } });
        } catch (RuntimeException ignored) {}
    }

    private void handle(byte[] raw) {
        Proto.R r = new Proto.R(raw);
        while (r.next()) {
            if (r.data == null) continue;
            switch (r.field) {
                case 1: { // configure
                    long code1 = 0;
                    Proto.R c = new Proto.R(r.data);
                    while (c.next()) if (c.field == 1 && c.data == null) code1 = c.v;
                    features = OUR_FEATURES & code1;
                    Proto.W dev = new Proto.W().i(3, 1).s(4, "1").s(5, "atvremote").s(6, "1.0.0");
                    send(new Proto.W().m(1, new Proto.W().i(1, features).m(2, dev)).bytes());
                    break;
                }
                case 2: send(new Proto.W().m(2, new Proto.W().i(1, features)).bytes()); break;
                case 3: { // error
                    String msg = "TV reported an error";
                    Proto.R c = new Proto.R(r.data);
                    while (c.next()) if (c.field == 2 && c.data != null) msg = c.str();
                    l.onTvError(msg);
                    break;
                }
                case 8: { // ping
                    long v = 0;
                    Proto.R c = new Proto.R(r.data);
                    while (c.next()) if (c.field == 1 && c.data == null) v = c.v;
                    send(new Proto.W().m(9, new Proto.W().i(1, v)).bytes());
                    break;
                }
                case 20: { // ime key inject: a text field got focus on the TV
                    String pkg = "";
                    Proto.R c = new Proto.R(r.data);
                    while (c.next()) {
                        if (c.field == 1 && c.data != null) {
                            Proto.R a = new Proto.R(c.data);
                            while (a.next()) if (a.field == 12 && a.data != null) pkg = a.str();
                        }
                    }
                    l.onImeFocus(pkg);
                    break;
                }
                case 21: {
                    Proto.R c = new Proto.R(r.data);
                    while (c.next()) {
                        if (c.field == 1 && c.data == null) imeCounter = (int) c.v;
                        if (c.field == 2 && c.data == null) fieldCounter = (int) c.v;
                    }
                    break;
                }
                case 22: l.onImeFocus(""); break;
                case 40: ready.countDown(); l.onReady(); break;
                case 50: {
                    int level = 0, max = 0;
                    boolean muted = false;
                    Proto.R c = new Proto.R(r.data);
                    while (c.next()) {
                        if (c.data != null) continue;
                        if (c.field == 6) max = (int) c.v;
                        else if (c.field == 7) level = (int) c.v;
                        else if (c.field == 8) muted = c.v != 0;
                    }
                    l.onVolume(level, max, muted);
                    break;
                }
                default: break;
            }
        }
    }

    public void sendKey(int keyCode, int direction) {
        send(new Proto.W().m(10, new Proto.W().i(1, keyCode).i(2, direction)).bytes());
    }

    public void sendKey(int keyCode) { sendKey(keyCode, DIR_SHORT); }

    /** Inserts text into the focused field on the TV. */
    public void sendText(String text) {
        if (text == null || text.isEmpty()) return;
        int p = text.length() - 1;
        Proto.W obj = new Proto.W().i(1, p).i(2, p).s(3, text);
        Proto.W edit = new Proto.W().i(1, 1).m(2, obj);
        Proto.W batch = new Proto.W().i(1, imeCounter).i(2, fieldCounter).m(3, edit);
        send(new Proto.W().m(21, batch).bytes());
    }

    public void launch(String appLink) {
        send(new Proto.W().m(90, new Proto.W().s(1, appLink)).bytes());
    }

    public boolean isOpen() { return !closed; }

    @Override public void close() {
        closed = true;
        try { if (sock != null) sock.close(); } catch (IOException ignored) {}
        writer.shutdown();
    }
}
