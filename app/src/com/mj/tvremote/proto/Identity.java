package com.mj.tvremote.proto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** The phone's client identity: a self-signed RSA certificate the TV remembers after pairing. */
public final class Identity {
    private final PrivateKey key;
    private final X509Certificate cert;

    public Identity(PrivateKey key, X509Certificate cert) { this.key = key; this.cert = cert; }

    public X509Certificate cert() { return cert; }
    public byte[] keyBytes() { return key.getEncoded(); }
    public byte[] certBytes() {
        try { return cert.getEncoded(); } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static Identity load(byte[] pkcs8, byte[] certDer) throws Exception {
        PrivateKey k = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        X509Certificate c = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(certDer));
        return new Identity(k, c);
    }

    public static Identity generate(String commonName) throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        byte[] der = selfSignedCert(kp, commonName);
        X509Certificate c = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(der));
        return new Identity(kp.getPrivate(), c);
    }

    // ---- minimal DER encoder for a self-signed X.509 v3 certificate ----
    private static byte[] tlv(int tag, byte[] content) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(tag);
        int n = content.length;
        if (n < 128) o.write(n);
        else if (n < 256) { o.write(0x81); o.write(n); }
        else if (n < 65536) { o.write(0x82); o.write(n >> 8); o.write(n & 0xFF); }
        else { o.write(0x83); o.write(n >> 16); o.write((n >> 8) & 0xFF); o.write(n & 0xFF); }
        o.write(content, 0, content.length);
        return o.toByteArray();
    }

    private static byte[] cat(byte[]... parts) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] p : parts) o.write(p, 0, p.length);
        return o.toByteArray();
    }

    private static byte[] utc(Date d) {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyMMddHHmmss'Z'");
        f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return tlv(0x17, f.format(d).getBytes());
    }

    static byte[] selfSignedCert(KeyPair kp, String cn) throws Exception {
        byte[] sigAlg = new byte[]{0x30, 0x0D, 0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x0B, 0x05, 0x00};
        byte[] name = tlv(0x30, tlv(0x31, tlv(0x30, cat(new byte[]{0x06, 0x03, 0x55, 0x04, 0x03}, tlv(0x0C, cn.getBytes("UTF-8"))))));
        byte[] serial = new byte[16];
        new SecureRandom().nextBytes(serial);
        serial[0] &= 0x7F;
        serial[0] |= 0x01;
        long now = System.currentTimeMillis();
        byte[] validity = tlv(0x30, cat(utc(new Date(now - 24L * 3600 * 1000)), utc(new Date(now + 20L * 365 * 24 * 3600 * 1000))));
        byte[] tbs = tlv(0x30, cat(
                new byte[]{(byte) 0xA0, 0x03, 0x02, 0x01, 0x02},
                tlv(0x02, serial),
                sigAlg,
                name,
                validity,
                name,
                kp.getPublic().getEncoded()));
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(kp.getPrivate());
        s.update(tbs);
        byte[] sig = s.sign();
        return tlv(0x30, cat(tbs, sigAlg, tlv(0x03, cat(new byte[]{0}, sig))));
    }

    // ---- TLS ----
    /** Thrown during the TLS handshake when the TV's certificate is not the one we saw before. */
    public static class TvChangedException extends java.security.cert.CertificateException {
        public TvChangedException() { super("TV certificate changed"); }
    }

    public static byte[] fingerprint(X509Certificate c) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(c.getEncoded());
    }

    public static boolean causedByTvChange(Throwable t) {
        for (int n = 0; t != null && n < 8; n++, t = t.getCause()) if (t instanceof TvChangedException) return true;
        return false;
    }

    public SSLSocketFactory socketFactory() throws Exception { return socketFactory(null, null); }

    /** pin: the TV certificate fingerprint to require (null = accept any, used while pairing and the first time we connect).
     *  seen: if not null, receives the fingerprint the TV presented. */
    public SSLSocketFactory socketFactory(final byte[] pin, final byte[][] seen) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        char[] pw = "tv".toCharArray();
        ks.setKeyEntry("client", key, pw, new Certificate[]{cert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pw);
        // The TV presents a self-signed certificate; trust is established by the on-screen pairing code.
        TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) throws java.security.cert.CertificateException {
                if (c == null || c.length == 0) throw new java.security.cert.CertificateException("no TV certificate");
                byte[] fp;
                try { fp = fingerprint(c[0]); } catch (Exception e) { throw new java.security.cert.CertificateException(e); }
                if (seen != null) seen[0] = fp;
                if (pin != null && !MessageDigest.isEqual(pin, fp)) throw new TvChangedException();
            }
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }};
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), trustAll, new SecureRandom());
        return ctx.getSocketFactory();
    }

    // ---- pairing secret ----
    static byte[] hexBytes(String hex) {
        if ((hex.length() & 1) == 1) hex = "0" + hex;
        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        return b;
    }

    /** secret = SHA256(clientMod | clientExp | serverMod | serverExp | code[2:]) as in the TV's pairing protocol. */
    public static byte[] pairingSecret(RSAPublicKey client, RSAPublicKey server, String code) throws Exception {
        if (code == null || code.length() != 6 || !code.matches("[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Code must be 6 hex characters");
        MessageDigest h = MessageDigest.getInstance("SHA-256");
        h.update(hexBytes(client.getModulus().toString(16).toUpperCase()));
        h.update(hexBytes("0" + client.getPublicExponent().toString(16).toUpperCase()));
        h.update(hexBytes(server.getModulus().toString(16).toUpperCase()));
        h.update(hexBytes("0" + server.getPublicExponent().toString(16).toUpperCase()));
        h.update(hexBytes(code.substring(2)));
        byte[] r = h.digest();
        if ((r[0] & 0xFF) != Integer.parseInt(code.substring(0, 2), 16)) throw new BadCodeException("Wrong code");
        return r;
    }

    public static final class BadCodeException extends java.io.IOException {
        public BadCodeException(String m) { super(m); }
    }
}
