package com.spazedog.lib.utilsLib;


import android.os.Parcel;

import com.spazedog.lib.utilsLib.JSONParcel.JSONException;

import java.lang.reflect.Constructor;

/**
 * This class can be used to easier add Multi Parceling to your classes.
 * Simply extend from this class and your class will implement the interfaces
 * and have a finished CREATOR field that can re-create your class.
 */
public abstract class MultiParcelableBuilder implements MultiParcelable {

    public MultiParcelableBuilder() {

    }

    public MultiParcelableBuilder(JSONParcel in, ClassLoader loader) {

    }

    public MultiParcelableBuilder(Parcel in, ClassLoader loader) {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToJSON(JSONParcel out) throws JSONException {
        out.writeString(getClass().getName());
    };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(getClass().getName());
    };

    public final void parcelData(Object value, Parcel dest, int flags) {
        ParcelHelper.parcelData(value, dest, flags);
    }

    public final Object unparcelData(Parcel source, ClassLoader loader) {
        return ParcelHelper.unparcelData(source, loader);
    }

    public static final MultiCreator<MultiParcelable> CREATOR = new MultiCreator<MultiParcelable>() {

        @Override
        public MultiParcelable createFromJSON(JSONParcel source, ClassLoader loader) throws JSONException, RuntimeException {
            try {
                Class<?> clazz = Class.forName(source.readString(), true, MultiParcelableBuilder.class.getClassLoader());
                Constructor<?> constrcutor = clazz.getDeclaredConstructor(JSONParcel.class, ClassLoader.class);
                constrcutor.setAccessible(true);

                return (MultiParcelable) constrcutor.newInstance(source, loader);

            } catch (Throwable e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        @Override
        public MultiParcelable createFromParcel(Parcel source) throws RuntimeException {
            try {
                Class<?> clazz = Class.forName(source.readString(), true, MultiParcelableBuilder.class.getClassLoader());
                Constructor<?> constrcutor = clazz.getDeclaredConstructor(Parcel.class, ClassLoader.class);
                constrcutor.setAccessible(true);

                return (MultiParcelable) constrcutor.newInstance(source, Parcel.class.getClassLoader());

            } catch (Throwable e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        @Override
        public MultiParcelable[] newArray(int size) {
            return new MultiParcelable[size];
        }
    };
}
