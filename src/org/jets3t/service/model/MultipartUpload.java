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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Represents a MultipartUpload operation.
 *
 * @author James Murty
 */
public class MultipartUpload {

    private String uploadId;
    private String bucketName;
    private String objectKey;
    private Map<String, Object> metadata = null;
    private String storageClass;
    private S3Owner initiator;
    private S3Owner owner;
    private Date initiatedDate;
    private List<MultipartPart> multipartsPartsUploaded = new ArrayList<MultipartPart>();

    public MultipartUpload(String uploadId, String bucketName, String objectKey)
    {
        this.uploadId = uploadId;
        this.objectKey = objectKey;
        this.bucketName = bucketName;
    }

    public MultipartUpload(String uploadId, String objectKey, String storageClass,
        S3Owner initiator, S3Owner owner, Date initiatedDate)
    {
        this.uploadId = uploadId;
        this.objectKey = objectKey;
        this.storageClass = storageClass;
        this.initiator = initiator;
        this.owner = owner;
        this.initiatedDate = initiatedDate;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " ["
            + "uploadId=" + getUploadId()
            + ", bucketName=" + getBucketName()
            + ", objectKey=" + getObjectKey()
            + (metadata != null ? ", metadata=" + getMetadata() : "")
            + (storageClass != null ? ", storageClass=" + getStorageClass() : "")
            + (initiator != null ? ", initiator=" + getInitiator() : "")
            + (owner != null ? ", owner=" + getOwner() : "")
            + (initiatedDate != null ? ", initiatedDate=" + getInitiatedDate() : "")
            + ", multipartsPartsUploaded=" + multipartsPartsUploaded
            + "]";
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getUploadId() {
        return uploadId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public S3Owner getOwner() {
        return owner;
    }

    public Date getInitiatedDate() {
        return initiatedDate;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void setBucketName(String name) {
        this.bucketName = name;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public S3Owner getInitiator() {
        return initiator;
    }

    public void addMultipartPartToUploadedList(MultipartPart part) {
        this.multipartsPartsUploaded.add(part);
    }

    public List<MultipartPart> getMultipartPartsUploaded() {
        return this.multipartsPartsUploaded;
    }

}
