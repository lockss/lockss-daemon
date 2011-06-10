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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author James Murty
 */
public abstract class BaseVersionOrDeleteMarker {
    private String key = null;
    private String versionId = null;
    private boolean isLatest = false;
    private Date lastModified = null;
    private S3Owner owner = null;

    public BaseVersionOrDeleteMarker(String key, String versionId, boolean isLatest,
        Date lastModified, S3Owner owner)
    {
        this.key = key;
        this.versionId = versionId;
        this.isLatest = isLatest;
        this.lastModified = lastModified;
        this.owner = owner;
    }

    public abstract boolean isDeleteMarker();

    public String getKey() {
        return key;
    }

    public String getVersionId() {
        return versionId;
    }

    public boolean isLatest() {
        return isLatest;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public S3Owner getOwner() {
        return owner;
    }

    public static String[] toVersionIds(BaseVersionOrDeleteMarker[] items) {
        List<String> versionsList = new ArrayList<String>();
        for (int i = 0; i < items.length; i++) {
            versionsList.add(items[i].getVersionId());
        }
        return versionsList.toArray(new String[versionsList.size()]);
    }

}
