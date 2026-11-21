package com.drakahell.claudesearch

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds use the App Check debug provider. On first launch it prints a debug token
 * to logcat (tag "DebugAppCheckProvider"); that token must be added in
 * Firebase console -> App Check -> Apps -> Manage debug tokens.
 */
internal object AppCheckSetup {
    fun install() {
        Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
