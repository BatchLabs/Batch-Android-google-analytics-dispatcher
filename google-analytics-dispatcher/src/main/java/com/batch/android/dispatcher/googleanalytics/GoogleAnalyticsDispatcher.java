package com.batch.android.dispatcher.googleanalytics;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.batch.android.Batch;
import com.batch.android.BatchEventDispatcher;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Google Analytics Event Dispatcher
 * The dispatcher should generate UTM tag from a Batch payload and send them to the Google Analytics SDK
 * See : https://ga-dev-tools.appspot.com/campaign-url-builder/
 * Only works for Google Analytics 360 properties, please migrate to Firebase as soon as possible.
 * See : https://support.google.com/firebase/answer/9167112
 */
public class GoogleAnalyticsDispatcher implements BatchEventDispatcher {

    /**
     * UTM tag keys
     */
    private static final String UTM_CAMPAIGN = "utm_campaign";
    private static final String UTM_SOURCE = "utm_source";
    private static final String UTM_MEDIUM = "utm_medium";
    private static final String UTM_CONTENT = "utm_content";

    /**
     * Key used to dispatch the Batch tracking Id on Google Analytics
     */
    private static final String BATCH_TRACKING_ID = "batch_tracking_id";

    /**
     * Event name used when logging on Google Analytics
     */
    private static final String NOTIFICATION_RECEIVE_NAME = "batch_notification_receive";
    private static final String NOTIFICATION_OPEN_NAME = "batch_notification_open";
    private static final String NOTIFICATION_DISMISS_NAME = "batch_notification_dismiss";
    private static final String IN_APP_SHOW_NAME = "batch_in_app_show";
    private static final String IN_APP_DISMISS_NAME = "batch_in_app_dismiss";
    private static final String IN_APP_CLOSE_NAME = "batch_in_app_close";
    private static final String IN_APP_AUTO_CLOSE_NAME = "batch_in_app_auto_close";
    private static final String IN_APP_GLOBAL_TAP_NAME = "batch_in_app_global_tap";
    private static final String IN_APP_CLICK_NAME = "batch_in_app_click";
    private static final String UNKNOWN_EVENT_NAME = "batch_unknown";

    private GoogleAnalytics googleAnalytics;
    private Tracker tracker;

    public GoogleAnalyticsDispatcher(Context context) {
        this.googleAnalytics = GoogleAnalytics.getInstance(context);
        this.tracker = null;
    }

    public void setTrackingId(String trackingId) {
        if (tracker == null) {
            tracker = googleAnalytics.newTracker(trackingId);
        }
    }

    public void setTrackingId(@IdRes int trackingId) {
        if (tracker == null) {
            tracker = googleAnalytics.newTracker(trackingId);
        }
    }

    @Override
    public void dispatchEvent(@NonNull Batch.EventDispatcher.Type type, @NonNull Batch.EventDispatcher.Payload payload) {
        if (tracker == null) {
            return;
        }

        BatchEventBuilder builder = new BatchEventBuilder();
        builder.setLabel("batch");
        builder.setAction(getGoogleAnalyticsEventName(type));

        if (type.isNotification()) {
            buildNotificationParams(builder, payload);
        } else if (type.isInApp()) {
            buildInAppParams(builder, payload);
        }

        tracker.send(builder.build());
    }

    private static void buildInAppParams(BatchEventBuilder builder, Batch.EventDispatcher.Payload payload)
    {
        builder.setCategory("in-app");
        builder.setCampaignName(payload.getTrackingId());
        builder.setCampaignSource("batch");
        builder.setCampaignMedium("in-app");
        builder.set(BATCH_TRACKING_ID, payload.getTrackingId());

        String deeplink = payload.getDeeplink();
        if (deeplink != null) {
            deeplink = deeplink.trim();
            Uri uri = Uri.parse(deeplink);

            String fragment = uri.getFragment();
            if (fragment != null && !fragment.isEmpty()) {
                Map<String, String> fragments = getFragmentMap(fragment);
                // Copy from fragment part of the deeplink
                builder.setCampaignContent(fragments.get(UTM_CONTENT));
            }
            // Copy from query parameters of the deeplink
            builder.setCampaignContent(uri.getQueryParameter(UTM_CONTENT));
        }

        // Load from custom payload
        builder.setCampaignName(payload.getCustomValue(UTM_CAMPAIGN));
        builder.setCampaignSource(payload.getCustomValue(UTM_SOURCE));
        builder.setCampaignMedium(payload.getCustomValue(UTM_MEDIUM));
    }

