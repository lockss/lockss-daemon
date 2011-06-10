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
package org.jets3t.service;

import org.jets3t.service.model.StorageObject;

/**
 * Stores a "chunk" of StorageObjects returned from a list command - this particular chunk may or may
 * not include all the objects available in a bucket.
 *
 * This class contains an array of objects and a the last key name returned by a prior
 * call to the method {@link S3Service#listObjectsChunked(String, String, String, long, String)}.
 *
 * @author James Murty
 */
public class StorageObjectsChunk {
    protected String prefix = null;
    protected String delimiter = null;
    protected StorageObject[] objects = null;
    protected String[] commonPrefixes = null;
    protected String priorLastKey = null;

    public StorageObjectsChunk(String prefix, String delimiter, StorageObject[] objects,
        String[] commonPrefixes, String priorLastKey)
    {
        this.prefix = prefix;
        this.delimiter = delimiter;
        this.objects = objects;
        this.commonPrefixes = commonPrefixes;
        this.priorLastKey = priorLastKey;
    }

    /**
     * @return
     * the objects in this chunk.
     */
    public StorageObject[] getObjects() {
        return objects;
    }

    /**
     * @return
     * the common prefixes in this chunk.
     */
    public String[] getCommonPrefixes() {
        return commonPrefixes;
    }


    /**
     * @return
     * the last key returned by the previous chunk if that chunk was incomplete, null otherwise.
     */
    public String getPriorLastKey() {
        return priorLastKey;
    }

    /**
     * @return
     * the prefix applied when this object chunk was generated. If no prefix was
     * applied, this method will return null.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return
     * the delimiter applied when this object chunk was generated. If no
     * delimiter was applied, this method will return null.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * A convenience method to check whether a listing of objects is complete
     * (true) or there are more objects available (false). Just a synonym for
     * <code>{@link #getPriorLastKey()} == null</code>.
     *
     * @return
     * true if the listing is complete and there are no more unlisted
     * objects, false if follow-up requests will return more objects.
     */
    public boolean isListingComplete() {
        return (priorLastKey == null);
    }

}
