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

import java.io.FileNotFoundException;
import java.io.IOException;

import com.devicespooflab.hooks.data.AppProfileStore;

public class ConfigProvider extends ContentProvider {

    public static final String AUTHORITY = "com.spoofmydevice.configprovider";
    public static final String FILE_NAME = "resolved_profile.conf";
    public static final Uri CONFIG_URI = Uri.parse("content://" + AUTHORITY + "/" + FILE_NAME);
    public static final String COLUMN_CONTENT = "content";
    public static final String METHOD_GET_CONFIG = "get_config";
    public static final String QUERY_PACKAGE = "package";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (getContext() == null || !FILE_NAME.equals(uri.getLastPathSegment()) || !"r".equals(mode)) {
            throw new FileNotFoundException("Unknown config uri: " + uri);
        }
        String content = readConfigContent(resolvePackageName(uri, null));
        if (content == null) {
            throw new FileNotFoundException("No profile is assigned to the requested package");
        }
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            Thread writer = new Thread(() -> {
                try (ParcelFileDescriptor.AutoCloseOutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                    outputStream.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                }
            }, "spoofmydevice-config-provider");
            writer.start();
            return pipe[0];
        } catch (IOException exception) {
            throw new FileNotFoundException(exception.getMessage());
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "text/plain";
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        String requestedPackage = selectionArgs != null && selectionArgs.length > 0 ? selectionArgs[0] : null;
        String content = readConfigContent(resolvePackageName(uri, requestedPackage));
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
        String content = readConfigContent(resolvePackageName(CONFIG_URI, arg));
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
    private String readConfigContent(String packageName) {
        return AppProfileStore.resolveConfig(getContext(), packageName);
    }

    private String resolvePackageName(Uri uri, String requestedPackage) {
        if (requestedPackage != null && !requestedPackage.trim().isEmpty()) {
            return requestedPackage.trim();
        }
        String queryPackage = uri.getQueryParameter(QUERY_PACKAGE);
        if (queryPackage != null && !queryPackage.trim().isEmpty()) {
            return queryPackage.trim();
        }
        return getCallingPackage();
    }
}
