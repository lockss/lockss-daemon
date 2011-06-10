/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2009 James Murty
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
package org.jets3t.service.mx;

import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageObject;

public interface MxInterface {

    public abstract void registerS3ServiceMBean();

    public abstract void registerS3ServiceExceptionMBean();

    public abstract void registerS3ServiceExceptionEvent();

    public abstract void registerS3ServiceExceptionEvent(String s3ErrorCode);

    public abstract void registerStorageBucketMBeans(StorageBucket[] buckets);

    public abstract void registerStorageBucketListEvent(String bucketName);

    public abstract void registerStorageObjectMBean(String bucketName,
        StorageObject[] objects);

    public abstract void registerStorageObjectPutEvent(String bucketName, String key);

    public abstract void registerStorageObjectGetEvent(String bucketName, String key);

    public abstract void registerStorageObjectHeadEvent(String bucketName, String key);

    public abstract void registerStorageObjectDeleteEvent(String bucketName,
        String key);

    public abstract void registerStorageObjectCopyEvent(String bucketName, String key);

}
