/*
 * This file is part of the UtilsLib Project: https://github.com/spazedog/utils-lib
 *
 * Copyright (c) 2015 Daniel Bergløv
 *
 * UtilsLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * UtilsLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with UtilsLib. If not, see <http://www.gnu.org/licenses/>
 */

package com.spazedog.lib.utilsLib;


import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class JSONParcel implements Parcelable {

    public static class JSONException extends Exception {
        public JSONException(String detailMessage) {
            super(detailMessage);
        }

        public JSONException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }

    protected static final int SCHEMA_VERSION = 1;

    protected static final int TYPE_NULL = -1;
    protected static final int TYPE_JSONPARCEL = 0;
    protected static final int TYPE_JSONPARCELARRAY = 1;
    protected static final int TYPE_STRING = 2;
    protected static final int TYPE_STRINGARRAY = 3;
    protected static final int TYPE_INTEGER = 4;
    protected static final int TYPE_INTEGERARRAY = 5;
    protected static final int TYPE_LONG = 6;
    protected static final int TYPE_LONGARRAY = 7;
    protected static final int TYPE_DOUBLE = 8;
    protected static final int TYPE_DOUBLEARRAY = 9;
    protected static final int TYPE_BYTE = 10;
    protected static final int TYPE_BYTEARRAY = 11;
    protected static final int TYPE_CHAR = 12;
    protected static final int TYPE_CHARARRAY = 13;
    protected static final int TYPE_BOOLEAN = 14;
    protected static final int TYPE_BOOLEANARRAY = 15;
    protected static final int TYPE_FLOAT = 16;
    protected static final int TYPE_FLOATARRAY = 17;
    protected static final int TYPE_OBJECTARRAY = 512;
    protected static final int TYPE_LIST = 768;
    protected static final int TYPE_MAP = 788;
    protected static final int TYPE_SET = 798;

    protected static byte[] BITWISE_SHIFTS_BYTE = new byte[]{0, 8, 16, 24};
    protected static byte[] BITWISE_SHIFTS_CHAR = new byte[]{0, 16};
    protected static Pattern JSON_MATCHER = Pattern.compile("^\\[([ildfs]-?[A-Za-z0-9.-_]+=?,?)*\\]$");
    protected static Pattern JSON_DISASSEMBLER = Pattern.compile(",");

    protected static final Map<ClassLoader, Map<String, JSONParcelable.JSONCreator>> mCreatorCache = new HashMap<ClassLoader, Map<String, JSONParcelable.JSONCreator>>();
    protected List<Object> mParceled = new ArrayList<Object>();
    protected int mSeek = 0;
    protected Context mContext;

    protected static String decompressString(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.decode(data, Base64.NO_WRAP|Base64.URL_SAFE));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            GZIPInputStream gzipStream = new GZIPInputStream(inputStream, 1024);
            byte[] buffer = new byte[1024];
            int bufferSize;

            while ((bufferSize = gzipStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bufferSize);
            }

            gzipStream.close();
            data = outputStream.toString();
            outputStream.close();

            return data;

        } catch (IOException e) {
            throw new Error(e.getMessage(), e);
        }
    }

    protected static String compressString(String data) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length());
            GZIPOutputStream zgipStream = new GZIPOutputStream(outputStream);
            try {
                zgipStream.write(data.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                zgipStream.write(data.getBytes());
            }
            zgipStream.close();
            data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP|Base64.URL_SAFE);
            outputStream.close();

            return data;

        } catch (IOException e) {
            throw new Error(e.getMessage(), e);
        }
    }

    /**
     * @see #JSONParcel(Context)
     */
    public JSONParcel() {

    }

    /**
     * Create an empty {@link JSONParcel} ready to write data to
     *
     * @param context
     *      This can be accessed using {@link #getContext()}. This is not used by this class, but can be
     *      used within {@link JSONParcelable#writeToJSON(JSONParcel)} if needed to load additional data only available through a {@link Context}
     */
    public JSONParcel(Context context) {
        if (context != null) {
            mContext = context.getApplicationContext();
        }
    }

    /**
     *
     * @see #JSONParcel(String, Context)
     * @throws JSONException
     */
    public JSONParcel(String json) throws JSONException {
        this(json, null);
    }

    /**
     * Create a new {@link JSONParcel} and create data from the given string
     *
     * @param json
     *      The data to append
     *
     * @param context
     *      This can be accessed using {@link #getContext()}. This is not used by this class, but can be
     *      used within {@link JSONParcelable#writeToJSON(JSONParcel)} if needed to load additional data only available through a {@link Context}
     *
     * @throws JSONException
     */
    public JSONParcel(String json, Context context) throws JSONException {
        this(context);

        if (JSON_MATCHER.matcher(json).matches()) {
            String[] entries = JSON_DISASSEMBLER.split(json.substring(1, json.length()-1));

            for (String entry : entries) {
                char type = entry.charAt(0);

                if (type == 'i') {
                    mParceled.add(Integer.valueOf(entry.substring(1)));

                } else if (type == 'l') {
                    mParceled.add(Long.valueOf(entry.substring(1)));

                } else if (type == 'f') {
                    mParceled.add(Float.valueOf(entry.substring(1)));

                } else if (type == 'd') {
                    mParceled.add(Double.valueOf(entry.substring(1)));

                } else if (type == 's') {
                    mParceled.add(entry.substring(1));

                } else {
                    throw new JSONException("The given string contains invalid entries\n" + json);
                }
            }

        } else {
            throw new JSONException("The given string is not a valid JSON string\n" + json);
        }
    }

    /**
     * Clears this instance and set's position at 0
     */
    public void clear() {
        mSeek = 0;
        mParceled.clear();
    }

    /**
     * Get the current position in the json array
     */
    public int getDataPosition() {
        return mSeek;
    }

    /**
     * Change the current position of the data array
     *
     * @param pos
     *      The new position in the data array, this can be both negative and positive numbers.
     *      Note that -1 is a special case as it points to the last entry. If the parcel only has index 0, whether it exists or not,
     *      then this entry will act as both the first and the last entry as it will be at both the beginning and the end of the array. Thereby
     *      both 0 and -1 will point to the same entry. These two number are always save to use in order to set the first and last entry without
     *      getting an {@link ArrayIndexOutOfBoundsException}<br /><br />
     *
     *      If you need to set the position at the end (Not the last entry, but ready for writing), set it to {@link #getDataSize()}
     *
     * @return
     *      The current position
     *
     * @throws ArrayIndexOutOfBoundsException
     *      If position given is larger than the parcel data size
     */
    public int setDataPosition(int pos) throws ArrayIndexOutOfBoundsException {
        int size = mParceled.size();
        pos = pos >= 0 ? pos : (pos == -1 && size == 0 ? 0 : size + pos);

        if (size >= pos) {
            int seek = mSeek;
            mSeek = pos;

            return seek;

        } else {
            throw new ArrayIndexOutOfBoundsException("Tried to set position " + pos + " on parcel with data size " + size);
        }
    }

    /**
     * Get the current data size (numbers of entries) in the data array
     */
    public int getDataSize() {
        return mParceled.size();
    }

    /**
     * Copies data from given {@link JSONParcel} between {@param offset} and {@param length} to this
     * instance at the current position
     *
     * @param source
     *      The {@link JSONParcel} object to copy from
     *
     * @param offset
     *      Offset position of the given {@link JSONParcel}
     *
     * @param length
     *      The length from {@param offset} to copy
     *
     * @throws JSONException
     */
    public void appendFrom(JSONParcel source, int offset, int length) throws JSONException {
        int dataLength = source.mParceled.size();
        offset = offset >= 0 ? offset : (offset == -1 && dataLength == 0 ? 0 : dataLength + offset);
        length = length > 0 ? offset + length : (length == -1 && dataLength == 0 ? 0 : dataLength + length);

        if (dataLength > offset && offset <= length && dataLength >= length) {
            for (int i=offset; i < length; i++) {
                if (mSeek == mParceled.size()) {
                    mParceled.add(source.mParceled.get(i));

                } else {
                    mParceled.set(mSeek, source.mParceled.get(i));
                }

                mSeek++;
            }

        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid offset and length. Tried to write from offset=" + offset + " to length=" + length + " on array with size=" + dataLength);
        }
    }

    /**
     * @see #appendFrom(JSONParcel, int, int)
     * @throws JSONException
     */
    public void appendFrom(JSONParcel source, int offset) throws JSONException {
        appendFrom(source, offset, 0);
    }

    /**
     * Get the {@link Context} widthin this instance. Note that this is always the {@link android.app.Application} version.
     * Also note that this might be <code>NULL</code> as a {@link Context} is not required to create an instance
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * @see #readJSONParcelable(ClassLoader)
     * @throws JSONException
     */
    public JSONParcelable[] readJSONParcelableArray() throws JSONException {
        return readJSONParcelableArray(null);
    }

    /**
     * Create an array of stored {@link JSONParcelable} objects that was added using {@link #writeJSONParcelableArray(JSONParcelable[])}
     *
     * @param classLoader
     *      {@link ClassLoader} that should be used to load the class implementing the {@link JSONParcelable} interface
     *
     * @throws JSONException
     */
    public JSONParcelable[] readJSONParcelableArray(ClassLoader classLoader) throws JSONException {
        int N = readInt();

        if (N >= 0) {
            JSONParcelable[] array = new JSONParcelable[N];

            for (int i = 0; i < N; i++) {
                array[i] = (JSONParcelable) readJSONParcelable(classLoader);
            }

            return array;
        }

        return null;
    }

    /**
     * Write an array of {@link JSONParcelable} objects to the parcel.
     * This can be re-created later using {@link #readJSONParcelable(ClassLoader)}
     *
     * @param data
     */
    public void writeJSONParcelableArray(JSONParcelable[] data) throws JSONException {
        if (data != null) {
            writeInt(data.length);

            for (int i=0; i < data.length; i++) {
                writeJSONParcelable(data[i]);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * @see #readJSONParcelable(ClassLoader)
     * @throws JSONException
     */
    public <T extends JSONParcelable> T readJSONParcelable() throws JSONException {
        return readJSONParcelable(null);
    }

    /**
     * Re-creates a {@link JSONParcelable} object that was written using {@link #writeJSONParcelable(JSONParcelable)}
     *
     * @param loader
     * @throws JSONException
     */
    public <T extends JSONParcelable> T readJSONParcelable(ClassLoader loader) throws JSONException {
        synchronized (mCreatorCache) {
            int schema = readInt();
            String className = readString();

            if (className != null && schema == SCHEMA_VERSION) {
                Map<String, JSONParcelable.JSONCreator> loaderCache = mCreatorCache.get(loader);

                if (loaderCache == null) {
                    loaderCache = new HashMap<String, JSONParcelable.JSONCreator>();
                    mCreatorCache.put(loader, loaderCache);
                }

                JSONParcelable.JSONCreator<T> creator = loaderCache.get(className);

                if (creator == null) {
                    try {
                        Class clazz = loader == null ? Class.forName(className) : Class.forName(className, true, loader);
                        Field field = clazz.getField("CREATOR");

                        creator = (JSONParcelable.JSONCreator) field.get(null);
                        loaderCache.put(className, creator);

                    } catch (Throwable e) {
                        throw new JSONException(e.getMessage());
                    }
                }

                return creator.createFromJSON(this, loader);
            }
        }

        return null;
    }

    /**
     * Write a {@link JSONParcelable} object to the parcel.
     * This can be re-created later using {@link #readJSONParcelable(ClassLoader)}
     *
     * @param data
     *      The {@link JSONParcelable} object that should receive the data
     */
    public void writeJSONParcelable(JSONParcelable data) throws JSONException {
        writeInt(SCHEMA_VERSION);

        if (data != null) {
            writeString(data.getClass().getName());
            data.writeToJSON(this);

        } else {
            writeString(null);
        }
    }

    /**
     * Read a {@link String} from the current position
     *
     * @throws JSONException
     */
    public String readString() throws JSONException {
        boolean notNull = readInt() > 0;

        if (notNull) {
            return decompressString((String) mParceled.get(mSeek++));
        }

        return null;
    }

    /**
     * Read a {@link String} array from the current position
     *
     * @throws JSONException
     */
    public String[] readStringArray() throws JSONException {
        int N = readInt();

        if (N >= 0) {
            String[] out = new String[N];

            for (int i=0; i < N; i++) {
                out[i] = readString();
            }

            return out;
        }

        return null;
    }

    /**
     * Compresses and writes the given {@link String} as encoded {@link Byte}[] to the parcel
     *
     * @param data
     *      The {@link String} that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeString(String data) throws JSONException {
        if (data != null) {
            writeInt(1);

           if (mSeek == mParceled.size()) {
                mParceled.add(compressString(data));

            } else {
                mParceled.set(mSeek, compressString(data));
            }

            mSeek++;

        } else {
            writeInt(0);
        }
    }

    /**
     * Writes the given {@link String} array as {@link Byte} to the parcel
     *
     * @param data
     *      The {@link String} array that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeStringArray(String[] data) throws JSONException {
        if (data != null) {
            writeInt(data.length);

            for (int i=0; i < data.length; i++) {
                writeString(data[i]);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * Read a {@link Long} number from the current location
     *
     * @throws JSONException
     */
    public long readLong() throws JSONException {
        return (Long) mParceled.get(mSeek++);
    }

    /**
     * Read a {@link Long} array from the current location
     *
     * @throws JSONException
     */
    public long[] readLongArray() throws JSONException {
        int N = readInt();

        if (N >= 0) {
            long[] out = new long[N];

            for (int i=0; i < N; i++) {
                out[i] = readLong();
            }

            return out;
        }

        return null;
    }

    /**
     * Writes a {@link Long} to the parcel at the current position
     *
     * @param data
     *      The {@link Long} that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeLong(long data) throws JSONException {
        if (mSeek == mParceled.size()) {
            mParceled.add(data);

        } else {
            mParceled.set(mSeek, data);
        }

        mSeek++;
    }

    /**
     * Writes an array of {@link Long} to the parcel at the current position
     *
     * @param data
     *      The {@link Long} array that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeLongArray(long[] data) throws JSONException {
        if (data != null) {
            writeInt(data.length);

            for (int i=0; i < data.length; i++) {
                writeLong(data[i]);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * Read a {@link Integer} number from the current location
     *
     * @throws JSONException
     */
    public int readInt() throws JSONException {
        return (Integer) mParceled.get(mSeek++);
    }

    /**
     * Read a {@link Integer} array from the current location
     *
     * @throws JSONException
     */
    public int[] readIntArray() throws JSONException {
        int N = readInt();

        if (N >= 0) {
            int[] out = new int[N];

            for (int i=0; i < N; i++) {
                out[i] = readInt();
            }

            return out;
        }

        return null;
    }

    /**
     * Writes a {@link Integer} to the parcel at the current position
     *
     * @param data
     *      The {@link Integer} that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeInt(int data) throws JSONException {
        if (mSeek == mParceled.size()) {
            mParceled.add(data);

        } else {
            mParceled.set(mSeek, data);
        }

        mSeek++;
    }

    /**
     * Writes an array of {@link Integer} to the parcel at the current position
     *
     * @param data
     *      The {@link Integer} array that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeIntArray(int[] data) throws JSONException {
        if (data != null) {
            writeInt(data.length);

            for (int i=0; i < data.length; i++) {
                writeInt(data[i]);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * Read a {@link Double} number from the current location
     *
     * @throws JSONException
     */
    public double readDouble() throws JSONException {
        return (Double) mParceled.get(mSeek++);
    }

    /**
     * Read a {@link Double} array from the current location
     *
     * @throws JSONException
     */
    public double[] readDoubleArray() throws JSONException {
        int N = readInt();

        if (N >= 0) {
            double[] out = new double[N];

            for (int i=0; i < N; i++) {
                out[i] = readDouble();
            }

            return out;
        }

        return null;
    }

    /**
     * Writes a {@link Double} to the parcel at the current position
     *
     * @param data
     *      The {@link Double} that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeDouble(double data) throws JSONException {
        if (mSeek == mParceled.size()) {
            mParceled.add(data);

        } else {
            mParceled.set(mSeek, data);
        }

        mSeek++;
    }

    /**
     * Writes an array of {@link Double} to the parcel at the current position
     *
     * @param data
     *      The {@link Double} array that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeDoubleArray(double[] data) throws JSONException {
        if (data != null) {
            writeInt(data.length);

            for (int i=0; i < data.length; i++) {
                writeDouble(data[i]);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * Read a {@link Float} number from the current location
     *
     * @throws JSONException
     */
    public float readFloat() throws JSONException {
        return (Float) mParceled.get(mSeek++);
    }

    /**
     * Read a {@link Float} array from the current location
     *
     * @throws JSONException
     */
    public float[] readFloatArray() throws JSONException {
        int N = readInt();

        if (N >= 0) {
            float[] out = new float[N];

            for (int i=0; i < N; i++) {
                out[i] = readFloat();
            }

            return out;
        }

        return null;
    }

    /**
     * Writes a {@link Float} to the parcel at the current position
     *
     * @param data
     *      The {@link Float} that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeFloat(float data) throws JSONException {
        if (mSeek == mParceled.size()) {
            mParceled.add(data);

        } else {
            mParceled.set(mSeek, data);
        }

        mSeek++;
    }

    /**
     * Writes an array of {@link Float} to the parcel at the current position
     *
     * @param data
     *      The {@link Float} array that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeFloatArray(float[] data) throws JSONException {
        if (data != null) {
            writeInt(data.length);

            for (int i=0; i < data.length; i++) {
                writeFloat(data[i]);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * Read a single {@link Byte} from the current location
     *
     * @throws JSONException
     */
    public byte readByte() throws JSONException {
        return (byte) readInt();
    }

    /**
     * Read a {@link Byte} array from the current location
     *
     * @throws JSONException
     */
    public byte[] readByteArray() throws JSONException {
        int N = readInt();

        if (N > 0) {
            byte[] out = new byte[N];
            int byteCount = BITWISE_SHIFTS_BYTE.length;
            int dividedSize = (int) (N / byteCount);
            int remainderSize = (N % byteCount);
            int arraySize = (dividedSize + (remainderSize > 0 ? 1 : 0));
            int in = 0;
            int offset, i, x, y;

            for (i=0, offset=0; i < arraySize; i++, offset+=byteCount) {
                y = i < dividedSize ? byteCount : remainderSize;

                for (x=0; x < y; x++) {
                    switch (x) {
                        case 0:
                            in = readInt();
                            out[offset + x] = (byte) in; break;

                        default: out[offset + x] = (byte) (in >>> BITWISE_SHIFTS_BYTE[x]);
                    }
                }
            }

            return out;

        } else if (N == 0) {
            return new byte[0];
        }

        return null;
    }

    /**
     * Writes one byte to the {@link JSONParcel} object. <br /><br />
     *
     * Avoid this method if you need to write multiple bytes. Instead use {@link #writeByteArray(byte[])}.
     * It will reduce the {@link Integer} ammount by adding multiple 8bit {@link Byte}'s per 32bit {@link Integer}'s.
     *
     * @param data
     *      The {@link Byte} to write
     */
    public void writeByte(byte data) throws JSONException {
        writeInt((data & 0xFF));
    }

    /**
     * @see #writeByteArray(byte[], int, int)
     * @throws JSONException
     */
    public void writeByteArray(byte[] data) throws JSONException {
        writeByteArray(data, 0, data != null ? data.length : 0);
    }

    /**
     * Writes a {@link Byte} array to the {@link JSONParcel} object.
     *
     * @param offset
     *      The offset position in the data array.
     *
     * @param length
     *      The ammount of bytes to write from the offset position.
     *
     * @throws ArrayIndexOutOfBoundsException
     *      If position given is larger than the parcel data size
     */
    public void writeByteArray(byte[] data, int offset, int length) throws ArrayIndexOutOfBoundsException, JSONException {
        if (data != null) {
            int dataLength = data.length;

            offset = offset >= 0 ? offset : (offset == -1 && dataLength == 0 ? 0 : dataLength + offset);
            length = length > 0 ? offset + length : (length == -1 && dataLength == 0 ? 0 : dataLength + length);

            if (dataLength > offset && offset <= length && dataLength >= length) {
                int len = length - offset;

                writeInt(len);

                int byteCount = BITWISE_SHIFTS_BYTE.length;
                int dividedSize = (int) (len / byteCount);
                int remainderSize = (len % byteCount);
                int arraySize = (dividedSize + (remainderSize > 0 ? 1 : 0));
                int out = 0;
                int i, x, y;

                for (i=0; i < arraySize; i++, offset+=byteCount) {
                    y = i < dividedSize ? byteCount : remainderSize;

                    for (x=0; x < y; x++) {
                        switch (x) {
                            case 0: out = (data[offset + x] & 0xFF); break;
                            default: out |= ((data[offset + x] & 0xFF) << BITWISE_SHIFTS_BYTE[x]);
                        }
                    }

                    if (y > 0) {
                        writeInt(out);
                    }
                }

            } else {
                throw new ArrayIndexOutOfBoundsException("Invalid offset and length. Tried to write from offset=" + offset + " to length=" + length + " on array with size=" + dataLength);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * @see #writeByteArray(byte[], int, int)
     * @throws ArrayIndexOutOfBoundsException
     * @throws JSONException
     */
    public void writeByteArray(byte[] data, int offset) throws ArrayIndexOutOfBoundsException, JSONException {
        writeByteArray(data, offset, 0);
    }

    /**
     * Read a {@link Character} from the current location
     *
     * @throws JSONException
     */
    public char readChar() throws JSONException {
        return (char) readInt();
    }

    /**
     * Read a {@link Character} array from the current location
     *
     * @throws JSONException
     */
    public char[] readCharArray() throws JSONException {
        int N = readInt();

        if (N > 0) {
            char[] out = new char[N];
            int charCount = BITWISE_SHIFTS_CHAR.length;
            int dividedSize = (int) (N / charCount);
            int remainderSize = (N % charCount);
            int arraySize = (dividedSize + (remainderSize > 0 ? 1 : 0));
            int in = 0;
            int offset, i, x, y;

            for (i=0, offset=0; i < arraySize; i++, offset+=charCount) {
                y = i < dividedSize ? charCount : remainderSize;

                for (x=0; x < y; x++) {
                    switch (x) {
                        case 0:
                            in = readInt();
                            out[offset + x] = (char) in; break;

                        default: out[offset + x] = (char) (in >>> BITWISE_SHIFTS_CHAR[x]);
                    }
                }
            }

            return out;

        } else if (N == 0) {
            return new char[0];
        }

        return null;
    }

    /**
     * Writes one char to the {@link JSONParcel} object. <br /><br />
     *
     * Avoid this method if you need to write multiple char's. Instead use {@link #writeCharArray(char[])}.
     * It will reduce the {@link Integer} ammount by adding multiple 8bit or 16bit {@link Character}'s per 32bit {@link Integer}'s.
     *
     * @param data
     *      The {@link Character} to write
     */
    public void writeChar(char data) throws JSONException {
        writeInt((data & 0xFFFF));
    }

    /**
     * Writes an array of {@link Character} to the parcel at the current position
     *
     * @param data
     *      The {@link Character} array that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeCharArray(char[] data) throws JSONException {
        if (data != null) {
            if (data.length > 0) {
                writeInt(data.length);

                int charCount = BITWISE_SHIFTS_CHAR.length;
                int dividedSize = (int) (data.length / charCount);
                int remainderSize = (data.length % charCount);
                int arraySize = (dividedSize + (remainderSize > 0 ? 1 : 0));
                int out = 0;
                int offset, i, x, y;

                for (i=0, offset=0; i < arraySize; i++, offset+=charCount) {
                    y = i < dividedSize ? charCount : remainderSize;

                    for (x=0; x < y; x++) {
                        switch (x) {
                            case 0: out = (data[offset + x] & 0xFFFF); break;
                            default: out |= ((data[offset + x] & 0xFFFF) << BITWISE_SHIFTS_CHAR[x]);
                        }
                    }

                    if (y > 0) {
                        writeInt(out);
                    }
                }

            } else {
                writeInt(0);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * Read a {@link Boolean} from the current location
     *
     * @throws JSONException
     */
    public boolean readBoolean() throws JSONException {
        return readInt() > 1;
    }

    /**
     * Read a {@link Boolean} array from the current location
     *
     * @throws JSONException
     */
    public boolean[] readBooleanArray() throws JSONException {
        int N = readInt();

        if (N > 0) {
            boolean[] out = new boolean[N];
            int charCount = 32;
            int dividedSize = (int) (N / charCount);
            int remainderSize = (N % charCount);
            int arraySize = (dividedSize + (remainderSize > 0 ? 1 : 0));
            int in = 0;
            int offset, i, x, y;

            for (i=0, offset=0; i < arraySize; i++, offset+=charCount) {
                y = i < dividedSize ? charCount : remainderSize;

                for (x=0; x < y; x++) {
                    switch (x) {
                        case 0:
                            in = readInt();

                        default:
                            out[offset + x] = (in & (0x1 << x)) != 0;
                    }
                }
            }

            return out;

        } else if (N == 0) {
            return new boolean[0];
        }

        return null;
    }

    /**
     * Writes a {@link Boolean} to the parcel at the current position
     *
     * @param data
     *      The {@link Boolean} that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeBoolean(boolean data) throws JSONException {
        writeInt(data ? 1 : 0);
    }

    /**
     * Writes an array of {@link Boolean} to the parcel at the current position
     *
     * @param data
     *      The {@link Boolean} array that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeBooleanArray(boolean[] data) throws JSONException {
        if (data != null) {
            if (data.length > 0) {
                writeInt(data.length);

                int charCount = 32;
                int dividedSize = (int) (data.length / charCount);
                int remainderSize = (data.length % charCount);
                int arraySize = (dividedSize + (remainderSize > 0 ? 1 : 0));
                int out = 0;
                int offset, i, x, y;

                for (i=0, offset=0; i < arraySize; i++, offset+=charCount) {
                    y = i < dividedSize ? charCount : remainderSize;

                    for (x=0; x < y; x++) {
                        switch (x) {
                            case 0: out = data[offset + x] ? 0x1 : 0x0; break;
                            default:
                                if (data[offset + x]) {
                                    out |= (0x1 << x);
                                }
                        }
                    }

                    if (y > 0) {
                        writeInt(out);
                    }
                }

            } else {
                writeInt(0);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * Writes array entries to the parcel as {@link Object} using {@link #writeValue(Object)}
     *
     * @param data
     *      The array that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeArray(Object[] data) throws JSONException {
        if (data != null) {
            writeInt(data.length);

            for (int i=0; i < data.length; i++) {
                writeValue(data[i]);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * @see #readArray(ClassLoader)
     * @throws JSONException
     */
    public Object[] readArray() throws JSONException {
        return readArray(null);
    }

    /**
     * Create an unknown array and fill it using {@link #readValue(ClassLoader)}
     *
     * @param classLoader
     *      A {@link ClassLoader} that will be used to instantiate the content
     *
     * @throws JSONException
     */
    public Object[] readArray(ClassLoader classLoader) throws JSONException {
        int N = readInt();

        if (N >= 0) {
            Object[] out = new Object[N];

            for (int i=0; i < N; i++) {
                out[i] = readValue(classLoader);
            }

            return out;
        }

        return null;
    }

    /**
     * @see #readList(ClassLoader)
     * @throws JSONException
     */
    public List<?> readList() throws JSONException {
        return readList(null);
    }

    /**
     * Create a new {@link ArrayList} and fill it using {@link #readValue(ClassLoader)}
     *
     * @param classLoader
     *      A {@link ClassLoader} that will be used to instantiate the content
     *
     * @throws JSONException
     */
    public List<?> readList(ClassLoader classLoader) throws JSONException {
        int N = readInt();

        if (N >= 0) {
            List<Object> out = new ArrayList<Object>();

            for (int i=0; i < N; i++) {
                out.add(readValue(classLoader));
            }

            return out;
        }

        return null;
    }

    /**
     * @see #fillList(List, ClassLoader)
     * @throws JSONException
     */
    public void fillList(List out) throws JSONException {
        fillList(out, null);
    }

    /**
     * Populate the given {@link List} object using {@link #readValue(ClassLoader)}
     *
     * @param out
     *      A {@link List} object to fill
     *
     * @param classLoader
     *      A {@link ClassLoader} that will be used to instantiate the content
     *
     * @throws JSONException
     */
    public void fillList(List out, ClassLoader classLoader) throws JSONException {
        int N = readInt();

        if (N >= 0) {
            for (int i=0; i < N; i++) {
                out.add(readValue(classLoader));
            }
        }
    }

    /**
     * Writes the data from a {@link List} to the parcel using {@link #writeValue(Object)} <br /><br />
     *
     * If the {@link List} contains {@link Byte}, {@link Character} or {@link Boolean}, consider using {@link #writeByteArray(byte[])}
     * , {@link #writeCharArray(char[])} and {@link #writeBooleanArray(boolean[])} instead as these will compress the data
     *
     * @param data
     *      The {@link List} that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeList(List data) throws JSONException {
        if (data != null) {
            int N = data.size();
            writeInt(N);

            for (int i=0; i < N; i++) {
                writeValue(data.get(i));
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * @see #readMap(ClassLoader)
     * @throws JSONException
     */
    public Map<String, ?> readMap() throws JSONException {
        return readMap(null);
    }

    /**
     * Create a new {@link HashMap} and fill it using {@link #readValue(ClassLoader)}
     * for the values and {@link #readString()} for the keys
     *
     * @param classLoader
     *      A {@link ClassLoader} that will be used to instantiate the content
     *
     * @throws JSONException
     */
    public Map<String, ?> readMap(ClassLoader classLoader) throws JSONException {
        int N = readInt();

        if (N >= 0) {
            Map<String, Object> out = new HashMap<String, Object>();
            String key;
            Object value;

            for (int i=0; i < N; i++) {
                key = readString();
                value = readValue(classLoader);

                out.put(key, value);
            }

            return out;
        }

        return null;
    }

    /**
     *
     * @see #fillMap(Map, ClassLoader)
     * @throws JSONException
     */
    public void fillMap(Map out) throws JSONException {
        fillMap(out, null);
    }

    /**
     * Populate the given {@link Map} object using {@link #readValue(ClassLoader)}
     * for the values and {@link #readString()} for the keys
     *
     * @param out
     *      A {@link Map} object to fill
     *
     * @param classLoader
     *      A {@link ClassLoader} that will be used to instantiate the content
     *
     * @throws JSONException
     */
    public void fillMap(Map out, ClassLoader classLoader) throws JSONException {
        int N = readInt();

        if (N >= 0) {
            String key;
            Object value;

            for (int i=0; i < N; i++) {
                key = readString();
                value = readValue(classLoader);

                out.put(key, value);
            }
        }
    }

    /**
     * Writes the data from a {@link Map} to the parcel using {@link #writeString(String)} for the keys
     * and {@link #writeValue(Object)} for the content
     *
     * @param data
     *      The {@link Map} that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeMap(Map data) throws JSONException {
        if (data != null) {
            int N = data.size();
            writeInt(N);

            for (Map.Entry<String, Object> entry : ((Map<String, Object>) data).entrySet()) {
                writeString(entry.getKey());
                writeValue(entry.getValue());
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * @see #readSet(ClassLoader)
     * @throws JSONException
     */
    public Set<?> readSet() throws JSONException {
        return readSet(null);
    }

    /**
     * Create a new {@link HashSet} and fill it using {@link #readValue(ClassLoader)}
     *
     * @param classLoader
     *      A {@link ClassLoader} that will be used to instantiate the content
     *
     * @throws JSONException
     */
    public Set<?> readSet(ClassLoader classLoader) throws JSONException {
        int N = readInt();

        if (N >= 0) {
            Set<Object> out = new HashSet<Object>();
            for (int i=0; i < N; i++) {
                out.add(readValue(classLoader));
            }

            return out;
        }

        return null;
    }

    /**
     *
     * @see #fillSet(Set, ClassLoader)
     * @throws JSONException
     */
    public void fillSet(Set out) throws JSONException {
        fillSet(out, null);
    }

    /**
     * Populate the given {@link Set} object using {@link #readValue(ClassLoader)}
     *
     * @param out
     *      A {@link Set} object to fill
     *
     * @param classLoader
     *      A {@link ClassLoader} that will be used to instantiate the content
     *
     * @throws JSONException
     */
    public void fillSet(Set out, ClassLoader classLoader) throws JSONException {
        int N = readInt();

        if (N >= 0) {
            for (int i=0; i < N; i++) {
                out.add(readValue(classLoader));
            }
        }
    }

    /**
     * Writes the data from a {@link Set} to the parcel using {@link #writeValue(Object)} <br /><br />
     *
     * If the {@link Set} contains {@link Byte}, {@link Character} or {@link Boolean}, consider using {@link #writeByteArray(byte[])}
     * , {@link #writeCharArray(char[])} and {@link #writeBooleanArray(boolean[])} instead as these will compress the data
     *
     * @param data
     *      The {@link Set} that should be written to the parcel
     *
     * @throws JSONException
     */
    public void writeSet(Set data) throws JSONException {
        if (data != null) {
            int N = data.size();
            writeInt(N);

            for (Object value : (Set<Object>) data) {
                writeValue(value);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     * @see #readValue(ClassLoader)
     * @throws JSONException
     */
    public Object readValue() throws JSONException {
        return readValue(null);
    }

    /**
     * Read data from the parcel as {@link Object}. This method will only work on data that
     * was written to the parcel using {@link #writeValue(Object)}
     *
     * @param classLoader
     *      A {@link ClassLoader} that will be used to instantiate the content
     *
     * @throws JSONException
     */
    public Object readValue(ClassLoader classLoader) throws JSONException {
        int type = readInt();

        switch (type) {
            case TYPE_NULL:
                return null;

            case TYPE_JSONPARCEL:
                return readJSONParcelable(classLoader);

            case TYPE_JSONPARCELARRAY:
                return readJSONParcelableArray(classLoader);

            case TYPE_STRING:
                return readString();

            case TYPE_STRINGARRAY:
                return readStringArray();

            case TYPE_INTEGER:
                return readInt();

            case TYPE_INTEGERARRAY:
                return readIntArray();

            case TYPE_LONG:
                return readLong();

            case TYPE_LONGARRAY:
                return readLongArray();

            case TYPE_DOUBLE:
                return readDouble();

            case TYPE_DOUBLEARRAY:
                return readDoubleArray();

            case TYPE_FLOAT:
                return readFloat();

            case TYPE_FLOATARRAY:
                return readFloatArray();

            case TYPE_BOOLEAN:
                return readBoolean();

            case TYPE_BOOLEANARRAY:
                return readBooleanArray();

            case TYPE_BYTE:
                return readByte();

            case TYPE_BYTEARRAY:
                return readByteArray();

            case TYPE_CHAR:
                return readChar();

            case TYPE_CHARARRAY:
                return readCharArray();

            case TYPE_OBJECTARRAY:
                return readArray(classLoader);

            case TYPE_LIST:
                return readList(classLoader);

            case TYPE_MAP:
                return readMap(classLoader);

            case TYPE_SET:
                return readSet(classLoader);

            default:
                throw null;
        }
    }

    /**
     * Write data to the parcel as {@link Object}. The fallowing DataTypes are supported: <br /><br />
     *
     * <code>NULL</code> <br />
     * {@link JSONParcelable} <br />
     * {@link JSONParcelable}[] <br />
     * {@link String} <br />
     * {@link String}[] <br />
     * {@link Integer} <br />
     * {@link Integer}[] <br />
     * {@link Long} <br />
     * {@link Long}[] <br />
     * {@link Double} <br />
     * {@link Double}[] <br />
     * {@link Float} <br />
     * {@link Float}[] <br />
     * {@link Boolean} <br />
     * {@link Boolean}[] <br />
     * {@link Byte} <br />
     * {@link Byte}[] <br />
     * {@link Character} <br />
     * {@link Character}[] <br />
     * {@link List} <br />
     * {@link Set} <br />
     * {@link Map} (With String keys)
     *
     * @param data
     *      Data that should be written to the parcel
     *
     * @throws ArrayIndexOutOfBoundsException
     * @throws JSONException
     */
    public void writeValue(Object data) throws ArrayIndexOutOfBoundsException, JSONException {
        if (data == null) {
            writeInt(TYPE_NULL);

        } else if (data instanceof JSONParcelable) {
            writeInt(TYPE_JSONPARCEL);
            writeJSONParcelable((JSONParcelable) data);

        } else if (data instanceof JSONParcelable[]) {
            writeInt(TYPE_JSONPARCELARRAY);
            writeJSONParcelableArray((JSONParcelable[]) data);

        } else if (data instanceof String) {
            writeInt(TYPE_STRING);
            writeString((String) data);

        } else if (data instanceof String[]) {
            writeInt(TYPE_STRINGARRAY);
            writeStringArray((String[]) data);

        } else if (data instanceof Integer) {
            writeInt(TYPE_INTEGER);
            writeInt((Integer) data);

        } else if (data instanceof int[]) {
            writeInt(TYPE_INTEGERARRAY);
            writeIntArray((int[]) data);

        } else if (data instanceof Long) {
            writeInt(TYPE_LONG);
            writeLong((Long) data);

        } else if (data instanceof long[]) {
            writeInt(TYPE_LONGARRAY);
            writeLongArray((long[]) data);

        } else if (data instanceof Double) {
            writeInt(TYPE_DOUBLE);
            writeDouble((Double) data);

        } else if (data instanceof double[]) {
            writeInt(TYPE_DOUBLEARRAY);
            writeDoubleArray((double[]) data);

        } else if (data instanceof Float) {
            writeInt(TYPE_FLOAT);
            writeFloat((Float) data);

        } else if (data instanceof float[]) {
            writeInt(TYPE_FLOATARRAY);
            writeFloatArray((float[]) data);

        } else if (data instanceof Boolean) {
            writeInt(TYPE_BOOLEAN);
            writeBoolean((Boolean) data);

        } else if (data instanceof boolean[]) {
            writeInt(TYPE_BOOLEANARRAY);
            writeBooleanArray((boolean[]) data);

        } else if (data instanceof Byte) {
            writeInt(TYPE_BYTE);
            writeByte((Byte) data);

        } else if (data instanceof byte[]) {
            writeInt(TYPE_BYTEARRAY);
            writeByteArray((byte[]) data);

        } else if (data instanceof Character) {
            writeInt(TYPE_CHAR);
            writeChar((Character) data);

        } else if (data instanceof char[]) {
            writeInt(TYPE_CHARARRAY);
            writeCharArray((char[]) data);

        } else if (data instanceof List) {
            writeInt(TYPE_LIST);
            writeList((List) data);

        } else if (data instanceof Map) {
            writeInt(TYPE_MAP);
            writeMap((Map) data);

        } else if (data instanceof Set) {
            writeInt(TYPE_SET);
            writeSet((Set) data);

        } else {
            Class<?> clazz = data.getClass();
            if (clazz.isArray() && clazz.getComponentType() == Object.class) {
                writeInt(TYPE_OBJECTARRAY);
                writeArray((Object[]) data);

            } else {
                writeInt(TYPE_NULL);
            }
        }
    }

    /**
     * Creates a JSON like {@link String} of the parcel content.
     * This can be placed in a storage like a database and
     * be used to re-create the parcel via {@link #JSONParcel(String, Context)}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int size = mParceled.size();

        builder.append("[");

        for (int i=0, x=size-1; i < size; i++, x--) {
            Object value = mParceled.get(i);

            if (value instanceof Integer) {
                builder.append("i");

            } else if (value instanceof Long) {
                builder.append("l");

            } else if (value instanceof Float) {
                builder.append("f");

            } else if (value instanceof Double) {
                builder.append("d");

            } else if (value instanceof String) {
                builder.append("s");
            }

            builder.append(String.valueOf(value));

            if (x > 0) {
                builder.append(",");
            }
        }

        builder.append("]");

        return builder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        int size = mParceled.size();

        dest.writeInt(size);

        for (Object value : mParceled) {
            dest.writeValue(value);
        }
    }

    public JSONParcel(Parcel source) {
        int size = source.readInt();

        if (size > 0) {
            for (int i=0; i < size; i++) {
                mParceled.add(source.readValue(null));
            }
        }
    }

    public static Parcelable.Creator<JSONParcel> CREATOR = new Parcelable.Creator<JSONParcel>() {
        @Override
        public JSONParcel createFromParcel(Parcel source) {
            return new JSONParcel(source);
        }

        @Override
        public JSONParcel[] newArray(int size) {
            return new JSONParcel[size];
        }
    };
}
