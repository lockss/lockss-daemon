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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.jets3t.service.Constants;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.utils.Mimetypes;

/**
 * An S3 object.
 *
 * @author James Murty
 */
public class S3Object extends StorageObject implements Cloneable {
    public static final String STORAGE_CLASS_STANDARD = "STANDARD";
    public static final String STORAGE_CLASS_REDUCED_REDUNDANCY = "REDUCED_REDUNDANCY";

    /*
     * S3-specific metadata/header names.
     */
    public static final String S3_VERSION_ID = "version-id";

    /**
     * Create an object representing a file. The object is initialised with the file's name
     * as its key, the file's content as its data, a content type based on the file's extension
     * (see {@link Mimetypes}), and a content length matching the file's size.
     * The file's MD5 hash value is also calculated and provided to S3, so the service
     * can verify that no data are corrupted in transit.
     * <p>
     * <b>NOTE:</b> The automatic calculation of a file's MD5 hash digest as performed by
     * this constructor could take some time for large files, or for many small ones.
     *
     * @param bucket
     * the bucket the object belongs to, or will be placed in.
     * @param file
     * the file the object will represent. This file must exist and be readable.
     *
     * @throws IOException when an i/o error occurred reading the file
     * @throws NoSuchAlgorithmException when this JRE doesn't support the MD5 hash algorithm
     */
    public S3Object(S3Bucket bucket, File file) throws NoSuchAlgorithmException, IOException {
        super(file);
        if (bucket != null) {
            this.bucketName = bucket.getName();
        }
    }

    /**
     * Create an object representing a file. The object is initialised with the file's name
     * as its key, the file's content as its data, a content type based on the file's extension
     * (see {@link Mimetypes}), and a content length matching the file's size.
     * The file's MD5 hash value is also calculated and provided to S3, so the service
     * can verify that no data are corrupted in transit.
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
    public S3Object(File file) throws NoSuchAlgorithmException, IOException {
        super(file);
    }

    /**
     * Create an object representing text data. The object is initialized with the given
     * key, the given string as its data content (encoded as UTF-8), a content type of
     * <code>text/plain; charset=utf-8</code>, and a content length matching the
     * string's length.
     * The given string's MD5 hash value is also calculated and provided to S3, so the service
     * can verify that no data are corrupted in transit.
     * <p>
     * <b>NOTE:</b> The automatic calculation of the MD5 hash digest as performed by
     * this constructor could take some time for large strings, or for many small ones.
     *
     * @param bucket
     * the bucket the object belongs to, or will be placed in.
     * @param key
     * the key name for the object.
     * @param dataString
     * the text data the object will contain. Text data will be encoded as UTF-8.
     * This string cannot be null.
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException when this JRE doesn't support the MD5 hash algorithm
     */
    public S3Object(S3Bucket bucket, String key, String dataString) throws NoSuchAlgorithmException, IOException
    {
        super(key, dataString);
        if (bucket != null) {
            this.bucketName = bucket.getName();
        }
    }

    /**
     * Create an object representing text data. The object is initialized with the given
     * key, the given string as its data content (encoded as UTF-8), a content type of
     * <code>text/plain; charset=utf-8</code>, and a content length matching the
     * string's length.
     * The given string's MD5 hash value is also calculated and provided to S3, so the service
     * can verify that no data are corrupted in transit.
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
    public S3Object(String key, String dataString) throws NoSuchAlgorithmException, IOException
    {
        super(key, dataString);
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
    public S3Object(String key, byte[] data) throws NoSuchAlgorithmException, IOException
    {
        super(key, data);
    }

    /**
     * Create an object without any associated data, and no associated bucket.
     *
     * @param key
     * the key name for the object.
     */
    public S3Object(String key) {
        super(key);
    }

    /**
     * Create an object without any associated data.
     *
     * @param bucket
     * the bucket the object belongs to, or will be placed in.
     * @param key
     * the key name for the object.
     */
    public S3Object(S3Bucket bucket, String key) {
        super(key);
        if (bucket != null) {
            this.bucketName = bucket.getName();
        }
    }

    /**
     * Create an object without any associated information whatsoever.
     */
    public S3Object() {
        super();
    }

    @Override
    public String toString() {
        return "S3Object [key=" + getKey() + ", bucket=" + (bucketName == null ? "<Unknown>" : bucketName)
            + ", lastModified=" + getLastModifiedDate() + ", dataInputStream=" + dataInputStream
            + (getStorageClass() != null ? ", storageClass=" + getStorageClass() : "")
            + ", Metadata=" + getMetadataMap() + "]";
    }

    /**
     * Set the object's ACL. If a pre-canned REST ACL is used, the plain-text representation
     * of the canned ACL is also added as a metadata header <code>x-amz-acl</code>.
     *
     * @param acl
     */
    @Override
    public void setAcl(AccessControlList acl) {
        this.acl = acl;

        if (acl != null) {
            String restHeaderAclValue = acl.getValueForRESTHeaderACL();
            if (restHeaderAclValue != null) {
                addMetadata(Constants.REST_HEADER_PREFIX + "acl", restHeaderAclValue);
            } else {
                // Non-REST canned ACLs are not added as headers...
            }
        }
    }

    public String getVersionId() {
        return (String) getMetadata(S3_VERSION_ID);
    }

    @Override
    public Object clone() {
        S3Object clone = new S3Object(getKey());
        clone.bucketName = bucketName;
        clone.dataInputStream = dataInputStream;
        clone.acl = acl;
        clone.isMetadataComplete = isMetadataComplete;
        clone.dataInputFile = dataInputFile;
        clone.storageClass = storageClass;
        clone.setOwner(this.getOwner());
        clone.addAllMetadata(getMetadataMap());
        return clone;
    }

    public static S3Object[] cast(StorageObject[] objects) {
        List<S3Object> results = new ArrayList<S3Object>();
        for (StorageObject object: objects) {
            results.add((S3Object)object);
        }
        return results.toArray(new S3Object[results.size()]);
    }

}
