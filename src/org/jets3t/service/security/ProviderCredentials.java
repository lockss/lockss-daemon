/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2010 James Murty
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.ServiceException;
import org.jets3t.service.utils.ServiceUtils;

/**
 * Abstract class to contain the credentials of a user.
 *
 * @author James Murty
 * @author Nikolas Coukouma
 * @author Google developers
 */
public abstract class ProviderCredentials {
    protected static final Log log = LogFactory.getLog(ProviderCredentials.class);

    protected static final int CREDENTIALS_STORAGE_VERSION = 3;
    protected static final String V2_KEYS_DELIMITER = "AWSKEYS";
    protected static final String V3_KEYS_DELIMITER = "\n";

    protected String accessKey = null;
    protected String secretKey = null;
    protected String friendlyName = null;

    /**
     * Construct credentials.
     *
     * @param accessKey
     * Access key for a storage account.
     * @param secretKey
     * Secret key for a storage account.
     */
    public ProviderCredentials(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * Construct credentials, and associate them with a human-friendly name.
     *
     * @param accessKey
     * Access key for a storage account.
     * @param secretKey
     * Secret key for a storage account.
     * @param friendlyName
     * a name identifying the owner of the credentials, such as 'James'.
     */
    public ProviderCredentials(String accessKey, String secretKey, String friendlyName) {
        this(accessKey, secretKey);
        this.friendlyName = friendlyName;
    }

    /**
     * @return
     * the Access Key.
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * @return
     * the Secret Key.
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * @return
     * the friendly name associated with a storage account, if available.
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * @return
     * true if there is a non-null and non-empty friendly name associated
     * with this account.
     */
    public boolean hasFriendlyName() {
        return (friendlyName != null && friendlyName.trim().length() > 0);
    }

    /**
     * @return
     * a string summarizing these credentials
     */
    public String getLogString() {
        return getAccessKey() + " : " + getSecretKey();
    }

    /**
     * @return
     * the string of data that needs to be encrypted (for serialization)
     */
    protected String getDataToEncrypt() {
        return getAccessKey() + V3_KEYS_DELIMITER + getSecretKey();
    }

    /**
     * @return
     * string representing this credential type's name (for serialization)
     */
    protected abstract String getTypeName();

    protected abstract String getVersionPrefix();

    /**
     * Encrypts ProviderCredentials with the given password and saves the encrypted data to a file.
     *
     * @param password
     * the password used to encrypt the credentials.
     * @param file
     * the file to write the encrypted credentials data to.
     * @param algorithm
     * the algorithm used to encrypt the output stream.
     *
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeySpecException
     * @throws IllegalStateException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     */
    public void save(String password, File file, String algorithm) throws InvalidKeyException,
        NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
        IllegalStateException, IllegalBlockSizeException, BadPaddingException,
        InvalidAlgorithmParameterException, IOException
    {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            save(password, fos, algorithm);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
     * Encrypts ProviderCredentials with the given password and saves the encrypted data to a file
     * using the default algorithm {@link EncryptionUtil#DEFAULT_ALGORITHM}.
     *
     * @param password
     * the password used to encrypt the credentials.
     * @param file
     * the file to write the encrypted credentials data to.
     *
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeySpecException
     * @throws IllegalStateException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     */
    public void save(String password, File file) throws InvalidKeyException,
        NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
        IllegalStateException, IllegalBlockSizeException, BadPaddingException,
        InvalidAlgorithmParameterException, IOException
    {
        save(password, file, EncryptionUtil.DEFAULT_ALGORITHM);
    }

    /**
     * Encrypts ProviderCredentials with the given password and writes the encrypted data to an
     * output stream.
     *
     * @param password
     * the password used to encrypt the credentials.
     * @param outputStream
     * the output stream to write the encrypted credentials data to, this stream must be closed by
     * the caller.
     * @param algorithm
     * the algorithm used to encrypt the output stream.
     *
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeySpecException
     * @throws IllegalStateException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     */
    public void save(String password, OutputStream outputStream, String algorithm) throws InvalidKeyException,
        NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
        IllegalStateException, IllegalBlockSizeException, BadPaddingException,
        InvalidAlgorithmParameterException, IOException
    {
        BufferedOutputStream bufferedOS = null;
        EncryptionUtil encryptionUtil = new EncryptionUtil(password, algorithm, EncryptionUtil.DEFAULT_VERSION);
        bufferedOS = new BufferedOutputStream(outputStream);

        // Encrypt credentials
        byte[] encryptedData = encryptionUtil.encrypt(getDataToEncrypt());

        // Write plain-text header information to file.
        bufferedOS.write((getVersionPrefix() + CREDENTIALS_STORAGE_VERSION + "\n").getBytes(Constants.DEFAULT_ENCODING));
        bufferedOS.write((encryptionUtil.getAlgorithm() + "\n").getBytes(Constants.DEFAULT_ENCODING));
        bufferedOS.write(((friendlyName == null? "" : friendlyName) + "\n").getBytes(Constants.DEFAULT_ENCODING));
        bufferedOS.write((getTypeName() + "\n").getBytes(Constants.DEFAULT_ENCODING));

        bufferedOS.write(encryptedData);
        bufferedOS.flush();
    }

    /**
     * Encrypts ProviderCredentials with the given password and writes the encrypted data to an
     * output stream using the default algorithm {@link EncryptionUtil#DEFAULT_ALGORITHM}.
     *
     * @param password
     * the password used to encrypt the credentials.
     * @param outputStream
     * the output stream to write the encrypted credentials data to, this stream must be closed by
     * the caller.
     *
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeySpecException
     * @throws IllegalStateException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     */
    public void save(String password, OutputStream outputStream) throws InvalidKeyException,
        NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
        IllegalStateException, IllegalBlockSizeException, BadPaddingException,
        InvalidAlgorithmParameterException, IOException
    {
        save(password, outputStream, EncryptionUtil.DEFAULT_ALGORITHM);
    }

    /**
     * Loads encrypted credentials from a file.
     *
     * @param password
     * the password used to decrypt the credentials. If null, the credentials are not decrypted
     * and only the version and friendly-name information is loaded.
     * @param file
     * a file containing an encrypted data encoding of an ProviderCredentials object.
     * @return
     * the decrypted credentials in an object.
     *
     * @throws ServiceException
     */
    public static ProviderCredentials load(String password, File file) throws ServiceException {
        if (log.isDebugEnabled()) {
            log.debug("Loading credentials from file: " + file.getAbsolutePath());
        }
        BufferedInputStream fileIS = null;
        try {
            fileIS = new BufferedInputStream(new FileInputStream(file));
            return load(password, fileIS);
        } catch (Throwable t) {
            throw new ServiceException("Failed to load credentials", t);
        } finally {
            if (fileIS != null) {
                try {
                    fileIS.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Loads encrypted credentials from a data input stream.
     *
     * @param password
     * the password used to decrypt the credentials. If null, the credentials are not decrypted
     * and only the version and friendly-name information is loaded.
     * @param inputStream
     * an input stream containing an encrypted  data encoding of an ProviderCredentials object.
     * @return
     * the decrypted credentials in an object.
     *
     * @throws ServiceException
     */
    public static ProviderCredentials load(String password, BufferedInputStream inputStream)
        throws ServiceException
    {
        boolean partialReadOnly = (password == null);
        if (partialReadOnly) {
            if (log.isDebugEnabled()) {
                log.debug("Loading partial information about credentials from input stream");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Loading credentials from input stream");
            }
        }

        try {
            EncryptionUtil encryptionUtil = null;
            byte[] encryptedKeys = new byte[2048];
            int encryptedDataIndex = 0;

            String version = null;
            int versionNum = 0;
            String algorithm = "";
            String friendlyName = "";
            boolean usingDevPay = false;

            // Read version information from credentials file.
            version = ServiceUtils.readInputStreamLineToString(inputStream, Constants.DEFAULT_ENCODING);

            // Extract the version number
            int firstColonOffset = version.indexOf(":");
            String versionString = version.substring(firstColonOffset + 1).trim();
            versionNum = Integer.parseInt(versionString);
            // Read algorithm and friendly name from file.
            algorithm = ServiceUtils.readInputStreamLineToString(inputStream, Constants.DEFAULT_ENCODING);
            friendlyName = ServiceUtils.readInputStreamLineToString(inputStream, Constants.DEFAULT_ENCODING);

            if (!partialReadOnly) {
                encryptionUtil = new EncryptionUtil(password, algorithm, EncryptionUtil.DEFAULT_VERSION);
            }

            if (3 <= versionNum) {
                String credentialsType = ServiceUtils.readInputStreamLineToString(inputStream, Constants.DEFAULT_ENCODING);
                usingDevPay = ("devpay".equals(credentialsType));
            }

            // Use AWS credentials classes as default non-abstract implementation
            if (partialReadOnly) {
                if (usingDevPay) {
                    return new AWSDevPayCredentials(null, null, friendlyName);
                } else {
                    return new AWSCredentials(null, null, friendlyName);
                }
            }

            // Read encrypted data bytes from file.
            encryptedDataIndex = inputStream.read(encryptedKeys);

            // Decrypt data.
            String keys = encryptionUtil.decryptString(encryptedKeys, 0, encryptedDataIndex);

            String[] parts = keys.split((3 <= versionNum)? V3_KEYS_DELIMITER : V2_KEYS_DELIMITER);
            int expectedParts = (usingDevPay? 4 : 2);
            if (parts.length != expectedParts) {
                throw new Exception("Number of parts (" + parts.length
                    + ") did not match the expected number of parts (" + expectedParts
                    + ") for this version (" + versionNum + ")");
            }

            // Use AWS credentials classes as default non-abstract implementation
            if (usingDevPay) {
                return new AWSDevPayCredentials(parts[0], parts[1], parts[2], parts[3], friendlyName);
            } else {
                return new AWSCredentials(parts[0], parts[1], friendlyName);
            }
        } catch (BadPaddingException bpe) {
            throw new ServiceException("Unable to decrypt credentials. Is your password correct?", bpe);
        } catch (Throwable t) {
            throw new ServiceException("Failed to load credentials", t);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

}
