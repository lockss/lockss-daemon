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
package org.jets3t.service.multithread;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

/**
 * S3 service wrapper that performs multiple S3 requests at a time using multi-threading and an
 * underlying thread-safe {@link S3Service} implementation.
 * <p>
 * This class provides a simplified interface to the {@link S3ServiceMulti} service. It will block while
 * doing its work, return the results of an operation when it is finished, and throw an exception if
 * anything goes wrong.
 * <p>
 * For a non-blocking multi-threading service that is more powerful, but also more complicated,
 * see {@link S3ServiceMulti}.
 *
 * @deprecated 0.8.0 use {@link org.jets3t.service.multi.SimpleThreadedStorageService} instead.
 *
 * @author James Murty
 */
@Deprecated
public class S3ServiceSimpleMulti {
    private S3Service s3Service = null;

    /**
     * Construct a multi-threaded service based on an S3Service.
     *
     * @param s3Service
     *        an S3Service implementation that will be used to perform S3 requests. This implementation
     *        <b>must</b> be thread-safe.
     */
    public S3ServiceSimpleMulti(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * Utility method to check an {@link S3ServiceEventAdaptor} for the occurrence of an error, and if
     * one is present to throw it.
     *
     * @param adaptor
     * @throws S3ServiceException
     */
    protected void throwError(S3ServiceEventAdaptor adaptor) throws S3ServiceException {
        if (adaptor.wasErrorThrown()) {
            Throwable thrown = adaptor.getErrorThrown();
            if (thrown instanceof S3ServiceException) {
                throw (S3ServiceException) thrown;
            } else {
                throw new S3ServiceException(thrown);
            }
        }
    }

    /**
     * Creates multiple buckets.
     *
     * @param buckets
     * the buckets to create.
     * @return
     * the created buckets.
     * @throws S3ServiceException
     */
    public S3Bucket[] createBuckets(final S3Bucket[] buckets) throws S3ServiceException {
        final List bucketList = new ArrayList();
        S3ServiceEventAdaptor adaptor = new S3ServiceEventAdaptor() {
            @Override
            public void s3ServiceEventPerformed(CreateBucketsEvent event) {
                super.s3ServiceEventPerformed(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    bucketList.addAll(Arrays.asList(event.getCreatedBuckets()));
                }
            };
        };
        (new S3ServiceMulti(s3Service, adaptor)).createBuckets(buckets);
        throwError(adaptor);
        return (S3Bucket[]) bucketList.toArray(new S3Bucket[bucketList.size()]);
    }

    /**
     * Creates/uploads multiple objects.
     *
     * @param bucket
     * the bucket to create the objects in.
     * @param objects
     * the objects to create/upload.
     * @return
     * the created/uploaded objects.
     * @throws S3ServiceException
     */
    public S3Object[] putObjects(final S3Bucket bucket, final S3Object[] objects) throws S3ServiceException {
        final List objectList = new ArrayList();
        S3ServiceEventAdaptor adaptor = new S3ServiceEventAdaptor() {
            @Override
            public void s3ServiceEventPerformed(CreateObjectsEvent event) {
                super.s3ServiceEventPerformed(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getCreatedObjects()));
                }
            };
        };
        (new S3ServiceMulti(s3Service, adaptor)).putObjects(bucket, objects);
        throwError(adaptor);
        return (S3Object[]) objectList.toArray(new S3Object[objectList.size()]);
    }

    /**
     * Copies multiple objects within or between buckets.
     *
     * @param sourceBucketName
     * the name of the bucket containing the objects that will be copied.
     * @param destinationBucketName
     * the name of the bucket to which the objects will be copied. The destination
     * bucket may be the same as the source bucket.
     * @param sourceObjectKeys
     * the key names of the objects that will be copied.
     * @param destinationObjects
     * objects that will be created by the copy operation. The AccessControlList
     * setting of each object will determine the access permissions of the
     * resultant object, and if the replaceMetadata flag is true the metadata
     * items in each object will also be applied to the resultant object.
     * @param replaceMetadata
     * if true, the metadata items in the destination objects will be stored
     * in S3 by using the REPLACE metadata copying option. If false, the metadata
     * items will be copied unchanged from the original objects using the COPY
     * metadata copying option.s
     */

    public Map[] copyObjects(final String sourceBucketName, final String destinationBucketName,
        final String[] sourceObjectKeys, final S3Object[] destinationObjects, boolean replaceMetadata)
        throws S3ServiceException
    {
        final List resultsList = new ArrayList();
        S3ServiceEventAdaptor adaptor = new S3ServiceEventAdaptor() {
            @Override
            public void s3ServiceEventPerformed(CopyObjectsEvent event) {
                super.s3ServiceEventPerformed(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    resultsList.addAll(Arrays.asList(event.getCopyResults()));
                }
            };
        };
        (new S3ServiceMulti(s3Service, adaptor)).copyObjects(sourceBucketName, destinationBucketName,
            sourceObjectKeys, destinationObjects, replaceMetadata);
        throwError(adaptor);
        return (Map[]) resultsList.toArray(new Map[resultsList.size()]);
    }

    /**
     * Deletes multiple objects
     *
     * @param bucket
     * the bucket containing the objects to delete.
     * @param objects
     * the objects to delete.
     * @throws S3ServiceException
     */
    public void deleteObjects(final S3Bucket bucket, final S3Object[] objects) throws S3ServiceException {
        final List objectList = new ArrayList();
        S3ServiceEventAdaptor adaptor = new S3ServiceEventAdaptor() {
            @Override
            public void s3ServiceEventPerformed(DeleteObjectsEvent event) {
                super.s3ServiceEventPerformed(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getDeletedObjects()));
                }
            };
        };
        (new S3ServiceMulti(s3Service, adaptor)).deleteObjects(bucket, objects);
        throwError(adaptor);
    }

    /**
     * Deletes multiple versions.
     *
     * @param versionIds
     * the identifiers of the object versions that will be deleted.
     * @param multiFactorSerialNumber
     * the serial number for a multi-factor authentication device.
     * @param multiFactorAuthCode
     * a multi-factor authentication code generated by a device.
     * @param bucketName
     * the name of the versioned bucket containing the object to be deleted.
     * @param objectKey
     * the key representing the object in S3.
     *
     * @throws S3ServiceException
     */
    public void deleteVersionsOfObjectWithMFA(final String[] versionIds,
        String multiFactorSerialNumber, String multiFactorAuthCode,
        String bucketName, String objectKey) throws S3ServiceException
    {
        S3ServiceEventAdaptor adaptor = new S3ServiceEventAdaptor();
        (new S3ServiceMulti(s3Service, adaptor)).deleteVersionsOfObjectWithMFA(
            versionIds, multiFactorSerialNumber, multiFactorAuthCode,
            bucketName, objectKey);
        throwError(adaptor);
    }

    /**
     * Deletes multiple versions.
     *
     * @param versionIds
     * the identifiers of the object versions that will be deleted.
     * @param bucketName
     * the name of the versioned bucket containing the object to be deleted.
     * @param objectKey
     * the key representing the object in S3.
     *
     * @throws S3ServiceException
     */
    public void deleteVersionsOfObject(final String[] versionIds,
        String bucketName, String objectKey) throws S3ServiceException
    {
        deleteVersionsOfObjectWithMFA(versionIds, null, null, bucketName, objectKey);
    }

    /**
     * Retrieves multiple objects (including details and data).
     * The objects' data will be stored in temporary files, and can be retrieved using
     * {@link S3Object#getDataInputStream()}.
     *
     * @param bucket
     * the bucket containing the objects.
     * @param objects
     * the objects to retrieve.
     * @return
     * the retrieved objects.
     * @throws S3ServiceException
     */
    public S3Object[] getObjects(S3Bucket bucket, S3Object[] objects) throws S3ServiceException {
        DownloadPackage[] downloadPackages = new DownloadPackage[objects.length];
        try {
            for (int i = 0; i < downloadPackages.length; i++) {
                // Create a temporary file for data, file will auto-delete on JVM exit.
                File tempFile = File.createTempFile("jets3t-", ".tmp");
                tempFile.deleteOnExit();

                downloadPackages[i] = new DownloadPackage(objects[i], tempFile);
            }
        } catch (IOException e) {
            throw new S3ServiceException("Unable to create temporary file to store object data", e);
        }

        final List objectList = new ArrayList();
        S3ServiceEventAdaptor adaptor = new S3ServiceEventAdaptor() {
            @Override
            public void s3ServiceEventPerformed(DownloadObjectsEvent event) {
                super.s3ServiceEventPerformed(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getDownloadedObjects()));
                }
            };
        };

        (new S3ServiceMulti(s3Service, adaptor)).downloadObjects(bucket, downloadPackages);
        throwError(adaptor);
        return (S3Object[]) objectList.toArray(new S3Object[objectList.size()]);
    }

    /**
     * Retrieves multiple objects (including details and data).
     * The objects' data will be stored in temporary files, and can be retrieved using
     * {@link S3Object#getDataInputStream()}.
     *
     * @param bucket
     * the bucket containing the objects.
     * @param objectKeys
     * the key names of the objects to retrieve.
     * @return
     * the retrieved objects.
     *
     * @throws S3ServiceException
     */
    public S3Object[] getObjects(final S3Bucket bucket, final String[] objectKeys) throws S3ServiceException {
        S3Object[] objects = new S3Object[objectKeys.length];
        for (int i = 0; i < objectKeys.length; i++) {
            objects[i] = new S3Object(objectKeys[i]);
        }
        return getObjects(bucket, objects);
    }

    /**
     * Retrieves details of multiple objects (details only, no data)
     *
     * @param bucket
     * the bucket containing the objects.
     * @param objects
     * the objects to retrieve.
     * @return
     * objects populated with the details retrieved.
     * @throws S3ServiceException
     */
    public S3Object[] getObjectsHeads(S3Bucket bucket, S3Object[] objects) throws S3ServiceException {
        String[] objectKeys = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            objectKeys[i] = objects[i].getKey();
        }
        return getObjectsHeads(bucket, objectKeys);
    }

    /**
     * Retrieves details of multiple objects (details only, no data)
     *
     * @param bucket
     * the bucket containing the objects.
     * @param objectKeys
     * the key names of the objects to retrieve.
     * @return
     * objects populated with the details retrieved.
     * @throws S3ServiceException
     */
    public S3Object[] getObjectsHeads(final S3Bucket bucket, final String[] objectKeys) throws S3ServiceException {
        final List objectList = new ArrayList();
        S3ServiceEventAdaptor adaptor = new S3ServiceEventAdaptor() {
            @Override
            public void s3ServiceEventPerformed(GetObjectHeadsEvent event) {
                super.s3ServiceEventPerformed(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getCompletedObjects()));
                }
            };
        };
        (new S3ServiceMulti(s3Service, adaptor)).getObjectsHeads(bucket, objectKeys);
        throwError(adaptor);
        return (S3Object[]) objectList.toArray(new S3Object[objectList.size()]);
    }

    /**
     * Retrieves Access Control List (ACL) settings for multiple objects.
     *
     * @param bucket
     * the bucket containing the objects.
     * @param objects
     * the objects whose ACLs will be retrieved.
     * @return
     * objects including the ACL information retrieved.
     * @throws S3ServiceException
     */
    public S3Object[] getObjectACLs(final S3Bucket bucket, final S3Object[] objects) throws S3ServiceException {
        final List objectList = new ArrayList();
        S3ServiceEventAdaptor adaptor = new S3ServiceEventAdaptor() {
            @Override
            public void s3ServiceEventPerformed(LookupACLEvent event) {
                super.s3ServiceEventPerformed(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getObjectsWithACL()));
                }
            };
        };
        (new S3ServiceMulti(s3Service, adaptor)).getObjectACLs(bucket, objects);
        throwError(adaptor);
        return (S3Object[]) objectList.toArray(new S3Object[objectList.size()]);
    }

    /**
     * Updates/sets Access Control List (ACL) settings for multiple objects.
     *
     * @param bucket
     * the bucket containing the objects.
     * @param objects
     * objects containing ACL settings that will be updated/set.
     * @return
     * objects whose ACL settings were updated/set.
     * @throws S3ServiceException
     */
    public S3Object[] putACLs(final S3Bucket bucket, final S3Object[] objects) throws S3ServiceException {
        final List objectList = new ArrayList();
        S3ServiceEventAdaptor adaptor = new S3ServiceEventAdaptor() {
            @Override
            public void s3ServiceEventPerformed(UpdateACLEvent event) {
                super.s3ServiceEventPerformed(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getObjectsWithUpdatedACL()));
                }
            };
        };
        (new S3ServiceMulti(s3Service, adaptor)).putACLs(bucket, objects);
        throwError(adaptor);
        return (S3Object[]) objectList.toArray(new S3Object[objectList.size()]);
    }

    /**
     * A convenience method to download multiple objects from S3 to pre-existing output streams, which
     * is particularly useful for downloading objects to files.
     *
     * @param bucket
     * the bucket containing the objects
     * @param downloadPackages
     * an array of download package objects that manage the output of data for an S3Object.
     *
     * @throws S3ServiceException
     */
    public void downloadObjects(final S3Bucket bucket, final DownloadPackage[] downloadPackages) throws S3ServiceException {
        S3ServiceEventAdaptor adaptor = new S3ServiceEventAdaptor();
        (new S3ServiceMulti(s3Service, adaptor)).downloadObjects(bucket, downloadPackages);
        throwError(adaptor);
    }


}
