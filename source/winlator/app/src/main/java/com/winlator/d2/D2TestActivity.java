package com.winlator.d2;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.winlator.core.AppUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class D2TestActivity extends D2Activity {
    public static final String EXTRA_CONTINUE = "d2test_continue";
    private static final String PLAN_PATH = "tools/testplan.json";
    private static final String PLAN_REF = "main";
    private static final String RESULTS_BRANCH = "test-results";
    private static final String RUNNER_NAME = "_d2test.bat";
    private static final String RUNNER_OUT = "_d2test-out.txt";
    private static final int PERMISSION_REQUEST_CODE = 4711;

    private static final String PREF_TOKEN = "d2test_token";
    private static final String PREF_LAST_DONE = "d2test_last_done_id";
    private static final String PREF_STATE = "d2test_state";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private SharedPreferences preferences;
    private TextView statusView;
    private TextView planView;
    private EditText tokenInput;
    private Button runButton;
    private LinearLayout resumeRow;
    private String loadedPlanJson;
    private volatile boolean busy = false;
    private static final int MAX_UPLOAD_ATTEMPTS = 10;
    private int uploadFailures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        preferences = getSharedPreferences("d2test", MODE_PRIVATE);
        buildUI();
        showStatusLog();

        if (!hasStoragePermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }

        if (getIntent().getBooleanExtra(EXTRA_CONTINUE, false) && preferences.contains(PREF_STATE)) {
            busy = true;
            handler.postDelayed(this::continueAfterRun, 1000);
        }
        else {
            resumeRow.setVisibility(preferences.contains(PREF_STATE) ? View.VISIBLE : View.GONE);
            if (hasToken()) loadPlan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && !hasStoragePermission()) {
            status("Ohne Speicherzugriff findet der Test den Spielordner nicht.");
        }
    }


    private void buildUI() {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("D2Android – Remote-Test");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Lädt den Testplan aus dem Repo. Erst nach deinem Tipp startet Diablo II mit den Einstellungen daraus; danach werden Logs und Screenshots auf GitHub hochgeladen. Version " + versionName());
        info.setPadding(0, dp(8), 0, dp(12));
        root.addView(info);

        tokenInput = new EditText(this);
        tokenInput.setHint(hasToken() ? "Token gespeichert – zum Ändern neu einfügen" : "GitHub-Token hier einfügen");
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(tokenInput, matchWidth());

        Button saveToken = new Button(this);
        saveToken.setText("Token speichern");
        saveToken.setOnClickListener((v) -> {
            String token = tokenInput.getText().toString().trim();
            if (token.isEmpty()) {
                status("Kein Token eingegeben.");
                return;
            }
            preferences.edit().putString(PREF_TOKEN, token).apply();
            tokenInput.setText("");
            tokenInput.setHint("Token gespeichert – zum Ändern neu einfügen");
            status("Token gespeichert.");
            loadPlan();
        });
        root.addView(saveToken, matchWidth());

        Button reload = new Button(this);
        reload.setText("Testplan neu laden");
        reload.setOnClickListener((v) -> loadPlan());
        root.addView(reload, matchWidth());

        planView = new TextView(this);
        planView.setPadding(0, dp(12), 0, dp(4));
        planView.setText("Noch kein Testplan geladen.");
        root.addView(planView, matchWidth());

        runButton = new Button(this);
        runButton.setText("Testplan ausführen");
        runButton.setEnabled(false);
        runButton.setOnClickListener((v) -> runLoadedPlan());
        root.addView(runButton, matchWidth());

        resumeRow = new LinearLayout(this);
        resumeRow.setOrientation(LinearLayout.HORIZONTAL);
        Button resume = new Button(this);
        resume.setText("Unterbrochenen Plan fortsetzen");
        resume.setOnClickListener((v) -> {
            resumeRow.setVisibility(View.GONE);
            busy = true;
            continueAfterRun();
        });
        Button discard = new Button(this);
        discard.setText("Verwerfen");
        discard.setOnClickListener((v) -> {
            preferences.edit().remove(PREF_STATE).apply();
            resumeRow.setVisibility(View.GONE);
            runButton.setEnabled(!busy && loadedPlanJson != null);
            status("Unterbrochener Plan verworfen.");
        });
        resumeRow.addView(resume, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
        resumeRow.addView(discard, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(resumeRow, matchWidth());

        statusView = new TextView(this);
        statusView.setTypeface(Typeface.MONOSPACE);
        statusView.setTextSize(12);
        statusView.setPadding(0, dp(12), 0, 0);
        root.addView(statusView, matchWidth());

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root);
        setContentView(scrollView);
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }


    private File statusFile() {
        return new File(workDir(), "status.log");
    }

    private void showStatusLog() {
        ArrayList<String> lines = new ArrayList<>();
        File file = statusFile();
        if (file.isFile()) lines.addAll(Arrays.asList(readText(file, 40000).split("\n")));
        int from = Math.max(0, lines.size() - 40);
        statusView.setText(String.join("\n", lines.subList(from, lines.size())));
    }

    private void status(final String message) {
        final String line = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date()) + "  " + message;
        synchronized (this) {
            try (FileWriter writer = new FileWriter(statusFile(), true)) {
                writer.write(line + "\n");
            }
            catch (IOException ignored) {}
        }
        handler.post(this::showStatusLog);
    }


    private void loadPlan() {
        if (!hasToken()) {
            status("Bitte zuerst den GitHub-Token speichern.");
            return;
        }
        planView.setText("Lade Testplan …");
        runButton.setEnabled(false);
        io.execute(() -> {
            try {
                final String text = new GitHubClient(token()).getFileText(PLAN_PATH, PLAN_REF);
                final TestPlan plan = TestPlan.parse(text);
                handler.post(() -> showPlan(plan, text));
            }
            catch (TestPlan.PlanException e) {
                handler.post(() -> planView.setText("Testplan ungültig – wird nicht ausgeführt:\n" + e.getMessage()));
            }
            catch (Exception e) {
                handler.post(() -> planView.setText("Testplan konnte nicht geladen werden: " + e.getMessage()));
            }
        });
    }

    private void showPlan(TestPlan plan, String json) {
        loadedPlanJson = json;
        boolean done = plan.id <= preferences.getInt(PREF_LAST_DONE, 0);
        StringBuilder sb = new StringBuilder();
        sb.append("Testplan #").append(plan.id).append(done ? " (bereits ausgeführt)" : " (neu)").append("\n");
        sb.append(plan.title).append("\n\n");
        int estimate = 0;
        for (TestPlan.Run run : plan.runs) {
            sb.append("• ").append(run.describe()).append("\n");
            estimate += run.timeoutSeconds + 25;
        }
        sb.append("\nDauer: höchstens ca. ").append(Math.max(1, estimate / 60)).append(" min. Bitte das Handy währenddessen nicht bedienen.");
        planView.setText(sb.toString());
        runButton.setText(done ? "Testplan erneut ausführen" : "Testplan ausführen");
        runButton.setEnabled(!busy && !preferences.contains(PREF_STATE));
    }

    private void runLoadedPlan() {
        if (busy || loadedPlanJson == null || preferences.contains(PREF_STATE)) return;
        try {
            TestPlan plan = TestPlan.parse(loadedPlanJson);
            if (!D2Runtime.isInstalled(this) || !D2Runtime.isPrefixReady(this)) {
                status("Die Laufzeit ist noch nicht eingerichtet: bitte einmal Diablo II starten und warten, bis das Hauptmenü erscheint.");
                return;
            }
            File gameDir;
            if (plan.gameFolder != null) {
                gameDir = GameLocator.resolve(plan.gameFolder);
                if (gameDir == null) {
                    status("Spielordner aus dem Plan nicht gefunden: Download/" + plan.gameFolder);
                    return;
                }
            }
            else gameDir = GameLocator.find();
            if (gameDir == null) {
                status("Spielordner nicht gefunden (gesucht im Download-Ordner: Ordner mit Game.exe und d2data.mpq).");
                return;
            }
            for (TestPlan.Run run : plan.runs) {
                if (run.gameFolder != null && GameLocator.resolve(run.gameFolder) == null) {
                    status("Spielordner aus Lauf " + run.name + " nicht gefunden: Download/" + run.gameFolder);
                    return;
                }
                if (!GameShadow.RENDERER_D2DX.equals(run.renderer)) {
                    status("Lauf " + run.name + ": nur D2DX ist noch möglich (D2GL ist entfallen).");
                    return;
                }
            }

            busy = true;
            runButton.setEnabled(false);
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
            String planName = "plan-" + plan.id + "-" + stamp + "-" + UUID.randomUUID().toString().substring(0, 4);
            File resultRoot = new File(new File(workDir(), "results"), planName);
            if (!resultRoot.mkdirs()) throw new IOException("Cannot create " + resultRoot);

            JSONObject state = new JSONObject();
            state.put("planJson", loadedPlanJson);
            state.put("planName", planName);
            state.put("resultRoot", resultRoot.getPath());
            state.put("gameDir", gameDir.getPath());
            state.put("runIndex", 0);
            saveState(state);

            status("Starte Testplan #" + plan.id + " – Spielordner " + gameDir.getPath());
            launchRun(state);
        }
        catch (Exception e) {
            busy = false;
            runButton.setEnabled(true);
            preferences.edit().remove(PREF_STATE).apply();
            status("Start fehlgeschlagen: " + e.getMessage());
        }
    }

    private void launchRun(final JSONObject state) throws JSONException, IOException, TestPlan.PlanException {
        final TestPlan plan = TestPlan.parse(state.getString("planJson"));
        final int index = state.getInt("runIndex");
        final TestPlan.Run run = plan.runs.get(index);
        final String runName = String.format(Locale.ROOT, "run-%02d-%s", index + 1, run.name);
        final File runDir = new File(state.getString("resultRoot"), runName);
        final File gameDir = run.gameFolder != null ? GameLocator.resolve(run.gameFolder) : new File(state.getString("gameDir"));
        if (gameDir == null) throw new IOException("Spielordner nicht gefunden: " + run.gameFolder);

        status("Lauf " + (index + 1) + "/" + plan.runs.size() + ": " + run.describe());
        java.util.Set<String> ignored = new java.util.TreeSet<>(run.container.keySet());
        if (!ignored.isEmpty()) status("Fest in der App, aus dem Plan ignoriert: " + String.join(", ", ignored));

        String d2dxCfg = GameShadow.buildD2DXConfig(run.d2dxGameSize, run.d2dxFiltering);
        File startDir = GameShadow.prepare(this, D2Runtime.driveC(this), gameDir, d2dxCfg);
        writeRunner(startDir, run);
        new File(startDir, RUNNER_OUT).delete();

        state.put("runStartedAt", System.currentTimeMillis());
        state.put("runDir", runDir.getPath());
        state.put("runGameDir", startDir.getPath());
        saveState(state);

        Intent intent = new Intent(this, D2GameActivity.class);
        intent.putExtra(D2GameActivity.EXTRA_DIR, D2Runtime.dosPath(this, startDir));
        intent.putExtra(D2GameActivity.EXTRA_PROGRAM, RUNNER_NAME);
        intent.putExtra(D2GameActivity.EXTRA_ENV, run.envVarsString());
        intent.putExtra(D2TestSession.EXTRA_RUN, run.toSessionJson(runName, runDir.getPath()).toString());
        startActivity(intent);
        finish();
    }

    private void continueAfterRun() {
        final JSONObject state = loadState();
        if (state == null) {
            busy = false;
            return;
        }
        try {
            collectRunArtifacts(state);
            TestPlan plan = TestPlan.parse(state.getString("planJson"));
            int next = state.getInt("runIndex") + 1;
            if (next < plan.runs.size()) {
                state.put("runIndex", next);
                saveState(state);
                handler.postDelayed(() -> {
                    try {
                        launchRun(state);
                    }
                    catch (Exception e) {
                        status("Fehler beim Start des nächsten Laufs: " + e.getMessage());
                        finishPlanWithError("launch");
                    }
                }, 1500);
            }
            else uploadResults(state);
        }
        catch (Exception e) {
            status("Fehler nach dem Lauf: " + e.getMessage());
            finishPlanWithError("continue");
        }
    }

    private void finishPlanWithError(String reason) {
        JSONObject state = loadState();
        if (state != null) {
            try {
                writeText(new File(state.getString("resultRoot"), "ABORTED.txt"), "Plan aborted: " + reason + "\n");
                uploadResults(state);
                return;
            }
            catch (Exception ignored) {}
        }
        preferences.edit().remove(PREF_STATE).apply();
        busy = false;
    }


    private void writeRunner(File gameDir, TestPlan.Run run) throws IOException {
        try (OutputStream stream = new FileOutputStream(new File(gameDir, RUNNER_NAME))) {
            stream.write(TestPlan.buildRunnerScript(run, RUNNER_OUT).getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private void collectRunArtifacts(JSONObject state) throws JSONException {
        File runDir = new File(state.getString("runDir"));
        File gameDir = new File(state.optString("runGameDir", state.getString("gameDir")));
        long startedAt = state.optLong("runStartedAt", 0);
        if (!runDir.isDirectory()) runDir.mkdirs();

        copyIfExists(new File(gameDir, RUNNER_OUT), new File(runDir, "runner-out.txt"));
        copyIfExists(new File(gameDir, RUNNER_NAME), new File(runDir, "runner.bat.txt"));
        File d2glLog = new File(gameDir, "d2gl.log");
        if (d2glLog.isFile() && d2glLog.lastModified() >= startedAt - 2000) copyIfExists(d2glLog, new File(runDir, "d2gl.log"));
        File d2dxLog = new File(gameDir, "d2dx_log.txt");
        if (d2dxLog.isFile() && d2dxLog.lastModified() >= startedAt - 2000) copyIfExists(d2dxLog, new File(runDir, "d2dx_log.txt"));
        copyIfExists(new File(gameDir, "d2dx.cfg"), new File(runDir, "d2dx.cfg.txt"));

        File[] files = gameDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().matches("(?i)D2\\d{6}\\.txt") && file.lastModified() >= startedAt - 2000) {
                    copyIfExists(file, new File(runDir, "crashlog-" + file.getName()));
                }
            }
        }
        status("Lauf beendet: " + runDir.getName() + " – " + summarizeRun(runDir));
    }

    private String summarizeRun(File runDir) {
        StringBuilder sb = new StringBuilder();
        File session = new File(runDir, "session.json");
        if (session.isFile()) {
            try {
                JSONObject json = new JSONObject(readText(session, 10000));
                sb.append(json.optString("endReason")).append(", ").append(json.optLong("durationMs") / 1000).append(" s");
            }
            catch (JSONException ignored) {}
        }
        else sb.append("keine Sitzungsdaten");
        for (String line : readText(new File(runDir, "runner-out.txt"), 200000).split("\n")) {
            if (line.startsWith("[d2test] exitcode")) sb.append(", ").append(line.substring(9).trim());
        }
        return sb.toString();
    }


    private void uploadResults(final JSONObject state) {
        busy = true;
        status("Lade Ergebnisse hoch …");
        io.execute(() -> {
            try {
                File resultRoot = new File(state.getString("resultRoot"));
                String planName = state.getString("planName");
                TestPlan plan = TestPlan.parse(state.getString("planJson"));
                File report = new File(resultRoot, "report.md");
                writeText(report, buildReport(state, plan));
                String uploadId = state.optString("uploadId", "");
                if (uploadId.isEmpty()) {
                    uploadId = UUID.randomUUID().toString();
                    state.put("uploadId", uploadId);
                    saveState(state);
                }
                File owner = new File(resultRoot, "upload-id.txt");
                writeText(owner, uploadId + "\n");

                ArrayList<File> files = new ArrayList<>();
                collectFiles(resultRoot, files);
                files.remove(owner);
                files.add(0, owner);
                files.remove(report);
                files.add(report);
                GitHubClient client = new GitHubClient(token());
                for (File file : files) {
                    String relative = resultRoot.toURI().relativize(file.toURI()).getPath();
                    byte[] content = readBytes(file);
                    if (content.length > 512 * 1024 && !relative.endsWith(".jpg")) {
                        content = gzip(content);
                        relative += ".gz";
                    }
                    String path = "results/" + planName + "/" + relative, message = "Test result plan #" + plan.id + ": " + relative;
                    if (file.equals(report)) client.replaceFile(path, content, message, RESULTS_BRANCH);
                    else client.createFile(path, content, message, RESULTS_BRANCH);
                }
                preferences.edit().putInt(PREF_LAST_DONE, plan.id).remove(PREF_STATE).apply();
                uploadFailures = 0;
                status("Fertig: " + files.size() + " Dateien hochgeladen (results/" + planName + "). Die Ergebnisse können jetzt ausgewertet werden.");
                busy = false;
                handler.post(this::loadPlan);
            }
            catch (Exception e) {
                uploadFailures++;
                boolean retry = e instanceof IOException && GitHubClient.isRetryable((IOException) e)
                    && uploadFailures < MAX_UPLOAD_ATTEMPTS;
                if (retry) {
                    status("Upload fehlgeschlagen (Versuch " + uploadFailures + "/" + MAX_UPLOAD_ATTEMPTS + "), neuer Versuch in 60 s: " + e.getMessage());
                    handler.postDelayed(() -> uploadResults(state), 60000);
                    return;
                }
                uploadFailures = 0;
                status("Upload abgebrochen: " + e.getMessage() + " – Ergebnisse bleiben lokal. „Unterbrochenen Plan fortsetzen“ versucht es erneut, „Verwerfen“ gibt den Testmodus frei.");
                busy = false;
                handler.post(() -> {
                    resumeRow.setVisibility(View.VISIBLE);
                    if (hasToken()) loadPlan();
                });
            }
        });
    }

    private String buildReport(JSONObject state, TestPlan plan) throws JSONException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Testergebnis Plan #").append(plan.id).append(": ").append(plan.title).append("\n\n");
        sb.append("- App: ").append(versionName()).append("\n");
        sb.append("- Gerät: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL);
        if (Build.VERSION.SDK_INT >= 31) sb.append(" (SoC ").append(Build.SOC_MODEL).append(")");
        sb.append(", Android ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("- Spielordner: `").append(state.optString("gameDir")).append("`\n");
        sb.append("- Laufzeit ").append(D2Runtime.bundledVersion(this)).append(" (Wine 9.2, Box64 0.4.4, fest)\n");
        sb.append("- Gefundene D2-Ordner unter Download:");
        for (File dir : GameLocator.findAll()) sb.append(" `").append(dir.getPath()).append("`");
        sb.append("\n\n");

        sb.append("## Läufe\n\n");
        File resultRoot = new File(state.getString("resultRoot"));
        File[] runDirs = resultRoot.listFiles(File::isDirectory);
        if (runDirs != null) {
            Arrays.sort(runDirs);
            for (File runDir : runDirs) sb.append("- **").append(runDir.getName()).append("**: ").append(summarizeRun(runDir)).append("\n");
        }
        sb.append("\n");
        for (TestPlan.Run run : plan.runs) sb.append("- ").append(run.describe()).append("\n");

        sb.append("\n## Spielordner (oberste Ebene)\n\n```\n");
        File[] gameFiles = new File(state.getString("gameDir")).listFiles();
        if (gameFiles != null) {
            Arrays.sort(gameFiles);
            for (File file : gameFiles) {
                sb.append(file.isDirectory() ? "     [dir] " : String.format(Locale.ROOT, "%10d ", file.length())).append(file.getName()).append("\n");
            }
        }
        sb.append("```\n");
        return sb.toString();
    }


    private void saveState(JSONObject state) {
        preferences.edit().putString(PREF_STATE, state.toString()).commit();
    }

    private JSONObject loadState() {
        String json = preferences.getString(PREF_STATE, null);
        if (json == null) return null;
        try {
            return new JSONObject(json);
        }
        catch (JSONException e) {
            preferences.edit().remove(PREF_STATE).apply();
            return null;
        }
    }

    private boolean hasToken() {
        return !token().isEmpty();
    }

    private String token() {
        return preferences.getString(PREF_TOKEN, "");
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private File workDir() {
        File dir = new File(getFilesDir(), "d2test");
        if (!dir.isDirectory()) dir.mkdirs();
        return dir;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " (" + AppUtils.getVersionCode(this) + ")";
        }
        catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    private static void collectFiles(File dir, ArrayList<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files);
        for (File file : files) {
            if (file.isDirectory()) collectFiles(file, out);
            else out.add(file);
        }
    }

    private static void copyIfExists(File source, File target) {
        if (!source.isFile()) return;
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
        catch (IOException ignored) {}
    }

    private static byte[] readBytes(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int offset = 0, n;
            while (offset < data.length && (n = in.read(data, offset, data.length - offset)) != -1) offset += n;
            return data;
        }
    }

    private static String readText(File file, int maxBytes) {
        if (!file.isFile()) return "";
        try (InputStream in = new FileInputStream(file)) {
            int size = (int) Math.min(file.length(), maxBytes);
            byte[] data = new byte[size];
            int offset = 0, n;
            while (offset < size && (n = in.read(data, offset, size - offset)) != -1) offset += n;
            return new String(data, 0, offset, StandardCharsets.ISO_8859_1);
        }
        catch (IOException e) {
            return "";
        }
    }

    private static byte[] gzip(byte[] data) throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream out = new java.util.zip.GZIPOutputStream(bytes)) {
            out.write(data);
        }
        return bytes.toByteArray();
    }

    private static void writeText(File file, String text) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
