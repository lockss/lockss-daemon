/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2009 James Murty
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


public class OriginAccessIdentity {
    private String id = null;
    private String s3CanonicalUserId = null;
    private String comment = null;
    private OriginAccessIdentityConfig config = null;

    public OriginAccessIdentity(String id, String s3CanonicalUserId,
            String comment)
    {
        this.id = id;
        this.s3CanonicalUserId = s3CanonicalUserId;
        this.comment = comment;
    }

    public OriginAccessIdentity(String id, String s3CanonicalUserId,
            OriginAccessIdentityConfig config)
    {
        this.id = id;
        this.s3CanonicalUserId = s3CanonicalUserId;
        this.config = config;
    }

    public OriginAccessIdentityConfig getConfig() {
        return this.config;
    }

    public boolean isSummary() {
        return getConfig() == null;
    }

    public String getId() {
        return id;
    }

    public String getS3CanonicalUserId() {
        return s3CanonicalUserId;
    }

    public String toString() {
        return "CloudFrontOriginAccessIdentity: id=" + id +
            ", s3CanonicalUserId=" + s3CanonicalUserId +
            (isSummary()
                ? ", comment=" + comment
                : ", config=[" + config + "]");
    }

}
