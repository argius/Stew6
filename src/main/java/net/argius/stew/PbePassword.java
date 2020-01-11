package net.argius.stew;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * The Password implementation using cipher with PBE.
 */
public final class PbePassword extends CipherPassword {

    private static final String TRANSFORMATION_NAME = "PBEWithMD5AndDES";
    private static final byte[] SALT = "0141STEW".getBytes();
    private static final int ITERATION = 10;

    @Override
    protected Cipher getCipherInstance(String code, int mode) {
        try {
            PBEKeySpec keySpec = new PBEKeySpec(code.toCharArray());
            SecretKey key = SecretKeyFactory.getInstance(TRANSFORMATION_NAME)
                                            .generateSecret(keySpec);
            PBEParameterSpec spec = new PBEParameterSpec(SALT, ITERATION);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_NAME);
            cipher.init(mode, key, spec);
            return cipher;
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

}
