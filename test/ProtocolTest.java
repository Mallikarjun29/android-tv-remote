import com.mj.tvremote.proto.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class ProtocolTest {
    static int fails = 0;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) fails++; }

    public static void main(String[] a) throws Exception {
        String dir = a[0];
        String mode = a[1];
        if (mode.equals("gen")) {
            Identity id = Identity.generate("tvremote-test");
            Files.write(Paths.get(dir, "client_cert.der"), id.certBytes());
            Files.write(Paths.get(dir, "client_key.der"), id.keyBytes());
            System.out.println("generated");
            return;
        }
        Identity id = Identity.load(Files.readAllBytes(Paths.get(dir, "client_key.der")), Files.readAllBytes(Paths.get(dir, "client_cert.der")));
        String code = new String(Files.readAllBytes(Paths.get(dir, "tv_code.txt"))).trim();

        // wrong code first (first byte mismatch is caught locally), then a code with right first byte but wrong tail
        PairingClient p0 = new PairingClient(id, "127.0.0.1", 16467);
        p0.start("TV Remote");
        try { p0.finish("000000"); check("wrong code rejected", false); } catch (Identity.BadCodeException e) { check("wrong code rejected locally", true); }
        p0.close();

        PairingClient p1 = new PairingClient(id, "127.0.0.1", 16467);
        p1.start("TV Remote");
        String badTail = code.substring(0, 2) + "0000";
        try { p1.finish(badTail); check("bad tail rejected by TV", false); } catch (Identity.BadCodeException e) { check("bad code with matching first byte rejected locally (hash mismatch)", true); } catch (Exception e) { check("bad tail rejected by TV: " + e, false); }
        p1.close();

        PairingClient p2 = new PairingClient(id, "127.0.0.1", 16467);
        p2.start("TV Remote");
        p2.finish(code.toLowerCase());
        check("pairing with correct code (lowercase input)", true);
        p2.close();

        final CountDownLatch ime = new CountDownLatch(1);
        final int[] vol = new int[]{-1, -1, -1};
        final String[] pkg = new String[1];
        final CountDownLatch ready = new CountDownLatch(1);
        RemoteClient rc = new RemoteClient(id, new RemoteClient.Listener() {
            public void onReady() { ready.countDown(); }
            public void onVolume(int l, int m, boolean mu) { vol[0] = l; vol[1] = m; vol[2] = mu ? 1 : 0; }
            public void onImeFocus(String p) { pkg[0] = p; ime.countDown(); }
            public void onTvError(String m) { System.out.println("tv error " + m); }
            public void onClosed(Exception e) { System.out.println("closed " + e); }
        });
        rc.connect("127.0.0.1", 16466, 5000);
        check("remote connect/ready", ready.await(2, TimeUnit.SECONDS));
        check("volume parsed 18/100/unmuted", vol[0] == 18 && vol[1] == 100 && vol[2] == 0);
        check("IME focus event with package", ime.await(4, TimeUnit.SECONDS) && "com.netflix.ninja".equals(pkg[0]));
        Thread.sleep(300);
        rc.sendKey(19);
        rc.sendKey(20, RemoteClient.DIR_START);
        rc.sendKey(20, RemoteClient.DIR_END);
        rc.sendKey(24); rc.sendKey(25); rc.sendKey(164); rc.sendKey(166); rc.sendKey(167); rc.sendKey(26);
        rc.sendKey(7); rc.sendKey(16);
        rc.sendText("abc");
        rc.sendText("é ü");
        rc.launch("market://launch?id=com.netflix.ninja");
        rc.launch("https://www.youtube.com/watch?v=xyz");
        Thread.sleep(800);
        rc.close();
        System.out.println(fails == 0 ? "ALL PASS" : "FAILURES " + fails);
        System.exit(fails == 0 ? 0 : 1);
    }
}
