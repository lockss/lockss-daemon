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

import org.jets3t.service.model.S3Object;

/**
 * Stores a "chunk" of S3Objects returned from a list command - this particular chunk may or may
 * not include all the objects available in a bucket.
 *
 * This class contains an array of objects and a the last key name returned by a prior
 * call to the method {@link S3Service#listObjectsChunked(String, String, String, long, String)}.
 *
 * @author James Murty
 *
 * @deprecated 0.8.0 use {@link StorageObjectsChunk} instead.
 */
@Deprecated
public class S3ObjectsChunk extends StorageObjectsChunk {

    public S3ObjectsChunk(String prefix, String delimiter, S3Object[] objects,
        String[] commonPrefixes, String priorLastKey)
    {
        super(prefix, delimiter, objects, commonPrefixes, priorLastKey);
    }

    @Override
    public S3Object[] getObjects() {
        return S3Object.cast(objects);
    }

}
