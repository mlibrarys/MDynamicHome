package com.mlibrary.multiapk.base.hack;

import android.app.Application;
import android.app.Instrumentation;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Created by yb.wang on 15/1/5.
 * Android中的Resource Hack
 */
public class AndroidHack {
    private static Object loadedApk = null;
    private static Object activityThread = null;

    private static Object getActivityThread() throws Exception {
        if (activityThread == null) {
            if (Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId()) {
                activityThread = SysHacks.ActivityThread_currentActivityThread.invoke(null);
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                //noinspection SynchronizeOnNonFinalField
                synchronized (SysHacks.ActivityThread_currentActivityThread) {
                    handler.post(new ActivityThreadGetter());
                    SysHacks.ActivityThread_currentActivityThread.wait();
                }
            }
        }
        return activityThread;
    }

    private static Object getLoadedApk(Object obj, String str) throws Exception {
        if (loadedApk == null) {
            WeakReference weakReference = (WeakReference) ((Map) SysHacks.ActivityThread_mPackages.get(obj)).get(str);
            if (weakReference != null) {
                loadedApk = weakReference.get();
            }
        }
        return loadedApk;
    }

    public static void injectResources(Application application, Resources resources) throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception("Failed to get ActivityThread.sCurrentActivityThread");
        }
        Object loadedApk = getLoadedApk(activityThread, application.getPackageName());
        if (loadedApk == null) {
            throw new Exception("Failed to get ActivityThread.mLoadedApk");
        }
        SysHacks.LoadedApk_mResources.set(loadedApk, resources);
        SysHacks.ContextImpl_mResources.set(application.getBaseContext(), resources);
        SysHacks.ContextImpl_mTheme.set(application.getBaseContext(), null);
    }

    public static Instrumentation getInstrumentation() throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread != null) {
            return SysHacks.ActivityThread_mInstrumentation.get(activityThread);
        }
        throw new Exception("Failed to get ActivityThread.sCurrentActivityThread");
    }

    public static void injectInstrumentationHook(Instrumentation instrumentation) throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception("Failed to get ActivityThread.sCurrentActivityThread");
        }
        SysHacks.ActivityThread_mInstrumentation.set(activityThread, instrumentation);
    }

    private static class ActivityThreadGetter implements Runnable {
        ActivityThreadGetter() {
        }

        public void run() {
            try {
                activityThread = SysHacks.ActivityThread_currentActivityThread.invoke(SysHacks.ActivityThread.getClazz());
            } catch (Exception e) {
                e.printStackTrace();
            }
            //noinspection SynchronizeOnNonFinalField
            synchronized (SysHacks.ActivityThread_currentActivityThread) {
                SysHacks.ActivityThread_currentActivityThread.notify();
            }
        }
    }
}
