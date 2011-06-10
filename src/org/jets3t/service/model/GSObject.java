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

import org.jets3t.service.acl.gs.GSAccessControlList;
import org.jets3t.service.utils.Mimetypes;

/**
 * A Google Storage object.
 *
 * @author James Murty
 */
public class GSObject extends StorageObject implements Cloneable {

    private GSAccessControlList acl = null;

    /**
     * Create an object representing a file. The object is initialised with the file's name
     * as its key, the file's content as its data, a content type based on the file's extension
     * (see {@link Mimetypes}), and a content length matching the file's size.
     * The file's MD5 hash value is also calculated and provided to the service so it
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
    public GSObject(File file) throws NoSuchAlgorithmException, IOException {
        super(file);
    }

    /**
     * Create an object representing text data. The object is initialized with the given
     * key, the given string as its data content (encoded as UTF-8), a content type of
     * <code>text/plain; charset=utf-8</code>, and a content length matching the
     * string's length.
     * The given string's MD5 hash value is also calculated and provided to the service
     * so it can verify that no data are corrupted in transit.
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
    public GSObject(String key, String dataString) throws NoSuchAlgorithmException, IOException
    {
        super(key, dataString);
    }

    /**
     * Create an object without any associated data, and no associated bucket.
     *
     * @param key
     * the key name for the object.
     */
    public GSObject(String key) {
        super(key);
    }

    /**
     * Create an object without any associated information whatsoever.
     */
    public GSObject() {
        super();
    }

    @Override
    public String toString() {
        return "GSObject [key=" + getKey()
            + ", lastModified=" + getLastModifiedDate() + ", dataInputStream=" + dataInputStream
            + ", Metadata=" + getMetadataMap() + "]";
    }

    /**
     * @return
     * the object's ACL, or null if it is unknown.
     */
    @Override
    public GSAccessControlList getAcl() {
        return acl;
    }

    /**
     * Set the object's ACL.
     *
     * @param acl
     */
    public void setAcl(GSAccessControlList acl) {
        this.acl = acl;
    }

    @Override
    public Object clone() {
        GSObject clone = new GSObject(getKey());
        clone.dataInputStream = dataInputStream;
        clone.acl = acl;
        clone.isMetadataComplete = isMetadataComplete;
        clone.dataInputFile = dataInputFile;
        clone.setOwner(this.getOwner());
        clone.addAllMetadata(getMetadataMap());
        return clone;
    }

    public static GSObject[] cast(StorageObject[] objects) {
        List<GSObject> results = new ArrayList<GSObject>();
        for (StorageObject object: objects) {
            results.add((GSObject)object);
        }
        return results.toArray(new GSObject[results.size()]);
    }

}
