package com.lucaszanella.sisgrad;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by lucaszanella on 6/12/16.
 */
public class Sha256Hex {
    public static String hash(String data) {
        MessageDigest md = null;
        try {//never gonna throw this error because "SHA-256" is always available
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(data.getBytes());
        return bytesToHex(md.digest());
    }
    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}

