package com.mj.tvremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements TvSession.Listener {
    private TvSession session;
    private Discovery discovery;
    private final Handler main = new Handler(Looper.getMainLooper());
    private FrameLayout holder;
    private LinearLayout tabBar;
    private View setupView, remoteView, appsView, kbView;
    private int tab = 0; // 0 remote, 1 apps, 2 keyboard
    private boolean resumed;

    // discovered TVs
    private final List<String[]> found = new ArrayList<String[]>();
    private String selectedHost;
    private boolean scanning;
    private boolean manualOpen;

    // setup widgets
    private LinearLayout tvList, step2, errorCard;
    private TextView errorTitle, errorBody, step1Sub;
    private EditText ipField, codeField;
    private TextView connectBtn;
    private LinearLayout manualBox;

    // remote widgets
    private TextView remoteTitle, remoteStatus, volLabel, hint, echo;
    private Ui.SwipeFrame swipe;
    private View padPage, keypadPage;
    private Ui.TouchPad touchPad;
    private boolean touchMode, keypadShown;
    private View dot1, dot2;
    private Ui.IconView muteIcon;
    private Switch touchSwitch;
    private TextView btn123;
    private final StringBuilder echoText = new StringBuilder();

    // apps widgets
    private LinearLayout appsGrid;
    private TextView appsStatus, appsHint;

    // keyboard widgets
    private TextView kbStatus, bannerTitle, bannerSub, autoBtn;
    private LinearLayout banner;
    private EditText kbField;
    private String kbPrev = "";
    private boolean kbGuard;
    private final TextView[] tabLabels = new TextView[3];
    private final Ui.IconView[] tabIcons = new Ui.IconView[3];
    private final View[] tabCells = new View[3];

    private int dp(float v) { return Ui.dp(this, v); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        session = new TvSession(this);
        session.setListener(this);
        discovery = new Discovery(this, new Discovery.Listener() {
            public void onFound(String name, String host) { addFound(name, host); }
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);
        holder = new FrameLayout(this);
        root.addView(holder, new LinearLayout.LayoutParams(-1, 0, 1));
        buildTabBar();
        root.addView(tabBar, new LinearLayout.LayoutParams(-1, dp(64)));
        setContentView(root);

        setupView = buildSetup();
        remoteView = buildRemote();
        appsView = buildApps();
        kbView = buildKeyboard();
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        session.resume();
        if (session.state != TvSession.State.CONNECTED) startScan();
        refresh();
    }

    @Override protected void onPause() {
        super.onPause();
        resumed = false;
        discovery.stop();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        session.setListener(null);
        session.close();
    }

    // ---------------------------------------------------------------- state

    @Override public void onStateChanged() {
        if (session.state == TvSession.State.CONNECTED) discovery.stop();
        refresh();
    }

    @Override public void onVolume() { updateVolume(); }

    @Override public void onImeFocus(String pkg) {
        updateKeyboardBanner();
        if (session.prefs().getBoolean("autoopen", true) && resumed) {
            kbGuard = true; kbField.setText(""); kbGuard = false; kbPrev = "";
            tab = 2;
            refresh();
            kbField.requestFocus();
            main.postDelayed(new Runnable() { public void run() { showKeyboard(); } }, 250);
        }
    }

    private void showKeyboard() {
        InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        kbField.requestFocus();
        m.showSoftInput(kbField, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        m.hideSoftInputFromWindow(kbField.getWindowToken(), 0);
    }

    private boolean connected() { return session.state == TvSession.State.CONNECTED; }

    private void refresh() {
        View want = !connected() ? setupView : tab == 0 ? remoteView : tab == 1 ? appsView : kbView;
        if (holder.getChildCount() == 0 || holder.getChildAt(0) != want) {
            holder.removeAllViews();
            holder.addView(want, new FrameLayout.LayoutParams(-1, -1));
            if (want != kbView) hideKeyboard();
        }
        for (int i = 0; i < 3; i++) {
            boolean sel = connected() && tab == i;
            int col = sel ? Ui.ACC : Ui.MU;
            tabIcons[i].set(new int[]{Ui.I_REMOTE, Ui.I_GRID, Ui.I_KEYBOARD}[i], col);
            tabLabels[i].setTextColor(col);
            tabLabels[i].setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            tabCells[i].setAlpha(connected() ? 1f : 0.35f);
        }
        if (!connected()) refreshSetup(); else { updateHeaders(); updateVolume(); updateKeyboardBanner(); }
    }

    private String tvLabel() { return session.tvName != null ? session.tvName : (session.host != null ? session.host : "TV"); }

    // ---------------------------------------------------------------- tab bar

    private void buildTabBar() {
        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(Ui.BG);
        String[] names = {"Remote", "Apps", "Keyboard"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            Ui.IconView ic = new Ui.IconView(this, Ui.I_REMOTE, Ui.MU);
            cell.addView(ic, new LinearLayout.LayoutParams(dp(26), dp(26)));
            TextView t = Ui.text(this, names[i], 12, Ui.MU, false);
            t.setGravity(Gravity.CENTER);
            cell.addView(t);
            cell.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
                if (!connected()) return;
                tab = idx; refresh();
                if (idx == 2) showKeyboard();
            } });
            tabIcons[i] = ic; tabLabels[i] = t; tabCells[i] = cell;
            tabBar.addView(cell, new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    // ---------------------------------------------------------------- header helper

    private LinearLayout statusRow(TextView[] out, boolean withSwitch) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(this);
        dot.setBackground(Ui.round(Ui.GRN, dp(5)));
        r.addView(dot, new LinearLayout.LayoutParams(dp(10), dp(10)));
        TextView t = Ui.text(this, "", 14, Ui.GRN, false);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = dp(8);
        r.addView(t, lp);
        out[0] = t;
        if (withSwitch) {
            TextView sw = Ui.text(this, "Switch", 13, Ui.TX, false);
            sw.setPadding(dp(14), dp(3), dp(14), dp(3));
            sw.setBackground(Ui.outlined(Ui.BG, Ui.MU, dp(1), dp(14)));
            sw.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { session.disconnect(); startScan(); } });
            LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(-2, -2);
            l2.leftMargin = dp(12);
            r.addView(sw, l2);
        }
        return r;
    }

    private void updateHeaders() {
        remoteTitle.setText(tvLabel());
        appsStatus.setText(tvLabel());
        kbStatus.setText(tvLabel());
    }

    // ---------------------------------------------------------------- setup screen

    private View buildSetup() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(20), dp(20), dp(20), dp(20));
        sv.addView(c, new ViewGroup.LayoutParams(-1, -2));

        c.addView(Ui.text(this, "Add your TV", 28, Ui.TX, true));
        step1Sub = Ui.text(this, "Step 1 of 2 · Choose your TV", 14, Ui.MU, false);
        c.addView(step1Sub);

        tvList = new LinearLayout(this);
        tvList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2);
        l.topMargin = dp(16);
        c.addView(tvList, l);

        LinearLayout row = new LinearLayout(this);
        TextView scan = pill("Scan again", Ui.ACC, Ui.ACC_DIM, 0);
        scan.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { startScan(); } });
        TextView notListed = pill("TV not listed?", Ui.TX, Ui.BG, Ui.MU);
        notListed.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            manualOpen = true; refreshSetup(); ipField.requestFocus();
        } });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(44), 1);
        LinearLayout.LayoutParams bb = new LinearLayout.LayoutParams(0, dp(44), 1);
        bb.leftMargin = dp(10);
        row.addView(scan, a); row.addView(notListed, bb);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
        rl.topMargin = dp(12);
        c.addView(row, rl);

        manualBox = new LinearLayout(this);
        manualBox.setOrientation(LinearLayout.VERTICAL);
        ipField = field("TV IP address, e.g. 192.168.1.24");
        ipField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        manualBox.addView(ipField, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView ipHelp = Ui.text(this, "On the TV: Settings > Network & Internet shows its IP address. Phone and TV must be on the same Wi-Fi.", 12, Ui.MU, false);
        ipHelp.setPadding(0, dp(6), 0, 0);
        manualBox.addView(ipHelp);
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(-1, -2);
        ml.topMargin = dp(12);
        c.addView(manualBox, ml);

        step2 = new LinearLayout(this);
        step2.setOrientation(LinearLayout.VERTICAL);
        TextView s2 = Ui.text(this, "Step 2 of 2 · Enter the code on your TV", 15, Ui.TX, true);
        step2.addView(s2);
        FrameLayout codeWrap = new FrameLayout(this);
        codeField = field("");
        codeField.setTextSize(24);
        codeField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        codeField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6), new InputFilter.AllCaps()});
        codeField.setLetterSpacing(0.15f);
        codeWrap.addView(codeField, new FrameLayout.LayoutParams(-1, dp(60)));
        TextView paste = Ui.text(this, "Paste", 14, Ui.TX, true);
        paste.setGravity(Gravity.CENTER);
        paste.setBackground(Ui.round(Ui.ACC_DIM, dp(18)));
        paste.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            String s = clipboardText();
            if (s != null) codeField.setText(s.replaceAll("[^0-9A-Fa-f]", "").toUpperCase());
        } });
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(dp(80), dp(36), Gravity.END | Gravity.CENTER_VERTICAL);
        pl.rightMargin = dp(12);
        codeWrap.addView(paste, pl);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-1, -2);
        cl.topMargin = dp(10);
        step2.addView(codeWrap, cl);
        TextView codeHelp = Ui.text(this, "6 characters (0-9, A-F), shown on the TV screen. Pasting works.", 12, Ui.MU, false);
        codeHelp.setPadding(0, dp(6), 0, 0);
        step2.addView(codeHelp);
        LinearLayout.LayoutParams s2l = new LinearLayout.LayoutParams(-1, -2);
        s2l.topMargin = dp(20);
        c.addView(step2, s2l);

        connectBtn = Ui.text(this, "Connect", 18, Ui.DK, true);
        connectBtn.setGravity(Gravity.CENTER);
        connectBtn.setBackground(Ui.pressable(Ui.ACC, 0xFF8CAAFF, dp(28)));
        connectBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { onConnectTapped(); } });
        LinearLayout.LayoutParams cbl = new LinearLayout.LayoutParams(-1, dp(56));
        cbl.topMargin = dp(20);
        c.addView(connectBtn, cbl);

        errorCard = new LinearLayout(this);
        errorCard.setOrientation(LinearLayout.VERTICAL);
        errorCard.setBackground(Ui.round(Ui.RED_BG, dp(16)));
        errorCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        errorTitle = Ui.text(this, "", 15, Ui.RED, true);
        errorBody = Ui.text(this, "", 13, Ui.TX, false);
        errorCard.addView(errorTitle); errorCard.addView(errorBody);
        LinearLayout.LayoutParams el = new LinearLayout.LayoutParams(-1, -2);
        el.topMargin = dp(16);
        c.addView(errorCard, el);

        View spacer = new View(this);
        c.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1));
        TextView foot = Ui.text(this, "Remote, Apps and Keyboard unlock once a TV is connected.", 12, Ui.MU, false);
        foot.setPadding(0, dp(24), 0, 0);
        c.addView(foot);
        return sv;
    }

    private TextView pill(String s, int textColor, int fill, int stroke) {
        TextView t = Ui.text(this, s, 14, textColor, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(stroke != 0 ? Ui.outlined(fill, stroke, dp(1), dp(22)) : Ui.round(fill, dp(22)));
        return t;
    }

    private EditText field(String hintText) {
        EditText e = new EditText(this);
        e.setTextColor(Ui.TX);
        e.setHintTextColor(0xFF6F7A96);
        e.setHint(hintText);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setPadding(dp(16), 0, dp(16), 0);
        e.setBackground(Ui.outlined(Ui.CARD, Ui.ACC, dp(2), dp(18)));
        return e;
    }

    private String clipboardText() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData d = cm.getPrimaryClip();
            if (d != null && d.getItemCount() > 0 && d.getItemAt(0).getText() != null) return d.getItemAt(0).getText().toString();
        } catch (Exception ignored) {}
        return null;
    }

    private void startScan() {
        found.clear();
        scanning = true;
        discovery.start();
        main.postDelayed(new Runnable() { public void run() { scanning = false; if (!connected()) refreshSetup(); } }, 8000);
        refreshSetup();
    }

    private void addFound(String name, String host) {
        for (String[] f : found) if (f[1].equals(host)) return;
        found.add(new String[]{name, host});
        if (selectedHost == null) selectedHost = host;
        refreshSetup();
    }

    private void onConnectTapped() {
        TvSession.State s = session.state;
        if (s == TvSession.State.NEED_CODE) {
            String code = codeField.getText().toString().trim();
            if (code.length() != 6) { Toast.makeText(this, "Enter the 6-character code from the TV", Toast.LENGTH_SHORT).show(); return; }
            session.submitCode(code);
            return;
        }
        if (s == TvSession.State.CONNECTING || s == TvSession.State.PAIRING) return;
        String host = null, name = null;
        String typed = ipField.getText().toString().trim();
        if (manualOpen && !typed.isEmpty()) { host = typed; name = typed; }
        else if (selectedHost != null) {
            host = selectedHost;
            for (String[] f : found) if (f[1].equals(host)) name = f[0];
        } else if (session.hasSavedTv() && !manualOpen) { host = session.host; name = session.tvName; }
        if (host == null) { Toast.makeText(this, "Pick a TV or enter its IP address first", Toast.LENGTH_SHORT).show(); return; }
        codeField.setText("");
        session.connect(host, name == null ? host : name);
    }

    private void refreshSetup() {
        TvSession.State s = session.state;
        boolean needCode = s == TvSession.State.NEED_CODE || s == TvSession.State.PAIRING;
        // TV list
        tvList.removeAllViews();
        List<String[]> shown = new ArrayList<String[]>(found);
        if (session.hasSavedTv()) {
            boolean has = false;
            for (String[] f : shown) if (f[1].equals(session.host)) has = true;
            if (!has) shown.add(0, new String[]{session.tvName != null ? session.tvName : session.host, session.host});
            if (selectedHost == null) selectedHost = session.host;
        }
        if (shown.isEmpty()) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Ui.round(Ui.CARD, dp(18)));
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            if (scanning) {
                card.addView(Ui.text(this, "Looking for TVs on your Wi-Fi...", 16, Ui.TX, true));
                card.addView(Ui.text(this, "This takes a few seconds.", 13, Ui.MU, false));
            } else {
                card.addView(Ui.text(this, "No TV found", 16, Ui.TX, true));
                TextView t = Ui.text(this, "Check that the TV is on, on the same Wi-Fi as this phone, and has the Android TV Remote service enabled. Then Scan again, or enter its IP address with \"TV not listed?\".", 13, Ui.MU, false);
                card.addView(t);
            }
            tvList.addView(card);
        }
        for (final String[] f : shown) {
            boolean sel = f[1].equals(selectedHost);
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(dp(14), dp(12), dp(14), dp(12));
            r.setBackground(sel ? Ui.outlined(0xFF1B2748, Ui.ACC, dp(2), dp(18)) : Ui.round(Ui.CARD, dp(18)));
            r.addView(new Ui.IconView(this, Ui.I_TV, Ui.MU), new LinearLayout.LayoutParams(dp(30), dp(30)));
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(dp(14), 0, 0, 0);
            TextView n = Ui.text(this, f[0], 16, Ui.TX, true);
            n.setSingleLine(true); n.setEllipsize(android.text.TextUtils.TruncateAt.END);
            col.addView(n);
            col.addView(Ui.text(this, f[1], 13, Ui.MU, false));
            r.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
            if (sel) {
                FrameLayout chk = new FrameLayout(this);
                chk.setBackground(Ui.round(Ui.ACC, dp(14)));
                chk.addView(new Ui.IconView(this, Ui.I_CHECK, Ui.DK), new FrameLayout.LayoutParams(dp(28), dp(28)));
                r.addView(chk, new LinearLayout.LayoutParams(dp(28), dp(28)));
            }
            r.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { selectedHost = f[1]; manualOpen = false; refreshSetup(); } });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(10);
            tvList.addView(r, lp);
        }
        manualBox.setVisibility(manualOpen ? View.VISIBLE : View.GONE);
        step2.setVisibility(needCode ? View.VISIBLE : View.GONE);
        step1Sub.setText(needCode ? "Step 1 of 2 · TV selected" : "Step 1 of 2 · Choose your TV");
        codeField.setEnabled(s == TvSession.State.NEED_CODE);
        if (s == TvSession.State.CONNECTING) { connectBtn.setText("Connecting..."); connectBtn.setAlpha(0.6f); }
        else if (s == TvSession.State.PAIRING) { connectBtn.setText("Pairing..."); connectBtn.setAlpha(0.6f); }
        else { connectBtn.setText(s == TvSession.State.NEED_CODE ? "Pair" : "Connect"); connectBtn.setAlpha(1f); }
        if (session.error != null) {
            errorCard.setVisibility(View.VISIBLE);
            boolean wrong = session.error.startsWith("Wrong code");
            errorTitle.setText(wrong ? "Wrong code" : "Couldn't connect");
            errorBody.setText(wrong ? "Check the code on the TV and try again." : session.error);
        } else errorCard.setVisibility(View.GONE);
    }

    // ---------------------------------------------------------------- remote screen

    private View buildRemote() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(8));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        remoteTitle = Ui.text(this, "", 22, Ui.TX, true);
        remoteTitle.setSingleLine(true);
        remoteTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        left.addView(remoteTitle);
        TextView[] o = new TextView[1];
        LinearLayout sr = statusRow(o, true);
        o[0].setText("Connected");
        remoteStatus = o[0];
        left.addView(sr);
        top.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        FrameLayout power = new FrameLayout(this);
        power.setBackground(Ui.pressable(Ui.RED_BG, 0xFF4A2530, dp(22)));
        power.addView(new Ui.IconView(this, Ui.I_POWER, Ui.RED), new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        Ui.button(power, new Runnable() { public void run() { session.key(Keys.POWER); } }, false);
        top.addView(power, new LinearLayout.LayoutParams(dp(44), dp(44)));
        c.addView(top, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        row2.addView(Ui.text(this, "Touch mode", 14, Ui.TX, false));
        touchSwitch = new Switch(this);
        touchSwitch.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) { touchMode = on; layoutPages(); }
        });
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-2, -2);
        sl.leftMargin = dp(10);
        row2.addView(touchSwitch, sl);
        row2.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1));
        btn123 = Ui.text(this, "123", 15, Ui.TX, true);
        btn123.setGravity(Gravity.CENTER);
        btn123.setBackground(Ui.pressable(Ui.ACC_DIM, 0xFF3A4D8F, dp(18)));
        btn123.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { if (!touchMode) { keypadShown = !keypadShown; layoutPages(); } } });
        row2.addView(btn123, new LinearLayout.LayoutParams(dp(90), dp(36)));
        LinearLayout.LayoutParams r2 = new LinearLayout.LayoutParams(-1, -2);
        r2.topMargin = dp(10);
        c.addView(row2, r2);

        swipe = new Ui.SwipeFrame(this);
        swipe.cb = new Ui.SwipeFrame.Callback() { public void swiped(boolean next) { if (!touchMode) { keypadShown = next; layoutPages(); } } };
        FrameLayout padHolder = new FrameLayout(this);
        Ui.DPadView dpad = new Ui.DPadView(this, new Ui.DPadView.Callback() { public void key(int k) { session.key(k); } });
        FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(dp(260), dp(260), Gravity.CENTER);
        padHolder.addView(dpad, dl);
        padPage = padHolder;
        keypadPage = buildKeypad();
        touchPad = new Ui.TouchPad(this, new Ui.TouchPad.Callback() { public void key(int k) { session.key(k); } });
        swipe.addView(padPage, new FrameLayout.LayoutParams(-1, -1));
        swipe.addView(keypadPage, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(-1, -1);
        tl.topMargin = dp(8); tl.bottomMargin = dp(8);
        swipe.addView(touchPad, tl);
        c.addView(swipe, new LinearLayout.LayoutParams(-1, 0, 1));

        hint = Ui.text(this, "", 12, Ui.MU, false);
        hint.setGravity(Gravity.CENTER);
        c.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        dot1 = new View(this); dot2 = new View(this);
        dots.addView(dot1, dotLp()); dots.addView(dot2, dotLp());
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, dp(14));
        c.addView(dots, dlp);

        // persistent bottom block
        LinearLayout nav = new LinearLayout(this);
        int[] icons = {Ui.I_BACK, Ui.I_HOME, Ui.I_MENU};
        final int[] keys = {Keys.BACK, Keys.HOME, Keys.MENU};
        for (int i = 0; i < 3; i++) {
            final int k = keys[i];
            FrameLayout b = new FrameLayout(this);
            b.setBackground(Ui.pressable(Ui.CARD, Ui.CARD_PRESSED, dp(24)));
            b.addView(new Ui.IconView(this, icons[i], Ui.TX), new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
            Ui.button(b, new Runnable() { public void run() { session.key(k); } }, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
            if (i > 0) lp.leftMargin = dp(10);
            nav.addView(b, lp);
        }
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-1, -2);
        nl.topMargin = dp(8);
        c.addView(nav, nl);

        LinearLayout vols = new LinearLayout(this);
        vols.setGravity(Gravity.CENTER_VERTICAL);
        vols.addView(pillControl("VOL", Keys.VOL_UP, Keys.VOL_DOWN), new LinearLayout.LayoutParams(dp(78), dp(140)));
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setGravity(Gravity.CENTER);
        FrameLayout mute = new FrameLayout(this);
        mute.setBackground(Ui.pressable(Ui.CARD, Ui.CARD_PRESSED, dp(32)));
        muteIcon = new Ui.IconView(this, Ui.I_UNMUTE, Ui.TX);
        mute.addView(muteIcon, new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
        Ui.button(mute, new Runnable() { public void run() { session.key(Keys.MUTE); } }, false);
        mid.addView(mute, new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView mt = Ui.text(this, "Mute", 12, Ui.MU, false);
        mid.addView(mt);
        volLabel = Ui.text(this, "", 12, Ui.MU, false);
        LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(-2, -2);
        vl.topMargin = dp(6);
        mid.addView(volLabel, vl);
        vols.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));
        vols.addView(pillControl("CH", Keys.CH_UP, Keys.CH_DOWN), new LinearLayout.LayoutParams(dp(78), dp(140)));
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-1, -2);
        vlp.topMargin = dp(8);
        c.addView(vols, vlp);
        layoutPages();
        return c;
    }

    private LinearLayout.LayoutParams dotLp() {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(dp(7), dp(7));
        l.leftMargin = dp(4); l.rightMargin = dp(4);
        return l;
    }

    private View pillControl(String label, final int up, final int down) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setGravity(Gravity.CENTER_HORIZONTAL);
        p.setBackground(Ui.round(Ui.CARD, dp(36)));
        p.setPadding(0, dp(8), 0, dp(8));
        TextView l1 = Ui.text(this, label, 12, Ui.MU, true);
        l1.setGravity(Gravity.CENTER);
        p.addView(l1, new LinearLayout.LayoutParams(-1, -2));
        View plus = halfButton(Ui.I_PLUS, up);
        View minus = halfButton(Ui.I_MINUS, down);
        p.addView(plus, new LinearLayout.LayoutParams(-1, 0, 1));
        p.addView(minus, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView l2 = Ui.text(this, label, 12, Ui.MU, true);
        l2.setGravity(Gravity.CENTER);
        p.addView(l2, new LinearLayout.LayoutParams(-1, -2));
        return p;
    }

    private View halfButton(int icon, final int key) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(Ui.pressable(Color.TRANSPARENT, Ui.CARD_PRESSED, dp(20)));
        f.addView(new Ui.IconView(this, icon, Ui.TX), new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        Ui.button(f, new Runnable() { public void run() { session.key(key); } }, true);
        return f;
    }

    private View buildKeypad() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(8), dp(4), dp(8), dp(4));
        echo = Ui.text(this, " ", 26, Ui.TX, true);
        echo.setGravity(Gravity.CENTER);
        c.addView(echo, new LinearLayout.LayoutParams(-1, dp(36)));
        String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"DEL", "0", "OK"}};
        for (String[] r : rows) {
            LinearLayout row = new LinearLayout(this);
            for (int i = 0; i < 3; i++) {
                final String k = r[i];
                View key;
                if (k.equals("DEL")) {
                    FrameLayout f = new FrameLayout(this);
                    f.addView(new Ui.IconView(this, Ui.I_BACKSPACE, Ui.TX), new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
                    key = f;
                } else {
                    TextView t = Ui.text(this, k, k.equals("OK") ? 20 : 28, k.equals("OK") ? Ui.DK : Ui.TX, true);
                    t.setGravity(Gravity.CENTER);
                    key = t;
                }
                key.setBackground(Ui.pressable(k.equals("OK") ? Ui.ACC : Ui.CARD, k.equals("OK") ? 0xFF8CAAFF : Ui.CARD_PRESSED, dp(22)));
                Ui.button(key, new Runnable() { public void run() { keypadPress(k); } }, k.equals("DEL"));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
                lp.leftMargin = dp(5); lp.rightMargin = dp(5);
                row.addView(key, lp);
            }
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, 0, 1);
            rl.topMargin = dp(5); rl.bottomMargin = dp(5);
            c.addView(row, rl);
        }
        return c;
    }

    private void keypadPress(String k) {
        if (k.equals("DEL")) {
            session.key(Keys.DEL);
            if (echoText.length() > 0) echoText.setLength(echoText.length() - 1);
        } else if (k.equals("OK")) {
            session.key(Keys.CENTER);
            echoText.setLength(0);
        } else {
            session.key(Keys.digit(k.charAt(0) - '0'));
            echoText.append(k);
        }
        echo.setText(echoText.length() == 0 ? " " : echoText.toString());
    }

    private void layoutPages() {
        boolean showTouch = touchMode;
        touchPad.setVisibility(showTouch ? View.VISIBLE : View.GONE);
        padPage.setVisibility(!showTouch && !keypadShown ? View.VISIBLE : View.GONE);
        keypadPage.setVisibility(!showTouch && keypadShown ? View.VISIBLE : View.GONE);
        swipe.enabled = !showTouch;
        btn123.setAlpha(showTouch ? 0.35f : 1f);
        btn123.setText(keypadShown ? "D-pad" : "123");
        hint.setText(showTouch ? "Touch mode: swipe on the pad to move" : "Tap 123 or swipe the page for the keypad");
        dot1.setBackground(Ui.round(!keypadShown && !showTouch ? Ui.ACC : 0xFF3A4260, dp(4)));
        dot2.setBackground(Ui.round(keypadShown && !showTouch ? Ui.ACC : 0xFF3A4260, dp(4)));
        dot1.setVisibility(showTouch ? View.INVISIBLE : View.VISIBLE);
        dot2.setVisibility(showTouch ? View.INVISIBLE : View.VISIBLE);
    }

    private void updateVolume() {
        if (volLabel == null) return;
        volLabel.setText(session.volume >= 0 ? "Volume " + session.volume : "");
        muteIcon.set(session.muted ? Ui.I_MUTE : Ui.I_UNMUTE, session.muted ? Ui.RED : Ui.TX);
    }

    // ---------------------------------------------------------------- apps screen

    private View buildApps() {
        ScrollView sv = new ScrollView(this);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        sv.addView(c, new ViewGroup.LayoutParams(-1, -2));
        c.addView(Ui.text(this, "Apps", 28, Ui.TX, true));
        TextView[] o = new TextView[1];
        c.addView(statusRow(o, false));
        appsStatus = o[0];
        appsGrid = new LinearLayout(this);
        appsGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(-1, -2);
        gl.topMargin = dp(12);
        c.addView(appsGrid, gl);
        appsHint = Ui.text(this, "Tap an app to open it on the TV. If nothing opens, the app may not be installed, or its package name differs on your TV. Long-press a tile to edit it.", 12, Ui.MU, false);
        appsHint.setPadding(0, dp(8), 0, dp(8));
        c.addView(appsHint);
        rebuildApps();
        return sv;
    }

    private List<AppStore.Entry> customApps() { return AppStore.parse(session.prefs().getString("custom", "")); }

    private String pkgFor(int i) { return session.prefs().getString("pkg" + i, BuiltinApps.PACKAGES[i]); }

    private void rebuildApps() {
        appsGrid.removeAllViews();
        List<View> tiles = new ArrayList<View>();
        for (int i = 0; i < BuiltinApps.NAMES.length; i++) {
            final int idx = i;
            int logoRes = getResources().getIdentifier(BuiltinApps.LOGOS[i], "drawable", getPackageName());
            View t = tile(BuiltinApps.NAMES[i], logoRes, i == 3 ? 0xFF000000 : 0xFFFFFFFF, 0, false);
            final View tt = t;
            final String name = BuiltinApps.NAMES[i];
            t.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { openApp(tt, "market://launch?id=" + pkgFor(idx)); } });
            t.setOnLongClickListener(new View.OnLongClickListener() { public boolean onLongClick(View v) { editBuiltin(idx); return true; } });
            tiles.add(t);
        }
        final List<AppStore.Entry> custom = customApps();
        for (int i = 0; i < custom.size(); i++) {
            final AppStore.Entry e = custom.get(i);
            final int ci = i;
            final View t = tile(e.name, 0, 0xFF28376B, e.isLink ? Ui.I_LINK : Ui.I_GRID, false);
            t.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { openApp(t, e.launchLink()); } });
            t.setOnLongClickListener(new View.OnLongClickListener() { public boolean onLongClick(View v) { confirmDelete(ci, e); return true; } });
            tiles.add(t);
        }
        // add tile
        LinearLayout add = new LinearLayout(this);
        add.setOrientation(LinearLayout.VERTICAL);
        add.setGravity(Gravity.CENTER);
        add.setBackground(Ui.outlined(0xFF141828, Ui.ACC, dp(2), dp(22)));
        FrameLayout plus = new FrameLayout(this);
        plus.setBackground(Ui.round(0xFF28376B, dp(32)));
        plus.addView(new Ui.IconView(this, Ui.I_PLUS, Ui.TX), new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
        add.addView(plus, new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView at = Ui.text(this, "Add custom app", 14, Ui.ACC, true);
        at.setGravity(Gravity.CENTER);
        at.setPadding(0, dp(10), 0, 0);
        add.addView(at);
        add.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { addDialog(); } });
        tiles.add(add);
        for (int i = 0; i < tiles.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            for (int j = 0; j < 2; j++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(160), 1);
                lp.leftMargin = j == 1 ? dp(6) : 0; lp.rightMargin = j == 0 ? dp(6) : 0;
                if (i + j < tiles.size()) row.addView(tiles.get(i + j), lp);
                else row.addView(new View(this), lp);
            }
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
            rl.bottomMargin = dp(12);
            appsGrid.addView(row, rl);
        }
    }

    /** A grid tile: logo (or icon) on a colored plate with the name below. Tag holds [label, bar]. */
    private View tile(String name, int logoRes, int plateColor, int iconType, boolean unused) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setBackground(Ui.pressable(Ui.CARD, Ui.CARD_PRESSED, dp(22)));
        t.setPadding(dp(10), dp(10), dp(10), dp(8));
        FrameLayout plate = new FrameLayout(this);
        plate.setBackground(Ui.round(plateColor, dp(14)));
        if (logoRes != 0) {
            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setImageResource(logoRes);
            iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            iv.setPadding(dp(16), dp(10), dp(16), dp(10));
            plate.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            plate.addView(new Ui.IconView(this, iconType, Ui.TX), new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER));
        }
        t.addView(plate, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView label = Ui.text(this, name, 14, Ui.TX, true);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(0, dp(8), 0, 0);
        t.addView(label, new LinearLayout.LayoutParams(-1, -2));
        t.setTag(new Object[]{label, name});
        return t;
    }

    private void openApp(final View tile, String link) {
        if (!connected()) return;
        Ui.haptic(tile);
        session.launch(link);
        Object[] tag = (Object[]) tile.getTag();
        final TextView label = (TextView) tag[0];
        final String name = (String) tag[1];
        label.setText("Opening...");
        label.setTextColor(Ui.ACC);
        main.postDelayed(new Runnable() { public void run() { label.setText(name); label.setTextColor(Ui.TX); } }, 2500);
    }

    private void editBuiltin(final int idx) {
        final EditText f = new EditText(this);
        f.setText(pkgFor(idx));
        f.setTextColor(Ui.TX);
        f.setSingleLine(true);
        f.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(f, new LinearLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(this).setTitle(BuiltinApps.NAMES[idx] + " package name")
                .setMessage("The TV opens apps by package name. If this app doesn't open, set the right one.")
                .setView(box)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int w) {
                    String s = f.getText().toString().trim();
                    if (AppStore.validPackage(s)) session.prefs().edit().putString("pkg" + idx, s).apply();
                    else Toast.makeText(MainActivity.this, "That isn't a valid package name", Toast.LENGTH_SHORT).show();
                } })
                .setNeutralButton("Reset", new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int w) {
                    session.prefs().edit().remove("pkg" + idx).apply();
                } })
                .setNegativeButton("Cancel", null).show();
    }

    private void confirmDelete(final int index, AppStore.Entry e) {
        new AlertDialog.Builder(this).setTitle("Remove " + e.name + "?")
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int w) {
                    List<AppStore.Entry> l = customApps();
                    if (index < l.size()) l.remove(index);
                    session.prefs().edit().putString("custom", AppStore.serialize(l)).apply();
                    rebuildApps();
                } })
                .setNegativeButton("Cancel", null).show();
    }

    private boolean addIsLink;

    private void addDialog() {
        addIsLink = false;
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(4));
        LinearLayout seg = new LinearLayout(this);
        final TextView tApp = pill("App", Ui.DK, Ui.ACC, 0), tLink = pill("Link", Ui.TX, Ui.CARD_PRESSED, 0);
        seg.addView(tApp, new LinearLayout.LayoutParams(0, dp(40), 1));
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(0, dp(40), 1);
        ll.leftMargin = dp(8);
        seg.addView(tLink, ll);
        box.addView(seg, new LinearLayout.LayoutParams(-1, -2));
        final EditText name = field("Name, e.g. YouTube");
        name.setBackground(Ui.outlined(Ui.BG, 0xFF3A4260, dp(1), dp(14)));
        final EditText val = field("Package name, e.g. com.google.android.youtube.tv");
        val.setBackground(Ui.outlined(Ui.BG, 0xFF3A4260, dp(1), dp(14)));
        val.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-1, dp(48));
        nl.topMargin = dp(14);
        box.addView(name, nl);
        LinearLayout vrow = new LinearLayout(this);
        vrow.addView(val, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView paste = pill("Paste", Ui.TX, Ui.ACC_DIM, 0);
        paste.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            String s = clipboardText();
            if (s != null) val.setText(s.trim());
        } });
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(dp(70), dp(48));
        pl.leftMargin = dp(8);
        vrow.addView(paste, pl);
        LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(-1, -2);
        vl.topMargin = dp(10);
        box.addView(vrow, vl);
        final TextView help = Ui.text(this, "App: the Android package name of an app installed on the TV.", 12, Ui.MU, false);
        help.setPadding(0, dp(8), 0, 0);
        box.addView(help);
        View.OnClickListener sw = new View.OnClickListener() { public void onClick(View v) {
            addIsLink = v == tLink;
            tApp.setBackground(Ui.round(addIsLink ? Ui.CARD_PRESSED : Ui.ACC, dp(20))); tApp.setTextColor(addIsLink ? Ui.TX : Ui.DK);
            tLink.setBackground(Ui.round(addIsLink ? Ui.ACC : Ui.CARD_PRESSED, dp(20))); tLink.setTextColor(addIsLink ? Ui.DK : Ui.TX);
            val.setHint(addIsLink ? "Link, e.g. https://www.youtube.com/watch?v=..." : "Package name, e.g. com.google.android.youtube.tv");
            help.setText(addIsLink ? "Link: a web or app link the TV opens in the app that handles it." : "App: the Android package name of an app installed on the TV.");
        } };
        tApp.setOnClickListener(sw); tLink.setOnClickListener(sw);
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Add custom").setView(box)
                .setPositiveButton("Add", null).setNegativeButton("Cancel", null).create();
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            String n = name.getText().toString().trim();
            String raw = val.getText().toString().trim();
            String v2 = addIsLink ? AppStore.normalizeLink(raw) : (AppStore.validPackage(raw) ? raw : null);
            if (n.isEmpty()) { Toast.makeText(MainActivity.this, "Give it a name", Toast.LENGTH_SHORT).show(); return; }
            if (v2 == null) { Toast.makeText(MainActivity.this, addIsLink ? "That doesn't look like a link" : "That isn't a valid package name", Toast.LENGTH_SHORT).show(); return; }
            List<AppStore.Entry> l = customApps();
            l.add(new AppStore.Entry(n, v2, addIsLink));
            session.prefs().edit().putString("custom", AppStore.serialize(l)).apply();
            rebuildApps();
            dlg.dismiss();
        } });
    }

    // ---------------------------------------------------------------- keyboard screen

    private View buildKeyboard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(8));
        c.addView(Ui.text(this, "Type on TV", 28, Ui.TX, true));
        TextView[] o = new TextView[1];
        c.addView(statusRow(o, false));
        kbStatus = o[0];

        banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(dp(16), dp(12), dp(16), dp(12));
        bannerTitle = Ui.text(this, "", 16, Ui.GRN, true);
        bannerSub = Ui.text(this, "", 13, Ui.TX, false);
        banner.addView(bannerTitle); banner.addView(bannerSub);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, -2);
        bl.topMargin = dp(12);
        c.addView(banner, bl);

        kbField = field("Type here");
        kbField.setTextSize(20);
        kbField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        kbField.setImeOptions(EditorInfo.IME_ACTION_SEND);
        kbField.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int d) {}
            public void afterTextChanged(Editable e) {
                if (kbGuard) return;
                String now = e.toString();
                TextDiff diff = TextDiff.between(kbPrev, now);
                for (int i = 0; i < diff.deletions; i++) session.key(Keys.DEL);
                if (!diff.insertion.isEmpty()) session.text(diff.insertion);
                kbPrev = now;
            }
        });
        kbField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent ev) { session.key(Keys.ENTER); return true; }
        });
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(-1, dp(60));
        fl.topMargin = dp(12);
        c.addView(kbField, fl);

        LinearLayout row = new LinearLayout(this);
        autoBtn = pill("", Ui.TX, Ui.ACC_DIM, 0);
        autoBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            boolean on = !session.prefs().getBoolean("autoopen", true);
            session.prefs().edit().putBoolean("autoopen", on).apply();
            updateAutoBtn();
        } });
        TextView hide = pill("Hide keyboard", Ui.TX, Ui.BG, Ui.MU);
        hide.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { hideKeyboard(); } });
        TextView send = pill("Send", Ui.DK, Ui.ACC, 0);
        send.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { session.key(Keys.ENTER); } });
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(40), 1.1f), p2 = new LinearLayout.LayoutParams(0, dp(40), 1.2f), p3 = new LinearLayout.LayoutParams(0, dp(40), 0.8f);
        p2.leftMargin = dp(8); p3.leftMargin = dp(8);
        row.addView(autoBtn, p1); row.addView(hide, p2); row.addView(send, p3);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
        rl.topMargin = dp(12);
        c.addView(row, rl);
        TextView note = Ui.text(this, "Text you type is sent live. Backspace works too. Send presses Enter on the TV.", 12, Ui.MU, false);
        note.setPadding(0, dp(10), 0, 0);
        c.addView(note);
        updateAutoBtn();
        return c;
    }

    private void updateAutoBtn() {
        boolean on = session.prefs().getBoolean("autoopen", true);
        autoBtn.setText("Auto-open: " + (on ? "ON" : "OFF"));
        autoBtn.setAlpha(on ? 1f : 0.6f);
    }

    private void updateKeyboardBanner() {
        if (banner == null) return;
        if (session.imeActive) {
            banner.setBackground(Ui.round(Ui.GRN_BG, dp(16)));
            bannerTitle.setText("Typing to: " + (session.imePkg == null ? "TV text field" : session.imePkg));
            bannerTitle.setTextColor(Ui.GRN);
            bannerSub.setText("Sends live as you type");
        } else {
            banner.setBackground(Ui.round(Ui.CARD, dp(16)));
            bannerTitle.setText("No text field open on the TV");
            bannerTitle.setTextColor(Ui.MU);
            bannerSub.setText("Open a search box on the TV and this opens by itself. You can still type here and it is sent as keys.");
        }
    }
}
