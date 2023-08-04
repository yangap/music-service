package com.anping.music.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Random;

/**
 * @author Anping Sec
 * @date 2023/03/07
 * description:
 */
public class QqEncrypt {
    private static final String encNonce = "CJBPACrRuNy7";
    private static final String signPrxfix = "zza";
    private static final char[] dir = "0234567890abcdefghijklmnopqrstuvwxyz".toCharArray();

    public static String getSign(String encParams) {
        return signPrxfix + uuidGenerate() + Md5Encrypt.convertToMd5(encNonce + encParams);
    }

    private static String uuidGenerate() {
        int minLen = 10;
        int maxLen = 16;
        Random ran = new Random(System.currentTimeMillis());
        int ranLen = ran.nextInt(maxLen - minLen) + minLen;
        StringBuilder sb = new StringBuilder(ranLen);
        for (int i = 0; i < ranLen; i++) {
            sb.append(dir[ran.nextInt(dir.length)]);
        }
        return sb.toString();
    }

    public static class Md5Encrypt {
        public static String convertToMd5(String plainText) {
            byte[] secretBytes = null;
            try {
                secretBytes = MessageDigest.getInstance("md5").digest(plainText.getBytes("utf-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (secretBytes == null) return "";
            StringBuilder md5code = new StringBuilder(new BigInteger(1, secretBytes).toString(16));
            for (int i = 0; i < 32 - md5code.length(); i++) {
                md5code.insert(0, "0");
            }
            return md5code.toString();
        }
    }

    public static void main(String[] args) {
        String sign = getSign("{\n" +
                "    \"req_6\": {\n" +
                "        \"module\": \"vkey.GetVkeyServer\",\n" +
                "        \"method\": \"CgiGetVkey\",\n" +
                "        \"param\": {\n" +
                "            \"guid\": \"191699105\",\n" +
                "            \"songmid\": [\n" +
                "                \"0009BCJK1nRaad\"\n" +
                "            ],\n" +
                "            \"songtype\": [\n" +
                "                0\n" +
                "            ],\n" +
                "            \"uin\": \"1429349216\",\n" +
                "            \"loginflag\": 1\n" +
                "        }\n" +
                "    }\n" +
                "}");
        System.out.println(sign);
    }
}
