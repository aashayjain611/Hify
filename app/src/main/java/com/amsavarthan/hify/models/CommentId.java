package com.amsavarthan.hify.models;

import android.support.annotation.NonNull;

public class CommentId {

    public String commentId;

    public <T extends CommentId> T withId(@NonNull final String id) {
        this.commentId = id;
        return (T) this;
    }

}
