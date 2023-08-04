package com.anping.music.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/3/16.
 */
public class RSAUtil {
	public static final String KEY_ALGORITHM = "RSA";
	/**
	 * 貌似默认是RSA/NONE/PKCS1Padding，未验证
	 */
	public static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
	public static final String PUBLIC_KEY = "publicKey";
	public static final String PRIVATE_KEY = "privateKey";

	/**
	 * RSA密钥长度必须是64的倍数，在512~65536之间。默认是1024
	 */
	public static final int KEY_SIZE = 2048;

	public static final String PLAIN_TEXT = "123456";

	public static void main(String[] args) throws UnsupportedEncodingException {
//		Map<String, String> keyMap = generateKeyBytes();
//
//		// 加密
//		PublicKey publicKey = restorePublicKey(keyMap.get(PUBLIC_KEY));
//
//		byte[] encodedText = RSAEncode(publicKey, PLAIN_TEXT.getBytes());
//		System.out.println("RSA encoded: " + Base64.encodeBase64String(encodedText));

//		// 解密
//		String str = "AFu0cJGe8x2XQVHpc5rHlou91Pk+nNh0M/mSSMP689hWIqvDd+JlA2/5sG2GU6gWZiaTg0Q" +
//				"V+MTefJzAU7tfYjmDRV8MGP+wqz9Rmil9yxJz6clLLKYkoyicDxgV3t4mZHrTIlQ//2z/DZY68qITvhzEDV+S2kF4GhDVmQwoldqsE6kCWUwo1TosSv5Sx39GKzFOpPctpAGpNXDiU623keDAPxTFQncQZ7e7bcIC/lNSJteGCfDJAUrZS82wa1eyQEcu1nMgLYnImi3HcXfSWfYzbdn67/fMHLCLm7+UKz7uLoP95F2mfuia+rZZWO6OmI3Sl5GVCEgALVUt5gaOKA==";
//		byte[] bytes = str.getBytes();
//		PrivateKey privateKey = restorePrivateKey("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCpC5bWyktajUGeBxV6D1U+npEFgZdhdZigEyCDr9gAX56DOWM/Dc5aUWxSow5ceCEdJnb/Ld3GSfz6thBSYmwryNYHT9OqML8C0FSY3rZtwzqJ+idHC0t7PnG6kG+ixJ+iVN7xqmPvpCKFWqgIE2BwwOQVSypS04M8LTv1zqATYf7OejLGWtdTzG0W2Uh1VxwH59bHtR5zZxxWddy2uPMgBLrwZ/Xue2h2gmldmGJXtQORWdzmvelCFo7oaoY6SPqOzs50NYOJ58STJdb+mpCKwNBkeNaE/IfvGgpr2neCBSoEbcLBWiRPAX+VbcWqG6nZnWtLXcB1ghDWTbLl+6DrAgMBAAECggEAGNZAEkzkp2hLtMk7qZRpJOstSY7RzBr4EsTcqRyD8wIZixQ6dcA0imaeMMIOUR3BV8QYBcQaapDiX3+yhDg7xm8aqzZaVg4VcndjhpZqXVQq3KzpanHJZFE3an7gOB0Fi1dG7mDnkAUgoVFBkPYv3EErAcnqbQLwrD3FKodyU4Z7qjU03AlxvDLid0FDFFd+PeulK48106N26Mcja/BqJHlgWVc/dC0MlADMdIl4Odxdb/Hk0aZSGjgwk2EtuQ5h71Re/9KQk3+8MLkLoSSTAQ/SQSDsuIRZvJAVeImUX3HLRmWD8FDBYzJT8xk6DUG7ADUMLjOW1qrhlAZ2ihRCoQKBgQDvdhXAfpRmK0uBZ2NcHLc8zsYeA4dLudb9NXFOULtOq5uLEaj2oEqRgx/o2pSM/9YVnrfj1go2LJKE86tdy4uFRk+5cj51Hpqpa3KM1d5S6tyenRgt5Ws43LWm2FuiefTeXV393U90fptrm1DUrRYGYmLHQJihN0d/QEy1R6ho0QKBgQC0uHqXTwZxAhivOWypi5NH9mBbViDCTGsSfneySbUPOn30pKpl+H1Vyvan0vZOpeLUaqlKWhOaZcWvoIcVqAfmcT7CzCp2tzk6TVchy8i0sB0bgDN+Esp9R/64y6T8Z75WVdG79ldepa6aCmfiYS25UkXSxjW9o0OuaHXZa4Yc+wKBgALF9FSJYsCYKcSy5NSxRHIA1wyL7+oxmcrO7qQAjVrI5AFFUBajHpn20czJk0Taxaj0cJ7D4NRlycCebdKlQ9VkThIQv6Ztl070/pug8nU1YR65JQmLCaXb8IKhKNaF4Q8eN7Nz97KasMQbRWFhK8shIpG6LspXMI3JudmzncCxAoGBAJbsNVzlXyDDOnAjSc7xxEblx6Sfce4xuX2RZk7bZTzZgVTsq21LLwiV3pVbBd26hYsVpFl2jkZz1bsNGaMJV8eoLVyq+o94Bm/dJPYRZIUZSsWrs9UGxloH5P7Y+ujbjsY1F1mcqWxee0kjhYw10eShg7Q5WaoMjbo6fKjdNfw/AoGBAN32gCVNs5tm69uJbBbBhGjuFX8nkefqkm8MVygMcPbdhZsp5oLvAbA+lR1FVawyNiu49m6R0i2h8+cClb0t1+3d5U6wi9RF4CLQ98tkO2JS3D7yjsNtFtLZFkQ7FTLi5GSjPy3a/iq7qoT+1fJnQdWWBOABWTcT9h67uvfwnjx/");
//		System.out.println("RSA decoded: " + RSADecode(privateKey, Base64.encodeBase64String(bytes)));
	}

