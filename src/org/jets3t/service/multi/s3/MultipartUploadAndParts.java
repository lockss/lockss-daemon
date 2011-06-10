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
package org.jets3t.service.multi.s3;

import java.util.ArrayList;
import java.util.List;

import org.jets3t.service.model.MultipartUpload;
import org.jets3t.service.model.S3Object;


/**
 * Packages together a MultipartUpload and a set of its component
 * S3Object parts.
 *
 * @author James Murty
 */
public class MultipartUploadAndParts {

    private MultipartUpload multipartUpload;
    private List<S3Object> partObjects;
    private Integer partNumberOffset;

    public MultipartUploadAndParts(MultipartUpload multipartUpload, List<S3Object> partObjects,
        Integer partNumberOffset)
    {
        this.multipartUpload = multipartUpload;
        this.partObjects = partObjects;
        this.partNumberOffset = partNumberOffset;
    }

    public MultipartUploadAndParts(MultipartUpload multipartUpload, List<S3Object> partObjects)
    {
        this(multipartUpload, partObjects, 1);
    }

    public MultipartUploadAndParts(MultipartUpload multipartUpload)
    {
        this(multipartUpload, new ArrayList<S3Object>(), 1);
    }

    public void addPartObject(S3Object partObject) {
        this.partObjects.add(partObject);
    }

    public MultipartUpload getMultipartUpload() {
        return multipartUpload;
    }

    public List<S3Object> getPartObjects() {
        return partObjects;
    }

    public Integer getPartNumberOffset() {
        return partNumberOffset;
    }

}
