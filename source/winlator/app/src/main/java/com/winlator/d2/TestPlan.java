package com.winlator.d2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class TestPlan {
    public static final String REGISTRY_ROOT = "HKCU\\Software\\Blizzard Entertainment\\Diablo II";
    public static final String REGISTRY_ROOT_HKLM = "HKLM\\Software\\Blizzard Entertainment\\Diablo II";
    private static final String REG_EXE_32 = "\"%SystemRoot%\\SysWOW64\\reg.exe\"";
    public static final String MAIN_WINE = "10.10", ALT_WINE = "9.2";
    public static final String MAIN_BOX64 = "0.4.4";
    private static final Map<String, String> REGISTRY_ROOTS = new LinkedHashMap<>();
    static {
        REGISTRY_ROOTS.put("d2", REGISTRY_ROOT);
        REGISTRY_ROOTS.put("d2-hklm", REGISTRY_ROOT_HKLM);
        REGISTRY_ROOTS.put("wine-debug", "HKCU\\Software\\Wine\\Debug");
        REGISTRY_ROOTS.put("wine-direct3d", "HKCU\\Software\\Wine\\Direct3D");
        REGISTRY_ROOTS.put("wine-x11", "HKCU\\Software\\Wine\\X11 Driver");
    }
    public static final int VK_ESCAPE = 0x1B, VK_RETURN = 0x0D, VK_SPACE = 0x20;

    private static final int MAX_RUNS = 12;
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9._-]{1,40}");
    private static final Pattern SCREEN_SIZE = Pattern.compile("\\d{3,4}x\\d{3,4}");
    private static final Set<String> RENDERERS = set("d2gl", "d2dx");
    private static final Pattern SAFE_CONFIG = Pattern.compile("[A-Za-z0-9_.,=|:-]{0,300}");
    private static final Pattern WINCOMPONENTS = Pattern.compile("([a-z0-9]+=[01])(,[a-z0-9]+=[01])*");
    private static final Pattern CPU_LIST = Pattern.compile("[0-9]{1,2}(,[0-9]{1,2}){0,15}");
    private static final Pattern ENV_VALUE = Pattern.compile("[A-Za-z0-9_.,+=:;-]{0,200}");
    private static final Pattern ENV_KEY_BOX64_DYNAREC = Pattern.compile("BOX64_DYNAREC_[A-Z0-9_]{1,40}");
    private static final Pattern REG_SUBKEY = Pattern.compile("[A-Za-z0-9 _-]{1,40}");
    private static final Pattern REG_NAME = Pattern.compile("[A-Za-z0-9 _-]{1,64}");
    private static final Pattern REG_STRING = Pattern.compile("[A-Za-z0-9 _.:;-]{0,200}");
    private static final Pattern GAME_FOLDER = Pattern.compile("[A-Za-z0-9 ._-]{1,60}(/[A-Za-z0-9 ._-]{1,60}){0,4}");

    private static final Set<String> PROGRAMS = set("Game.exe", "D2VidTst.exe");
    private static final Set<String> ARGS = set("-w", "-3dfx", "-glide", "-opengl", "-d3d", "-log", "-ns", "-nosound",
        "-direct", "-txt", "-lq", "-vsync", "-nopreload", "-sndbkg", "-nofixaspect", "-fixaspect");
    private static final Set<String> VULKAN_DRIVERS = set("turnip", "vortek");
    private static final Set<String> OPENGL_DRIVERS = set("zink", "virgl", "gladio");
    private static final Set<String> DX_WRAPPERS = set("wined3d", "dxvk");
    private static final Set<String> BOX64_PRESETS = set("STABILITY", "CONSERVATIVE", "INTERMEDIATE", "PERFORMANCE");
    private static final Set<String> AUDIO_DRIVERS = set("alsa", "pulseaudio");
    private static final Set<String> WINE_VERSIONS = set(MAIN_WINE, ALT_WINE);
    private static final Set<String> BOX64_VERSIONS = set(MAIN_BOX64);
    private static final Set<String> ENV_KEYS = set("WINEDEBUG", "WINEDLLOVERRIDES", "WINEESYNC", "WINEFSYNC",
        "TU_DEBUG", "ZINK_DEBUG", "ZINK_DESCRIPTORS", "MESA_GL_VERSION_OVERRIDE", "MESA_GLSL_VERSION_OVERRIDE",
        "MESA_EXTENSION_MAX_YEAR", "MESA_SHADER_CACHE_DISABLE", "mesa_glthread", "MESA_VK_WSI_PRESENT_MODE",
        "vblank_mode", "DXVK_HUD", "DXVK_FRAME_RATE", "BOX64_LOG", "BOX64_SHOWSEGV", "BOX64_DLSYM_ERROR", "BOX64_DYNAREC",
        "BOX64_NOBANNER", "BOX64_MAXCPU", "BOX64_AVX");
    private static final Set<Integer> KEYS = new HashSet<>(Arrays.asList(VK_ESCAPE, VK_RETURN, VK_SPACE));

    public final int id;
    public final String title;
    public final String gameFolder;
    public final List<Run> runs;

    private TestPlan(int id, String title, String gameFolder, List<Run> runs) {
        this.id = id;
        this.title = title;
        this.gameFolder = gameFolder;
        this.runs = Collections.unmodifiableList(runs);
    }

    public static final class RegistryValue {
        public final String root;
        public final String subkey;
        public final String name;
        public final String type;
        public final String data;
        public final boolean delete;

        RegistryValue(String root, String subkey, String name, String type, String data, boolean delete) {
            this.root = root;
            this.subkey = subkey;
            this.name = name;
            this.type = type;
            this.data = data;
            this.delete = delete;
        }

        public String key() {
            String base = REGISTRY_ROOTS.get(root);
            return subkey.isEmpty() ? base : base + "\\" + subkey;
        }

        public String regExe() {
            return root.equals("d2-hklm") ? REG_EXE_32 : "reg";
        }
    }

    public static final class KeyPress {
        public final double atSeconds;
        public final int vk;

        KeyPress(double atSeconds, int vk) {
            this.atSeconds = atSeconds;
            this.vk = vk;
        }
    }

    public static final class Run {
        public String name;
        public String program;
        public String gameFolder;
        public String renderer = "d2gl";
        public String d2dxGameSize;
        public int d2dxFiltering = 0;
        public List<String> args = new ArrayList<>();
        public final Map<String, String> container = new LinkedHashMap<>();
        public final Map<String, String> envVars = new LinkedHashMap<>();
        public final List<RegistryValue> registry = new ArrayList<>();
        public int timeoutSeconds;
        public final List<Integer> screenshotsAt = new ArrayList<>();
        public final List<KeyPress> keys = new ArrayList<>();

        public String commandLine() {
            StringBuilder sb = new StringBuilder(program);
            for (String arg : args) sb.append(' ').append(arg);
            return sb.toString();
        }

        public String envVarsString() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : envVars.entrySet()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(e.getKey()).append('=').append(e.getValue());
            }
            return sb.toString();
        }

        public JSONObject toSessionJson(String sessionName, String resultDir) throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", sessionName);
            json.put("resultDir", resultDir);
            json.put("timeoutSeconds", timeoutSeconds);
            JSONArray shots = new JSONArray();
            for (int seconds : screenshotsAt) shots.put(seconds);
            json.put("screenshotsAt", shots);
            JSONArray keyArray = new JSONArray();
            for (KeyPress key : keys) keyArray.put(new JSONObject().put("at", key.atSeconds).put("vk", key.vk));
            json.put("keys", keyArray);
            return json;
        }

        public String wineVersion() {
            String value = container.get("wineVersion");
            return value != null ? value : MAIN_WINE;
        }

        public String box64Version() {
            String value = container.get("box64Version");
            return value != null ? value : MAIN_BOX64;
        }

        public String describe() {
            return name + ": " + commandLine() + (gameFolder != null ? " in " + gameFolder : "")
                + (renderer.equals("d2dx") ? " | D2DX" + (d2dxGameSize != null ? " " + d2dxGameSize : "") + " filter " + d2dxFiltering : "")
                + " | " + container
                + (envVars.isEmpty() ? "" : " | env " + envVars);
        }
    }

    public static class PlanException extends Exception {
        public PlanException(String message) {
            super(message);
        }
    }

    public static boolean isAllowedKey(int vk) {
        return KEYS.contains(vk);
    }

    public static String buildRunnerScript(Run run, String outName) {
        String out = " >> \"%D2DIR%\\" + outName + "\" 2>&1\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append("@echo off\r\n");
        sb.append("rem Generated by D2Android remote test (D2TestActivity). Do not edit.\r\n");
        sb.append("cd /d \"%~dp0\"\r\n");
        sb.append("set \"D2DIR=%~dp0\"\r\n");
        sb.append("set \"D2DIR=%D2DIR:~0,-1%\"\r\n");
        sb.append("echo [d2test] runner start %DATE% %TIME% > \"%D2DIR%\\").append(outName).append("\" 2>&1\r\n");
        sb.append("reg add \"").append(REGISTRY_ROOT).append("\" /v \"InstallPath\" /t REG_SZ /d \"%D2DIR%\\\\\" /f").append(out);
        sb.append("reg add \"").append(REGISTRY_ROOT_HKLM).append("\" /v \"InstallPath\" /t REG_SZ /d \"%D2DIR%\\\\\" /f").append(out);
        sb.append(REG_EXE_32).append(" add \"").append(REGISTRY_ROOT_HKLM).append("\" /v \"InstallPath\" /t REG_SZ /d \"%D2DIR%\\\\\" /f").append(out);
        sb.append("reg add \"").append(REGISTRY_ROOT).append("\" /v \"Save Path\" /t REG_SZ /d \"%D2DIR%\\save\\\\\" /f").append(out);
        for (RegistryValue value : run.registry) {
            if (value.delete) {
                sb.append(value.regExe()).append(" delete \"").append(value.key()).append("\" /v \"").append(value.name).append("\" /f").append(out);
            }
            else {
                sb.append(value.regExe()).append(" add \"").append(value.key()).append("\" /v \"").append(value.name).append("\" /t ")
                    .append(value.type).append(" /d \"").append(value.data).append("\" /f").append(out);
            }
        }
        sb.append("reg query \"").append(REGISTRY_ROOT).append("\" /s").append(out);
        sb.append(REG_EXE_32).append(" query \"").append(REGISTRY_ROOT_HKLM).append("\" /s").append(out);
        sb.append("if exist d2gl.log del d2gl.log\r\n");
        sb.append("echo [d2test] start ").append(run.commandLine()).append(" %TIME%").append(out);
        sb.append(run.commandLine()).append("\r\n");
        sb.append("echo [d2test] exitcode %ERRORLEVEL% %TIME%").append(out);
        sb.append("echo [d2test] registry after run").append(out);
        sb.append("reg query \"").append(REGISTRY_ROOT).append("\" /s").append(out);
        sb.append("tasklist").append(out);
        sb.append("echo [d2test] runner end %TIME%").append(out);
        return sb.toString();
    }

    public static TestPlan parse(String jsonText) throws PlanException {
        try {
            JSONObject json = new JSONObject(jsonText);
            int id = json.getInt("id");
            if (id <= 0) throw new PlanException("id muss > 0 sein");
            String title = json.optString("title", "");
            if (title.length() > 200) title = title.substring(0, 200);

            JSONObject defaults = json.optJSONObject("defaults");
            JSONArray runsJson = json.getJSONArray("runs");
            if (runsJson.length() == 0 || runsJson.length() > MAX_RUNS) throw new PlanException("1 bis " + MAX_RUNS + " Läufe erlaubt");

            List<Run> runs = new ArrayList<>();
            Set<String> names = new HashSet<>();
            for (int i = 0; i < runsJson.length(); i++) {
                JSONObject runJson = runsJson.getJSONObject(i);
                Run run = new Run();
                if (defaults != null) apply(run, defaults, "defaults");
                apply(run, runJson, "Lauf " + (i + 1));
                finish(run, i);
                if (!names.add(run.name)) throw new PlanException("Doppelter Laufname: " + run.name);
                runs.add(run);
            }
            String gameFolder = json.has("gameFolder") ? gameFolder(json.getString("gameFolder"), "Plan") : null;
            return new TestPlan(id, title, gameFolder, runs);
        }
        catch (JSONException e) {
            throw new PlanException("Ungültiges JSON: " + e.getMessage());
        }
    }

    private static String gameFolder(String value, String where) throws PlanException {
        require(GAME_FOLDER.matcher(value).matches(), where + ": gameFolder invalid");
        for (String segment : value.split("/")) {
            require(!segment.trim().isEmpty() && !segment.equals(".") && !segment.equals(".."), where + ": gameFolder invalid");
        }
        return value;
    }

    private static void apply(Run run, JSONObject src, String where) throws JSONException, PlanException {
        for (Iterator<String> it = src.keys(); it.hasNext(); ) {
            String key = it.next();
            switch (key) {
                case "name": {
                    String name = src.getString(key);
                    require(NAME.matcher(name).matches(), where + ": name invalid");
                    run.name = name;
                    break;
                }
                case "program": {
                    String program = matchIgnoreCase(PROGRAMS, src.getString(key));
                    require(program != null, where + ": program nicht erlaubt");
                    run.program = program;
                    break;
                }
                case "args": {
                    JSONArray args = src.getJSONArray(key);
                    run.args = new ArrayList<>();
                    for (int i = 0; i < args.length(); i++) {
                        String arg = args.getString(i).toLowerCase(Locale.ROOT);
                        require(ARGS.contains(arg), where + ": start parameter not allowed: " + arg);
                        run.args.add(arg);
                    }
                    break;
                }
                case "container":
                    applyContainer(run, src.getJSONObject(key), where);
                    break;
                case "gameFolder":
                    run.gameFolder = gameFolder(src.getString(key), where);
                    break;
                case "renderer":
                    run.renderer = oneOf(RENDERERS, src.getString(key).toLowerCase(Locale.ROOT), where, key);
                    break;
                case "d2dxGameSize": {
                    String size = matching(SCREEN_SIZE, src.getString(key), where, key);
                    String[] wh = size.split("x");
                    int w = Integer.parseInt(wh[0]), h = Integer.parseInt(wh[1]);
                    require(w >= 640 && w <= 2560 && h >= 480 && h <= 1440, where + ": d2dxGameSize outside 640x480–2560x1440");
                    run.d2dxGameSize = size;
                    break;
                }
                case "d2dxFiltering": {
                    int filtering = src.getInt(key);
                    require(filtering >= 0 && filtering <= 2, where + ": d2dxFiltering 0–2");
                    run.d2dxFiltering = filtering;
                    break;
                }
                case "registry": {
                    JSONArray values = src.getJSONArray(key);
                    require(run.registry.size() + values.length() <= 30, where + ": too many registry values");
                    for (int i = 0; i < values.length(); i++) run.registry.add(parseRegistry(values.getJSONObject(i), where));
                    break;
                }
                case "timeoutSeconds": {
                    int timeout = src.getInt(key);
                    require(timeout >= 10 && timeout <= 600, where + ": timeoutSeconds 10–600");
                    run.timeoutSeconds = timeout;
                    break;
                }
                case "screenshotsAt": {
                    JSONArray shots = src.getJSONArray(key);
                    run.screenshotsAt.clear();
                    require(shots.length() <= 10, where + ": at most 10 screenshots");
                    for (int i = 0; i < shots.length(); i++) run.screenshotsAt.add(shots.getInt(i));
                    break;
                }
                case "keys": {
                    JSONArray keys = src.getJSONArray(key);
                    run.keys.clear();
                    require(keys.length() <= 20, where + ": at most 20 key presses");
                    for (int i = 0; i < keys.length(); i++) {
                        JSONObject k = keys.getJSONObject(i);
                        int vk = k.getInt("vk");
                        require(KEYS.contains(vk), where + ": key not allowed (only esc, enter, space)");
                        run.keys.add(new KeyPress(k.getDouble("at"), vk));
                    }
                    break;
                }
                case "comment":
                    break;
                default:
                    throw new PlanException(where + ": unknown field " + key);
            }
        }
    }

    private static void applyContainer(Run run, JSONObject c, String where) throws JSONException, PlanException {
        for (Iterator<String> it = c.keys(); it.hasNext(); ) {
            String key = it.next();
            switch (key) {
                case "vulkanDriver":
                    run.container.put(key, oneOf(VULKAN_DRIVERS, c.getString(key).toLowerCase(Locale.ROOT), where, key));
                    break;
                case "openglDriver":
                    run.container.put(key, oneOf(OPENGL_DRIVERS, c.getString(key).toLowerCase(Locale.ROOT), where, key));
                    break;
                case "dxwrapper":
                    run.container.put(key, oneOf(DX_WRAPPERS, c.getString(key).toLowerCase(Locale.ROOT), where, key));
                    break;
                case "box64Preset":
                    run.container.put(key, oneOf(BOX64_PRESETS, c.getString(key).toUpperCase(Locale.ROOT), where, key));
                    break;
                case "audioDriver":
                    run.container.put(key, oneOf(AUDIO_DRIVERS, c.getString(key).toLowerCase(Locale.ROOT), where, key));
                    break;
                case "wineVersion":
                    run.container.put(key, oneOf(WINE_VERSIONS, c.getString(key), where, key));
                    break;
                case "box64Version":
                    run.container.put(key, oneOf(BOX64_VERSIONS, c.getString(key), where, key));
                    break;
                case "screenSize":
                    run.container.put(key, matching(SCREEN_SIZE, c.getString(key), where, key));
                    break;
                case "graphicsDriverConfig":
                case "dxwrapperConfig":
                    run.container.put(key, matching(SAFE_CONFIG, c.getString(key), where, key));
                    break;
                case "wincomponents":
                    run.container.put(key, matching(WINCOMPONENTS, c.getString(key), where, key));
                    break;
                case "cpuList":
                    run.container.put(key, matching(CPU_LIST, c.getString(key), where, key));
                    break;
                case "startupSelection": {
                    int value = c.getInt(key);
                    require(value >= 0 && value <= 2, where + ": startupSelection 0–2");
                    run.container.put(key, String.valueOf(value));
                    break;
                }
                case "envVars": {
                    JSONObject env = c.getJSONObject(key);
                    for (Iterator<String> it2 = env.keys(); it2.hasNext(); ) {
                        String name = it2.next();
                        require(ENV_KEYS.contains(name) || ENV_KEY_BOX64_DYNAREC.matcher(name).matches(), where + ": environment variable not allowed: " + name);
                        String value = env.getString(name);
                        require(ENV_VALUE.matcher(value).matches(), where + ": value for " + name + " invalid");
                        require(!name.equals(D2FrameRate.ENV) || value.isEmpty() || D2FrameRate.validEnvValue(value),
                            where + ": " + name + " must be a whole number 0–" + D2FrameRate.MAX_ENV);
                        if (value.isEmpty()) run.envVars.remove(name);
                        else run.envVars.put(name, value);
                    }
                    break;
                }
                default:
                    throw new PlanException(where + ": unknown container setting " + key);
            }
        }
    }

    private static RegistryValue parseRegistry(JSONObject r, String where) throws JSONException, PlanException {
        String root = r.optString("root", "d2");
        require(REGISTRY_ROOTS.containsKey(root), where + ": registry area not allowed: " + root);
        String subkey = r.optString("subkey", "");
        require(subkey.isEmpty() || root.startsWith("d2"), where + ": subkeys only in the areas d2 and d2-hklm");
        require(subkey.isEmpty() || REG_SUBKEY.matcher(subkey).matches(), where + ": registry subkey invalid");
        String name = r.getString("name");
        require(REG_NAME.matcher(name).matches(), where + ": registry name invalid");
        if (r.optBoolean("delete", false)) return new RegistryValue(root, subkey, name, "", "", true);

        String type = r.optString("type", "dword").toLowerCase(Locale.ROOT);
        if (type.equals("dword")) {
            long value = r.getLong("value");
            require(value >= 0 && value <= 0xFFFFFFFFL, where + ": DWORD out of range");
            return new RegistryValue(root, subkey, name, "REG_DWORD", String.valueOf(value), false);
        }
        else if (type.equals("string")) {
            String value = r.getString("value");
            require(REG_STRING.matcher(value).matches(), where + ": registry text invalid");
            return new RegistryValue(root, subkey, name, "REG_SZ", value, false);
        }
        throw new PlanException(where + ": registry type only dword or string");
    }

    private static void finish(Run run, int index) throws PlanException {
        if (run.name == null) run.name = "run" + (index + 1);
        if (run.program == null) run.program = "Game.exe";
        if (run.timeoutSeconds == 0) run.timeoutSeconds = 45;
        for (int seconds : run.screenshotsAt) {
            require(seconds >= 1 && seconds <= run.timeoutSeconds, run.name + ": screenshot time outside the run");
        }
        for (KeyPress key : run.keys) {
            require(key.atSeconds >= 0 && key.atSeconds <= run.timeoutSeconds, run.name + ": key time outside the run");
        }
    }

    private static String oneOf(Set<String> allowed, String value, String where, String key) throws PlanException {
        require(allowed.contains(value), where + ": " + key + " nicht erlaubt: " + value);
        return value;
    }

    private static String matching(Pattern pattern, String value, String where, String key) throws PlanException {
        require(pattern.matcher(value).matches(), where + ": " + key + " invalid");
        return value;
    }

    private static String matchIgnoreCase(Set<String> allowed, String value) {
        for (String candidate : allowed) if (candidate.equalsIgnoreCase(value)) return candidate;
        return null;
    }

    private static void require(boolean condition, String message) throws PlanException {
        if (!condition) throw new PlanException(message);
    }

    private static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
