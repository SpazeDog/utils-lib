package com.spazedog.lib.utilsLib;


import android.os.Parcelable;

/**
 * Simple convenience interface for adding both {@link JSONParcelable} and {@link Parcelable}
 * to a class
 */
public interface MultiParcelable extends JSONParcelable, Parcelable {

    public interface MultiCreator<T extends MultiParcelable> extends JSONParcelable.JSONCreator<T>, Parcelable.Creator<T> {

    }
}
