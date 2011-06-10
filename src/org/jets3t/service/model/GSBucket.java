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
package org.jets3t.service.model;

import java.util.ArrayList;
import java.util.List;

import org.jets3t.service.acl.gs.GSAccessControlList;

/**
 * Represents a bucket in the Google Storage service.
 *
 * @author James Murty
 */
public class GSBucket extends StorageBucket {
    private GSAccessControlList acl = null;

    /**
     * Create a bucket without any name or location specified
     */
    public GSBucket() {
    }

    /**
     * Create a bucket with a name. All buckets share a single namespace,
     * so choose a unique name for your bucket.
     * @param name the name for the bucket
     */
    public GSBucket(String name) {
        super(name);
    }

    @Override
    public String toString() {
        return "GSBucket [name=" + getName() + "] Metadata=" + getMetadataMap();
    }

    /**
     * @return
     * the bucket's Access Control List, or null if it is unknown.
     */
    @Override
    public GSAccessControlList getAcl() {
        return acl;
    }

    /**
     * Sets the bucket's Access Control List - this should only be used internally
     * by JetS3t methods that retrieve information directly from the service.
     *
     * @param acl
     */
    public void setAcl(GSAccessControlList acl) {
        this.acl = acl;
    }

    public static GSBucket[] cast(StorageBucket[] buckets) {
        List<GSBucket> results = new ArrayList<GSBucket>();
        for (StorageBucket bucket: buckets) {
            results.add((GSBucket)bucket);
        }
        return results.toArray(new GSBucket[results.size()]);
    }

}
