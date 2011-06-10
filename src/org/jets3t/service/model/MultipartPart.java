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
package org.jets3t.service.model;

import java.util.Comparator;
import java.util.Date;


/**
 * Represents a Part of a MultipartUpload operation.
 *
 * @author James Murty
 */
public class MultipartPart {
    private Integer partNumber;
    private Date lastModified;
    private String etag;
    private Long size;

    public MultipartPart(Integer partNumber, Date lastModified, String etag, Long size)
    {
        if (partNumber == null) {
            throw new IllegalArgumentException("Null part number not allowed.");
        }
        if (lastModified == null) {
            throw new IllegalArgumentException("Null last modified not allowed.");
        }
        if (etag == null) {
            throw new IllegalArgumentException("Null etag not allowed.");
        }
        if (size == null) {
            throw new IllegalArgumentException("Null size not allowed.");
        }
        this.partNumber = partNumber;
        this.lastModified = lastModified;
        this.size = size;
        // Strip quote characters from etag value
        this.etag = etag.replaceAll("\"", "");
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof MultipartPart) {
            MultipartPart p = (MultipartPart) other;
            return this.partNumber.equals(p.partNumber)
                && this.lastModified.equals(p.lastModified)
                && this.size.equals(p.size)
                && this.etag.equals(p.etag);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " ["
            + "partNumber=" + getPartNumber()
            + ", lastModified=" + getLastModified()
            + ", etag=" + getEtag()
            + ", size=" + getSize()
            + "]";
    }

    public String getEtag() {
        return etag;
    }

    public Long getSize() {
        return size;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public static class PartNumberComparator implements Comparator<MultipartPart> {
        public int compare(MultipartPart o1, MultipartPart o2) {
            if (o1 == o2) {
                return 0;
            } else {
                return o1.getPartNumber().compareTo(o2.getPartNumber());
            }
        }
    }

}
