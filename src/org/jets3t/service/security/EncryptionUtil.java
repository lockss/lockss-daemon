/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2010 James Murty
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jets3t.service.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.utils.ServiceUtils;

/**
 * Utility class to handle encryption and decryption in the JetS3t suite.
 * <p>
 * This class uses properties obtained through {@link org.jets3t.service.Jets3tProperties}.
 * For more information on these properties please refer to
 * <a href="http://www.jets3t.org/toolkit/configuration.html">JetS3t Configuration</a>
 * </p>
 *
 * @author James Murty
 */
public class EncryptionUtil {
    private static final Log log = LogFactory.getLog(EncryptionUtil.class);

    public static final String DEFAULT_VERSION = "2";
    public static final String DEFAULT_ALGORITHM = "PBEWithMD5AndDES";

    private String algorithm = null;
    private String version = null;
    private SecretKey key = null;
    private AlgorithmParameterSpec algParamSpec = null;

    int ITERATION_COUNT = 5000;
    byte[] salt = {
        (byte)0xA4, (byte)0x0B, (byte)0xC8, (byte)0x34,
        (byte)0xD6, (byte)0x95, (byte)0xF3, (byte)0x13
    };

    static {
        try {
            Class bouncyCastleProviderClass =
                Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            if (bouncyCastleProviderClass != null) {
                Provider bouncyCastleProvider = (Provider) bouncyCastleProviderClass
                    .getConstructor(new Class[] {}).newInstance(new Object[] {});
                Security.addProvider(bouncyCastleProvider);
            }
            if (log.isDebugEnabled()) {
                log.debug("Loaded security provider BouncyCastleProvider");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to load security provider BouncyCastleProvider");
            }
        }
    }

    /**
     * Constructs class configured with the provided password, and set up to use the encryption
     * method specified.
     *
     * @param encryptionKey
     *        the password to use for encryption/decryption.
     * @param algorithm
     *        the Java name of an encryption algorithm to use, eg PBEWithMD5AndDES
     * @param version
     *        the version of encyption to use, for historic and future compatibility.
     *        Unless using an historic version, this should always be
     *        {@link #DEFAULT_VERSION}
     *
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeySpecException
     */
    public EncryptionUtil(String encryptionKey, String algorithm, String version) throws
        InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException
    {
        this.algorithm = algorithm;
        this.version = version;
        if (log.isDebugEnabled()) {
            log.debug("Cryptographic properties: algorithm=" + this.algorithm + ", version=" + this.version);
        }

        if (!DEFAULT_VERSION.equals(version)) {
            throw new RuntimeException("Unrecognised crypto version setting: " + version);
        }

        PBEKeySpec keyspec = new PBEKeySpec(encryptionKey.toCharArray(), salt, ITERATION_COUNT, 32);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
        key = skf.generateSecret(keyspec);
        algParamSpec = new PBEParameterSpec(salt, ITERATION_COUNT);
    }

    /**
     * Constructs class configured with the provided password, and set up to use the default encryption
     * algorithm PBEWithMD5AndDES.
     *
     * @param encryptionKey
     *        the password to use for encryption/decryption.
     *
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeySpecException
     */
    public EncryptionUtil(String encryptionKey) throws InvalidKeyException,
        NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException
    {
        this(encryptionKey, "PBEWithMD5AndDES", DEFAULT_VERSION);
    }

