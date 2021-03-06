package com.batch.android.dispatcher.googleanalytics;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;

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
     * Event name used when logging on Google Analytics
     */
    private static final String NOTIFICATION_DISPLAY_NAME = "batch_notification_display";
    private static final String NOTIFICATION_OPEN_NAME = "batch_notification_open";
    private static final String NOTIFICATION_DISMISS_NAME = "batch_notification_dismiss";
    private static final String MESSAGING_SHOW_NAME = "batch_in_app_show";
    private static final String MESSAGING_CLOSE_NAME = "batch_in_app_close";
    private static final String MESSAGING_AUTO_CLOSE_NAME = "batch_in_app_auto_close";
    private static final String MESSAGING_CLICK_NAME = "batch_in_app_click";
    private static final String UNKNOWN_EVENT_NAME = "batch_unknown";

    private GoogleAnalytics googleAnalytics;
    private Tracker tracker;

    public GoogleAnalyticsDispatcher(Context context) {
        this.googleAnalytics = GoogleAnalytics.getInstance(context);
        this.tracker = null;
    }

    public static void setTrackingId(Context context, @XmlRes int trackingId)
    {
        GoogleAnalyticsDispatcher dispatcher = GoogleAnalyticsRegistrar.getInstance(context);
        dispatcher.setTrackingId(trackingId);
    }

    public static void setTrackingId(Context context, String trackingId)
    {
        GoogleAnalyticsDispatcher dispatcher = GoogleAnalyticsRegistrar.getInstance(context);
        dispatcher.setTrackingId(trackingId);
    }

    void setTrackingId(String trackingId) {
        if (tracker == null) {
            tracker = googleAnalytics.newTracker(trackingId);
        }
    }

    void setTrackingId(@XmlRes int trackingId) {
        if (tracker == null) {
            tracker = googleAnalytics.newTracker(trackingId);
        }
    }

    /**
     * Callback when a new event just happened in the Batch SDK.
     *
     * @param type The type of the event
     * @param payload The payload associated with the event
     */
    @Override
    public void dispatchEvent(@NonNull Batch.EventDispatcher.Type type, @NonNull Batch.EventDispatcher.Payload payload) {
        if (tracker == null) {
            return;
        }

        BatchEventBuilder builder = new BatchEventBuilder();
        builder.setLabel("batch");
        builder.setAction(getGoogleAnalyticsEventName(type));

        if (type.isNotificationEvent()) {
            buildNotificationParams(builder, payload);
        } else if (type.isMessagingEvent()) {
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
        builder.setTrackingId(payload.getTrackingId());

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
            builder.setCampaignContent(getQueryParameterCaseInsensitive(uri, UTM_CONTENT));
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
            case NOTIFICATION_DISPLAY:
                return NOTIFICATION_DISPLAY_NAME;
            case NOTIFICATION_OPEN:
                return NOTIFICATION_OPEN_NAME;
            case NOTIFICATION_DISMISS:
                return NOTIFICATION_DISMISS_NAME;
            case MESSAGING_SHOW:
                return MESSAGING_SHOW_NAME;
            case MESSAGING_CLOSE:
                return MESSAGING_CLOSE_NAME;
            case MESSAGING_AUTO_CLOSE:
                return MESSAGING_AUTO_CLOSE_NAME;
            case MESSAGING_CLICK:
                return MESSAGING_CLICK_NAME;
        }
        return UNKNOWN_EVENT_NAME;
    }

}
