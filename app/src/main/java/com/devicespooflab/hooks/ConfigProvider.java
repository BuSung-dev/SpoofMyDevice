package com.devicespooflab.hooks;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.os.Bundle;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;

public class ConfigProvider extends ContentProvider {

    public static final String AUTHORITY = "com.spoofmydevice.configprovider";
    public static final String FILE_NAME = "device_profile.conf";
    public static final Uri CONFIG_URI = Uri.parse("content://" + AUTHORITY + "/" + FILE_NAME);
    public static final String COLUMN_CONTENT = "content";
    public static final String METHOD_GET_CONFIG = "get_config";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (getContext() == null || !FILE_NAME.equals(uri.getLastPathSegment())) {
            throw new FileNotFoundException("Unknown config uri: " + uri);
        }

        File configFile = new File(getContext().getFilesDir(), FILE_NAME);
        if (!configFile.exists()) {
            throw new FileNotFoundException("Config file does not exist yet");
        }

        return ParcelFileDescriptor.open(configFile, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "text/plain";
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        String content = readConfigContent();
        if (content == null) {
            return null;
        }
        MatrixCursor cursor = new MatrixCursor(new String[]{COLUMN_CONTENT});
        cursor.addRow(new Object[]{content});
        return cursor;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (!METHOD_GET_CONFIG.equals(method)) {
            return super.call(method, arg, extras);
        }
        String content = readConfigContent();
        if (content == null) {
            return null;
        }
        Bundle bundle = new Bundle();
        bundle.putString(COLUMN_CONTENT, content);
        return bundle;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable
    private String readConfigContent() {
        if (getContext() == null) {
            return null;
        }

        File configFile = new File(getContext().getFilesDir(), FILE_NAME);
        if (!configFile.exists()) {
            return null;
        }

        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            byte[] bytes = new byte[(int) configFile.length()];
            int read = inputStream.read(bytes);
            if (read < 0) {
                return "";
            }
            return new String(bytes, 0, read, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }
}
