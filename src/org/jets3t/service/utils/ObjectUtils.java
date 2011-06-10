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
package org.jets3t.service.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.io.BytesProgressWatcher;
import org.jets3t.service.io.GZipDeflatingInputStream;
import org.jets3t.service.io.ProgressMonitoredInputStream;
import org.jets3t.service.io.TempFile;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.multi.DownloadPackage;
import org.jets3t.service.security.EncryptionUtil;

/**
 * Utility class for preparing files for upload into S3, or for download from
 * S3. The methods in this class build the appropriate objects to wrap uploaded
 * files or objects in S3 that will be downloaded to a local file.
 *
 * @author James Murty
 */
public class ObjectUtils {
    private static final Log log = LogFactory.getLog(ObjectUtils.class);

    /**
     * Prepares a file for upload to a named object in S3, potentially transforming it if
     * zipping or encryption is requested.
     * <p>
     * The file will have the following metadata items added:
     * <ul>
     * <li>{@link Constants#METADATA_JETS3T_LOCAL_FILE_DATE}: The local file's last modified date
     *     in ISO 8601 format</li>
     * <li><tt>Content-Type</tt> : A content type guessed from the file's extension, or
     *     {@link Mimetypes#MIMETYPE_BINARY_OCTET_STREAM} if the file is a directory</li>
     * <li><tt>Content-Length</tt> : The size of the file</li>
     * <li><tt>MD5-Hash</tt> : An MD5 hash of the file's data</li>
     * <li>{@link StorageObject#METADATA_HEADER_ORIGINAL_HASH_MD5}: An MD5 hash of the
     *     original file's data (added if gzipping or encryption is applied)</li>
     * </ul>
     *
     * @param objectKey
     * the object key name to use in S3
     * @param dataFile
     * the file to prepare for upload.
     * @param encryptionUtil
     * if this variable is null no encryption will be applied, otherwise the provided
     * encryption utility object will be used to encrypt the file's data.
     * @param gzipFile
     * if true the file will be Gzipped.
     * @param progressWatcher
     * watcher to monitor progress of file transformation and hash generation.
     *
     * @return
     * an S3Object representing the file, or a transformed copy of the file, complete with
     * all JetS3t-specific metadata items set and ready for upload to S3.
     *
     * @throws Exception
     * exceptions could include IO failures, gzipping and encryption failures.
     */
    public static S3Object createObjectForUpload(String objectKey, File dataFile,
        EncryptionUtil encryptionUtil, boolean gzipFile, BytesProgressWatcher progressWatcher)
        throws Exception
    {
        S3Object s3Object = new S3Object(objectKey);

        // Set object explicitly to private access by default.
        s3Object.setAcl(AccessControlList.REST_CANNED_PRIVATE);

        s3Object.addMetadata(Constants.METADATA_JETS3T_LOCAL_FILE_DATE,
            ServiceUtils.formatIso8601Date(new Date(dataFile.lastModified())));

        if (dataFile.isDirectory()) {
            s3Object.setContentLength(0);
            s3Object.setContentType(Mimetypes.MIMETYPE_BINARY_OCTET_STREAM);
        } else {
            s3Object.setContentType(Mimetypes.getInstance().getMimetype(dataFile));
            File uploadFile = transformUploadFile(dataFile, s3Object, encryptionUtil,
                gzipFile, progressWatcher);
            s3Object.setContentLength(uploadFile.length());
            s3Object.setDataInputFile(uploadFile);

            // Compute the upload file's MD5 hash.
            InputStream inputStream = new BufferedInputStream(new FileInputStream(uploadFile));
            if (progressWatcher != null) {
                inputStream = new ProgressMonitoredInputStream(inputStream, progressWatcher);
            }
            s3Object.setMd5Hash(ServiceUtils.computeMD5Hash(inputStream));

            if (!uploadFile.equals(dataFile)) {
                // Compute the MD5 hash of the *original* file, if upload file has been altered
                // through encryption or gzipping.
                inputStream = new BufferedInputStream(new FileInputStream(dataFile));
                if (progressWatcher != null) {
                    inputStream = new ProgressMonitoredInputStream(inputStream, progressWatcher);
                }

                s3Object.addMetadata(
                    S3Object.METADATA_HEADER_ORIGINAL_HASH_MD5,
                    ServiceUtils.toBase64(ServiceUtils.computeMD5Hash(inputStream)));
            }
        }
        return s3Object;
    }

