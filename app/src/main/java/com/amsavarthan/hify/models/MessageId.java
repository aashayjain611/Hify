package com.amsavarthan.hify.models;

import android.support.annotation.NonNull;

public class MessageId {

    public String msgId;

    public <T extends MessageId> T withId(@NonNull final String id) {
        this.msgId = id;
        return (T) this;
    }

}
