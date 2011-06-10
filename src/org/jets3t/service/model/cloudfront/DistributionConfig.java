/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 James Murty
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

import java.util.Arrays;


public class DistributionConfig {
    private Origin origin = null;
    private String callerReference = null;
    private String[] cnames = new String[0];
    private String comment = null;
    private boolean enabled = false;
    private String etag = null;
    private LoggingStatus loggingStatus = null;
    // Private distribution settings
    private boolean trustedSignerSelf = false;
    private String[] trustedSignerAwsAccountNumbers = new String[0];
    private String[] requiredProtocols = new String[0];
    private String defaultRootObject = null;

    public DistributionConfig(Origin origin, String callerReference,
        String[] cnames, String comment, boolean enabled,
        LoggingStatus loggingStatus, boolean trustedSignerSelf,
        String[] trustedSignerAwsAccountNumbers,
        String[] requiredProtocols, String defaultRootObject)
    {
        this.origin = origin;
        this.callerReference = callerReference;
        this.cnames = cnames;
        this.comment = comment;
        this.enabled = enabled;
        this.loggingStatus = loggingStatus;
        this.trustedSignerSelf = trustedSignerSelf;
        this.trustedSignerAwsAccountNumbers = trustedSignerAwsAccountNumbers;
        this.requiredProtocols = requiredProtocols;
        this.defaultRootObject = defaultRootObject;
    }

    public DistributionConfig(Origin origin, String callerReference,
            String[] cnames, String comment, boolean enabled,
            LoggingStatus loggingStatus)
    {
        this(origin, callerReference, cnames, comment, enabled,
                loggingStatus, false, null, null, null);
    }

    public Origin getOrigin() {
        return origin;
    }

    public String getCallerReference() {
        return callerReference;
    }

    public String[] getCNAMEs() {
        return this.cnames;
    }

    public String getComment() {
        return comment;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public LoggingStatus getLoggingStatus() {
        return loggingStatus;
    }

    public boolean isLoggingEnabled() {
        return this.loggingStatus != null;
    }

    public boolean isPrivate() {
        return (this.getOrigin() instanceof S3Origin
            && ((S3Origin)this.getOrigin()).getOriginAccessIdentity() != null);
    }

    public String[] getTrustedSignerAwsAccountNumbers() {
        return this.trustedSignerAwsAccountNumbers;
    }

    public boolean isTrustedSignerSelf() {
        return this.trustedSignerSelf;
    }

    public boolean hasTrustedSignerAwsAccountNumbers() {
        return getTrustedSignerAwsAccountNumbers() != null
            && getTrustedSignerAwsAccountNumbers().length > 0;
    }

    public boolean isUrlSigningRequired() {
        return isTrustedSignerSelf() || hasTrustedSignerAwsAccountNumbers();
    }

    public boolean isStreamingDistributionConfig() {
        return (this instanceof StreamingDistributionConfig);
    }

    public void setRequiredProtocols(String[] protocols) {
        this.requiredProtocols = protocols;
    }

    public String[] getRequiredProtocols() {
        return this.requiredProtocols;
    }

    public boolean isHttpsProtocolRequired() {
        return
            this.requiredProtocols != null
            && this.requiredProtocols.length > 0
            && "https".equals(this.requiredProtocols[0]);
    }

    public void setHttpsProtocolRequired(boolean value) {
        if (value) {
            this.requiredProtocols = new String[] {"https"};
        } else {
            this.requiredProtocols = new String[0];
        }
    }

    public String getDefaultRootObject() {
        return defaultRootObject;
    }

    @Override
    public String toString() {
        return
            (isStreamingDistributionConfig()
                ? "StreamingDistributionConfig"
                : "DistributionConfig")
            + ": origin=" + origin
            + ", callerReference=" + callerReference + ", comment=" + comment
            + ", enabled=" + enabled +
            (isPrivate()
                ? ", Private:originAccessIdentity=" + ((S3Origin)getOrigin()).getOriginAccessIdentity()
                : ", Public") +
            (isUrlSigningRequired()
                    ? ", TrustedSigners:self=" + isTrustedSignerSelf()
                        + (hasTrustedSignerAwsAccountNumbers()
                            ? ":awsAccountNumbers="
                                + Arrays.asList(getTrustedSignerAwsAccountNumbers())
                            : "")
                    : "") +
            (etag != null ? ", etag=" + etag : "") +
            (!isLoggingEnabled()
                ?     ""
                :     ", LoggingStatus: bucket=" + loggingStatus.getBucket() +
                    ", prefix=" + loggingStatus.getPrefix()) +
            (getRequiredProtocols() == null || getRequiredProtocols().length == 0
                ? ""
                : ", RequiredProtocols=" + Arrays.asList(getRequiredProtocols())) +
            ", CNAMEs=" + Arrays.asList(cnames) +
                ", DefaultRootObject=" + defaultRootObject;
    }

}
