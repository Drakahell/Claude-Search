package com.drakahell.claudesearch

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Release builds prove their authenticity with Play Integrity. Requires the app's
 * SHA-256 fingerprint to be registered in Firebase console (done before publishing).
 */
internal object AppCheckSetup {
    fun install() {
        Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
