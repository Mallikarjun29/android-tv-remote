package com.mj.tvremote.proto;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import javax.net.ssl.SSLSocket;

/** Pairing with an Android TV (port 6467). The TV shows a 6 character hex code. */
public final class PairingClient implements Closeable {
    private static final int STATUS_OK = 200;
    private final Identity id;
    private final String host;
    private final int port;
    private SSLSocket sock;
    private InputStream in;
    private OutputStream out;
    private RSAPublicKey serverKey;

    public PairingClient(Identity id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    private static Proto.W outer(int field, Proto.W body) {
        Proto.W w = new Proto.W().i(1, 2).i(2, STATUS_OK);
        if (body != null) w.m(field, body);
        return w;
    }

    private static final class Msg {
        long status;
        boolean ack, options, configAck, secretAck;
    }

    private Msg read() throws IOException {
        Msg m = new Msg();
        try {
            Proto.R r = new Proto.R(Proto.readFrame(in));
            while (r.next()) {
                if (r.field == 2) m.status = r.v;
                else if (r.field == 11) m.ack = true;
                else if (r.field == 20) m.options = true;
                else if (r.field == 31) m.configAck = true;
                else if (r.field == 41) m.secretAck = true;
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("Bad reply from TV", e);
        }
        if (m.status != STATUS_OK) {
            if (m.status == 402) throw new Identity.BadCodeException("Wrong code");
            throw new IOException("TV returned status " + m.status);
        }
        return m;
    }

    private void send(Proto.W w) throws IOException { Proto.writeFrame(out, w.bytes()); }

    /** Connects and runs the handshake up to the point where the TV displays its code. */
    public void start(String clientName) throws Exception {
        sock = (SSLSocket) id.socketFactory().createSocket();
        sock.connect(new InetSocketAddress(host, port), 5000);
        sock.setSoTimeout(10000);
        sock.startHandshake();
        in = sock.getInputStream();
        out = sock.getOutputStream();
        Certificate[] peer = sock.getSession().getPeerCertificates();
        if (peer.length == 0 || !(peer[0].getPublicKey() instanceof RSAPublicKey)) throw new IOException("TV certificate not supported");
        serverKey = (RSAPublicKey) peer[0].getPublicKey();

        send(outer(10, new Proto.W().s(1, "atvremote").s(2, clientName)));
        if (!read().ack) throw new IOException("TV did not acknowledge pairing request");
        Proto.W enc = new Proto.W().i(1, 3).i(2, 6); // hexadecimal, 6 symbols
        send(outer(20, new Proto.W().m(1, enc).i(3, 1))); // we are the input device
        if (!read().options) throw new IOException("TV did not send pairing options");
        send(outer(30, new Proto.W().m(1, new Proto.W().i(1, 3).i(2, 6)).i(2, 1)));
        if (!read().configAck) throw new IOException("TV did not accept pairing configuration");
    }

    /** Completes pairing with the code shown on the TV. Throws BadCodeException if wrong. */
    public void finish(String code) throws Exception {
        byte[] secret = Identity.pairingSecret((RSAPublicKey) id.cert().getPublicKey(), serverKey, code.trim());
        send(outer(40, new Proto.W().b(1, secret)));
        if (!read().secretAck) throw new IOException("TV did not confirm pairing");
    }

    @Override public void close() {
        try { if (sock != null) sock.close(); } catch (IOException ignored) {}
    }
}
