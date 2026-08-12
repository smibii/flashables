package com.smibii.flashables.helper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private static void formatPrint(LogLevel level, String stackTrace, Object... args) {
        LocalTime now = LocalTime.now();
        String formattedTime = formatTime(now);

        StringBuilder message = new StringBuilder();
        for (Object arg : args) {
            if (arg == null) arg = "null";
            message.append(" ").append(arg);
        }

        String formattedLog = String.format("%s[%s] [%s] [%s]%s%s",
                level.color.code,
                formattedTime,
                level.name,
                stackTrace,
                message,
                LogColor.RESET.code
        );

        System.out.println(formattedLog);
    }

    private static String getStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement caller = stackTrace[3];
        return String.format("%s:%d - %s",
                caller.getFileName(),
                caller.getLineNumber(),
                caller.getMethodName()
        );
    }

    public static void log(Object... args) {
        formatPrint(LogLevel.LOG, getStackTrace(), args);
    }

    public static void info(Object... args) {
        formatPrint(LogLevel.INFO, getStackTrace(), args);
    }

    public static void error(Object... args) {
        formatPrint(LogLevel.ERROR, getStackTrace(), args);
    }

    public static void warn(Object... args) {
        formatPrint(LogLevel.WARN, getStackTrace(), args);
    }

    public static void debug(Object... args) {
        formatPrint(LogLevel.DEBUG, getStackTrace(), args);
    }

    public static void success(Object... args) {
        formatPrint(LogLevel.SUCCESS, getStackTrace(), args);
    }

    public static void trace() { trace(-1); }
    public static void trace(int limit) {
        StringBuilder stringTrace = new StringBuilder();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (limit > 0) {
            for (int i = 0; i < limit; i++) {
                stringTrace.append(stackTrace[i].toString()).append("\n");
            }
        } else {
            for (StackTraceElement stackTraceElement : stackTrace) {
                stringTrace.append(stackTraceElement.toString()).append("\n");
            }
        }
        formatPrint(LogLevel.LOG, getStackTrace(), stringTrace.toString());
    }

    private enum LogColor {
        BLUE("\u001B[34m"),
        RED("\u001B[31m"),
        ORANGE("\u001B[38;5;214m"),
        DARK_GRAY("\u001B[37m"),
        GREEN("\u001B[32m"),
        RESET("\u001B[0m");
        public final String code;

        LogColor(String code) {
            this.code = code;
        }
    }

    private enum LogLevel {
        LOG("LOG    ", LogColor.RESET),
        INFO("INFO   ", LogColor.BLUE),
        ERROR("ERROR  ", LogColor.RED),
        WARN("WARN   ", LogColor.ORANGE),
        DEBUG("DEBUG  ", LogColor.DARK_GRAY),
        SUCCESS("SUCCESS", LogColor.GREEN);

        public final String name;
        public final LogColor color;

        LogLevel(String name, LogColor color) {
            this.name = name;
            this.color = color;
        }
    }
}
