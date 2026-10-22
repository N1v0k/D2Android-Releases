package com.winlator.d2;

import com.winlator.R;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class D2LicensesActivity extends D2Activity {
    static final String INDEX = "licenses/index.tsv";

    private LinearLayout content;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        content.setPadding(pad, pad, pad, pad);
        scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(D2Style.BACKGROUND);
        scrollView.addView(content);
        setContentView(scrollView);
        showList();
    }

    @Override
    public void onBackPressed() {
        if (content.getTag() != null) showList();
        else super.onBackPressed();
    }

    private void showList() {
        content.removeAllViews();
        content.setTag(null);
        TextView title = new TextView(this);
        title.setText(getString(R.string.licenses_title));
        title.setTextSize(24);
        D2Style.heading(title);
        content.addView(title);
        TextView note = new TextView(this);
        note.setText(getString(R.string.licenses_lgpl_note));
        note.setPadding(0, dp(8), 0, dp(12));
        D2Style.text(note);
        content.addView(note);

        for (Part part : readIndex(getAssets())) {
            TextView row = new TextView(this);
            row.setText(part.name + "\n" + part.version + " · " + part.license);
            row.setPadding(0, dp(10), 0, dp(10));
            row.setTextSize(15);
            D2Style.text(row);
            if (!part.files.isEmpty()) {
                row.setOnClickListener((v) -> showPart(part));
                row.setBackgroundResource(android.R.drawable.list_selector_background);
            }
            content.addView(row);
        }
        scrollView.scrollTo(0, 0);
    }

    private void showPart(Part part) {
        content.removeAllViews();
        content.setTag(part.id);
        TextView title = new TextView(this);
        title.setText(part.name);
        title.setTextSize(20);
        D2Style.heading(title);
        content.addView(title);
        TextView meta = new TextView(this);
        meta.setText(part.version + " · " + part.license + "\n" + part.source + (part.note.isEmpty() ? "" : "\n" + part.note));
        meta.setPadding(0, dp(6), 0, dp(12));
        D2Style.hint(meta);
        content.addView(meta);
        for (String file : part.files) {
            TextView text = new TextView(this);
            text.setTypeface(Typeface.MONOSPACE);
            text.setTextSize(11);
            text.setPadding(0, 0, 0, dp(16));
            D2Style.text(text);
            String body = read(getAssets(), file);
            text.setText(body != null ? body : getString(R.string.licenses_missing, file));
            content.addView(text);
        }
        scrollView.scrollTo(0, 0);
    }

    static final class Part {
        final String id, name, version, license, source, note;
        final List<String> files = new ArrayList<>();

        Part(String[] f) {
            id = f[0];
            name = f[1];
            version = f[2];
            license = f[3];
            source = f[4];
            note = f.length > 6 ? f[6] : "";
            for (String file : f[5].split(",")) if (!file.trim().isEmpty() && !file.trim().equals("-")) files.add(file.trim());
        }
    }

    static List<Part> readIndex(AssetManager assets) {
        List<Part> parts = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(assets.open(INDEX), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] f = line.split("\t", -1);
                if (f.length >= 6) parts.add(new Part(f));
            }
        }
        catch (IOException e) {
            android.util.Log.w("D2Android", "licenses: no index", e);
        }
        return parts;
    }

    static String read(AssetManager assets, String path) {
        StringBuilder out = new StringBuilder();
        try {
            String[] children = assets.list(path);
            if (children != null && children.length > 0) {
                for (String child : children) {
                    String body = read(assets, path + "/" + child);
                    out.append("== ").append(child).append(" ==\n").append(body != null ? body : "").append('\n');
                }
                return out.toString();
            }
            try (InputStream in = assets.open(path)) {
                byte[] buffer = new byte[16384];
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                int n;
                while ((n = in.read(buffer)) > 0) bytes.write(buffer, 0, n);
                return bytes.toString("UTF-8");
            }
        }
        catch (IOException e) {
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
