package com.devicespooflab.hooks.data

import android.content.Context

object AppProfileSession {

    @Volatile
    private var cachedState: AppProfileStore.State? = null

    @Synchronized
    fun ensureLoaded(
        context: Context,
        store: AppProfileStore,
        presets: List<DevicePreset>,
        defaultProfileName: String,
    ): AppProfileStore.State {
        cachedState?.let { return it }
        return store.ensureLoaded(context, presets, defaultProfileName).also(::update)
    }

    @Synchronized
    fun getOrLoad(context: Context, store: AppProfileStore): AppProfileStore.State {
        cachedState?.let { return it }
        return store.load(context).also(::update)
    }

    fun current(): AppProfileStore.State? = cachedState

    @Synchronized
    fun update(state: AppProfileStore.State) {
        cachedState = state
    }
}
