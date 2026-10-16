package com.mj.tvremote;

import java.util.Random;

/** A client name that is unique per install. Android TVs file a pairing under the client name, so two phones that both
 *  say "TV Remote" make the TV replace the first phone's pairing when the second one pairs. */
public final class ClientName {
    public static final String LEGACY = "TV Remote";

    public static String make(String model, Random r) {
        String m = model == null ? "" : model.replaceAll("[^A-Za-z0-9 ._-]", "").trim();
        if (m.length() > 18) m = m.substring(0, 18).trim();
        String tag = String.format("%04x", r.nextInt(0x10000));
        return m.isEmpty() ? LEGACY + " " + tag : LEGACY + " - " + m + " " + tag;
    }
}
