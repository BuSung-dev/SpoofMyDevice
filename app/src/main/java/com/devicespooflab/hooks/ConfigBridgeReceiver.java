package com.devicespooflab.hooks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.devicespooflab.hooks.data.AppProfileStore;

public class ConfigBridgeReceiver extends BroadcastReceiver {

    public static final String ACTION_GET_CONFIG = "com.spoofmydevice.action.GET_CONFIG";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_PACKAGE_NAME = "package_name";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || !ACTION_GET_CONFIG.equals(intent.getAction())) {
            return;
        }
        String content = AppProfileStore.resolveConfig(
            context,
            intent.getStringExtra(EXTRA_PACKAGE_NAME)
        );
        if (content != null) {
            Bundle extras = getResultExtras(true);
            extras.putString(EXTRA_CONTENT, content);
            setResultExtras(extras);
        }
    }
}
