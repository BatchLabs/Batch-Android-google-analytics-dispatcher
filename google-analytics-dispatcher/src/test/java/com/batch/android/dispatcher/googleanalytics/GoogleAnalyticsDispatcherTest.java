package com.batch.android.dispatcher.googleanalytics;


import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.batch.android.Batch;
import com.batch.android.BatchMessage;
import com.batch.android.BatchPushPayload;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * Test the Google Analytics Event Dispatcher implementation
 * The dispatcher should respect the UTM protocol from Google tools
 * See : https://ga-dev-tools.appspot.com/campaign-url-builder/
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
@PowerMockIgnore({"org.powermock.*", "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(GoogleAnalytics.class)
public class GoogleAnalyticsDispatcherTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    private Tracker tracker;
    private GoogleAnalyticsDispatcher googleAnalyticsDispatcher;

    @Before
    public void setUp() {
        Context context = PowerMockito.mock(Context.class);
        GoogleAnalytics googleAnalytics = PowerMockito.mock(GoogleAnalytics.class);
        tracker = PowerMockito.mock(Tracker.class);

        PowerMockito.mockStatic(GoogleAnalytics.class);
        Mockito.when(GoogleAnalytics.getInstance(context)).thenReturn(googleAnalytics);
        Mockito.when(googleAnalytics.newTracker(Mockito.anyString())).thenReturn(tracker);
        Mockito.when(googleAnalytics.newTracker(Mockito.anyInt())).thenReturn(tracker);

        googleAnalyticsDispatcher = new GoogleAnalyticsDispatcher(context);
        googleAnalyticsDispatcher.setTrackingId(0);
    }

    @Test
    public void testNotificationNoData() {

        TestEventPayload payload = new TestEventPayload(null,
                null,
                new Bundle());

        Map<String, String> expected = new HashMap<String, String>() {{
            put("&t", "event"); // Type
            put("&ea", "batch_notification_receive"); // Action
            put("&ec", "push"); // Category
            put("&el", "batch"); // Label
            put("&cs", "batch"); // Campaign Source
            put("&cm", "push"); // Campaign Medium
        }};

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_RECEIVE, payload);
        Mockito.verify(tracker).send(mapEq(expected));
    }

    @Test
    public void testNotificationDeeplinkQueryVars() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?utm_source=batchsdk&utm_medium=push-batch&utm_campaign=yoloswag&utm_content=button1",
                new Bundle());

        Map<String, String> expected = new HashMap<String, String>() {{
            put("&t", "event"); // Type
            put("&ea", "batch_notification_receive"); // Action
            put("&ec", "push"); // Category
            put("&el", "batch"); // Label
            put("&cn", "yoloswag"); // Campaign name
            put("&cs", "batchsdk"); // Campaign Source
            put("&cm", "push-batch"); // Campaign Medium
            put("&cc", "button1"); // Campaign Content
        }};

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_RECEIVE, payload);
        Mockito.verify(tracker).send(mapEq(expected));
    }

    @Test
    public void testNotificationDeeplinkQueryVarsEncode() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?utm_source=%5Bbatchsdk%5D&utm_medium=push-batch&utm_campaign=yoloswag&utm_content=button1",
                new Bundle());

        Map<String, String> expected = new HashMap<String, String>() {{
            put("&t", "event"); // Type
            put("&ea", "batch_notification_receive"); // Action
            put("&ec", "push"); // Category
            put("&el", "batch"); // Label
            put("&cn", "yoloswag"); // Campaign name
            put("&cs", "[batchsdk]"); // Campaign Source
            put("&cm", "push-batch"); // Campaign Medium
            put("&cc", "button1"); // Campaign Content
        }};

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_RECEIVE, payload);
        Mockito.verify(tracker).send(mapEq(expected));
    }

    @Test
    public void testNotificationDeeplinkFragmentVars() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com#utm_source=batch-sdk&utm_medium=pushbatch01&utm_campaign=154879548754&utm_content=notif001",
                new Bundle());

        Map<String, String> expected = new HashMap<String, String>() {{
            put("&t", "event"); // Type
            put("&ea", "batch_notification_open"); // Action
            put("&ec", "push"); // Category
            put("&el", "batch"); // Label
            put("&cn", "154879548754"); // Campaign name
            put("&cs", "batch-sdk"); // Campaign Source
            put("&cm", "pushbatch01"); // Campaign Medium
            put("&cc", "notif001"); // Campaign Content
        }};

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_OPEN, payload);
        Mockito.verify(tracker).send(mapEq(expected));
    }

    @Test
    public void testNotificationDeeplinkFragmentVarsEncode() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com/test#utm_source=%5Bbatch-sdk%5D&utm_medium=pushbatch01&utm_campaign=154879548754&utm_content=notif001",
                new Bundle());

        Map<String, String> expected = new HashMap<String, String>() {{
            put("&t", "event"); // Type
            put("&ea", "batch_notification_open"); // Action
            put("&ec", "push"); // Category
            put("&el", "batch"); // Label
            put("&cn", "154879548754"); // Campaign name
            put("&cs", "[batch-sdk]"); // Campaign Source
            put("&cm", "pushbatch01"); // Campaign Medium
            put("&cc", "notif001"); // Campaign Content
        }};

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_OPEN, payload);
        Mockito.verify(tracker).send(mapEq(expected));
    }

    @Test
    public void testNotificationCustomPayload() {

        Bundle customPayload = new Bundle();
        customPayload.putString("utm_medium", "654987");
        customPayload.putString("utm_source", "jesuisuntest");
        customPayload.putString("utm_campaign", "heinhein");
        customPayload.putString("utm_content", "allo118218");
        TestEventPayload payload = new TestEventPayload(null,
                null,
                customPayload);

        Map<String, String> expected = new HashMap<String, String>() {{
            put("&t", "event"); // Type
            put("&ea", "batch_notification_receive"); // Action
            put("&ec", "push"); // Category
            put("&el", "batch"); // Label
            put("&cn", "heinhein"); // Campaign name
            put("&cs", "jesuisuntest"); // Campaign Source
            put("&cm", "654987"); // Campaign Medium
        }};

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_RECEIVE, payload);
        Mockito.verify(tracker).send(mapEq(expected));
    }

    @Test
    public void testNotificationDeeplinkPriority() {
        // priority: Custom Payload > Query vars > Fragment vars
        Bundle customPayload = new Bundle();
        customPayload.putString("utm_medium", "654987");
        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?utm_source=batchsdk&utm_campaign=yoloswag#utm_source=batch-sdk&utm_medium=pushbatch01&utm_campaign=154879548754&utm_content=notif001",
                customPayload);

        Map<String, String> expected = new HashMap<String, String>() {{
            put("&t", "event"); // Type
            put("&ea", "batch_notification_open"); // Action
            put("&ec", "push"); // Category
            put("&el", "batch"); // Label
            put("&cn", "yoloswag"); // Campaign name
            put("&cs", "batchsdk"); // Campaign Source
            put("&cm", "654987"); // Campaign Medium
            put("&cc", "notif001"); // Campaign Content
        }};

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_OPEN, payload);
        Mockito.verify(tracker).send(mapEq(expected));
    }

    @Test
    public void testNotificationDeeplinkNonTrimmed() {
        Bundle customPayload = new Bundle();
        TestEventPayload payload = new TestEventPayload(null,
                "   \n     https://batch.com?utm_source=batchsdk&utm_campaign=yoloswag     \n ",
                customPayload);

        Map<String, String> expected = new HashMap<String, String>() {{
            put("&t", "event"); // Type
            put("&ea", "batch_notification_open"); // Action
            put("&ec", "push"); // Category
            put("&el", "batch"); // Label
            put("&cn", "yoloswag"); // Campaign name
            put("&cs", "batchsdk"); // Campaign Source
            put("&cm", "push"); // Campaign Medium
        }};

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_OPEN, payload);
        Mockito.verify(tracker).send(mapEq(expected));
    }

    @Test
    public void testNotificationDismissCampaign() {

        Bundle customPayload = new Bundle();
        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?utm_campaign=yoloswag",
                customPayload);

        Map<String, String> expected = new HashMap<String, String>() {{
            put("&t", "event"); // Type
            put("&ea", "batch_notification_dismiss"); // Action
            put("&ec", "push"); // Category
            put("&el", "batch"); // Label
            put("&cn", "yoloswag"); // Campaign name
            put("&cs", "batch"); // Campaign Source
            put("&cm", "push"); // Campaign Medium
        }};

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_DISMISS, payload);
        Mockito.verify(tracker).send(mapEq(expected));
    }

    @Test
    public void testInAppNoData() {

        TestEventPayload payload = new TestEventPayload(null,
                null,
                new Bundle());

        Map<String, String> expected = new HashMap<String, String>() {{
            put("&t", "event"); // Type
            put("&ea", "batch_in_app_show"); // Action
            put("&ec", "in-app"); // Category
            put("&el", "batch"); // Label
            put("&cs", "batch"); // Campaign Source
            put("&cm", "in-app"); // Campaign Medium
            put("batch_tracking_id", null);
        }};

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.IN_APP_SHOW, payload);
        Mockito.verify(tracker).send(mapEq(expected));
    }

    /*
    @Test
    public void testInAppShowUppercaseQueryVars() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?uTm_ConTENT=jesuisuncontent",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("batch_tracking_id", null);
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", null);
        expected.putString("content", "jesuisuncontent");

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.IN_APP_SHOW, payload);
        Mockito.verify(tracker).send(Mockito.eq("batch_in_app_show"), mapEq(expected));
    }

    @Test
    public void testInAppShowUppercaseFragmentVars() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com#UtM_CoNtEnT=jesuisuncontent",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("batch_tracking_id", null);
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", null);
        expected.putString("content", "jesuisuncontent");

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.IN_APP_SHOW, payload);
        Mockito.verify(tracker).send(Mockito.eq("batch_in_app_show"), mapEq(expected));
    }

    @Test
    public void testInAppTrackingId() {

        TestEventPayload payload = new TestEventPayload("jesuisunid",
                null,
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", "jesuisunid");
        expected.putString("batch_tracking_id", "jesuisunid");

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.IN_APP_GLOBAL_TAP, payload);
        Mockito.verify(tracker).send(Mockito.eq("batch_in_app_global_tap"), mapEq(expected));
    }

    @Test
    public void testInAppDeeplinkContentQueryVars() {

        TestEventPayload payload = new TestEventPayload("jesuisunid",
                "https://batch.com?utm_content=jesuisuncontent",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", "jesuisunid");
        expected.putString("batch_tracking_id", "jesuisunid");
        expected.putString("content", "jesuisuncontent");

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.IN_APP_CLOSE, payload);
        Mockito.verify(tracker).send(Mockito.eq("batch_in_app_close"), mapEq(expected));
    }

    @Test
    public void testInAppDeeplinkFragmentQueryVars() {

        TestEventPayload payload = new TestEventPayload("jesuisunid",
                "https://batch.com#utm_content=jesuisuncontent00587",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", "jesuisunid");
        expected.putString("batch_tracking_id", "jesuisunid");
        expected.putString("content", "jesuisuncontent00587");

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.IN_APP_DISMISS, payload);
        Mockito.verify(tracker).send(Mockito.eq("batch_in_app_dismiss"), mapEq(expected));
    }

    @Test
    public void testInAppDeeplinkContentPriority() {

        TestEventPayload payload = new TestEventPayload("jesuisunid",
                "https://batch.com?utm_content=jesuisuncontent002#utm_content=jesuisuncontent015",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", "jesuisunid");
        expected.putString("batch_tracking_id", "jesuisunid");
        expected.putString("content", "jesuisuncontent002");

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.IN_APP_AUTO_CLOSE, payload);
        Mockito.verify(tracker).send(Mockito.eq("batch_in_app_auto_close"), mapEq(expected));
    }

    @Test
    public void testInAppDeeplinkContentNoId() {

        TestEventPayload payload = new TestEventPayload(null,
                "https://batch.com?utm_content=jesuisuncontent",
                new Bundle());

        Bundle expected = new Bundle();
        expected.putString("medium", "in-app");
        expected.putString("source", "batch");
        expected.putString("campaign", null);
        expected.putString("batch_tracking_id", null);
        expected.putString("content", "jesuisuncontent");

        googleAnalyticsDispatcher.dispatchEvent(Batch.EventDispatcher.Type.IN_APP_CLICK, payload);
        Mockito.verify(tracker).send(Mockito.eq("batch_in_app_click"), mapEq(expected));
    }
     */

    private static class TestEventPayload implements Batch.EventDispatcher.Payload {

        private String trackingId;
        private String deeplink;
        private Bundle customPayload;

        TestEventPayload(String trackingId,
                         String deeplink, Bundle customPayload)
        {
            this.trackingId = trackingId;
            this.deeplink = deeplink;
            this.customPayload = customPayload;
        }

        @Nullable
        @Override
        public String getTrackingId()
        {
            return trackingId;
        }

        @Nullable
        @Override
        public String getDeeplink()
        {
            return deeplink;
        }

        @Nullable
        @Override
        public String getCustomValue(@NonNull String key)
        {
            if (customPayload == null) {
                return null;
            }
            return customPayload.getString(key);
        }

        @Override
        public boolean isPositiveAction() {
            return false;
        }

        @Nullable
        @Override
        public BatchMessage getInAppPayload()
        {
            return null;
        }

        @Nullable
        @Override
        public BatchPushPayload getPushPayload()
        {
            return null;
        }
    }

    private static Map<String, String> mapEq(Map<String, String> expected) {
        return Mockito.argThat(new MapObjectMatcher(expected));
    }

    private static class MapObjectMatcher implements ArgumentMatcher<Map<String, String>>
    {
        Map<String, String> expected;

        private MapObjectMatcher(Map<String, String> expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Map<String, String> map) {
            return equalMaps(map, expected);
        }

        private boolean equalMaps(Map<String, String> one, Map<String, String> two) {
            if (one.size() != two.size()) {
                return false;
            }

            return one.equals(two);
        }
    }

}
