package com.easyads.sdk.appopen

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

import java.lang.ref.WeakReference

internal class AppOpenLifecycleTracker(
    private val appOpenManager: AppOpenAdManager
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var currentActivityRef: WeakReference<Activity>? = null
    private var isColdStart = true

    private val currentActivity: Activity?
        get() = currentActivityRef?.get()

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isColdStart) {
            isColdStart = false
            return
        }
        currentActivity?.let { activity ->
            appOpenManager.showOnResume(activity)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (!activity.javaClass.name.startsWith("com.google.android.gms.ads")) {
            currentActivityRef = WeakReference(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!activity.javaClass.name.startsWith("com.google.android.gms.ads")) {
            currentActivityRef = WeakReference(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
}
