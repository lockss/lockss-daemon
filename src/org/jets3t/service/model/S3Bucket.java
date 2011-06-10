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

/**
 * Represents an S3 bucket.
 *
 * @author James Murty
 */
public class S3Bucket extends StorageBucket {
    public static final String LOCATION_US = null;
    public static final String LOCATION_US_STANDARD = null;
    public static final String LOCATION_US_WEST = "us-west-1";
    public static final String LOCATION_EUROPE = "EU";
    public static final String LOCATION_ASIA_PACIFIC_SOUTHEAST = "ap-southeast-1";
    /**
     * Alias of {@link #LOCATION_ASIA_PACIFIC_SOUTHEAST}
     */
    public static final String LOCATION_ASIA_PACIFIC = LOCATION_ASIA_PACIFIC_SOUTHEAST;
    /**
     * Alias of {@link #LOCATION_ASIA_PACIFIC_SOUTHEAST}
     */
    public static final String LOCATION_ASIA_PACIFIC_SINGAPORE = LOCATION_ASIA_PACIFIC_SOUTHEAST;
    public static final String LOCATION_ASIA_PACIFIC_NORTHEAST = "ap-northeast-1";
    /**
     * Alias of {@link #LOCATION_ASIA_PACIFIC_NORTHEAST}
     */
    public static final String LOCATION_ASIA_PACIFIC_TOKYO = LOCATION_ASIA_PACIFIC_NORTHEAST;


    private String location = LOCATION_US;
    private boolean isLocationKnown = false;
    private boolean requesterPays = false;
    private boolean isRequesterPaysKnown = false;

    /**
     * Create a bucket without any name or location specified
     */
    public S3Bucket() {
        super();
    }

    /**
     * Create a bucket with a name. All buckets in S3 share a single namespace,
     * so choose a unique name for your bucket.
     * @param name the name for the bucket
     */
    public S3Bucket(String name) {
        super(name);
    }

    /**
     * Create a bucket with a name and a location. All buckets in S3 share a single namespace,
     * so choose a unique name for your bucket.
     * @param name the name for the bucket
     * @param location A string representing the location. Legal values include
     * {@link #LOCATION_US} and null (which are equivalent), or
     * {@link #LOCATION_EUROPE}.
     */
    public S3Bucket(String name, String location) {
        this(name);
        this.location = location;
        this.isLocationKnown = true;
    }

    @Override
    public String toString() {
        return "S3Bucket [name=" + getName() +
            ",location=" + getLocation() +
            ",creationDate=" + getCreationDate() + ",owner=" + getOwner()
            + "] Metadata=" + getMetadataMap();
    }

    /**
     * Set's the bucket's location. This method should only be used internally by
     * JetS3t methods that retrieve information directly from S3.
     *
     * @param location
     * A string representing the location. Legal values include
     * {@link #LOCATION_US} and null (which are equivalent), or
     * {@link #LOCATION_EUROPE}.
     */
    public void setLocation(String location) {
        this.location = location;
        this.isLocationKnown = true;
    }

    /**
     * @return
     * true if this object knows the bucket's location, false otherwise.
     */
    public boolean isLocationKnown() {
        return this.isLocationKnown;
    }

    /**
     * @return
     * the bucket's location represented as a string. "EU"
     * denotes a bucket located in Europe, while null denotes a bucket located
     * in the US.
     */
    public String getLocation() {
        return location;
    }


    /**
     * Set's the bucket's Requester Pays Configuration setting.
     * This method should only be used internally by JetS3t methods that
     * retrieve information directly from S3.
     *
     * @param requesterPays
     * true if the bucket is configured for Requester Pays, false if it is
     * configured for Owner pays.
     */
    public void setRequesterPays(boolean requesterPays) {
        this.requesterPays = requesterPays;
        this.isRequesterPaysKnown = true;
    }

    /**
     * @return
     * true if this bucket object knows its Requester Pays status, false otherwise.
     */
    public boolean isRequesterPaysKnown() {
        return this.isRequesterPaysKnown;
    }

    /**
     * Return the Requester Pays status of this bucket, if it is known.
     * <p>
     * WARNING:
     * Before you use this method, always check with the {@link #isRequesterPaysKnown}
     * method to ensure that the Requester Pays status has been set, otherwise
     * the result of this method is meaningless.
     *
     * @return
     * true if the bucket is configured for Requester Pays, false if it is
     * configured for Owner pays or the Request Pays configuration status is
     * unknown.
     */
    public boolean isRequesterPays() {
        return requesterPays;
    }

    public static S3Bucket[] cast(StorageBucket[] buckets) {
        List<S3Bucket> results = new ArrayList<S3Bucket>();
        for (StorageBucket bucket: buckets) {
            results.add((S3Bucket)bucket);
        }
        return results.toArray(new S3Bucket[results.size()]);
    }

}
