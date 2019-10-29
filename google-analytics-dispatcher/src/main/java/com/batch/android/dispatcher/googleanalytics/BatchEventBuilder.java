package com.batch.android.dispatcher.googleanalytics;

import com.google.android.gms.analytics.HitBuilders;

public class BatchEventBuilder extends HitBuilders.EventBuilder {

    /**
     * Google Analytics internal UTM tag keys
     */
    private static final String SOURCE = "&cs";
    private static final String MEDIUM = "&cm";
    private static final String CAMPAIGN = "&cn";
    private static final String CONTENT = "&cc";

    public BatchEventBuilder setCampaignSource(String source) {
        if (source != null) {
            this.set(SOURCE, source);
        }
        return this;
    }

    public BatchEventBuilder setCampaignMedium(String medium) {
        if (medium != null) {
            this.set(MEDIUM, medium);
        }
        return this;
    }

    public BatchEventBuilder setCampaignContent(String content) {
        if (content != null) {
            this.set(CONTENT, content);
        }
        return this;
    }

    public BatchEventBuilder setCampaignName(String name) {
        if (name != null) {
            this.set(CAMPAIGN, name);
        }
        return this;
    }

}
