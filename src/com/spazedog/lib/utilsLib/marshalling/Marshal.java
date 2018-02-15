package com.spazedog.lib.utilsLib.marshalling;

import com.spazedog.lib.utilsLib.collection.SwiftList;
import com.spazedog.lib.utilsLib.collection.SwiftMap;
import com.spazedog.lib.utilsLib.collection.SwiftSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 */
public final class Marshal {

    private static final int TYPE_NULL = -1;
    private static final int TYPE_MARSHALABLE = 0;
    private static final int TYPE_MARSHALABLEARRAY = 1;
    private static final int TYPE_STRING = 2;
    private static final int TYPE_STRINGARRAY = 3;
    private static final int TYPE_INTEGER = 4;
    private static final int TYPE_INTEGERARRAY = 5;
    private static final int TYPE_LONG = 6;
    private static final int TYPE_LONGARRAY = 7;
    private static final int TYPE_DOUBLE = 8;
    private static final int TYPE_DOUBLEARRAY = 9;
    private static final int TYPE_BYTE = 10;
    private static final int TYPE_BYTEARRAY = 11;
    private static final int TYPE_CHAR = 12;
    private static final int TYPE_CHARARRAY = 13;
    private static final int TYPE_BOOLEAN = 14;
    private static final int TYPE_BOOLEANARRAY = 15;
    private static final int TYPE_FLOAT = 16;
    private static final int TYPE_FLOATARRAY = 17;
    private static final int TYPE_SERIALIZEABLE = 18;
    private static final int TYPE_SERIALIZEABLEARRAY = 19;
    private static final int TYPE_OBJECTARRAY = 512;
    private static final int TYPE_LIST = 768;
    private static final int TYPE_MAP = 788;
    private static final int TYPE_SET = 798;

    /** @ignore */
    private static int SCHEMA_VERSION = 1;

    /** @ignore */
    private static Pattern INPUT_MATCHER = Pattern.compile("^[0-9]+:((-?[0-9.]+[Ee][+-][0-9]+[fd])|(-?[0-9]+[il])|([0-9]+[bc]))+$");

    /** @ignore */
    private int mPointer = 0;

    /** @ignore */
    private List<Object> mData = new SwiftList<Object>();

    /**
     *
     */
    public Marshal() {}

    /**
     * @param input
     * 		A parcel generated string
     */
    public Marshal(String input) {
        if (input != null && INPUT_MATCHER.matcher(input).matches()) {
            input = input.trim();

            char c;
            StringBuilder builder = new StringBuilder();
            int length = input.length();

            for (int i=0; i < length; i++) {
                c = input.charAt(i);

                if (c == 'i') {
                    mData.add(Integer.valueOf(builder.toString()));

                } else if (c == 'l') {
                    mData.add(Long.valueOf(builder.toString()));

                } else if (c == 'f') {
                    mData.add(Float.parseFloat(builder.toString()));

                } else if (c == 'd') {
                    mData.add(Double.parseDouble(builder.toString()));

                } else if (c == 'b') {
                    mData.add((byte) (Integer.valueOf(builder.toString()) & 0xFF));

                } else if (c == 'c') {
                    mData.add((char) (Integer.valueOf(builder.toString()) & 0xFFFF));

                } else if (c == ':') {
                    // Marshalled version, not important at the moment

                } else {
                    builder.append(c); continue;
                }

                builder.setLength(0);
            }

        } else if (input != null && input.length() > 0) {
            throw new RuntimeException("The given string is not a valid marshalled string\n" + input);
        }
    }

