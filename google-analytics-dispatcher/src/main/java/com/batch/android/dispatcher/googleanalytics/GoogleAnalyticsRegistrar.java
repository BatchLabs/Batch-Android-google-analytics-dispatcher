package com.batch.android.dispatcher.googleanalytics;

import android.content.Context;

import com.batch.android.BatchEventDispatcher;
import com.batch.android.eventdispatcher.DispatcherRegistrar;

/**
 * Google Analytics Registrar
 * The class will instantiate from the SDK using reflection
 * See the library {@link android.Manifest} for more information
 */
public class GoogleAnalyticsRegistrar implements DispatcherRegistrar {

    /**
     * Singleton instance
     */
    private static GoogleAnalyticsDispatcher instance = null;

    /**
     * Singleton accessor
     * @param context Context used to initialize the dispatcher
     * @return Dispatcher instance
     */
    static GoogleAnalyticsDispatcher getInstance(Context context)
    {
        if (instance == null) {
            instance = new GoogleAnalyticsDispatcher(context);
        }
        return instance;
    }

    /**
     * Singleton accessor
     * @param context Context used to initialize the dispatcher
     * @return Dispatcher instance
     */
    @Override
    public BatchEventDispatcher getDispatcher(Context context)
    {
        return getInstance(context);
    }

}
