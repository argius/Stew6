package net.argius.stew;

import java.io.*;
import javax.crypto.*;

/**
 * A skeletal implementation of the Password interface using ciphers.
 */
public abstract class CipherPassword implements Password {

    private static String secretKey = "";

    private String transformedString;

    @Override
    public final String getTransformedString() {
        if (transformedString != null) {
            return transformedString;
        }
        return "";
    }

    @Override
    public final void setTransformedString(String transformedString) {
        if (transformedString != null) {
            this.transformedString = transformedString;
        }
    }

    @Override
    public final String getRawString() {
        if (transformedString != null) {
            return decrypt(transformedString);
        }
        return "";
    }

    @Override
    public final void setRawString(String rowString) {
        if (rowString != null) {
            this.transformedString = encrypt(rowString);
        }
    }

    @Override
    public final boolean hasPassword() {
        return transformedString != null;
    }

    /**
     * Sets a secret key.
     * @param secretKey
     */
    public static void setSecretKey(String secretKey) {
        assert secretKey != null && secretKey.length() > 0;
        CipherPassword.secretKey = secretKey;
    }

    /**
     * Encrypts a password.
     * @param rowString
     * @return the encrypted password
     */
    private String encrypt(String rowString) {
        try {
            Cipher cipher = getCipherInstance(secretKey, Cipher.ENCRYPT_MODE);
            byte[] encrypted = cipher.doFinal(rowString.getBytes());
            return toHexString(encrypted);
        } catch (BadPaddingException | IllegalBlockSizeException ex) {
            // FIXME reconsider handling
            System.err.println(ex);
        } catch (Exception ex) {
            System.err.println(ex);
        }
        return "";
    }

    /**
     * Decrypts a password.
     * @param cryptedString
     * @return the decrypted password
     */
    private String decrypt(String cryptedString) {
        try {
            Cipher cipher = getCipherInstance(secretKey, Cipher.DECRYPT_MODE);
            byte[] decrypted = cipher.doFinal(toBytes(cryptedString));
            return new String(decrypted);
        } catch (BadPaddingException | IllegalBlockSizeException ex) {
            // FIXME reconsider handling
            System.err.println(ex);
        } catch (Exception ex) {
            System.err.println(ex);
        }
        return "";
    }

    private static String toHexString(byte[] bytes) {
        StringBuffer buffer = new StringBuffer();
        for (byte b : bytes) {
            buffer.append(String.format("%02X", b & 0xFF));
        }
        return buffer.toString();
    }

    private static byte[] toBytes(String hexString) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < hexString.length(); i += 2) {
            String s = hexString.substring(i, i + 2);
            bos.write(Integer.parseInt(s, 16));
        }
        return bos.toByteArray();
    }

    /**
     * Gets a instance of Cipher class.
     * @param key
     * @param mode Cipher.DECRYPT_MODE or Cipher.DECRYPT_MODE
     * @return the instance of Cipher class
     * @see Cipher
     */
    protected abstract Cipher getCipherInstance(String key, int mode);

}