    /**
     *
     */
    public Marshal(Reader input) {
        char c;
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[256];
        int length = 0;

        try {
            while ((length = input.read(buffer)) > 0) {
                for (int i=0; i < length; i++) {
                    c = buffer[i];

                    if (c == 'i') {
                        mData.add(Integer.valueOf(builder.toString()));

                    } else if (c == 'l') {
                        mData.add(Long.valueOf(builder.toString()));

                    } else if (c == 'f') {
                        mData.add(Float.parseFloat(builder.toString()));

                    } else if (c == 'd') {
                        mData.add(Double.parseDouble(builder.toString()));

                    } else if (c == 'b') {
                        mData.add((byte) (Integer.valueOf(builder.toString()) & 0xFF));

                    } else if (c == 'c') {
                        mData.add((char) (Integer.valueOf(builder.toString()) & 0xFFFF));

                    } else if (c == ':') {
                        // Marshalled version, not important at the moment

                    } else {
                        builder.append(c); continue;
                    }

                    builder.setLength(0);
                }
            }

        } catch (Exception e) {}
    }

    /**
     *
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int size = mData.size();

        builder.append(SCHEMA_VERSION);
        builder.append(':');

        for (int i=0; i < size; i++) {
            Object value = mData.get(i);

            if (value instanceof Byte) {
                builder.append(String.valueOf( (int) ((Byte) value) & 0xFF ));
                builder.append('b');

            } else if (value instanceof Character) {
                builder.append(String.valueOf( (int) ((Character) value) & 0xFFFF ));
                builder.append('c');

            } else if (value instanceof Float) {
                builder.append(String.format("%.3E", (Float) value));
                builder.append('f');

            } else if (value instanceof Double) {
                builder.append(String.format("%.9E", (Double) value));
                builder.append('d');

            } else {
                builder.append(String.valueOf(value));

                if (value instanceof Integer) {
                    builder.append('i');

                } else if (value instanceof Long) {
                    builder.append('l');
                }
            }
        }

        return builder.toString();
    }

    /**
     *
     */
    public void toStream(Writer output) {
        int size = mData.size();

        try {
            output.write(String.valueOf(SCHEMA_VERSION));
            output.write(':');

            for (int i = 0; i < size; i++) {
                Object value = mData.get(i);

                if (value instanceof Byte) {
                    output.write(String.valueOf((int) ((Byte) value) & 0xFF));
                    output.write('b');

                } else if (value instanceof Character) {
                    output.write(String.valueOf((int) ((Character) value) & 0xFFFF));
                    output.write('c');

                } else if (value instanceof Float) {
                    output.write(String.format("%.3E", (Float) value));
                    output.write('f');

                } else if (value instanceof Double) {
                    output.write(String.format("%.9E", (Double) value));
                    output.write('d');

                } else {
                    output.write(String.valueOf(value));

                    if (value instanceof Integer) {
                        output.write('i');

                    } else if (value instanceof Long) {
                        output.write('l');
                    }
                }
            }

        } catch (Exception e) {}
    }

    /**
     *
     */
    public void clear() {
        mPointer = 0;
        mData.clear();
    }

    /**
     *
     */
    public void rewind() {
        mPointer = 0;
    }

    /**
     *
     */
    public int getOffset() {
        return mPointer;
    }

    /**
     *
     */
    public void setOffset(int offset) {
        mPointer = offset < 0 ? mData.size() + offset : offset;

        if (mPointer >= mData.size() && offset != 0 && offset != -1) {
            throw new RuntimeException("Offset '" + offset + " is out of parcel range'");
        }

        mPointer = offset;
    }

    /**
     *
     */
    public Object readOffset() {
        return readRaw();
    }

    /**
     *
     */
    public int getLength() {
        return mData.size();
    }

    /**
     *
     */
    public void appendFrom(Marshal src, int length) {
        if (length < 0) {
            length = ((src.getLength() - src.getOffset()) + length) + 1;
        }

        while (length-- > 0) {
            writeRaw( src.readOffset() );
        }
    }


    /* =============================================================
     * -------------------------------------------------------------
     * RAW
     */

    /**
     * @ignore
     */
    private void writeRaw(Object data) {
        if (mPointer == mData.size()) {
            mData.add(data);

        } else {
            mData.set(mPointer, data);
        }

        mPointer++;
    }

