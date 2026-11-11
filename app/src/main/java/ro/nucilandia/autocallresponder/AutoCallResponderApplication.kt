package ro.nucilandia.autocallresponder

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class AutoCallResponderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appInstance = this
        SettingsManager.init(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    companion object {
        lateinit var appInstance: AutoCallResponderApplication
            private set
    }
}

class AppLifecycleObserver : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        // App moved to foreground
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        // App moved to background
    }
}
