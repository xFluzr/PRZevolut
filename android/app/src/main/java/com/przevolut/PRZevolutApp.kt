package com.przevolut

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Klasa aplikacji — wymagana przez Hilt do Dependency Injection.
 * Zarejestrowana w AndroidManifest.xml jako android:name=".PRZevolutApp"
 */
@HiltAndroidApp
class PRZevolutApp : Application()
