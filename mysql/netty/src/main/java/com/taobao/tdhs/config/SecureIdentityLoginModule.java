package com.taobao.tdhs.config;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.StringUtils;

public class SecureIdentityLoginModule {

    private static byte[] ENC_KEY_BYTES = "jaas is the way".getBytes();

    private String        userName;

    private String        password;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDecodedPassword() throws Exception {
        return new String(decode(password));
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SecureIdentityLoginModule other = (SecureIdentityLoginModule) obj;
        if (password == null) {
            if (other.password != null) return false;
        } else if (!password.equals(other.password)) return false;
        if (userName == null) {
            if (other.userName != null) return false;
        } else if (!userName.equals(other.userName)) return false;
        return true;
    }

    public static String encode(String encKey, String secret) throws NoSuchAlgorithmException, NoSuchPaddingException,
                                                             InvalidKeyException, IllegalBlockSizeException,
                                                             BadPaddingException {
        byte[] kbytes = SecureIdentityLoginModule.ENC_KEY_BYTES;
        if (StringUtils.isNotBlank(encKey)) {
            kbytes = encKey.getBytes();
        }
        SecretKeySpec key = new SecretKeySpec(kbytes, "Blowfish");
        Cipher cipher = Cipher.getInstance("Blowfish");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encoding = cipher.doFinal(secret.getBytes());
        BigInteger n = new BigInteger(encoding);
        return n.toString(16);
    }

    public static String encode(String secret) throws NoSuchPaddingException, NoSuchAlgorithmException,
                                              InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return SecureIdentityLoginModule.encode(null, secret);
    }

    public static String decode(String encKey, String secret) throws NoSuchPaddingException, NoSuchAlgorithmException,
                                                             InvalidKeyException, BadPaddingException,
                                                             IllegalBlockSizeException {
        byte[] kbytes = SecureIdentityLoginModule.ENC_KEY_BYTES;
        if (StringUtils.isNotBlank(encKey)) {
            kbytes = encKey.getBytes();
        }
        SecretKeySpec key = new SecretKeySpec(kbytes, "Blowfish");
        BigInteger n = new BigInteger(secret, 16);
        byte[] encoding = n.toByteArray();
        Cipher cipher = Cipher.getInstance("Blowfish");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decode = cipher.doFinal(encoding);
        return new String(decode);
    }

    public static char[] decode(String secret) throws NoSuchPaddingException, NoSuchAlgorithmException,
                                              InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return SecureIdentityLoginModule.decode(null, secret).toCharArray();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Encoded password: " + new String(SecureIdentityLoginModule.encode("Config_RsA+")));
        System.out.println("decoded password: "
                           + new String(SecureIdentityLoginModule.decode("-5ba6f4c59342aa2ce70e0b8bdc250194e9eba007b67f1e12")));
    }
}