	/**
	 * 生成密钥对。注意这里是生成密钥对KeyPair，再由密钥对获取公私钥
	 *
	 * @return
	 */
	public static Map<String, String> generateKeyBytes() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
			keyPairGenerator.initialize(KEY_SIZE);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
			RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

			Map<String, String> keyMap = new HashMap<>();
			keyMap.put(PUBLIC_KEY, Base64.encodeBase64String(publicKey.getEncoded()));
			keyMap.put(PRIVATE_KEY, Base64.encodeBase64String(privateKey.getEncoded()));
			return keyMap;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 还原公钥，X509EncodedKeySpec 用于构建公钥的规范
	 *
	 * @param keyBytes
	 * @return
	 */
	public static PublicKey restorePublicKey(String keyBytes) {
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.decodeBase64(keyBytes));
		try {
			KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
			PublicKey publicKey = factory.generatePublic(x509EncodedKeySpec);
			return publicKey;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 还原私钥，PKCS8EncodedKeySpec 用于构建私钥的规范
	 *
	 * @param keyBytes
	 * @return
	 */
	public static PrivateKey restorePrivateKey(String keyBytes) {
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(keyBytes));
		try {
			KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
			PrivateKey privateKey = factory.generatePrivate(pkcs8EncodedKeySpec);
			return privateKey;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 加密，三步走。
	 *
	 * @param key
	 * @param plainText
	 * @return
	 */
	public static byte[] RSAEncode(PublicKey key, byte[] plainText) {

		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return cipher.doFinal(plainText);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * 解密，三步走。
	 *
	 * @param key
	 * @param encodedText
	 * @return
	 */
	public static String RSADecode(PrivateKey key, String encodedText) {

		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key);
			return new String(cipher.doFinal(Base64.decodeBase64(encodedText)));
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			e.printStackTrace();
		}
		return null;

	}
}