package com.spazedog.lib.utilsLib.marshalling;

/**
 *
 */
public interface Marshalable {

    /**
     *
     */
    void writeToMarshal(Marshal dest);

    /**
     *
     */
    String getMarshalSignature();
}
