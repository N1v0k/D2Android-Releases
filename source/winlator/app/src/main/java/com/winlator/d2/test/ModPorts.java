package com.winlator.d2.test;










public final class ModPorts {

    public static final int APP;

    public static final int CMD;

    public static final int SPIELTEST;

    static {
        int app = port("D2MOD_APP_PORT", 7958), cmd = port("D2MOD_CMD_PORT", 7959);
        if (app == cmd) {
            System.err.println("D2MOD_APP_PORT and D2MOD_CMD_PORT both " + app + ": ports 7958/7959");
            app = 7958;
            cmd = 7959;
        }
        APP = app;
        CMD = cmd;
        SPIELTEST = port("D2SPIELTEST_PORT", 7960);
    }

    private ModPorts() {
    }

    static int port(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null) return fallback;
        if (value.matches("[0-9]{1,15}")) {
            long port = Long.parseLong(value);
            if (port >= 1 && port <= 65535) return (int) port;
        }
        System.err.println(name + "=\"" + value + "\" invalid, port " + fallback);
        return fallback;
    }
}
