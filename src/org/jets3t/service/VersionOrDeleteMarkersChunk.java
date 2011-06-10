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
package org.jets3t.service;

import org.jets3t.service.model.BaseVersionOrDeleteMarker;

public class VersionOrDeleteMarkersChunk {
    private String prefix = null;
    private String delimiter = null;
    private BaseVersionOrDeleteMarker[] items = null;
    private String[] commonPrefixes = null;
    private String nextKeyMarker = null;
    private String nextVersionIdMarker = null;

    public VersionOrDeleteMarkersChunk(String prefix, String delimiter,
        BaseVersionOrDeleteMarker[] items, String[] commonPrefixes,
        String nextKeyMarker, String nextVersionIdMarker)
    {
        this.prefix = prefix;
        this.delimiter = delimiter;
        this.items = items;
        this.commonPrefixes = commonPrefixes;
        this.nextKeyMarker = nextKeyMarker;
        this.nextVersionIdMarker = nextVersionIdMarker;
    }

    public BaseVersionOrDeleteMarker[] getItems() {
        return items;
    }

    public int getItemCount() {
        return items.length;
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
    public String getNextKeyMarker() {
        return nextKeyMarker;
    }

    public String getNextVersionIdMarker() {
        return nextVersionIdMarker;
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

    public boolean isListingComplete() {
        return (nextKeyMarker != null && nextVersionIdMarker != null);
    }

}
