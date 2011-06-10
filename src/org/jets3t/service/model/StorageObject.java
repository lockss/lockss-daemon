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
package org.jets3t.service.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.io.RepeatableFileInputStream;
import org.jets3t.service.utils.Mimetypes;
import org.jets3t.service.utils.ServiceUtils;

/**
 * A generic storage object.
 *
 * @author James Murty
 */
public class StorageObject extends BaseStorageItem implements Cloneable {

    private static final Log log = LogFactory.getLog(StorageObject.class);

    /*
     * Metadata names used by JetS3t.
     */
    public static final String METADATA_HEADER_HASH_MD5 = "md5-hash";
    public static final String METADATA_HEADER_ORIGINAL_HASH_MD5 = "original-md5-hash";

    protected AccessControlList acl = null;
    protected transient InputStream dataInputStream = null;
    protected boolean isMetadataComplete = false;
    protected String bucketName = null;
    protected String storageClass = null;

    /**
     * Store references to files when the object's data comes from a file, to allow for lazy
     * opening of the file's input streams.
     */
    protected File dataInputFile = null;

    /**
     * Create an object representing a file. The object is initialised with the file's name
     * as its key, the file's content as its data, a content type based on the file's extension
     * (see {@link Mimetypes}), and a content length matching the file's size.
     * The file's MD5 hash value is also calculated and provided to the destination service,
     * so the service can verify that no data are corrupted in transit.
     * <p>
     * <b>NOTE:</b> The automatic calculation of a file's MD5 hash digest as performed by
     * this constructor could take some time for large files, or for many small ones.
     *
     * @param file
     * the file the object will represent. This file must exist and be readable.
     *
     * @throws IOException when an i/o error occurred reading the file
     * @throws NoSuchAlgorithmException when this JRE doesn't support the MD5 hash algorithm
     */
    public StorageObject(File file) throws NoSuchAlgorithmException, IOException {
        this(file.getName());
        setContentLength(file.length());
        setContentType(Mimetypes.getInstance().getMimetype(file));
        if (!file.exists()) {
            throw new FileNotFoundException("Cannot read from file: " + file.getAbsolutePath());
        }
        setDataInputFile(file);
        setMd5Hash(ServiceUtils.computeMD5Hash(new FileInputStream(file)));
    }

    /**
     * Create an object representing text data. The object is initialized with the given
     * key, the given string as its data content (encoded as UTF-8), a content type of
     * <code>text/plain; charset=utf-8</code>, and a content length matching the
     * string's length.
     * The given string's MD5 hash value is also calculated and provided to the target
     * service, so the service can verify that no data are corrupted in transit.
     * <p>
     * <b>NOTE:</b> The automatic calculation of the MD5 hash digest as performed by
     * this constructor could take some time for large strings, or for many small ones.
     *
     * @param key
     * the key name for the object.
     * @param dataString
     * the text data the object will contain. Text data will be encoded as UTF-8.
     * This string cannot be null.
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException when this JRE doesn't support the MD5 hash algorithm
     */
    public StorageObject(String key, String dataString) throws NoSuchAlgorithmException, IOException
    {
        this(key);
        ByteArrayInputStream bais = new ByteArrayInputStream(
            dataString.getBytes(Constants.DEFAULT_ENCODING));
        setDataInputStream(bais);
        setContentLength(bais.available());
        setContentType("text/plain; charset=utf-8");
        setMd5Hash(ServiceUtils.computeMD5Hash(dataString.getBytes(Constants.DEFAULT_ENCODING)));
    }

    /**
     * Create an object representing binary data. The object is initialized with the given
     * key, the bytes as its data content, a content type of
     * <code>application/octet-stream</code>, and a content length matching the
     * byte array's length.
     * The MD5 hash value of the byte data is also calculated and provided to the target
     * service, so the service can verify that no data are corrupted in transit.
     *
     * @param key
     * the key name for the object.
     * @param data
     * the byte data the object will contain, cannot be null.
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException when this JRE doesn't support the MD5 hash algorithm
     */
    public StorageObject(String key, byte[] data) throws NoSuchAlgorithmException, IOException
    {
        this(key);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        setDataInputStream(bais);
        setContentLength(bais.available());
        setContentType(Mimetypes.MIMETYPE_OCTET_STREAM);
        setMd5Hash(ServiceUtils.computeMD5Hash(data));
    }

