package com.mj.tvremote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Colors, small view helpers and the custom-drawn views. */
public final class Ui {
    private Ui() {}
    public static final int BG = 0xFF0F121C, CARD = 0xFF1C2132, CARD_PRESSED = 0xFF2B3350, ACC = 0xFF5E8CFF, TX = 0xFFECF0FA,
            MU = 0xFFB2BCD4, DK = 0xFF0A0E1E, GRN = 0xFF6EDC96, RED = 0xFFFF7882, ACC_DIM = 0xFF28376B, RED_BG = 0xFF28161E,
            GRN_BG = 0xFF14301F;

    public static int dp(Context c, float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics())); }

    public static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    public static GradientDrawable outlined(int fill, int stroke, int strokePx, float radiusPx) {
        GradientDrawable g = round(fill, radiusPx);
        g.setStroke(strokePx, stroke);
        return g;
    }

    public static StateListDrawable pressable(int normal, int pressed, float radiusPx) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, round(pressed, radiusPx));
        s.addState(new int[]{}, round(normal, radiusPx));
        return s;
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) { return new LinearLayout.LayoutParams(w, h, weight); }

    public static void haptic(View v) { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }

    /** Makes a view act as a button: pressed state, haptic tick, and optional press-and-hold repeat. */
    public static void button(final View v, final Runnable action, final boolean repeat) {
        final Handler h = new Handler(Looper.getMainLooper());
        final Runnable[] rep = new Runnable[1];
        rep[0] = new Runnable() { public void run() { action.run(); h.postDelayed(rep[0], 130); } };
        v.setClickable(true);
        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        haptic(view);
                        action.run();
                        if (repeat) h.postDelayed(rep[0], 450);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.setPressed(false);
                        h.removeCallbacks(rep[0]);
                        return true;
                }
                return true;
            }
        });
    }

    public static final int I_BACK = 1, I_HOME = 2, I_MENU = 3, I_POWER = 4, I_MUTE = 5, I_REMOTE = 6, I_GRID = 7, I_KEYBOARD = 8,
            I_TV = 9, I_CHECK = 10, I_PLUS = 11, I_MINUS = 12, I_LINK = 13, I_BACKSPACE = 14, I_UNMUTE = 15;

    /** A simple stroked icon drawn on a canvas. */
    public static final class IconView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int type, color = TX;
        public IconView(Context c, int type, int color) {
            super(c);
            this.type = type; this.color = color;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
        public void set(int type, int color) { this.type = type; this.color = color; invalidate(); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), s = Math.min(w, h), cx = w / 2, cy = h / 2, u = s / 24f;
            p.setColor(color);
            p.setStrokeWidth(2.2f * u);
            p.setStyle(Paint.Style.STROKE);
            Path q = new Path();
            switch (type) {
                case I_BACK:
                    c.drawLine(cx + 9 * u, cy, cx - 9 * u, cy, p);
                    q.moveTo(cx - 2 * u, cy - 7 * u); q.lineTo(cx - 9 * u, cy); q.lineTo(cx - 2 * u, cy + 7 * u);
                    c.drawPath(q, p); break;
                case I_HOME:
                    q.moveTo(cx - 10 * u, cy); q.lineTo(cx, cy - 9 * u); q.lineTo(cx + 10 * u, cy);
                    c.drawPath(q, p);
                    q = new Path();
                    q.moveTo(cx - 7 * u, cy - 2 * u); q.lineTo(cx - 7 * u, cy + 9 * u); q.lineTo(cx + 7 * u, cy + 9 * u); q.lineTo(cx + 7 * u, cy - 2 * u);
                    c.drawPath(q, p); break;
                case I_MENU:
                    for (int i = -1; i <= 1; i++) c.drawLine(cx - 9 * u, cy + i * 7 * u, cx + 9 * u, cy + i * 7 * u, p);
                    break;
                case I_POWER:
                    c.drawArc(new RectF(cx - 8 * u, cy - 7 * u, cx + 8 * u, cy + 9 * u), -60, 300, false, p);
                    c.drawLine(cx, cy - 10 * u, cx, cy - 1 * u, p); break;
                case I_MUTE: case I_UNMUTE:
                    p.setStyle(Paint.Style.FILL);
                    q.moveTo(cx - 10 * u, cy - 3 * u); q.lineTo(cx - 5 * u, cy - 3 * u); q.lineTo(cx + 1 * u, cy - 8 * u); q.lineTo(cx + 1 * u, cy + 8 * u);
                    q.lineTo(cx - 5 * u, cy + 3 * u); q.lineTo(cx - 10 * u, cy + 3 * u); q.close();
                    c.drawPath(q, p);
                    p.setStyle(Paint.Style.STROKE);
                    if (type == I_MUTE) {
                        c.drawLine(cx + 5 * u, cy - 4 * u, cx + 11 * u, cy + 4 * u, p);
                        c.drawLine(cx + 11 * u, cy - 4 * u, cx + 5 * u, cy + 4 * u, p);
                    } else {
                        c.drawArc(new RectF(cx - 2 * u, cy - 5 * u, cx + 8 * u, cy + 5 * u), -50, 100, false, p);
                        c.drawArc(new RectF(cx - 4 * u, cy - 9 * u, cx + 12 * u, cy + 9 * u), -50, 100, false, p);
                    }
                    break;
                case I_REMOTE:
                    c.drawRoundRect(new RectF(cx - 5 * u, cy - 10 * u, cx + 5 * u, cy + 10 * u), 3 * u, 3 * u, p);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx, cy - 5 * u, 1.8f * u, p); break;
                case I_GRID:
                    for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) {
                        float x = cx + (i == 0 ? -9 : 1) * u, y = cy + (j == 0 ? -9 : 1) * u;
                        c.drawRoundRect(new RectF(x, y, x + 8 * u, y + 8 * u), 2 * u, 2 * u, p);
                    } break;
                case I_KEYBOARD:
                    c.drawRoundRect(new RectF(cx - 11 * u, cy - 7 * u, cx + 11 * u, cy + 7 * u), 3 * u, 3 * u, p);
                    p.setStrokeWidth(2.6f * u);
                    for (int i = -1; i <= 1; i++) { c.drawPoint(cx + i * 5 * u, cy - 2.5f * u, p); c.drawPoint(cx + i * 5 * u, cy + 1.5f * u, p); }
                    c.drawLine(cx - 5 * u, cy + 4.5f * u, cx + 5 * u, cy + 4.5f * u, p); break;
                case I_TV:
                    c.drawRoundRect(new RectF(cx - 11 * u, cy - 8 * u, cx + 11 * u, cy + 5 * u), 3 * u, 3 * u, p);
                    c.drawLine(cx - 5 * u, cy + 9 * u, cx + 5 * u, cy + 9 * u, p); break;
                case I_CHECK:
                    q.moveTo(cx - 8 * u, cy); q.lineTo(cx - 2 * u, cy + 6 * u); q.lineTo(cx + 8 * u, cy - 6 * u);
                    c.drawPath(q, p); break;
                case I_PLUS:
                    p.setStrokeWidth(3f * u);
                    c.drawLine(cx - 9 * u, cy, cx + 9 * u, cy, p); c.drawLine(cx, cy - 9 * u, cx, cy + 9 * u, p); break;
                case I_MINUS:
                    p.setStrokeWidth(3f * u);
                    c.drawLine(cx - 9 * u, cy, cx + 9 * u, cy, p); break;
                case I_LINK:
                    c.drawArc(new RectF(cx - 12 * u, cy - 5 * u, cx + 1 * u, cy + 5 * u), 90, 180, false, p);
                    c.drawArc(new RectF(cx - 1 * u, cy - 5 * u, cx + 12 * u, cy + 5 * u), 270, 180, false, p);
                    c.drawLine(cx - 5 * u, cy, cx + 5 * u, cy, p); break;
                case I_BACKSPACE:
                    q.moveTo(cx - 11 * u, cy); q.lineTo(cx - 6 * u, cy - 7 * u); q.lineTo(cx + 10 * u, cy - 7 * u); q.lineTo(cx + 10 * u, cy + 7 * u); q.lineTo(cx - 6 * u, cy + 7 * u); q.close();
                    c.drawPath(q, p);
                    c.drawLine(cx - 2 * u, cy - 3 * u, cx + 5 * u, cy + 3 * u, p); c.drawLine(cx - 2 * u, cy + 3 * u, cx + 5 * u, cy - 3 * u, p); break;
            }
        }
    }

    /** Circular D-pad: four arrows around a plain center. */
    public static final class DPadView extends View {
        public interface Callback { void key(int code); }
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Callback cb;
        private int down = 0; // 0 none, else key code
        private final Handler h = new Handler(Looper.getMainLooper());
        private final Runnable rep = new Runnable() { public void run() { if (down != 0) { cb.key(down); h.postDelayed(this, 130); } } };

        public DPadView(Context c, Callback cb) { super(c); this.cb = cb; }

        private int hit(float x, float y) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy);
            float dx = x - cx, dy = y - cy, d = (float) Math.hypot(dx, dy);
            if (d > r) return 0;
            if (d < r * 0.38f) return Keys.CENTER;
            if (Math.abs(dx) > Math.abs(dy)) return dx > 0 ? Keys.RIGHT : Keys.LEFT;
            return dy > 0 ? Keys.DOWN : Keys.UP;
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down = hit(e.getX(), e.getY());
                    if (down == 0) return false;
                    haptic(this); cb.key(down); invalidate();
                    if (down != Keys.CENTER) h.postDelayed(rep, 450);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    down = 0; h.removeCallbacks(rep); invalidate(); return true;
            }
            return true;
        }

        @Override protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy);
            p.setStyle(Paint.Style.FILL);
            p.setColor(CARD);
            c.drawCircle(cx, cy, r, p);
            p.setColor(down != 0 && down != Keys.CENTER ? CARD_PRESSED : CARD);
            if (down != 0 && down != Keys.CENTER) {
                // highlight the pressed quadrant
                c.save();
                Path clip = new Path(); clip.addCircle(cx, cy, r, Path.Direction.CW); c.clipPath(clip);
                Path wedge = new Path(); wedge.moveTo(cx, cy);
                float L = r * 1.5f;
                if (down == Keys.UP) { wedge.lineTo(cx - L, cy - L); wedge.lineTo(cx + L, cy - L); }
                else if (down == Keys.DOWN) { wedge.lineTo(cx - L, cy + L); wedge.lineTo(cx + L, cy + L); }
                else if (down == Keys.LEFT) { wedge.lineTo(cx - L, cy - L); wedge.lineTo(cx - L, cy + L); }
                else { wedge.lineTo(cx + L, cy - L); wedge.lineTo(cx + L, cy + L); }
                wedge.close();
                c.drawPath(wedge, p);
                c.restore();
            }
            p.setColor(down == Keys.CENTER ? 0xFF8CAAFF : ACC);
            c.drawCircle(cx, cy, r * 0.30f, p);
            p.setColor(TX);
            float a = r * 0.09f, d = r * 0.68f;
            tri(c, cx, cy - d, a, 0); tri(c, cx, cy + d, a, 2); tri(c, cx - d, cy, a, 3); tri(c, cx + d, cy, a, 1);
        }

        private void tri(Canvas c, float x, float y, float a, int dir) {
            Path t = new Path();
            float w = a * 1.6f;
            if (dir == 0) { t.moveTo(x, y - a); t.lineTo(x - w, y + a); t.lineTo(x + w, y + a); }
            else if (dir == 2) { t.moveTo(x, y + a); t.lineTo(x - w, y - a); t.lineTo(x + w, y - a); }
            else if (dir == 3) { t.moveTo(x - a, y); t.lineTo(x + a, y - w); t.lineTo(x + a, y + w); }
            else { t.moveTo(x + a, y); t.lineTo(x - a, y - w); t.lineTo(x - a, y + w); }
            t.close();
            c.drawPath(t, p);
        }
    }

    /** Touch mode: swipe = arrow keys, tap = select, two-finger tap = Back. */
    public static final class TouchPad extends View {
        public interface Callback { void key(int code); }
        private final Callback cb;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float ax, ay;
        private boolean moved;
        private int maxPointers;
        private long downTime;
        private final float step;

        public TouchPad(Context c, Callback cb) { super(c); this.cb = cb; step = dp(c, 44); }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    ax = e.getX(); ay = e.getY(); moved = false; maxPointers = 1; downTime = e.getEventTime();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_POINTER_DOWN:
                    maxPointers = Math.max(maxPointers, e.getPointerCount()); return true;
                case MotionEvent.ACTION_MOVE:
                    if (maxPointers > 1) return true;
                    float dx = e.getX() - ax, dy = e.getY() - ay;
                    if (Math.abs(dx) > step || Math.abs(dy) > step) {
                        moved = true;
                        int k = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? Keys.RIGHT : Keys.LEFT) : (dy > 0 ? Keys.DOWN : Keys.UP);
                        haptic(this); cb.key(k);
                        ax = e.getX(); ay = e.getY();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    long dt = e.getEventTime() - downTime;
                    if (!moved && dt < 400) {
                        haptic(this);
                        cb.key(maxPointers >= 2 ? Keys.BACK : Keys.CENTER);
                    }
                    return true;
            }
            return true;
        }

        @Override protected void onDraw(Canvas c) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(CARD);
            float r = dp(getContext(), 28);
            c.drawRoundRect(new RectF(0, 0, getWidth(), getHeight()), r, r, p);
            p.setStyle(Paint.Style.STROKE); p.setColor(ACC); p.setStrokeWidth(dp(getContext(), 1.5f));
            p.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(getContext(), 8), dp(getContext(), 6)}, 0));
            c.drawRoundRect(new RectF(dp(getContext(), 10), dp(getContext(), 10), getWidth() - dp(getContext(), 10), getHeight() - dp(getContext(), 10)), r, r, p);
            p.setPathEffect(null);
            p.setStyle(Paint.Style.FILL); p.setColor(MU); p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(dp(getContext(), 16));
            c.drawText("Swipe to move", getWidth() / 2f, getHeight() / 2f - dp(getContext(), 10), p);
            p.setTextSize(dp(getContext(), 13));
            c.drawText("Tap = select  ·  Two-finger tap = Back", getWidth() / 2f, getHeight() / 2f + dp(getContext(), 16), p);
        }
    }

    /** Container that turns a horizontal swipe into a page change, leaving taps for children. */
    public static final class SwipeFrame extends FrameLayout {
        public interface Callback { void swiped(boolean toNext); }
        public Callback cb;
        public boolean enabled = true;
        private float x0, y0;
        private boolean fired;
        public SwipeFrame(Context c) { super(c); }
        @Override public boolean onInterceptTouchEvent(MotionEvent e) {
            if (!enabled) return false;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: x0 = e.getX(); y0 = e.getY(); fired = false; break;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getX() - x0, dy = e.getY() - y0;
                    if (!fired && Math.abs(dx) > dp(getContext(), 60) && Math.abs(dx) > 2 * Math.abs(dy)) {
                        fired = true;
                        if (cb != null) cb.swiped(dx < 0);
                        return true;
                    }
                    break;
            }
            return false;
        }
        @Override public boolean onTouchEvent(MotionEvent e) { return fired || super.onTouchEvent(e); }
    }

    public static void gravityCenter(TextView t) { t.setGravity(Gravity.CENTER); }
    public static ViewGroup.LayoutParams match() { return new ViewGroup.LayoutParams(-1, -1); }
}
