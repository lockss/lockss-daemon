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
package contribs.mx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.mx.MxDelegate;
import org.jets3t.service.mx.MxInterface;

public class MxImpl implements MxInterface {
    private static final Log log = LogFactory.getLog(MxDelegate.class);

    public MxImpl() {
        log.debug("JMX instrumentation implementation started."
            + " S3BucketMx enabled? " + S3BucketMx.isEnabled
            + ", S3ObjectMx enabled? " + S3ObjectMx.isEnabled);
    }

    // MBean registrations and events are all delegated to the implementation classes.

    public void registerS3ServiceMBean() {
        S3ServiceMx.registerMBean();
    }

    public void registerS3ServiceExceptionMBean() {
        S3ServiceExceptionMx.registerMBean();
    }

    public void registerS3ServiceExceptionEvent() {
        S3ServiceExceptionMx.increment();
    }

    public void registerS3ServiceExceptionEvent(String s3ErrorCode) {
        S3ServiceExceptionMx.increment(s3ErrorCode);
    }

    public void registerStorageBucketMBeans(StorageBucket[] buckets) {
        S3BucketMx.registerMBeans(buckets);
    }

    public void registerStorageBucketListEvent(String bucketName) {
        S3BucketMx.list(bucketName);
    }

    public void registerStorageObjectMBean(String bucketName, StorageObject[] objects) {
        S3ObjectMx.registerMBeans(bucketName, objects);
    }

    public void registerStorageObjectPutEvent(String bucketName, String key) {
        S3ObjectMx.put(bucketName, key);
    }

    public void registerStorageObjectGetEvent(String bucketName, String key) {
        S3ObjectMx.get(bucketName, key);
    }

    public void registerStorageObjectHeadEvent(String bucketName, String key) {
        S3ObjectMx.head(bucketName, key);
    }

    public void registerStorageObjectDeleteEvent(String bucketName, String key) {
        S3ObjectMx.delete(bucketName, key);
    }

    public void registerStorageObjectCopyEvent(String bucketName, String key) {
        S3ObjectMx.copy(bucketName, key);
    }

}