    /**
     * Create an object without any associated data.
     *
     * @param key
     * the key name for the object.
     */
    public StorageObject(String key) {
        super(key);
    }

    @Override
    public Object clone() {
        StorageObject clone = new StorageObject(getKey());
        clone.dataInputStream = dataInputStream;
        clone.acl = acl;
        clone.isMetadataComplete = isMetadataComplete;
        clone.dataInputFile = dataInputFile;
        clone.setOwner(this.getOwner());
        clone.addAllMetadata(getMetadataMap());
        return clone;
    }

    @Override
    public String toString() {
        return "StorageObject [key=" + getKey()
            + ", lastModified=" + getLastModifiedDate() + ", dataInputStream=" + dataInputStream
            + ", Metadata=" + getMetadataMap() + "]";
    }

    /**
     * Create an object without any associated information whatsoever.
     */
    public StorageObject() {
        super();
    }

    /**
     * @return
     * the name of the bucket this object belongs to or will be placed into, or null if none is set.
     */
    public String getBucketName() {
        return bucketName;
    }


    /**
     * Set the name of the bucket this object belongs to or will be placed into.
     * @param bucketName the name for the bucket.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Returns an input stream containing this object's data, or null if there is
     * no data associated with the object.
     * <p>
     * If you are downloading data from a service, you should consider verifying the
     * integrity of the data you read from this stream using one of the
     * {@link #verifyData(InputStream)} methods.
     *
     * @return
     * input stream containing the object's service-side data, or null if no data.
     *
     * @throws ServiceException
     */
    public InputStream getDataInputStream() throws ServiceException {
        if (dataInputStream == null && dataInputFile != null) {
            try {
                // Use a repeatable file data input stream, so transmissions can be retried if necessary.
                dataInputStream = new RepeatableFileInputStream(dataInputFile);
            } catch (FileNotFoundException e) {
                throw new ServiceException("Cannot open file input stream", e);
            }
        }
        return dataInputStream;
    }

    /**
     * Sets an input stream containing the data content to associate with this object.
     * <p>
     * <b>Note</b>: If the data content comes from a file, use the alternate method
     * {@link #setDataInputFile(File)} which allows objects to lazily open files and avoid any
     * Operating System limits on the number of files that may be opened simultaneously.
     * <p>
     * <b>Note 2</b>: This method does not calculate an MD5 hash of the input data,
     * which means the target service will not be able to recognize if data are corrupted in transit.
     * To allow the target service to verify data you upload, you should set the MD5 hash value of
     * your data using {@link #setMd5Hash(byte[])}.
     * <p>
     * This method will set the object's file data reference to null.
     *
     * @param dataInputStream
     * an input stream containing the object's data.
     */
    public void setDataInputStream(InputStream dataInputStream) {
        this.dataInputFile = null;
        this.dataInputStream = dataInputStream;
    }

    /**
     * Sets the file containing the data content to associate with this object. This file will
     * be automatically opened as an input stream only when absolutely necessary, that is when
     * {@link #getDataInputStream()} is called.
     * <p>
     * <b>Note 2</b>: This method does not calculate an MD5 hash of the input data,
     * which means the target service will not be able to recognize if data are corrupted in transit.
     * To allow the target service to verify data you upload, you should set the MD5 hash value of
     * your data using {@link #setMd5Hash(byte[])}.
     * <p>
     * This method will set the object's input stream data reference to null.
     *
     * @param dataInputFile
     * a file containing the object's data.
     */
    public void setDataInputFile(File dataInputFile) {
        this.dataInputStream = null;
        this.dataInputFile = dataInputFile;
    }

    /**
     * @return
     * Return the file that contains this object's data, if such a file has been
     * provided. Null otherwise.
     */
    public File getDataInputFile() {
        return this.dataInputFile;
    }


    /**
     * Closes the object's data input stream if it exists.
     *
     * @throws IOException
     */
    public void closeDataInputStream() throws IOException {
        if (this.dataInputStream != null) {
            this.dataInputStream.close();
            this.dataInputStream = null;
        }
    }