    /**
     * Prepares a file for upload to a named object in S3, potentially transforming it if
     * zipping or encryption is requested.
     * <p>
     * The file will have the following metadata items added:
     * <ul>
     * <li>{@link Constants#METADATA_JETS3T_LOCAL_FILE_DATE}: The local file's last modified date
     *     in ISO 8601 format</li>
     * <li><tt>Content-Type</tt> : A content type guessed from the file's extension, or
     *     {@link Mimetypes#MIMETYPE_BINARY_OCTET_STREAM} if the file is a directory</li>
     * <li><tt>Content-Length</tt> : The size of the file</li>
     * <li><tt>MD5-Hash</tt> : An MD5 hash of the file's data</li>
     * <li>{@link StorageObject#METADATA_HEADER_ORIGINAL_HASH_MD5}: An MD5 hash of the original file's
     *     data (added if gzipping or encryption is applied)</li>
     * </ul>
     *
     * @param objectKey
     * the object key name to use in S3
     * @param dataFile
     * the file to prepare for upload.
     * @param encryptionUtil
     * if this variable is null no encryption will be applied, otherwise the provided
     * encryption utility object will be used to encrypt the file's data.
     * @param gzipFile
     * if true the file will be Gzipped.
     *
     * @return
     * an S3Object representing the file, or a transformed copy of the file, complete with
     * all JetS3t-specific metadata items set and ready for upload to S3.
     *
     * @throws Exception
     * exceptions could include IO failures, gzipping and encryption failures.
     */
    public static S3Object createObjectForUpload(String objectKey, File dataFile,
        EncryptionUtil encryptionUtil, boolean gzipFile) throws Exception
    {
        return createObjectForUpload(objectKey, dataFile, encryptionUtil, gzipFile, null);
    }

    /**
     * Prepares a file prior to upload by encrypting and/or gzipping it according to the
     * options specified by the user, and returning the transformed file. If no
     * transformations are required, the original dataFile will be returned.
     *
     * @param dataFile
     * the file to prepare for upload.
     * @param s3Object
     * the object that will be created in S3 to store the file.
     * @param encryptionUtil
     * if this variable is null no encryption will be applied, otherwise the provided
     * encryption utility object will be used to encrypt the file's data.
     * @param gzipFile
     * if true the file will be Gzipped.
     * @param progressWatcher
     * watcher to monitor progress of file transformation and hash generation. Note
     * that if encryption and/or gzipping is enabled, the underlying file will be
     * read 3 times instead of once.
     *
     * @return
     * the original file if no encryption/gzipping options are set, otherwise a
     * temporary file with encrypted and/or gzipped data from the original file.
     *
     * @throws Exception
     * exceptions could include IO failures, gzipping and encryption failures.
     */
    private static File transformUploadFile(File dataFile, S3Object s3Object,
        EncryptionUtil encryptionUtil, boolean gzipFile, BytesProgressWatcher progressWatcher) throws Exception
    {
        if (!gzipFile && (encryptionUtil == null)) {
            // No file pre-processing required.
            return dataFile;
        }

        String actionText = "";

        // Create a temporary file to hold data transformed from the original file.
        final File tempUploadFile = new TempFile(File.createTempFile("JetS3t",".tmp"));
        tempUploadFile.deleteOnExit();

        // Transform data from original file, gzipping or encrypting as specified in user's options.
        OutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            inputStream = new BufferedInputStream(new FileInputStream(dataFile));
            outputStream = new BufferedOutputStream(new FileOutputStream(tempUploadFile));

            String contentEncoding = null;
            if (gzipFile) {
                inputStream = new GZipDeflatingInputStream(inputStream);
                contentEncoding = "gzip";
                s3Object.addMetadata(Constants.METADATA_JETS3T_COMPRESSED, "gzip");
                actionText += "Compressing";
            }
            if (encryptionUtil != null) {
                inputStream = encryptionUtil.encrypt(inputStream);
                contentEncoding = null;
                s3Object.setContentType(Mimetypes.MIMETYPE_OCTET_STREAM);
                s3Object.addMetadata(Constants.METADATA_JETS3T_CRYPTO_ALGORITHM,
                    encryptionUtil.getAlgorithm());
                s3Object.addMetadata(Constants.METADATA_JETS3T_CRYPTO_VERSION,
                    EncryptionUtil.DEFAULT_VERSION);
                actionText += (actionText.length() == 0? "Encrypting" : " and encrypting");
            }
            if (contentEncoding != null) {
                s3Object.addMetadata("Content-Encoding", contentEncoding);
            }

            if (log.isDebugEnabled()) {
                log.debug("Transforming upload file '" + dataFile + "' to temporary file '"
                    + tempUploadFile.getAbsolutePath() + "': " + actionText);
            }

            if (progressWatcher != null) {
                inputStream = new ProgressMonitoredInputStream(inputStream, progressWatcher);
            }

            // Write transformed data to temporary file.
            byte[] buffer = new byte[8192];
            int c = -1;
            while ((c = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, c);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }

        return tempUploadFile;
    }