    protected Cipher initEncryptModeCipher() throws NoSuchAlgorithmException, NoSuchPaddingException,
        InvalidKeyException, InvalidAlgorithmParameterException
    {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, algParamSpec);
        return cipher;
    }

    protected Cipher initDecryptModeCipher() throws NoSuchAlgorithmException, NoSuchPaddingException,
        InvalidKeyException, InvalidAlgorithmParameterException
    {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, algParamSpec);
        return cipher;
    }

    /**
     * Encrypts a UTF-8 string to byte data.
     *
     * @param data
     * data to encrypt.
     * @return
     * encrypted data.
     *
     * @throws IllegalStateException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public byte[] encrypt(String data) throws IllegalStateException, IllegalBlockSizeException,
        BadPaddingException, UnsupportedEncodingException, InvalidKeySpecException,
        InvalidKeyException, InvalidAlgorithmParameterException,
        NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initEncryptModeCipher();
        return cipher.doFinal(data.getBytes(Constants.DEFAULT_ENCODING));
    }

    /**
     * Decrypts byte data to a UTF-8 string.
     *
     * @param data
     * data to decrypt.
     * @return
     * UTF-8 string of decrypted data.
     *
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws UnsupportedEncodingException
     * @throws IllegalStateException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public String decryptString(byte[] data) throws InvalidKeyException,
        InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalStateException,
        IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initEncryptModeCipher();
        return new String(cipher.doFinal(data), Constants.DEFAULT_ENCODING);
    }

    /**
     * Decrypts a UTF-8 string.
     *
     * @param data
     * data to decrypt.
     * @param startIndex
     * start index of data to decrypt.
     * @param endIndex
     * end index of data to decrypt.
     * @return
     * UTF-8 string of decrypted data.
     *
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws UnsupportedEncodingException
     * @throws IllegalStateException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public String decryptString(byte[] data, int startIndex, int endIndex)
        throws InvalidKeyException, InvalidAlgorithmParameterException,
        UnsupportedEncodingException, IllegalStateException, IllegalBlockSizeException,
        BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initDecryptModeCipher();
        return new String(cipher.doFinal(data, startIndex, endIndex), Constants.DEFAULT_ENCODING);
    }

    /**
     * Encrypts byte data to bytes.
     *
     * @param data
     * data to encrypt.
     * @return
     * encrypted data.
     *
     * @throws IllegalStateException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public byte[] encrypt(byte[] data) throws IllegalStateException, IllegalBlockSizeException,
        BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,
        NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initEncryptModeCipher();
        return cipher.doFinal(data);
    }

    /**
     * Decrypts byte data to bytes.
     *
     * @param data
     * data to decrypt
     * @return
     * decrypted data.
     *
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalStateException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public byte[] decrypt(byte[] data) throws InvalidKeyException,
        InvalidAlgorithmParameterException, IllegalStateException, IllegalBlockSizeException,
        BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initDecryptModeCipher();
        return cipher.doFinal(data);
    }

    /**
     * Decrypts a byte data range to bytes.
     *
     * @param data
     * @param startIndex
     * @param endIndex
     * @return
     * decrypted data.
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalStateException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public byte[] decrypt(byte[] data, int startIndex, int endIndex) throws InvalidKeyException,
        InvalidAlgorithmParameterException, IllegalStateException, IllegalBlockSizeException,
        BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initDecryptModeCipher();
        return cipher.doFinal(data, startIndex, endIndex);
    }

    /**
     * Wraps an input stream in an encrypting cipher stream.
     *
     * @param is
     * @return
     * encrypting cipher input stream.
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public CipherInputStream encrypt(InputStream is) throws InvalidKeyException,
        InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initEncryptModeCipher();
        return new CipherInputStream(is, cipher);
    }

    /**
     * Wraps an input stream in an decrypting cipher stream.
     *
     * @param is
     * @return
     * decrypting cipher input stream.
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public CipherInputStream decrypt(InputStream is) throws InvalidKeyException,
        InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initDecryptModeCipher();
        return new CipherInputStream(is, cipher);
    }

    /**
     * Wraps an output stream in an encrypting cipher stream.
     *
     * @param os
     * @return
     * encrypting cipher output stream.
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public CipherOutputStream encrypt(OutputStream os) throws InvalidKeyException,
        InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initEncryptModeCipher();
        return new CipherOutputStream(os, cipher);
    }

    /**
     * Wraps an output stream in a decrypting cipher stream.
     *
     * @param os
     * @return
     * decrypting cipher output stream.
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public CipherOutputStream decrypt(OutputStream os) throws InvalidKeyException,
        InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initDecryptModeCipher();
        return new CipherOutputStream(os, cipher);
    }

    /**
     * Returns an estimate of the number of bytes that will result when data
     * of the given length is encrypted. The accuracy of this estimate may
     * depend on the cipher you are using, so be wary of trusting this estimate
     * without supporting evidence.
     *
     * @param inputSize
     * The number of bytes you intend to encrypt.
     *
     * @return
     * an estimate of the number of bytes that will be generated by the
     * encryption cipher for the given number of bytes of input.
     *
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public long getEncryptedOutputSize(long inputSize) throws InvalidKeyException,
        InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException
    {
        Cipher cipher = initEncryptModeCipher();
        long outputSize = 0;

        // Break input size into integer-sized chunks so we can estimate values
        // for large inputs.
        int maxChunk = Integer.MAX_VALUE - 8192;
        while (inputSize >= maxChunk) {
            outputSize += cipher.getOutputSize(maxChunk);
            inputSize -= maxChunk;
        }
        outputSize += cipher.getOutputSize((int)inputSize);
        return outputSize;
    }

    /**
     * @return
     * the Java name of the cipher algorithm being used by this class.
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns true if the given cipher is available and can be used by this encryption
     * utility. To determine whether the cipher can actually be used a test string is
     * encrypted using the cipher.
     *
     * @param cipher
     * @return
     * true if the cipher is available and can be used, false otherwise.
     */
    public static boolean isCipherAvailableForUse(String cipher) {
        try {
            EncryptionUtil encryptionUtil =
                new EncryptionUtil("Sample Key", cipher, EncryptionUtil.DEFAULT_VERSION);
            encryptionUtil.encrypt("Testing encryption...");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Availability test failed for encryption cipher " + cipher);
            }
            return false;
        }
        return true;
    }

    /**
     * Lists the PBE ciphers available on the system, optionally eliminating those
     * ciphers that are apparently available but cannot actually be used (perhaps due to
     * the lack of export-grade JCE settings).
     *
     * @param testAvailability
     * if true each apparently available cipher is tested and only those that pass
     * {@link #isCipherAvailableForUse(String)} are returned.
     *
     * @return
     * a list of all the available PBE cipher names on the system.
     */
    public static String[] listAvailablePbeCiphers(boolean testAvailability) {
        Set ciphers = Security.getAlgorithms("Cipher");
        Set pbeCiphers = new HashSet();
        for (Iterator iter = ciphers.iterator(); iter.hasNext(); ) {
            String cipher = (String) iter.next();
            if (cipher.toLowerCase().startsWith("pbe")) {
                if (!testAvailability || isCipherAvailableForUse(cipher)) {
                    pbeCiphers.add(cipher);
                }
            }
        }
        return (String[]) pbeCiphers.toArray(new String[pbeCiphers.size()]);
    }

    public static Provider[] listAvailableProviders() {
        return Security.getProviders();
    }

    /**
     * Generate an RSA SHA1 signature of the given data using the given private
     * key DER certificate.
     *
     * Based on example code from:
     * http://www.java2s.com/Tutorial/Java/0490__Security/RSASignatureGeneration.htm
     * http://forums.sun.com/thread.jspa?threadID=5175986
     *
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     * @throws InvalidKeySpecException
     * @throws NoSuchProviderException
     */
    public static byte[] signWithRsaSha1(byte[] derPrivateKeyBytes, byte[] dataToSign)
        throws NoSuchAlgorithmException, InvalidKeyException, SignatureException,
        InvalidKeySpecException, NoSuchProviderException
    {
        // Build an RSA private key from private key data
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(derPrivateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privSpec);

        // Sign data
        Signature signature = Signature.getInstance("SHA1withRSA", "BC");
        signature.initSign(privateKey, new SecureRandom());
        signature.update(dataToSign);

        byte[] signatureBytes = signature.sign();
        return signatureBytes;
    }

    /**
     * Convert a PEM encoded RSA certificate file into a DER format byte array.
     *
     * @param is
     * Input stream for PEM encoded RSA certificate data.
     *
     * @return
     * The RSA certificate data in DER format.
     *
     * @throws IOException
     */
    public static byte[] convertRsaPemToDer(InputStream is) throws IOException {
        String pemData = ServiceUtils.readInputStreamToString(is, "UTF-8");
        // Strip PEM header and footer
        int headerEndOffset = pemData.indexOf('\n');
        int footerStartOffset = pemData.indexOf("-----END");
        String strippedPemData = pemData.substring(headerEndOffset + 1, footerStartOffset - 1);

        // Decode Base64 PEM data to DER bytes
        byte[] derBytes = ServiceUtils.fromBase64(strippedPemData);
        return derBytes;
    }

    public static void main(String[] args) throws Exception {
        Provider[] providers = EncryptionUtil.listAvailableProviders();
        System.out.println("Providers:");
        for (int i = 0; i < providers.length; i++) {
            System.out.println(" - " + providers[i]);
        }

        String[] ciphers = EncryptionUtil.listAvailablePbeCiphers(false);
        System.out.println("PBE Ciphers available (untested):");
        for (int i = 0; i < ciphers.length; i++) {
            System.out.println(" - " + ciphers[i]);
        }

        ciphers = EncryptionUtil.listAvailablePbeCiphers(true);
        System.out.println("PBE Ciphers available (tested):");
        for (int i = 0; i < ciphers.length; i++) {
            System.out.println(" - " + ciphers[i]);
        }
    }

}
