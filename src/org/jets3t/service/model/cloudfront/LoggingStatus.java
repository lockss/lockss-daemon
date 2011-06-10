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

import org.jets3t.service.CloudFrontService;

/**
 * Represents the logging status of a CloudFront distribution.
 * <p>
 * For logging to be enabled, both the <code>bucket</code> and <code>prefix</code>
 * properties must be non-null and the named bucket must exist.
 *
 * @author James Murty
 */
public class LoggingStatus {
    private String bucket = null;
    private String prefix = null;

    public LoggingStatus() {
        this.prefix = "";
    }

    /**
     * @param bucket
     * the Amazon S3 bucket in which log files will be stored, specified as a full
     * S3 sub-domain path (e.g. 'jets3t.s3.amazonaws.com' for the 'jets3t' bucket)
     * @param prefix
     * a prefix to apply to log file names. May be an empty string, but cannot
     * be null.
     */
    public LoggingStatus(String bucket, String prefix) {
        this.bucket = bucket;
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix
     * a prefix to apply to log file names. May be an empty string, but cannot
     * be null.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getBucket() {
        return bucket;
    }

    /**
     * @return
     * the logging bucket name, without the suffix ".s3.amazonaws.com".
     */
    public String getShortBucketName() {
        if (bucket.endsWith(CloudFrontService.DEFAULT_BUCKET_SUFFIX)) {
            return bucket.substring(0, bucket.length() - CloudFrontService.DEFAULT_BUCKET_SUFFIX.length());
        } else {
            return bucket;
        }
    }

    /**
     * @param bucket
     * the Amazon S3 bucket in which log files will be stored, specified as a full
     * S3 sub-domain path (e.g. 'jets3t.s3.amazonaws.com' for the 'jets3t' bucket)
     */
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

}
