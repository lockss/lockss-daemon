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
package org.jets3t.service.model.cloudfront;

import org.jets3t.service.CloudFrontService;


public class S3Origin extends Origin {
    public static final String ORIGIN_ACCESS_IDENTITY_PREFIX =
        "origin-access-identity/cloudfront/";

    private String originAccessIdentity = null;

    /**
     * An S3 bucket origin.
     *
     * @param dnsName
     * a full S3 sub-domain path (e.g. 'jets3t.s3.amazonaws.com' for the 'jets3t' bucket)
     * @param originAccessIdentity
     * Identifier of the origin access identity that can authorize access to
     * S3 objects via a private distribution. If provided the distribution will be
     * private, if null the distribution will be be public.
     */
    public S3Origin(String dnsName, String originAccessIdentity) {
        super(dnsName);
        // Ensure origin access identity has required prefix
        if (originAccessIdentity != null
            && !originAccessIdentity.startsWith(ORIGIN_ACCESS_IDENTITY_PREFIX))
        {
            this.originAccessIdentity = ORIGIN_ACCESS_IDENTITY_PREFIX + originAccessIdentity;
        } else {
            this.originAccessIdentity = originAccessIdentity;
        }
    }

    /**
     * An S3 bucket origin.
     *
     * @param dnsName
     * a full S3 sub-domain path (e.g. 'jets3t.s3.amazonaws.com' for the 'jets3t' bucket)
     */
    public S3Origin(String dnsName) {
        this(dnsName, null);
    }

    public String getOriginAccessIdentity() {
        return this.originAccessIdentity;
    }

    /**
     * @return
     * the origin bucket's name, without the suffix ".s3.amazonaws.com"
     */
    public String getOriginAsBucketName() {
        String bucketName = getDnsName();
        if (bucketName.endsWith(CloudFrontService.DEFAULT_BUCKET_SUFFIX)) {
            return bucketName.substring(0, bucketName.length() - CloudFrontService.DEFAULT_BUCKET_SUFFIX.length());
        } else {
            return bucketName;
        }
    }

    @Override
    public String toString() {
        return "S3Origin: dnsName=" + getDnsName() +
            (getOriginAccessIdentity() != null
                ? ", originAccessIdentity=" + getOriginAccessIdentity()
                : "");
    }

}
