package com.rftgscorer;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author gc
 */
class Player implements Serializable {

    String name;
    int chips;



    int getTotalScores() {
        return 12;
    }
}