    /**
     * @ignore
     */
    private Object readRaw() {
        return mData.get(mPointer++);
    }


    /* =============================================================
     * -------------------------------------------------------------
     * SERIALIZABLE
     */

    /**
     *
     */
    public <T extends Serializable> void writeSerializable(T data) {
        if (data != null) {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);

                objectStream.writeObject(data);
                writeByteArray(byteStream.toByteArray());

                objectStream.close();

            } catch (Throwable e) {
                throw new RuntimeException(e.getMessage(), e);
            }

        } else {
            writeByteArray(null);
        }
    }

    /**
     *
     */
    public <T extends Serializable> void writeSerializableArray(T[] data) {
        if (data != null) {
            writeInt(data.length);

            for (int i=0; i < data.length; i++) {
                writeSerializable(data[i]);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T readSerializable() {
        try {
            byte[] data = readByteArray();

            if (data != null) {
                ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
                ObjectInputStream objectStream = new ObjectInputStream(byteStream);

                T object = (T) objectStream.readObject();

                objectStream.close();

                return object;
            }

        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return null;
    }

    /**
     *
     */
    public Serializable[] readSerializableArray() {
        int N = readInt();

        if (N >= 0) {
            Serializable[] array = new Serializable[N];

            for (int i = 0; i < N; i++) {
                array[i] = readSerializable();
            }

            return array;
        }

        return null;
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T[] readSerializableArray(Class<T> clazz) {
        int N = readInt();

        if (N >= 0) {
            T[] array = (T[]) Array.newInstance(clazz, N);

            for (int i = 0; i < N; i++) {
                array[i] = readSerializable();
            }

            return array;
        }

        return null;
    }


    /* =============================================================
     * -------------------------------------------------------------
     * MARSHALABLE
     */

    /**
     *
     */
    public <T extends Marshalable> void writeMarshalable(T data) {
        if (data != null) {
            String signature = data.getMarshalSignature();

            if (signature != null) {
                try {
                    Class.forName(signature);

                } catch (Throwable e) {
                    signature = null;
                }
            }

            if (signature == null) {
                signature = data.getClass().getName();
            }

            writeString(signature);
            data.writeToMarshal(this);

        } else {
            writeString(null);
        }
    }

    /**
     *
     */
    public <T extends Marshalable> void writeMarshalableArray(T[] data) {
        if (data != null) {
            writeInt(data.length);

            for (int i=0; i < data.length; i++) {
                writeMarshalable(data[i]);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public <T extends Marshalable> T readMarshalable() {
        String className = readString();

        if (className != null) {
            try {
                return (T) Class.forName(className).getConstructor(Marshal.class).newInstance(this);

            } catch (Throwable e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     *
     */
    public Marshalable[] readMarshalableArray() {
        int N = readInt();

        if (N >= 0) {
            Marshalable[] array = new Marshalable[N];

            for (int i = 0; i < N; i++) {
                array[i] = readMarshalable();
            }

            return array;
        }

        return null;
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public <T extends Marshalable> T[] readMarshalableArray(Class<T> clazz) {
        int N = readInt();

        if (N >= 0) {
            T[] array = (T[]) Array.newInstance(clazz, N);

            for (int i = 0; i < N; i++) {
                array[i] = readMarshalable();
            }

            return array;
        }

        return null;
    }


    /* =============================================================
     * -------------------------------------------------------------
     * STRING
     */

    /**
     *
     */
    public void writeString(String data) {
        char[] chars = null;

        if (data != null) {
            chars = data.toCharArray();
        }

        writeCharArray(chars);
    }

    /**
     *
     */
    public void writeStringArray(String[] data) {
        if (data != null) {
            writeInt(data.length);

            for (String str : data) {
                writeString(str);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     *
     */
    public String readString() {
        char[] chars = readCharArray();

        if (chars != null) {
            return new String(chars);
        }

        return null;
    }

    /**
     *
     */
    public String[] readStringArray() {
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


    /* =============================================================
     * -------------------------------------------------------------
     * INTEGER
     */

    /**
     *
     */
    public void writeInt(int data) {
        writeRaw(data);
    }

    /**
     *
     */
    public void writeIntArray(int[] data) {
        if (data != null) {
            writeInt(data.length);

            for (int in : data) {
                writeInt(in);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     *
     */
    public int readInt() {
        return (Integer) readRaw();
    }

    /**
     *
     */
    public int[] readIntArray() {
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


    /* =============================================================
     * -------------------------------------------------------------
     * LONG
     */

    /**
     *
     */
    public void writeLong(long data) {
        writeRaw(data);
    }

    /**
     *
     */
    public void writeLongArray(long[] data) {
        if (data != null) {
            writeInt(data.length);

            for (long in : data) {
                writeLong(in);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     *
     */
    public long readLong() {
        return (Long) readRaw();
    }

    /**
     *
     */
    public long[] readLongArray() {
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


    /* =============================================================
     * -------------------------------------------------------------
     * FLOAT
     */

    /**
     *
     */
    public void writeFloat(float data) {
        writeRaw(data);
    }

    /**
     *
     */
    public void writeFloatArray(float[] data) {
        if (data != null) {
            writeInt(data.length);

            for (float fl : data) {
                writeFloat(fl);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     *
     */
    public float readFloat() {
        return (Float) readRaw();
    }

    /**
     *
     */
    public float[] readFloatArray() {
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


    /* =============================================================
     * -------------------------------------------------------------
     * DOUBLE
     */

    /**
     *
     */
    public void writeDouble(double data) {
        writeRaw(data);
    }

    /**
     *
     */
    public void writeDoubleArray(double[] data) {
        if (data != null) {
            writeInt(data.length);

            for (double fl : data) {
                writeDouble(fl);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     *
     */
    public double readDouble() {
        return (Double) readRaw();
    }

    /**
     *
     */
    public double[] readDoubleArray() {
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


    /* =============================================================
     * -------------------------------------------------------------
     * BYTE
     */

    /**
     *
     */
    public void writeByte(byte data) {
        writeRaw(data);
    }

    /**
     *
     */
    public void writeByteArray(byte[] data) {
        if (data != null) {
            writeInt(data.length);

            for (byte by : data) {
                writeByte(by);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     *
     */
    public byte readByte() {
        return (Byte) readRaw();
    }

    /**
     *
     */
    public byte[] readByteArray() {
        int N = readInt();

        if (N >= 0) {
            byte[] out = new byte[N];

            for (int i=0; i < N; i++) {
                out[i] = readByte();
            }

            return out;
        }

        return null;
    }


    /* =============================================================
     * -------------------------------------------------------------
     * BYTE
     */

    /**
     *
     */
    public void writeChar(char data) {
        writeRaw(data);
    }

    /**
     *
     */
    public void writeCharArray(char[] data) {
        if (data != null) {
            writeInt(data.length);

            for (char by : data) {
                writeChar(by);
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     *
     */
    public char readChar() {
        return (Character) readRaw();
    }

    /**
     *
     */
    public char[] readCharArray() {
        int N = readInt();

        if (N >= 0) {
            char[] out = new char[N];

            for (int i=0; i < N; i++) {
                out[i] = readChar();
            }

            return out;
        }

        return null;
    }


    /* =============================================================
     * -------------------------------------------------------------
     * OBJECT ARRAYS
     */

    /**
     *
     */
    public void writeArray(Object[] data) {
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
     *
     */
    public Object[] readArray() {
        int N = readInt();

        if (N >= 0) {
            Object[] out = new Object[N];

            for (int i=0; i < N; i++) {
                out[i] = readValue();
            }

            return out;
        }

        return null;
    }


    /* =============================================================
     * -------------------------------------------------------------
     * COLLECTIONS
     */

    /**
     *
     */
    public void writeList(List<?> data) {
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
     *
     */
    public List<?> readList() {
        int N = readInt();

        if (N >= 0) {
            List<Object> out = new SwiftList<Object>();

            for (int i=0; i < N; i++) {
                out.add(readValue());
            }

            return out;
        }

        return null;
    }

    /**
     *
     */
    public void writeMap(Map data) {
        if (data != null) {
            int N = data.size();
            writeInt(N);

            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) data).entrySet()) {
                writeValue(entry.getKey());
                writeValue(entry.getValue());
            }

        } else {
            writeInt(-1);
        }
    }

    /**
     *
     */
    public Map<?, ?> readMap() {
        int N = readInt();

        if (N >= 0) {
            Map<Object, Object> out = new SwiftMap<Object, Object>();
            Object key;
            Object value;

            for (int i=0; i < N; i++) {
                key = readValue();
                value = readValue();

                out.put(key, value);
            }

            return out;
        }

        return null;
    }

    /**
     *
     */
    public void writeSet(Set data) {
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
     *
     */
    public Set<?> readSet() {
        int N = readInt();

        if (N >= 0) {
            Set<Object> out = new SwiftSet<Object>();
            for (int i=0; i < N; i++) {
                out.add(readValue());
            }

            return out;
        }

        return null;
    }


    /* =============================================================
     * -------------------------------------------------------------
     * AUTOMATIC
     */

    /**
     *
     */
    public Object readValue() {
        int type = readInt();

        switch (type) {
            case TYPE_NULL:
                return null;

            case TYPE_MARSHALABLE:
                return readMarshalable();

            case TYPE_MARSHALABLEARRAY:
                return readMarshalableArray();

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
                return readInt() > 0;

            case TYPE_BOOLEANARRAY:
                int size = readInt();
                boolean[] ret = new boolean[size];

                for (int i=0; i < size; i++) {
                    ret[i] = readInt() > 0;
                }

                return ret;

            case TYPE_BYTE:
                return readByte();

            case TYPE_BYTEARRAY:
                return readByteArray();

            case TYPE_CHAR:
                return readChar();

            case TYPE_CHARARRAY:
                return readCharArray();

            case TYPE_OBJECTARRAY:
                return readArray();

            case TYPE_LIST:
                return readList();

            case TYPE_MAP:
                return readMap();

            case TYPE_SET:
                return readSet();

            case TYPE_SERIALIZEABLE:
                return readSerializable();

            case TYPE_SERIALIZEABLEARRAY:
                return readSerializableArray();

            default:
                return null;
        }
    }

    /**
     *
     */
    public void writeValue(Object data) {
        if (data == null) {
            writeInt(TYPE_NULL);

        } else if (data instanceof Marshalable) {
            writeInt(TYPE_MARSHALABLE);
            writeMarshalable((Marshalable) data);

        } else if (data instanceof Marshalable[]) {
            writeInt(TYPE_MARSHALABLEARRAY);
            writeMarshalableArray((Marshalable[]) data);

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
            writeInt(((Boolean) data) ? 1 : 0);

        } else if (data instanceof boolean[]) {
            boolean[] arr = (boolean[]) data;

            writeInt(TYPE_BOOLEANARRAY);
            writeInt(arr.length);

            for (int i=0; i < arr.length; i++) {
                writeInt(arr[i] ? 1 : 0);
            }

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
            writeList((List<?>) data);

        } else if (data instanceof Map) {
            writeInt(TYPE_MAP);
            writeMap((Map<?,?>) data);

        } else if (data instanceof Set) {
            writeInt(TYPE_SET);
            writeSet((Set<?>) data);

        } else if (data instanceof Serializable) {
            writeInt(TYPE_SERIALIZEABLE);
            writeSerializable((Serializable) data);

        } else if (data instanceof Serializable[]) {
            writeInt(TYPE_SERIALIZEABLEARRAY);
            writeSerializableArray((Serializable[]) data);

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
}

