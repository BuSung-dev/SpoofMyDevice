package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks shell getprop access so apps cannot see original and spoofed values at the same time.
 * Many device info apps gather properties from both SystemProperties and a subprocess call to getprop.
 */
public final class GetPropHooks {

    private static final String TAG = "SpoofMyDevice-GetProp";

    private GetPropHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                ProcessBuilder.class,
                "start",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Process originalProcess = (Process) param.getResult();
                        if (originalProcess == null) {
                            return;
                        }

                        List<String> command = readCommand(param.thisObject);
                        GetPropRequest request = parseRequest(command);
                        if (request == null) {
                            return;
                        }

                        Process replacement = buildReplacementProcess(originalProcess, request);
                        if (replacement != null) {
                            param.setResult(replacement);
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook ProcessBuilder.start(): " + throwable.getMessage());
        }

        hookRuntimeExec();
    }

    private static void hookRuntimeExec() {
        hookRuntimeExecVariant(new Object[]{String.class});
        hookRuntimeExecVariant(new Object[]{String[].class});
        hookRuntimeExecVariant(new Object[]{String.class, String[].class});
        hookRuntimeExecVariant(new Object[]{String[].class, String[].class});
        hookRuntimeExecVariant(new Object[]{String.class, String[].class, java.io.File.class});
        hookRuntimeExecVariant(new Object[]{String[].class, String[].class, java.io.File.class});
    }

    private static void hookRuntimeExecVariant(Object[] parameterTypes) {
        Object[] hookArgs = Arrays.copyOf(parameterTypes, parameterTypes.length + 1);
        hookArgs[parameterTypes.length] = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Process originalProcess = (Process) param.getResult();
                if (originalProcess == null || param.args.length == 0 || param.args[0] == null) {
                    return;
                }

                GetPropRequest request = null;
                Object commandArg = param.args[0];
                if (commandArg instanceof String) {
                    request = parseShellCommand((String) commandArg);
                } else if (commandArg instanceof String[]) {
                    request = parseRequest(Arrays.asList((String[]) commandArg));
                }

                if (request == null) {
                    return;
                }

                Process replacement = buildReplacementProcess(originalProcess, request);
                if (replacement != null) {
                    param.setResult(replacement);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", hookArgs);
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> readCommand(Object processBuilder) {
        try {
            Object value = XposedHelpers.callMethod(processBuilder, "command");
            if (value instanceof List) {
                return new ArrayList<>((List<String>) value);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static GetPropRequest parseRequest(List<String> command) {
        if (command == null || command.isEmpty()) {
            return null;
        }

        List<String> normalized = new ArrayList<>(command.size());
        for (String part : command) {
            normalized.add(part == null ? "" : part.trim());
        }

        String first = leafName(normalized.get(0));
        if ("getprop".equals(first)) {
            if (normalized.size() >= 2) {
                return new GetPropRequest(false, normalized.get(1));
            }
            return new GetPropRequest(true, null);
        }

        if (("sh".equals(first) || "su".equals(first)) && normalized.size() >= 3) {
            int commandIndex = normalized.indexOf("-c");
            if (commandIndex >= 0 && commandIndex + 1 < normalized.size()) {
                return parseShellCommand(normalized.get(commandIndex + 1));
            }
        }
        return null;
    }

    private static GetPropRequest parseShellCommand(String shellCommand) {
        if (shellCommand == null) {
            return null;
        }
        String trimmed = shellCommand.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 0) {
            return null;
        }
        String first = leafName(parts[0]);
        if (!"getprop".equals(first)) {
            return null;
        }
        if (parts.length >= 2) {
            return new GetPropRequest(false, parts[1]);
        }
        return new GetPropRequest(true, null);
    }

    private static Process buildReplacementProcess(Process originalProcess, GetPropRequest request) {
        if (request.fullDump) {
            String originalStdout = readStream(originalProcess.getInputStream());
            String originalStderr = readStream(originalProcess.getErrorStream());
            int exitCode = waitForExit(originalProcess);
            String rewritten = rewriteGetPropDump(originalStdout);
            return new SimpleProcess(rewritten, originalStderr, exitCode);
        }

        String key = request.propertyKey;
        if (key == null || key.isEmpty()) {
            return null;
        }

        String spoofedValue = ConfigManager.getSystemProperty(key, null);
        if (spoofedValue == null) {
            return null;
        }

        readStream(originalProcess.getInputStream());
        String originalStderr = readStream(originalProcess.getErrorStream());
        int exitCode = waitForExit(originalProcess);
        return new SimpleProcess(spoofedValue + "\n", originalStderr, exitCode);
    }

    private static String rewriteGetPropDump(String stdout) {
        Map<String, String> spoofed = new LinkedHashMap<>(ConfigManager.getEffectiveSystemProperties());
        if (spoofed.isEmpty()) {
            return stdout;
        }

        Map<String, String> merged = new LinkedHashMap<>();
        if (stdout != null && !stdout.isEmpty()) {
            List<String> lines = Arrays.asList(stdout.split("\\r?\\n"));
            for (String line : lines) {
                ParsedProperty parsed = parsePropertyLine(line);
                if (parsed == null) {
                    continue;
                }
                merged.put(parsed.key, parsed.value);
            }
        }

        merged.putAll(spoofed);

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : merged.entrySet()) {
            builder.append('[')
                .append(entry.getKey())
                .append("]: [")
                .append(entry.getValue() == null ? "" : entry.getValue())
                .append("]\n");
        }
        return builder.toString();
    }

    private static ParsedProperty parsePropertyLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("[") || !trimmed.contains("]: [") || !trimmed.endsWith("]")) {
            return null;
        }
        int middle = trimmed.indexOf("]: [");
        if (middle <= 1) {
            return null;
        }
        String key = trimmed.substring(1, middle);
        String value = trimmed.substring(middle + 4, trimmed.length() - 1);
        return new ParsedProperty(key, value);
    }

    private static String readStream(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (IOException ignored) {
            return "";
        }
    }

    private static int waitForExit(Process process) {
        try {
            return process.waitFor();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    private static String leafName(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = value.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return (slash >= 0 ? normalized.substring(slash + 1) : normalized).toLowerCase(Locale.US);
    }

    private static final class GetPropRequest {
        final boolean fullDump;
        final String propertyKey;

        GetPropRequest(boolean fullDump, String propertyKey) {
            this.fullDump = fullDump;
            this.propertyKey = propertyKey;
        }
    }

    private static final class ParsedProperty {
        final String key;
        final String value;

        ParsedProperty(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final class SimpleProcess extends Process {
        private final ByteArrayInputStream inputStream;
        private final ByteArrayInputStream errorStream;
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private final int exitCode;

        SimpleProcess(String stdout, String stderr, int exitCode) {
            this.inputStream = new ByteArrayInputStream((stdout == null ? "" : stdout).getBytes(StandardCharsets.UTF_8));
            this.errorStream = new ByteArrayInputStream((stderr == null ? "" : stderr).getBytes(StandardCharsets.UTF_8));
            this.exitCode = exitCode;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public InputStream getErrorStream() {
            return errorStream;
        }

        @Override
        public int waitFor() {
            return exitCode;
        }

        @Override
        public int exitValue() {
            return exitCode;
        }

        @Override
        public void destroy() {
        }
    }
}
