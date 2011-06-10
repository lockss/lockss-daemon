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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jets3t.service.CloudFrontService;

public class Distribution {
    private String id = null;
    private String status = null;
    private Date lastModifiedTime = null;
    private String domainName = null;
    private Map activeTrustedSigners = new HashMap();
    private Origin origin = null;
    private String cnames[] = new String[0];
    private String comment = null;
    private boolean enabled = false;
    private DistributionConfig config = null;

    public Distribution(String id, String status, Date lastModifiedDate,
        String domainName, Origin origin, String[] cnames, String comment,
        boolean enabled)
    {
        this.id = id;
        this.status = status;
        this.lastModifiedTime = lastModifiedDate;
        this.domainName = domainName;
        this.origin = origin;
        this.cnames = cnames;
        this.comment = comment;
        this.enabled = enabled;
    }

    public Distribution(String id, String status, Date lastModifiedDate,
        String domainName, Map activeTrustedSigners, DistributionConfig config)
    {
        this.id = id;
        this.status = status;
        this.lastModifiedTime = lastModifiedDate;
        this.domainName = domainName;
        this.activeTrustedSigners = activeTrustedSigners;
        this.config = config;
    }

    public boolean isSummary() {
        return getConfig() == null;
    }

    public String getComment() {
        return comment;
    }

    public String getDomainName() {
        return domainName;
    }

    public Map getActiveTrustedSigners() {
        return activeTrustedSigners;
    }

    public String getId() {
        return id;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public Origin getOrigin() {
        return origin;
    }

    public String[] getCNAMEs() {
        return cnames;
    }
    public boolean isEnabled() {
        return enabled;
    }

    public String getStatus() {
        return status;
    }

    /**
     * @return
     * true if this distribution's status is "Deployed".
     */
    public boolean isDeployed() {
        return "Deployed".equals(getStatus());
    }

    public DistributionConfig getConfig() {
        return config;
    }

    public boolean isStreamingDistribution() {
        return (this instanceof StreamingDistribution);
    }

    @Override
    public String toString() {
        return
            (isStreamingDistribution()
                ? "CloudFrontStreamingDistribution"
                : "CloudFrontDistribution")
            + ": id=" + id + ", status=" + status
            + ", domainName=" + domainName
            + ", activeTrustedSigners=" + activeTrustedSigners
            + ", lastModifiedTime=" + lastModifiedTime +
            (isSummary()
                ? ", origin=" + origin + ", comment=" + comment
                    + ", enabled=" + enabled + ", CNAMEs=" + Arrays.asList(cnames)
                : ", config=[" + config + "]");
    }

}