    /**
     * @return
     * the ETag value of the object as returned by the service when an object is created. The ETag
     * value does not include quote (") characters. This value will be null if the object's ETag value
     * is not known, such as when an object has not yet been uploaded to the service.
     */
    public String getETag() {
        String etag = (String) getMetadata(METADATA_HEADER_ETAG);
        if (etag != null) {
            if (etag.startsWith("\"") && etag.endsWith("\"")) {
                return etag.substring(1, etag.length() -1);
            }
        }
        return etag;
    }

    /**
     * Set the ETag value of the object based on information returned from the service.
     * This method should only by used by code that reads service responses.
     *
     * @param etag
     * the ETag value as provided by the service.
     */
    public void setETag(String etag) {
        addMetadata(METADATA_HEADER_ETAG, etag);
    }

    /**
     * @return
     * the hex-encoded MD5 hash of an object's data contents as stored in the JEtS3t-specific
     * metadata item <code>md5-hash</code>, or null if the hash value is not available.
     */
    public String getMd5HashAsHex() {
        return (String) getMetadata(METADATA_HEADER_HASH_MD5);
    }

    /**
     * @return
     * the Base64-encoded MD5 hash of an object's data contents as stored in the metadata
     * item <code>Content-MD5</code>, or as derived from an <code>ETag</code> or
     * <code>md5-hash</code> hex-encoded version of the hash. Returns null if the hash value is not
     * available.
     */
    public String getMd5HashAsBase64() {
        String md5HashBase64 = (String) getMetadata(METADATA_HEADER_CONTENT_MD5);
        if (md5HashBase64 == null) {
            // Try converting the object's ETag (a hex-encoded md5 hash).
            final String eTag = getETag();
            if (eTag != null  &&  ServiceUtils.isEtagAlsoAnMD5Hash(eTag)) {
                return ServiceUtils.toBase64(ServiceUtils.fromHex(eTag));
            }
            // Try converting the object's md5-hash (another hex-encoded md5 hash).
            if (getMd5HashAsHex() != null) {
                return ServiceUtils.toBase64(ServiceUtils.fromHex(getMd5HashAsHex()));
            }
        }
        return md5HashBase64;
    }

    /**
     * Set the MD5 hash value of this object's data.
     * The hash value is stored as metadata under <code>Content-MD5</code> (Base64-encoded)
     * and the JetS3t-specific <code>md5-hash</code> (Hex-encoded).
     *
     * @param md5Hash
     * the MD5 hash value of the object's data.
     */
    public void setMd5Hash(byte[] md5Hash) {
        addMetadata(METADATA_HEADER_HASH_MD5, ServiceUtils.toHex(md5Hash));
        addMetadata(METADATA_HEADER_CONTENT_MD5, ServiceUtils.toBase64(md5Hash));
    }

    /**
     * @return
     * the last modified date of this object, as provided by the service. If the last modified date is not
     * available (e.g. if the object has only just been created) the object's creation date is
     * returned instead. If both last modified and creation dates are unavailable, null is returned.
     */
    public Date getLastModifiedDate() {
        Date lastModifiedDate = (Date) getMetadata(METADATA_HEADER_LAST_MODIFIED_DATE);
        if (lastModifiedDate == null) {
            // Perhaps this object has just been created, in which case we can use the Date metadata.
            lastModifiedDate = (Date) getMetadata(METADATA_HEADER_DATE);
        }
        return lastModifiedDate;
    }

    /**
     * Set this object's last modified date based on information returned from the service.
     * This method should only by used internally by code that reads the last modified date
     * from a service response; it must not be set prior to uploading data to the service.
     *
     * @param lastModifiedDate
     */
    public void setLastModifiedDate(Date lastModifiedDate) {
        addMetadata(METADATA_HEADER_LAST_MODIFIED_DATE, lastModifiedDate);
    }

    /**
     * @return
     * the content length, or size, of this object's data, or 0 if it is unknown.
     */
    public long getContentLength() {
        Object contentLength = getMetadata(METADATA_HEADER_CONTENT_LENGTH);
        if (contentLength == null) {
            return 0;
        } else {
            return Long.parseLong(contentLength.toString());
        }
    }

    /**
     * Set this object's content length. The content length is set internally by JetS3t for
     * objects that are retrieved from a service. For objects that are uploaded, JetS3t
     * automatically calculates the content length if the data is provided to the String- or
     * File-based object constructor. If you manually provide data to this object via the
     * {@link #setDataInputStream(InputStream)} or {@link #setDataInputFile(File)} methods,
     * you must also set the content length value.
     * @param size
     */
    public void setContentLength(long size) {
        addMetadata(METADATA_HEADER_CONTENT_LENGTH, String.valueOf(size));
    }

