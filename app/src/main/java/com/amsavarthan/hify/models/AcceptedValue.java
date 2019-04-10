package com.amsavarthan.hify.models;

import android.support.annotation.NonNull;

public class AcceptedValue {

    public boolean accepted;

    public <T extends AcceptedValue> T withAccepted(@NonNull final boolean id) {
        this.accepted = id;
        return (T) this;
    }

}
