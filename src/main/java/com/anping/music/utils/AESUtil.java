package com.anping.music.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Anping Sec
 * @date 2023/06/26
 * description:
 */
public class AESUtil {

    public static final String CHARSET_NAME = "UTF-8";

    public static byte[] encrypt(String data, String key, String ivs, String mode) {
        mode = mode.toUpperCase();
        byte[] encrypted = null;
        try {
            byte[] byteContent = data.getBytes(CHARSET_NAME);
            Cipher cipher = Cipher.getInstance("AES/" + mode + "/PKCS5Padding");
            byte[] keyBytes = key.getBytes(CHARSET_NAME);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            if ("ECB".equals(mode)) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            } else {
                IvParameterSpec iv = new IvParameterSpec(ivs.getBytes());
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
            }
            encrypted = cipher.doFinal(byteContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encrypted;
    }

    public static byte[] decrypt(byte[] byteContent, String key, String ivs, String mode) {
        mode = mode.toUpperCase();
        byte[] original = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/" + mode + "/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
            if ("ECB".equals(mode)) {
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            } else {
                IvParameterSpec iv = new IvParameterSpec(ivs.getBytes());
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);
            }
            original = cipher.doFinal(byteContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return original;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] bytes = new byte[length];
        String hexDigits = "0123456789abcdef";
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            int h = hexDigits.indexOf(hexChars[pos]) << 4;
            int l = hexDigits.indexOf(hexChars[pos + 1]);
            if (l == -1) {
                // 非16进制字符
                return null;
            }
            bytes[i] = (byte) (h | l);
        }
        return bytes;
    }

    public static String bytesToBase64(byte[] byteArray) {
        return new String(new Base64().encode(byteArray));
    }
    public static byte[] base64ToBytes(String base64EncodedString) {
        return new Base64().decode(base64EncodedString);
    }
}
