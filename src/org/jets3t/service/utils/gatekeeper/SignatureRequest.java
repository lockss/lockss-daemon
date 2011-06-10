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
package org.jets3t.service.utils.gatekeeper;

import java.util.HashMap;
import java.util.Map;

import org.jets3t.service.model.S3Object;

/**
 * Represents a signature request - that is, a request that a Gatekeeper allow a specific operation
 * (signature type) on a specific object in S3. The operations that may be requested are: get, head, put.
 *
 * @author James Murty
 *
 */
public class SignatureRequest {
    public static final String SIGNATURE_TYPE_GET = "get";
    public static final String SIGNATURE_TYPE_HEAD = "head";
    public static final String SIGNATURE_TYPE_PUT = "put";
    public static final String SIGNATURE_TYPE_DELETE = "delete";
    public static final String SIGNATURE_TYPE_ACL_LOOKUP = "acl-lookup";
    public static final String SIGNATURE_TYPE_ACL_UPDATE = "acl-update";

    private String signatureType = null;
    private String objectKey = null;
    private String bucketName = null;
    private Map objectMetadata = new HashMap();
    private String signedUrl = null;
    private String declineReason = null;

    /**
     * Constructs an empty signature request.
     */
    public SignatureRequest() {
    }

    /**
     * Constructs a signature request for an operation on a specific object key.
     *
     * @param signatureType
     * @param objectKey
     */
    public SignatureRequest(String signatureType, String objectKey) {
        setSignatureType(signatureType);
        this.objectKey = objectKey;
    }

    /**
     * @return
     * the name of the bucket in which an object is stored, may be null.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket in which an object is stored - this is not generally required.
     *
     * @param bucketName
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * @return
     * the key name of the object on which the operation will be performed.
     */
    public String getObjectKey() {
        return objectKey;
    }

    /**
     * Sets the key name of the object on which the operation will be performed.
     *
     * @param objectKey
     */
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    /**
     * @return
     * the object's metadata as included in the Gatekeeer message.
     */
    public Map getObjectMetadata() {
        return objectMetadata;
    }

    /**
     * Sets the object's metadata, that will be included in the Gatekeeer message.
     *
     * @param objectMetadata
     */
    public void setObjectMetadata(Map objectMetadata) {
        this.objectMetadata.putAll(objectMetadata);
    }

    /**
     * Adds to the object's metadata, that will be included in the Gatekeeer message.
     *
     * @param metadataName
     * @param metadataValue
     */
    public void addObjectMetadata(String metadataName, String metadataValue) {
        this.objectMetadata.put(metadataName, metadataValue);
    }

    /**
     * @return
     * the operation being requested.
     */
    public String getSignatureType() {
        return signatureType;
    }

    /**
     * Sets the signature type (operation) being requested for the object in this request.
     *
     * @param signatureType
     * the operation being requested, must match one of the <tt>SIGNATURE_TYPE_xyz</tt> constants
     * in this class.
     */
    public void setSignatureType(String signatureType) {
        if (!SIGNATURE_TYPE_GET.equals(signatureType)
            && !SIGNATURE_TYPE_HEAD.equals(signatureType)
            && !SIGNATURE_TYPE_PUT.equals(signatureType)
            && !SIGNATURE_TYPE_DELETE.equals(signatureType)
            && !SIGNATURE_TYPE_ACL_LOOKUP.equals(signatureType)
            && !SIGNATURE_TYPE_ACL_UPDATE.equals(signatureType))
        {
            throw new IllegalArgumentException("Illegal signature type: " + signatureType);
        }
        this.signatureType = signatureType;
    }

    /**
     * Approve the request by setting the signed URL for this request - performed by a
     * Gatekeeper service when a request has been allowed.
     *
     * @param signedUrl
     * a URL signed to allow the requested operation on the S3 object.
     */
    public void signRequest(String signedUrl) {
        this.signedUrl = signedUrl;
    }

    /**
     * @return
     * the signed URL for this request, if available. If this method is called before a
     * Gatekeeper service has provided a signed URL, or the the Gatekeeper has refused to provide
     * a signed URL, this method will return null.
     */
    public String getSignedUrl() {
        return this.signedUrl;
    }

    /**
     * Decline the request by setting the decline reason for this request - performed by a
     * Gatekeeper service when a request has been disallowed.
     *
     * @param reason
     * a short explanation for why the request was not allowed, such as "Unrecognised user".
     */
    public void declineRequest(String reason) {
        this.declineReason = reason;
    }

    /**
     * @return
     * the reason this request was declined.
     */
    public String getDeclineReason() {
        return this.declineReason;
    }

    /**
     * Returns true if this request has been allowed and includes a signed URL, false otherwise.
     */
    public boolean isSigned() {
        return getSignedUrl() != null;
    }

    public S3Object buildObject() {
        S3Object object = new S3Object(getObjectKey());
        object.addAllMetadata(getObjectMetadata());
        return object;
    }

}