    /**
     * Creates a download package representing an S3Object that will be downloaded, and the
     * target file the downloaded data will be written to.
     * <p>
     * Downloaded data may be transformed if the S3Object is encrypted or gzipped and the
     * appropriate options are set.
     *
     * @param object
     * the object
     * @param fileTarget
     * the file to which downloaded (and possibly transformed) data will be written.
     * @param automaticUnzip
     * if true, gzipped objects will be decrypted on-the-fly as they are downloaded.
     * @param automaticDecrypt
     * if true, encrypted files will be decrypted on-the-fly as they are downloaded (in which
     * case the encryptionPassword must be correct)
     * @param encryptionPassword
     * the password required to decrypt encrypted objects.
     *
     * @return
     * a download package representing an S3Object and a taret file for the object's data.
     * @throws Exception
     */
    public static DownloadPackage createPackageForDownload(StorageObject object, File fileTarget,
        boolean automaticUnzip, boolean automaticDecrypt, String encryptionPassword) throws Exception
    {
        // Recognize directory place-holder objects and ignore them
        if (object.isDirectoryPlaceholder()) {
            return null;
        }
        else {
            boolean isZipped = false;
            EncryptionUtil encryptionUtil = null;

            if (automaticUnzip &&
                ("gzip".equalsIgnoreCase(object.getContentEncoding())
                || object.containsMetadata(Constants.METADATA_JETS3T_COMPRESSED)))
            {
                // Object data is gzipped.
                isZipped = true;
            }
            if (automaticDecrypt
                && object.containsMetadata(Constants.METADATA_JETS3T_CRYPTO_ALGORITHM))
            {
                // Object is encrypted.
                if (encryptionPassword == null) {
                    throw new ServiceException(
                        "One or more objects are encrypted, and cannot be downloaded unless "
                        + " the encyption password is provided");
                }

                String algorithm = (String) object.getMetadata(
                    Constants.METADATA_JETS3T_CRYPTO_ALGORITHM);
                String version = (String) object.getMetadata(
                    Constants.METADATA_JETS3T_CRYPTO_VERSION);
                if (version == null) {
                    version = EncryptionUtil.DEFAULT_VERSION;
                }
                encryptionUtil = new EncryptionUtil(encryptionPassword, algorithm, version);
            }

            return new DownloadPackage(object, fileTarget, isZipped, encryptionUtil);
        }
    }

    /**
     * Creates a download package representing an S3Object that will be downloaded, and the
     * target file the downloaded data will be written to.
     * <p>
     * Downloaded data may be transformed if the S3Object is encrypted or gzipped and the
     * appropriate options are set.
     *
     * @param object
     * the object
     * @param fileTarget
     * the file to which downloaded (and possibly transformed) data will be written.
     * @param automaticUnzip
     * if true, gzipped objects will be decrypted on-the-fly as they are downloaded.
     * @param automaticDecrypt
     * if true, encrypted files will be decrypted on-the-fly as they are downloaded (in which
     * case the encryptionPassword must be correct)
     * @param encryptionPassword
     * the password required to decrypt encrypted objects.
     *
     * @deprecated 0.8.0 use
     * {@link #createPackageForDownload(StorageObject, File, boolean, boolean, String)} instead.
     *
     * @return
     * a download package representing an S3Object and a taret file for the object's data.
     * @throws Exception
     */
    @Deprecated
    public static org.jets3t.service.multithread.DownloadPackage createPackageForDownload(
        S3Object object, File fileTarget, boolean automaticUnzip, boolean automaticDecrypt,
        String encryptionPassword) throws Exception
    {
        // Recognize directory place-holder objects and ignore them
        if (object.isDirectoryPlaceholder()) {
            return null;
        }
        else {
            boolean isZipped = false;
            EncryptionUtil encryptionUtil = null;

            if (automaticUnzip &&
                ("gzip".equalsIgnoreCase(object.getContentEncoding())
                || object.containsMetadata(Constants.METADATA_JETS3T_COMPRESSED)))
            {
                // Object data is gzipped.
                isZipped = true;
            }
            if (automaticDecrypt
                && object.containsMetadata(Constants.METADATA_JETS3T_CRYPTO_ALGORITHM))
            {
                // Object is encrypted.
                if (encryptionPassword == null) {
                    throw new ServiceException(
                        "One or more objects are encrypted, and cannot be downloaded unless "
                        + " the encyption password is provided");
                }

                String algorithm = (String) object.getMetadata(
                    Constants.METADATA_JETS3T_CRYPTO_ALGORITHM);
                String version = (String) object.getMetadata(
                    Constants.METADATA_JETS3T_CRYPTO_VERSION);
                if (version == null) {
                    version = EncryptionUtil.DEFAULT_VERSION;
                }
                encryptionUtil = new EncryptionUtil(encryptionPassword, algorithm, version);
            }

            return new org.jets3t.service.multithread.DownloadPackage(
                object, fileTarget, isZipped, encryptionUtil);
        }
    }

    public static String convertDirPlaceholderKeyNameToDirName(String objectKey) {
        String dirPlaceholderKey = objectKey;
        if (dirPlaceholderKey.endsWith("_$folder$")) {
            int suffixPos = dirPlaceholderKey.indexOf("_$");
            dirPlaceholderKey = dirPlaceholderKey.substring(0, suffixPos);
        }
        if (!dirPlaceholderKey.endsWith(Constants.FILE_PATH_DELIM)) {
            dirPlaceholderKey = dirPlaceholderKey + Constants.FILE_PATH_DELIM;
        }
        return dirPlaceholderKey;
    }

}