    /**
     * @return
     * the content type of the object
     */
    public String getContentType() {
        return (String) getMetadata(METADATA_HEADER_CONTENT_TYPE);
    }

    /**
     * Set the content type of the object. JetS3t can help you determine the
     * content type when the associated data is a File (see {@link Mimetypes}).
     * You should set the content type for associated String or InputStream data.
     *
     * @param contentType
     */
    public void setContentType(String contentType) {
        addMetadata(METADATA_HEADER_CONTENT_TYPE, contentType);
    }

    /**
     * @return
     * the content language of this object, or null if it is unknown.
     */
    public String getContentLanguage() {
        return (String) getMetadata(METADATA_HEADER_CONTENT_LANGUAGE);
    }

    /**
     * Set the content language of the object.
     * @param contentLanguage
     */
    public void setContentLanguage(String contentLanguage) {
        addMetadata(METADATA_HEADER_CONTENT_LANGUAGE, contentLanguage);
    }

    /**
     * @return
     * the content disposition of this object, or null if it is unknown.
     */
    public String getContentDisposition() {
        return (String) getMetadata(METADATA_HEADER_CONTENT_DISPOSITION);
    }

    /**
     * Set the content disposition of the object.
     * @param contentDisposition
     */
    public void setContentDisposition(String contentDisposition) {
        addMetadata(METADATA_HEADER_CONTENT_DISPOSITION, contentDisposition);
    }

    /**
     * @return
     * the content encoding of this object, or null if it is unknown.
     */
    public String getContentEncoding() {
        return (String) getMetadata(METADATA_HEADER_CONTENT_ENCODING);
    }

    /**
     * Set the content encoding of this object.
     * @param contentEncoding
     */
    public void setContentEncoding(String contentEncoding) {
        addMetadata(METADATA_HEADER_CONTENT_ENCODING, contentEncoding);
    }

    /**
     * @return
     * the key of this object.
     */
    public String getKey() {
        return super.getName();
    }

    /**
     * Set the key of this object.
     * @param key the key for this object.
     */
    public void setKey(String key) {
        super.setName(key);
    }

    /**
     * @return
     * the object's ACL, or null if it is unknown.
     */
    public AccessControlList getAcl() {
        return acl;
    }

    /**
     * Set the object's ACL.
     *
     * @param acl
     */
    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    /**
     * @return
     * the storage class of the object.
     */
    public String getStorageClass() {
        return this.storageClass;
    }

    /**
     * Set the storage class for this object.
     *
     * @param storageClass
     */
    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    /**
     * @return
     * true if the object's metadata are considered complete, such as when the object's metadata
     * has been retrieved from the service by a HEAD request. If this value is not true, the metadata
     * information in this object should not be considered authoritative.
     */
    public boolean isMetadataComplete() {
        return isMetadataComplete;
    }

    /**
     * Object metadata are only complete when it is populated with all values following
     * a HEAD or GET request.
     * This method should only by used by code that reads service responses.
     *
     * @param isMetadataComplete
     */
    public void setMetadataComplete(boolean isMetadataComplete) {
        this.isMetadataComplete = isMetadataComplete;
    }

    /**
     * Add metadata information to the object. If date metadata items (as recognized by name)
     * are added and the value is not a date the value is parsed as an RFC 822 or
     * ISO 8601 string.
     * @param name
     * @param value
     */
    @Override
    public void addMetadata(String name, String value) {
        if (METADATA_HEADER_LAST_MODIFIED_DATE.equals(name)
            || METADATA_HEADER_DATE.equals(name))
        {
            try {
                Date parsedDate = null;
                // We shouldn't get ISO 8601 dates here but let's be paranoid...
                if (value.toString().indexOf("-") >= 0) {
                    parsedDate = ServiceUtils.parseIso8601Date(value);
                } else {
                    parsedDate = ServiceUtils.parseRfc822Date(value);
                }
                super.addMetadata(name, parsedDate);
                return;
            } catch (ParseException e) {
                if (log.isErrorEnabled()) {
                    log.error("Unable to parse value we expect to be a valid date: "
                        + name + "=" + value, e);
                }
            }
        }

        super.addMetadata(name, value);
    }

