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

/**
 * Represents a completed object resulting from a MultipartUpload operation.
 *
 * @author James Murty
 */
public class MultipartCompleted {
    private String location;
    private String bucketName;
    private String objectKey;
    private String etag;
    private String versionId;

    public MultipartCompleted(String location, String bucketName, String objectKey, String etag)
    {
        this.location = location;
        this.bucketName = bucketName;
        this.etag = etag;
        this.objectKey = objectKey;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " ["
            + "location=" + getLocation()
            + ", bucketName=" + getBucketName()
            + ", objectKey=" + getObjectKey()
            + ", etag=" + getEtag()
            + (versionId != null ? ", etag=" + getEtag() : "")
            + "]";
    }

    public String getEtag() {
        return etag;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getLocation() {
        return location;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

}