    private static void buildNotificationParams(BatchEventBuilder builder, Batch.EventDispatcher.Payload payload)
    {
        builder.setCategory("push");
        builder.setCampaignSource("batch");
        builder.setCampaignMedium("push");

        String deeplink = payload.getDeeplink();
        if (deeplink != null) {
            deeplink = deeplink.trim();
            Uri uri = Uri.parse(deeplink);

            String fragment = uri.getFragment();
            if (fragment != null && !fragment.isEmpty()) {
                Map<String, String> fragments = getFragmentMap(fragment);
                // Copy from fragment part of the deeplink
                builder.setCampaignContent(fragments.get(UTM_CONTENT));
                builder.setCampaignMedium(fragments.get(UTM_MEDIUM));
                builder.setCampaignSource(fragments.get(UTM_SOURCE));
                builder.setCampaignName(fragments.get(UTM_CAMPAIGN));
            }
            // Copy from query parameters of the deeplink
            builder.setCampaignContent(getQueryParameterCaseInsensitive(uri, UTM_CONTENT));
            builder.setCampaignMedium(getQueryParameterCaseInsensitive(uri, UTM_MEDIUM));
            builder.setCampaignSource(getQueryParameterCaseInsensitive(uri, UTM_SOURCE));
            builder.setCampaignName(getQueryParameterCaseInsensitive(uri, UTM_CAMPAIGN));
        }

        // Load from custom payload
        builder.setCampaignName(payload.getCustomValue(UTM_CAMPAIGN));
        builder.setCampaignSource(payload.getCustomValue(UTM_SOURCE));
        builder.setCampaignMedium(payload.getCustomValue(UTM_MEDIUM));
    }

    private static Map<String, String> getFragmentMap(String fragment)
    {
        String[] params = fragment.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String[] parts = param.split("=");
            if (parts.length >= 2) {
                map.put(parts[0].toLowerCase(), parts[1]);
            }
        }
        return map;
    }

    private static String getQueryParameterCaseInsensitive(Uri uri, String keyFrom)
    {
        Set<String> keys = uri.getQueryParameterNames();
        for (String key : keys) {
            if (keyFrom.equalsIgnoreCase(key)) {
                return uri.getQueryParameter(key);
            }
        }
        return null;
    }


    private static String getGoogleAnalyticsEventName(Batch.EventDispatcher.Type type) {
        switch (type) {
            case NOTIFICATION_RECEIVE:
                return NOTIFICATION_RECEIVE_NAME;
            case NOTIFICATION_OPEN:
                return NOTIFICATION_OPEN_NAME;
            case NOTIFICATION_DISMISS:
                return NOTIFICATION_DISMISS_NAME;
            case IN_APP_SHOW:
                return IN_APP_SHOW_NAME;
            case IN_APP_DISMISS:
                return IN_APP_DISMISS_NAME;
            case IN_APP_CLOSE:
                return IN_APP_CLOSE_NAME;
            case IN_APP_AUTO_CLOSE:
                return IN_APP_AUTO_CLOSE_NAME;
            case IN_APP_GLOBAL_TAP:
                return IN_APP_GLOBAL_TAP_NAME;
            case IN_APP_CLICK:
                return IN_APP_CLICK_NAME;
        }
        return UNKNOWN_EVENT_NAME;
    }

}
