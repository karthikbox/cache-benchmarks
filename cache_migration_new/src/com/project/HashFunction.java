package com.project;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
/**
 * Created by karthik on 11/8/15.
 */
public class HashFunction {


    public static BigInteger generateMD5(String message) throws HashGenerationException {
        return hashString(message, "MD5");
    }


    private static BigInteger hashString(String message, String algorithm)
            throws HashGenerationException {

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));

            return new BigInteger(1,hashedBytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            throw new HashGenerationException("Could not generate hash from String", ex);
        }
    }

}
