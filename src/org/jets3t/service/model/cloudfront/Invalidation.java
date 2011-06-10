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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Invalidation {
    private String id = null;
    private String status = null;
    private Date createTime = null;
    private String callerReference = null;
    private List<String> objectKeys = new ArrayList<String>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public List<String> getObjectKeys() {
        return objectKeys;
    }

    public void setObjectKeys(List<String> objectKeys) {
        this.objectKeys = objectKeys;
    }

    public String getCallerReference() {
        return callerReference;
    }

    public void setCallerReference(String callerReference) {
        this.callerReference = callerReference;
    }

    @Override
    public String toString() {
        return "Invalidation"
            + ": id=" + id + ", status=" + status
            + ", createTime=" + createTime
            + ", callerReference=" + callerReference
            + ", objectKeys=" + objectKeys;
    }

}
