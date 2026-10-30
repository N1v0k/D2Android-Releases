package com.winlator.d2;

import android.app.Activity;
import android.content.Intent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;















final class D2SaveMirror {
    static final String EXTRA = "d2_saves_spiegel";
    static final String RESULT = "ergebnis.txt";

    private D2SaveMirror() {
    }


    static boolean handle(Activity activity, Intent intent) {
        String direction = intent != null ? intent.getStringExtra(EXTRA) : null;
        if (direction == null) return false;
        File mirror = activity.getExternalFilesDir("spiegel");
        new Thread(() -> {
            String result = run(direction, mirror);
            D2Trace.log("saves mirror " + direction + ": " + result);
            if (mirror != null) {
                try (FileWriter out = new FileWriter(new File(mirror, RESULT))) {
                    out.write(result + "\n");
                }
                catch (IOException ignored) {

                }
            }
            activity.runOnUiThread(activity::finish);
        }, "d2-saves-mirror").start();
        return true;
    }


    private static String run(String direction, File mirror) {
        if (!"aus".equals(direction) && !"ein".equals(direction)) return "fehler: aus oder ein, nicht " + direction;
        if (mirror == null) return "fehler: kein App-Ordner auf dem gemeinsamen Speicher";

        new File(mirror, RESULT).delete();
        if (!D2SpieltestPort.tokenFresh("saves mirror")) return "fehler: kein frischer adb-Marker";
        if (D2TouchOverlay.isGameRunning() || !D2Saves.beginImport()) return "fehler: das Spiel laeuft oder startet gerade";
        try {
            if (!D2Store.ready()) return "nicht intern";
            if ("aus".equals(direction)) D2Umzug.mirrorOut(D2Store.saves(), mirror);
            else D2Umzug.mirrorIn(mirror, D2Store.base());
            return "ok " + direction;
        }
        catch (IOException | RuntimeException e) {
            return "fehler: " + e.getMessage();
        }
        finally {
            D2Saves.endImport();
        }
    }

}
