package com.rftgscorer;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * @author gc
 */
class Settings implements Serializable {

    Card.GameType gameType = Card.GameType.BASE;
}
