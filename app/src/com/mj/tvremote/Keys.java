package com.mj.tvremote;

/** Android KeyEvent codes used by the remote. */
public final class Keys {
    private Keys() {}
    public static final int HOME = 3, BACK = 4, UP = 19, DOWN = 20, LEFT = 21, RIGHT = 22, CENTER = 23,
            VOL_UP = 24, VOL_DOWN = 25, POWER = 26, ENTER = 66, DEL = 67, MENU = 82, MUTE = 164,
            CH_UP = 166, CH_DOWN = 167, D0 = 7;
    /** Key code for a digit 0-9. */
    public static int digit(int d) { return D0 + d; }
}
