/*
 * Copyright (C) 2026 Zexolver
 *
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * see the LICENSE file at the root of this repository.
 */
package com.android.launcher3.debug;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Temporary diagnostic helper: writes uncaught-exception stack traces to a plain text file in
 * app-specific external storage (no permissions required, reachable via a file manager at
 * Android/data/&lt;applicationId&gt;/files/crash_log.txt), so a crash can be diagnosed from a
 * device with no ADB/logcat access. Debug builds only; does not alter normal crash behavior
 * (the system's default handler still runs afterwards).
 */
public final class CrashLogger {

    private static final String TAG = "CrashLogger";
    private static final String FILE_NAME = "crash_log.txt";

    private CrashLogger() {}

    public static void install(Context context) {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                writeToFile(context, thread, throwable);
            } catch (Throwable loggingFailure) {
                Log.e(TAG, "Failed to write crash log", loggingFailure);
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }

    private static void writeToFile(Context context, Thread thread, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("=== " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date()) + " (thread: " + thread.getName() + ") ===");
        throwable.printStackTrace(pw);
        pw.println();
        String text = sw.toString();

        Log.e(TAG, text);

        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            return;
        }
        File file = new File(dir, FILE_NAME);
        try (FileWriter writer = new FileWriter(file, /* append= */ true)) {
            writer.write(text);
        } catch (Exception e) {
            Log.e(TAG, "Could not write " + file, e);
        }
    }
}
