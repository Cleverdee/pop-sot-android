package com.cleverdee.pop3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/* The start screen: play, or manage the game files. If the game crashed last time, a crash-report screen
   comes first. */
public class MenuActivity extends Activity {
    private static final String GAME = "Prince of Persia: The Two Thrones";

    private LinearLayout mRoot;
    private boolean mMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoot = Ui.page(this);
        File crash = new File(getExternalFilesDir(null), "crash.txt");
        if (crash.isFile()) showCrash(crash, this::showMain);
        else showMain();
    }

    /* The game crashed last time (host/window_android.c or GameActivity wrote crash.txt): offer to share a report,
       the crash and the end of the game's log, through any app the player likes. */
    private void showCrash(File crash, Runnable next) {
        mMain = false;
        Ui.clear(mRoot);
        Ui.heading(mRoot, "The game closed unexpectedly");
        text("Help get it fixed: share or copy the crash report and send it to the developer any way you like, "
           + "on Reddit, GitHub or in a message. It has your phone model, Android version and the game's log; "
           + "nothing personal.", 15, Ui.SOFT);
        final String report = crashReport(crash);
        crash.delete();
        button("Share report", v -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, GAME + " crash report");
            send.putExtra(Intent.EXTRA_TEXT, report);
            try { startActivity(Intent.createChooser(send, "Send the crash report")); }
            catch (android.content.ActivityNotFoundException e) { /* nothing to send it with */ }
        });
        button("Copy report", v -> {
            android.content.ClipboardManager cb = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(android.content.ClipData.newPlainText(GAME + " crash report", report));
            android.widget.Toast.makeText(this, "Copied", android.widget.Toast.LENGTH_SHORT).show();
        });
        Ui.button(mRoot, "Continue", true, v -> next.run());
    }

    private String crashReport(File crash) {
        StringBuilder r = new StringBuilder(GAME + " crash report\n");
        String version = "?";
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { /* unknown */ }
        r.append("App ").append(version).append(", ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
         .append(", Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(')');
        if (Build.VERSION.SDK_INT >= 31) r.append(", ").append(Build.SOC_MANUFACTURER).append(' ').append(Build.SOC_MODEL);
        r.append("\n\n");
        for (String l : readLines(crash)) r.append(l).append('\n');
        List<String> log = readLines(new File(getExternalFilesDir(null), "last_log.txt"));
        r.append("\nlog (last lines):\n");
        for (int i = Math.max(0, log.size() - 120); i < log.size(); i++) r.append(log.get(i)).append('\n');
        return r.toString();
    }

    static List<String> readLines(File f) {
        List<String> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            for (String l; (l = r.readLine()) != null; ) if (!l.trim().isEmpty()) out.add(l.trim());
        } catch (IOException e) { /* none */ }
        return out;
    }

    @Override
    public void onBackPressed() {
        if (mMain) { super.onBackPressed(); return; }
        showMain();
    }

    /* ------------------------------------------------------------------ screens */

    private TextView text(String s, int size, int color) { return Ui.text(mRoot, s, size, color); }

    private View button(String s, View.OnClickListener l) { return Ui.button(mRoot, s, l); }

    private void showMain() {
        mMain = true;
        Ui.clear(mRoot);
        Ui.title(mRoot, "for Android");
        button("Play", v -> { startActivity(new Intent(this, GameActivity.class)); finish(); });
        if (!bundled())   /* add, replace or delete the player's own game files (SetupActivity) */
            Ui.button(mRoot, "Game files", true, v -> { startActivity(new Intent(this, SetupActivity.class).putExtra(SetupActivity.EXTRA_MANAGE, true)); finish(); });
    }

    private boolean bundled() {
        try (InputStream in = getAssets().open("game_manifest.txt")) { return true; } catch (IOException e) { return false; }
    }
}