    /**
     * Add all the metadata information to the object from the provided map.
     *
     * @param metadata
     */
    @Override
    public void addAllMetadata(Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry: metadata.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                addMetadata(entry.getKey().toString(), (String) value);
            } else if (value instanceof Date) {
                addMetadata(entry.getKey().toString(), (Date) value);
            } else if (value instanceof S3Owner) {
                addMetadata(entry.getKey().toString(), (S3Owner) value);
            }
        }
    }

    /**
     * Returns only those object metadata items that can be modified in
     * a service. This list excludes those that are set by the the service, and
     * those that are specific to a particular HTTP request/response
     * session (such as request identifiers).
     *
     * @return
     * the limited set of metadata items that users can control.
     */
    public Map<String, Object> getModifiableMetadata() {
        Map<String, Object> objectMetadata = new HashMap<String, Object>(getMetadataMap());
        objectMetadata.remove(METADATA_HEADER_CONTENT_LENGTH);
        objectMetadata.remove(METADATA_HEADER_DATE);
        objectMetadata.remove(METADATA_HEADER_ETAG);
        objectMetadata.remove(METADATA_HEADER_LAST_MODIFIED_DATE);
        objectMetadata.remove("id-2"); // HTTP request-specific information
        objectMetadata.remove("request-id"); // HTTP request-specific information
        return objectMetadata;
    }

    @SuppressWarnings("deprecation")
    public boolean isDirectoryPlaceholder() {
        // Recognize "standard" directory place-holder indications used by
        // Amazon's AWS Console and Panic's Transmit.
        if (this.getKey().endsWith("/")
            && this.getContentLength() == 0)
        {
            return true;
        }
        // Recognize s3sync.rb directory placeholders by MD5/ETag value.
        if ("d66759af42f282e1ba19144df2d405d0".equals(this.getETag()))
        {
            return true;
        }
        // Recognize place-holder objects created by the Google Storage console
        // or S3 Organizer Firefox extension.
        if (this.getKey().endsWith("_$folder$")
            && this.getContentLength() == 0)
        {
            return true;
        }
        // Recognize legacy JetS3t directory place-holder objects, only gives
        // accurate results if an object's metadata is populated.
        if (this.getContentLength() == 0
            && Mimetypes.MIMETYPE_JETS3T_DIRECTORY.equals(this.getContentType()))
        {
            return true;
        }
        return false;
    }

    /**
     * Calculates the MD5 hash value of the given data object, and compares it
     * against this object's hash (as stored in the Content-MD5 header for
     * uploads, or the ETag header for downloads).
     *
     * @param downloadedFile
     * @return
     * true if the calculated MD5 hash value of the file matches this object's
     * hash value, false otherwise.
     *
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public boolean verifyData(File downloadedFile)
        throws NoSuchAlgorithmException, FileNotFoundException, IOException
    {
        return getMd5HashAsBase64().equals(
            ServiceUtils.toBase64(
                ServiceUtils.computeMD5Hash(
                    new FileInputStream(downloadedFile))));
    }

    /**
     * Calculates the MD5 hash value of the given data object, and compares it
     * against this object's hash (as stored in the Content-MD5 header for
     * uploads, or the ETag header for downloads).
     *
     * @param downloadedData
     * @return
     * true if the calculated MD5 hash value of the bytes matches this object's
     * hash value, false otherwise.
     *
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public boolean verifyData(byte[] downloadedData)
        throws NoSuchAlgorithmException, FileNotFoundException, IOException
    {
        return getMd5HashAsBase64().equals(
            ServiceUtils.toBase64(
                ServiceUtils.computeMD5Hash(downloadedData)));
    }

    /**
     * Calculates the MD5 hash value of the given data object, and compares it
     * against this object's hash (as stored in the Content-MD5 header for
     * uploads, or the ETag header for downloads).
     *
     * @param downloadedDataStream
     * the input stream of a downloaded object.
     *
     * @return
     * true if the calculated MD5 hash value of the input stream matches this
     * object's hash value, false otherwise.
     *
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public boolean verifyData(InputStream downloadedDataStream)
        throws NoSuchAlgorithmException, FileNotFoundException, IOException
    {
        return getMd5HashAsBase64().equals(
            ServiceUtils.toBase64(
                ServiceUtils.computeMD5Hash(downloadedDataStream)));
    }

}
