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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class to represent storage items that can contain metadata: both objects and buckets.
 *
 * @author James Murty
 */
public abstract class BaseStorageItem {
    /*
     * Standard HTTP metadata/header names.
     */
    public static final String METADATA_HEADER_CREATION_DATE = "Date";
    public static final String METADATA_HEADER_LAST_MODIFIED_DATE = "Last-Modified";
    public static final String METADATA_HEADER_DATE = "Date";
    public static final String METADATA_HEADER_CONTENT_MD5 = "Content-MD5";
    public static final String METADATA_HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String METADATA_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String METADATA_HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String METADATA_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String METADATA_HEADER_CONTENT_LANGUAGE = "Content-Language";
    public static final String METADATA_HEADER_ETAG = "ETag";

    /*
     * Metadata names common to S3 and Google Storage.
     */

    private String name = null;
    private StorageOwner owner = null;

    /**
     *  Map to metadata associated with this object.
     */
    private final Map<String, Object> metadata = new HashMap<String, Object>();


    protected BaseStorageItem(String name) {
        this.name = name;
    }

    protected BaseStorageItem() {
    }

    /**
     * @return
     * the name of the bucket.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the bucket.
     * @param name the name for the bucket
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return
     * an <b>immutable</b> map containing all the metadata associated with this object.
     */
    public Map<String, Object> getMetadataMap() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * @param name
     * the metadata item name.
     *
     * @return
     * the value of the metadata with the given name, or null if no such metadata item exists.
     */
    public Object getMetadata(String name) {
        return this.metadata.get(name);
    }

    /**
     * @param name
     * the metadata item name.
     *
     * @return
     * true if this object contains a metadata item with the given name, false otherwise.
     */
    public boolean containsMetadata(String name) {
        return this.metadata.keySet().contains(name);
    }

    /**
     * Adds a metadata item to the object.
     *
     * @param name
     * the metadata item name.
     * @param value
     * the metadata item value.
     */
    public void addMetadata(String name, String value) {
        this.metadata.put(name, value);
    }

    /**
     * Adds a Date metadata item to the object.
     *
     * @param name
     * the metadata item name.
     * @param value
     * the metadata item's date value.
     */
    public void addMetadata(String name, Date value) {
        this.metadata.put(name, value);
    }

    /**
     * Adds an owner metadata item to the object.
     *
     * @param name
     * the metadata item name.
     * @param value
     * the metadata item's owner value.
     */
    public void addMetadata(String name, StorageOwner value) {
        this.metadata.put(name, value);
    }

    /**
     * Adds all the items in the provided map to this object's metadata.
     *
     * @param metadata
     * metadata items to add.
     */
    public void addAllMetadata(Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
    }

    /**
     * Removes a metadata item from the object.
     *
     * @param name
     * the name of the metadata item to remove.
     */
    public void removeMetadata(String name) {
        this.metadata.remove(name);
    }

    /**
     * Removes all the metadata items associated with this object, then adds all the items
     * in the provided map. After performing this operation, the metadata list will contain
     * only those items in the provided map.
     *
     * @param metadata
     * metadata items to add.
     */
    public void replaceAllMetadata(Map<String, Object> metadata) {
        this.metadata.clear();
        addAllMetadata(metadata);
    }

    /**
     * @return
     * this object's owner, or null if the owner is not available.
     */
    public StorageOwner getOwner() {
        return this.owner;
    }

    /**
     * Set this object's owner object based on information returned from the service.
     * This method should only by used by code that reads service responses.
     *
     * @param owner
     */
    public void setOwner(StorageOwner owner) {
        this.owner = owner;
    }

}
