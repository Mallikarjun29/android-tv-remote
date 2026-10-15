package com.mj.tvremote.proto;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/** Minimal protobuf wire-format writer/reader, enough for the Android TV Remote v2 messages. */
public final class Proto {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private Proto() {}

    public static final class W {
        private final ByteArrayOutputStream o = new ByteArrayOutputStream();

        public W varint(long v) {
            while ((v & ~0x7FL) != 0) {
                o.write((int) ((v & 0x7F) | 0x80));
                v >>>= 7;
            }
            o.write((int) v);
            return this;
        }

        public W tag(int f, int wt) { return varint(((long) f << 3) | wt); }
        public W i(int f, long v) { tag(f, 0); return varint(v); }

        public W b(int f, byte[] d) {
            tag(f, 2);
            varint(d.length);
            o.write(d, 0, d.length);
            return this;
        }

        public W s(int f, String s) { return b(f, s.getBytes(UTF8)); }
        public W m(int f, W w) { return b(f, w.bytes()); }
        public byte[] bytes() { return o.toByteArray(); }
    }

    public static final class R {
        private final byte[] d;
        private int p;
        private final int end;
        public int field;
        public int wt;
        public long v;
        public byte[] data;

        public R(byte[] d) { this.d = d; this.p = 0; this.end = d.length; }

        private long readVar() {
            long r = 0;
            int shift = 0;
            while (true) {
                if (p >= end || shift > 63) throw new IllegalArgumentException("bad varint");
                int b = d[p++] & 0xFF;
                r |= (long) (b & 0x7F) << shift;
                if ((b & 0x80) == 0) return r;
                shift += 7;
            }
        }

        public boolean next() {
            if (p >= end) return false;
            long t = readVar();
            field = (int) (t >>> 3);
            wt = (int) (t & 7);
            switch (wt) {
                case 0: v = readVar(); data = null; break;
                case 2: {
                    long n = readVar();
                    if (n < 0 || p + n > end) throw new IllegalArgumentException("bad length");
                    data = Arrays.copyOfRange(d, p, p + (int) n);
                    p += (int) n;
                    break;
                }
                case 1: if (p + 8 > end) throw new IllegalArgumentException("trunc"); p += 8; data = null; break;
                case 5: if (p + 4 > end) throw new IllegalArgumentException("trunc"); p += 4; data = null; break;
                default: throw new IllegalArgumentException("unsupported wire type " + wt);
            }
            return true;
        }

        public String str() { return new String(data, UTF8); }
    }

    /** Messages on the wire are prefixed with a varint length. */
    public static void writeFrame(OutputStream out, byte[] msg) throws IOException {
        W w = new W();
        w.varint(msg.length);
        byte[] hdr = w.bytes();
        byte[] all = new byte[hdr.length + msg.length];
        System.arraycopy(hdr, 0, all, 0, hdr.length);
        System.arraycopy(msg, 0, all, hdr.length, msg.length);
        out.write(all);
        out.flush();
    }

    public static byte[] readFrame(InputStream in) throws IOException {
        long len = 0;
        int shift = 0;
        while (true) {
            int b = in.read();
            if (b < 0) throw new EOFException("connection closed");
            len |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
            if (shift > 35) throw new IOException("bad frame length");
        }
        if (len < 0 || len > (1 << 20)) throw new IOException("frame too large: " + len);
        byte[] buf = new byte[(int) len];
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) throw new EOFException("connection closed mid-frame");
            off += n;
        }
        return buf;
    }
}
