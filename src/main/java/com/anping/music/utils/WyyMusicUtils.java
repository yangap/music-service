package com.anping.music.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * @author Anping Sec
 * @date 2023/04/04
 * description:
 */
public class WyyMusicUtils {

    public static final String IVS = "0102030405060708";

    /**
     * AES第一次加密key，第二次为随机key
     */
    public static final String PRESENT_KEY = "0CoJUm6Qyw8W8jud";

    public static String eapiKey = "e82ckenh8dichen8";

    public static final String dir = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String randomString(Integer len) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int index = (int) (Math.random() * dir.length());
            str.append(dir.charAt(index));
        }
        return str.toString();
    }

    public static String fill(String str, int size) {
        StringBuilder strBuilder = new StringBuilder(str);
        while (strBuilder.length() < size) {
            strBuilder.insert(0, "0");
        }
        str = strBuilder.toString();
        return str;
    }


    public static String rsaEncrypt(String text, String pubKey, String modulus) {
        List<String> list = Arrays.asList(text.split(""));
        Collections.reverse(list);
        String _text = StringUtils.join(list, "");
        BigInteger biText = new BigInteger(1, _text.getBytes());
        BigInteger biEx = new BigInteger(1, AESUtil.hexToBytes(pubKey));
        BigInteger biMod = new BigInteger(1, AESUtil.hexToBytes(modulus));
        String biRet = biText.modPow(biEx, biMod).toString(16);
        return fill(biRet, 256);
    }

    public static String gzipDecrypt(String text) {
        InputStream ins = new ByteArrayInputStream(text.getBytes());
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(ins);
            byteArrayOutputStream = new ByteArrayOutputStream();
            int len = -1;
            byte[] buffer = new byte[1024];
            while ((len = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (byteArrayOutputStream != null) byteArrayOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return byteArrayOutputStream != null ? new String(byteArrayOutputStream.toByteArray()) : null;
    }

    public static Map<String, Object> weapiEncrypt(String param) {
        String e = "010001";
        String f = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
        String i = randomString(16);
        Map<String, Object> map = new HashMap<>();
        String encText = AESUtil.bytesToBase64(AESUtil.encrypt(param, PRESENT_KEY, IVS, "cbc"));
        encText = AESUtil.bytesToBase64(AESUtil.encrypt(encText, i, IVS, "cbc"));
        String encSecKey = rsaEncrypt(i, e, f);
        map.put("params", encText);
        map.put("encSecKey", encSecKey);
        return map;
    }

    /**
     * header = {
     * osver: cookie.osver, //系统版本
     * deviceId: cookie.deviceId, //encrypt.base64.encode(imei + '\t02:00:00:00:00:00\t5106025eb79a5247\t70ffbaac7')
     * appver: cookie.appver || '8.9.70', // app版本
     * versioncode: cookie.versioncode || '140', //版本号
     * mobilename: cookie.mobilename, //设备model
     * buildver: cookie.buildver || Date.now().toString().substr(0, 10),
     * resolution: cookie.resolution || '1920x1080', //设备分辨率
     * __csrf: csrfToken,
     * os: cookie.os || 'android',
     * channel: cookie.channel,
     * requestId: `${Date.now()}_${Math.floor(Math.random() * 1000)
     * .toString()
     * .padStart(4, '0')}`,
     * }
     */
    public static Map<String, Object> eapiEncrypt(String url, String text) {
        String message = "nobody" + url + "use" + text + "md5forencrypt";
        String digest = QqEncrypt.Md5Encrypt.convertToMd5(message);
        String data = url + "-36cd479b6b5-" + text + "-36cd479b6b5-" + digest;
        String params = AESUtil.bytesToHex(AESUtil.encrypt(data, eapiKey, "", "ecb")).toUpperCase();
        Map<String, Object> map = new HashMap<>();
        map.put("params", params);
        return map;
    }

    public static Map<String, Object> eapiEncryptWithHeader(String url, JSONObject param) {
        param.put("header", getEapiHeader());
        return eapiEncrypt(url, param.toString());
    }

    public static JSONObject getEapiHeader() {
        JSONObject header = new JSONObject();
        header.put("os", "OSX");
        header.put("deviceId", "40C4753B-2370-5B1B-BE0D-CCEA768B5965%7C1C36F52A-CC62-43B3-8CC3-737451AC0587");
        String requestId = "1598671" + (int) (Math.random() * 10);
        header.put("requestId", requestId);
        header.put("appver", "2.3.14");
        return header;
    }

    public static String eapiDecrypt(String params) {
        byte[] bytes = AESUtil.hexToBytes(params.toLowerCase());
        String result = null;
        try {
            result = new String(AESUtil.decrypt(bytes, eapiKey, "", "ecb"), "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    public static void main(String[] args) {
        System.out.println(WyyMusicUtils.weapiEncrypt("{}"));
        String str = eapiDecrypt("9E44C61C7604F33F328DE9633B8B0E69E14D621EB896C63E8E0FBC1DFA867E56C8B11D41A6AEDBF3FDE560882692FD0A97C1B8F8BEA4E566AA6D924FAAE85644C14A52C6F96BE45FA5C61E52F113223D30C97ECEE25C90478F6565C21C37DF27997618B142217B52D5C5D82DEA15A0F958A7375AE30064E5903926145EC4067E96A55E5D6FF5B2311D9B0F864E4337A63C871BA155C4387273B76FC7FF2F522CE6CD11A3EBD4CB91DEE84E490BF8D9086FFF22C6A90DB73B53A7BFC2E2E4B85E3DB7E3F5AD5BC191D1253BF52CDFCF48B40B1BDDBD1E1C1D8F18A923CC5C6F59021587A412369DAC354F8F2258FEDC0370F7389578B9D64B9548CE518849D0DF8CB781D071A97D84878EC3E3376B726A6236314E3433D711325E65A3B68ADA2D99401727C568A7268B5A17B28BD3515762ACD4DE49FBBAA61113E9AFA46706EA395054962C4E482EE81D2207B2F7CB567F2889447BDE92234E7AB7163C2B6954544F4AB046F7F44A08A88063C4E594CF1A2B77E7770535111700857C7B565D508DC0D609FD562B332BEFD9709A5B2502F6950495953263F9057169D24D0CF33C5EB36694CB196A7019316D55D4B9F0774CAC4531CBBA185EC027395197232F8D6E1080022810D5B47D7F4C00EFE0551E4104BA336171A7FCC327136A42DD50D22C226D061EFA2D3777FA06B9739BEF128D4E67943F6F32F45F68B683B475539F01857A74C6CA11898AAFC051BFC8393D960B49D381C2C7FA990E9101833AF4D075CD90EE1F5900526BFEDA6E4EFA9DD2C0A00E51FF40DFBCB1E4A22E1439007C06F09F9E99C635992055A71E3AE344F91D89A34F23ADD4C17202E1E9E98EC3AD69554DABED177F0298D4022CA4BB2DA51726635398324E564F52C130DB5B7E7764632971D166226C334D008874237C53BBDDEAB97A6AF8088FB157FF728760FF598B4BE45AE758C84B6E69653CAB107F46AC7AFFFB98F67ADD9DFDA716B803C8D357B6D462B0890AAE23B3BF15A172AC9613901DF9B541CE11F4E90283329D650698976E5C93D6726CA804BEB2F911501BF3EB6895B0A0CDB1BC9BC7BAE9E83E93D75F2E640EDDCBBC70BFD135A5C5E73E954E9F4A0975027CEB6C0C4E9B4F913EDC5E18D079BCFF18F0F3F4838A269293CBEF3BC0A9AD889C89B780703AFD3FF5946296AE494F03738EAE405BFF6EB16F8A8487220714C9752500992A43B88671DCA6266B9524107281E859BCA85C550FB522FCB2060B9693A76795E94A0C99015FB7ACD658EB913D7E336433F3B75A90428A13729FECE02ABBC3BF2CD9FBC0361501E4DBEC8CD3315D01A347B79FF4F76D06A510ED1FDC1019BCBF3CC89B65C38769E5AC3D37CCDE572FBB650B35A23556B1601123200258145DF4085D0A46D3EBF9EE6A4E1E03EEDC07A806C7E430628FC782BA3D7B8547635141E4288BFF");
        System.out.println(str);
        Map<String, Object> stringObjectMap = eapiEncrypt("/api/v6/playlist/detail", "{\"header\":\"{\\\"os\\\":\\\"osx\\\",\\\"appver\\\":\\\"2.3.16\\\",\\\"deviceId\\\":\\\"40C4753B-2370-5B1B-BE0D-CCEA768B5965%7C1C36F52A-CC62-43B3-8CC3-737451AC0587\\\",\\\"requestId\\\":\\\"66444283\\\",\\\"clientSign\\\":\\\"\\\",\\\"osver\\\":\\\"%E7%89%88%E6%9C%AC13.4%EF%BC%88%E7%89%88%E5%8F%B722F66%EF%BC%89\\\",\\\"Nm-GCore-Status\\\":\\\"1\\\",\\\"X-antiCheatToken\\\":\\\"9ca17ae2e6ffcda170e2e6eed5dc44afbca4dab25eaab48fa3d45e968b9f83d16db896ffd7f47af2b1a183cc2af0feaec3b92afcbe8bb4f63a8398fcaad05a978e9fb2d84fa68affd9dc63838a9bb6e16fb193ee9e\\\",\\\"MConfig-Info\\\":\\\"{\\\\\\\"IuRPVVmc3WWul9fT\\\\\\\":{\\\\\\\"version\\\\\\\":104448,\\\\\\\"appver\\\\\\\":\\\\\\\"2.3.16\\\\\\\"}}\\\",\\\"MG-Product-Name\\\":\\\"music\\\"}\",\"t\":\"-1\",\"os\":\"OSX\",\"id\":\"609770869\",\"checkToken\":\"9ca17ae2e6ffcda170e2e6eed5dc44afbca4dab25eaab48fa3d45e968b9f83d16db896ffd7f47af2b1a183cc2af0feaec3b92afcbe8bb4f63a8398fcaad05a978e9fb2d84fa68affd9dc63838a9bb6e16fb193ee9e\",\"verifyId\":1,\"deviceId\":\"1702e6c32fc5ff54d961250caefb0294\",\"n\":\"500\",\"s\":\"0\"}");
        System.out.println(stringObjectMap.get("params"));
    }
}
