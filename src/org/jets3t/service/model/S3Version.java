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
package org.jets3t.service.model;

import java.util.Date;

/**
 * An S3 version.
 *
 * @author James Murty
 */
public class S3Version extends BaseVersionOrDeleteMarker {
    private String etag = null;
    private long size = 0;
    private String storageClass = null;

    public S3Version(String key, String versionId, boolean isLatest,
        Date lastModified, S3Owner owner, String etag, long size, String storageClass)
    {
        this(key, versionId, isLatest, lastModified, owner);
        this.etag = etag;
        this.size = size;
        this.storageClass = storageClass;
    }

    public S3Version(String key, String versionId, boolean isLatest,
        Date lastModified, S3Owner owner)
    {
        super(key, versionId, isLatest, lastModified, owner);
    }

    public S3Version(String key, String versionId)
    {
        super(key, versionId, false, null, null);
    }

    public boolean isDeleteMarker() {
        return false;
    }

    public String getEtag() {
        return etag;
    }

    public long getSize() {
        return size;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public String toString() {
        return "S3Version [key=" + getKey() + ", versionId=" + getVersionId()
            + ", isLatest=" + isLatest() + ", lastModified=" + getLastModified()
            + ", owner=" + getOwner() + ", etag=" + getEtag() + ", size=" + getSize()
            + ", storageClass=" + getStorageClass()
            + "]";
    }

}
