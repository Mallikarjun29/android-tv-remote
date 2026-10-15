package com.mj.tvremote;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import java.util.LinkedList;

/** Finds Android TVs on the local network via mDNS (_androidtvremote2._tcp). */
public final class Discovery {
    public interface Listener { void onFound(String name, String host); }

    private final NsdManager nsd;
    private final WifiManager wifi;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private NsdManager.DiscoveryListener dl;
    private WifiManager.MulticastLock lock;
    private final LinkedList<NsdServiceInfo> queue = new LinkedList<NsdServiceInfo>();
    private boolean resolving;

    public Discovery(Context c, Listener l) {
        nsd = (NsdManager) c.getApplicationContext().getSystemService(Context.NSD_SERVICE);
        wifi = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        listener = l;
    }

    public void start() {
        stop();
        try {
            lock = wifi.createMulticastLock("tvremote");
            lock.setReferenceCounted(false);
            lock.acquire();
        } catch (Exception ignored) {}
        dl = new NsdManager.DiscoveryListener() {
            public void onStartDiscoveryFailed(String t, int e) {}
            public void onStopDiscoveryFailed(String t, int e) {}
            public void onDiscoveryStarted(String t) {}
            public void onDiscoveryStopped(String t) {}
            public void onServiceFound(NsdServiceInfo s) { enqueue(s); }
            public void onServiceLost(NsdServiceInfo s) {}
        };
        try { nsd.discoverServices("_androidtvremote2._tcp.", NsdManager.PROTOCOL_DNS_SD, dl); } catch (Exception ignored) { dl = null; }
    }

    public void stop() {
        if (dl != null) { try { nsd.stopServiceDiscovery(dl); } catch (Exception ignored) {} dl = null; }
        if (lock != null && lock.isHeld()) { try { lock.release(); } catch (Exception ignored) {} }
        synchronized (queue) { queue.clear(); resolving = false; }
    }

    private void enqueue(NsdServiceInfo s) {
        synchronized (queue) { queue.add(s); }
        next();
    }

    private void next() {
        final NsdServiceInfo s;
        synchronized (queue) {
            if (resolving || queue.isEmpty()) return;
            s = queue.removeFirst();
            resolving = true;
        }
        try {
            nsd.resolveService(s, new NsdManager.ResolveListener() {
                public void onResolveFailed(NsdServiceInfo i, int e) { done(); }
                public void onServiceResolved(final NsdServiceInfo i) {
                    if (i.getHost() != null) {
                        final String host = i.getHost().getHostAddress();
                        final String name = i.getServiceName();
                        main.post(new Runnable() { public void run() { listener.onFound(name, host); } });
                    }
                    done();
                }
            });
        } catch (Exception e) { done(); }
    }

    private void done() { synchronized (queue) { resolving = false; } next(); }
}
