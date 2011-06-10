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

import java.util.Date;
import java.util.Map;

public class StreamingDistribution extends Distribution {

    public StreamingDistribution(String id, String status, Date lastModifiedDate,
        String domainName, Origin origin, String[] cnames, String comment,
        boolean enabled)
    {
        super(id, status, lastModifiedDate, domainName, origin, cnames, comment, enabled);
    }

    public StreamingDistribution(String id, String status, Date lastModifiedDate,
        String domainName, Map activeTrustedSigners, DistributionConfig config)
    {
        super(id, status, lastModifiedDate, domainName, activeTrustedSigners, config);
    }

}
