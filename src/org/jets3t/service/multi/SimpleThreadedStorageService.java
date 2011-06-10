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
package org.jets3t.service.multi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jets3t.service.StorageService;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.multi.event.CopyObjectsEvent;
import org.jets3t.service.multi.event.CreateBucketsEvent;
import org.jets3t.service.multi.event.CreateObjectsEvent;
import org.jets3t.service.multi.event.DeleteObjectsEvent;
import org.jets3t.service.multi.event.DownloadObjectsEvent;
import org.jets3t.service.multi.event.GetObjectHeadsEvent;
import org.jets3t.service.multi.event.LookupACLEvent;
import org.jets3t.service.multi.event.ServiceEvent;
import org.jets3t.service.multi.event.UpdateACLEvent;

/**
 * S3 service wrapper that performs multiple S3 requests at a time using multi-threading and an
 * underlying thread-safe {@link StorageService} implementation.
 * <p>
 * This class provides a simplified interface to the {@link ThreadedStorageService} service.
 * It will block while doing its work, return the results of an operation when it is finished,
 * and throw an exception if anything goes wrong.
 * <p>
 * For a non-blocking multi-threading service that is more powerful, but also more complicated,
 * see {@link ThreadedStorageService}.
 *
 * @author James Murty
 */
public class SimpleThreadedStorageService {
    private StorageService service = null;

    /**
     * Construct a multi-threaded service based on a StorageService.
     *
     * @param service
     * a StorageService implementation that will be used to perform S3 requests.
     */
    public SimpleThreadedStorageService(StorageService service) {
        this.service = service;
    }

    /**
     * Utility method to check an {@link StorageServiceEventAdaptor} for the occurrence of an error,
     * and if one is present to throw it.
     *
     * @param adaptor
     * @throws ServiceException
     */
    protected void throwError(StorageServiceEventAdaptor adaptor) throws ServiceException {
        if (adaptor.wasErrorThrown()) {
            Throwable thrown = adaptor.getErrorThrown();
            if (thrown instanceof ServiceException) {
                throw (ServiceException) thrown;
            } else {
                throw new ServiceException(thrown);
            }
        }
    }

    /**
     * Creates multiple buckets.
     *
     * @param bucketNames
     * name of the buckets to create.
     * @return
     * the created buckets.
     * @throws ServiceException
     */
    public StorageBucket[] createBuckets(final String[] bucketNames) throws ServiceException {
        final List<StorageBucket> bucketList = new ArrayList<StorageBucket>();
        StorageServiceEventAdaptor adaptor = new StorageServiceEventAdaptor() {
            @Override
            public void event(CreateBucketsEvent event) {
                super.event(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    bucketList.addAll(Arrays.asList(event.getCreatedBuckets()));
                }
            };
        };
        (new ThreadedStorageService(service, adaptor)).createBuckets(bucketNames);
        throwError(adaptor);
        return bucketList.toArray(new StorageBucket[bucketList.size()]);
    }

    /**
     * Creates/uploads multiple objects.
     *
     * @param bucketName
     * the bucket where objects will be stored.
     * @param objects
     * the objects to create/upload.
     * @return
     * the created/uploaded objects.
     * @throws ServiceException
     */
    public StorageObject[] putObjects(String bucketName,
        final StorageObject[] objects) throws ServiceException
    {
        final List<StorageObject> objectList = new ArrayList<StorageObject>();
        StorageServiceEventAdaptor adaptor = new StorageServiceEventAdaptor() {
            @Override
            public void event(CreateObjectsEvent event) {
                super.event(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getCreatedObjects()));
                }
            };
        };
        (new ThreadedStorageService(service, adaptor)).putObjects(bucketName, objects);
        throwError(adaptor);
        return objectList.toArray(new StorageObject[objectList.size()]);
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
        final String[] sourceObjectKeys, final StorageObject[] destinationObjects, boolean replaceMetadata)
        throws ServiceException
    {
        final List resultsList = new ArrayList();
        StorageServiceEventAdaptor adaptor = new StorageServiceEventAdaptor() {
            @Override
            public void event(CopyObjectsEvent event) {
                super.event(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    resultsList.addAll(Arrays.asList(event.getCopyResults()));
                }
            };
        };
        (new ThreadedStorageService(service, adaptor)).copyObjects(sourceBucketName, destinationBucketName,
            sourceObjectKeys, destinationObjects, replaceMetadata);
        throwError(adaptor);
        return (Map[]) resultsList.toArray(new Map[resultsList.size()]);
    }

    /**
     * Deletes multiple objects
     *
     * @param bucketName
     * name of the bucket containing the objects to delete.
     * @param objects
     * the objects to delete.
     * @throws ServiceException
     */
    public void deleteObjects(String bucketName, final StorageObject[] objects) throws ServiceException {
        final List objectList = new ArrayList();
        StorageServiceEventAdaptor adaptor = new StorageServiceEventAdaptor() {
            @Override
            public void event(DeleteObjectsEvent event) {
                super.event(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getDeletedObjects()));
                }
            };
        };
        (new ThreadedStorageService(service, adaptor)).deleteObjects(bucketName, objects);
        throwError(adaptor);
    }

    /**
     * Retrieves multiple objects (including details and data).
     * The objects' data will be stored in temporary files, and can be retrieved using
     * {@link StorageObject#getDataInputStream()}.
     *
     * @param bucketName
     * name of the bucket containing the objects.
     * @param objects
     * the objects to retrieve.
     * @return
     * the retrieved objects.
     * @throws ServiceException
     */
    public StorageObject[] getObjects(String bucketName, StorageObject[] objects) throws ServiceException {
        DownloadPackage[] downloadPackages = new DownloadPackage[objects.length];
        try {
            for (int i = 0; i < downloadPackages.length; i++) {
                // Create a temporary file for data, file will auto-delete on JVM exit.
                File tempFile = File.createTempFile("jets3t-", ".tmp");
                tempFile.deleteOnExit();

                downloadPackages[i] = new DownloadPackage(objects[i], tempFile);
            }
        } catch (IOException e) {
            throw new ServiceException("Unable to create temporary file to store object data", e);
        }

        final List<StorageObject> objectList = new ArrayList<StorageObject>();
        StorageServiceEventAdaptor adaptor = new StorageServiceEventAdaptor() {
            @Override
            public void event(DownloadObjectsEvent event) {
                super.event(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getDownloadedObjects()));
                }
            };
        };
        (new ThreadedStorageService(service, adaptor)).downloadObjects(bucketName, downloadPackages);
        throwError(adaptor);
        return objectList.toArray(new StorageObject[objectList.size()]);
    }

    /**
     * Retrieves multiple objects (including details and data).
     * The objects' data will be stored in temporary files, and can be retrieved using
     * {@link StorageObject#getDataInputStream()}.
     *
     * @param bucketName
     * name of the bucket containing the objects.
     * @param objectKeys
     * the key names of the objects to retrieve.
     * @return
     * the retrieved objects.
     *
     * @throws ServiceException
     */
    public StorageObject[] getObjects(String bucketName, final String[] objectKeys)
        throws ServiceException
    {
        StorageObject[] objects = new StorageObject[objectKeys.length];
        for (int i = 0; i < objectKeys.length; i++) {
            objects[i] = new StorageObject(objectKeys[i]);
        }
        return getObjects(bucketName, objects);
    }

    /**
     * Retrieves details of multiple objects (details only, no data)
     *
     * @param bucketName
     * name of the bucket containing the objects.
     * @param objects
     * the objects to retrieve.
     * @return
     * objects populated with the details retrieved.
     * @throws ServiceException
     */
    public StorageObject[] getObjectsHeads(String bucketName, StorageObject[] objects) throws ServiceException {
        String[] objectKeys = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            objectKeys[i] = objects[i].getKey();
        }
        return getObjectsHeads(bucketName, objectKeys);
    }

    /**
     * Retrieves details of multiple objects (details only, no data)
     *
     * @param bucketName
     * name of the bucket containing the objects.
     * @param objectKeys
     * the key names of the objects to retrieve.
     * @return
     * objects populated with the details retrieved.
     * @throws ServiceException
     */
    public StorageObject[] getObjectsHeads(String bucketName, final String[] objectKeys) throws ServiceException {
        final List<StorageObject> objectList = new ArrayList<StorageObject>();
        StorageServiceEventAdaptor adaptor = new StorageServiceEventAdaptor() {
            @Override
            public void event(GetObjectHeadsEvent event) {
                super.event(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getCompletedObjects()));
                }
            };
        };
        (new ThreadedStorageService(service, adaptor)).getObjectsHeads(bucketName, objectKeys);
        throwError(adaptor);
        return objectList.toArray(new StorageObject[objectList.size()]);
    }

    /**
     * Retrieves Access Control List (ACL) settings for multiple objects.
     *
     * @param bucketName
     * name of the bucket containing the objects.
     * @param objects
     * the objects whose ACLs will be retrieved.
     * @return
     * objects including the ACL information retrieved.
     * @throws ServiceException
     */
    public StorageObject[] getObjectACLs(String bucketName, final StorageObject[] objects) throws ServiceException {
        final List<StorageObject> objectList = new ArrayList<StorageObject>();
        StorageServiceEventAdaptor adaptor = new StorageServiceEventAdaptor() {
            @Override
            public void event(LookupACLEvent event) {
                super.event(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getObjectsWithACL()));
                }
            };
        };
        (new ThreadedStorageService(service, adaptor)).getObjectACLs(bucketName, objects);
        throwError(adaptor);
        return objectList.toArray(new StorageObject[objectList.size()]);
    }

    /**
     * Updates/sets Access Control List (ACL) settings for multiple objects.
     *
     * @param bucketName
     * name of the bucket containing the objects.
     * @param objects
     * objects containing ACL settings that will be updated/set.
     * @return
     * objects whose ACL settings were updated/set.
     * @throws ServiceException
     */
    public StorageObject[] putACLs(String bucketName, final StorageObject[] objects) throws ServiceException {
        final List<StorageObject> objectList = new ArrayList<StorageObject>();
        StorageServiceEventAdaptor adaptor = new StorageServiceEventAdaptor() {
            @Override
            public void event(UpdateACLEvent event) {
                super.event(event);
                if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    objectList.addAll(Arrays.asList(event.getObjectsWithUpdatedACL()));
                }
            };
        };
        (new ThreadedStorageService(service, adaptor)).putACLs(bucketName, objects);
        throwError(adaptor);
        return objectList.toArray(new StorageObject[objectList.size()]);
    }

    /**
     * A convenience method to download multiple objects from S3 to pre-existing output streams, which
     * is particularly useful for downloading objects to files.
     *
     * @param bucketName
     * name of the bucket containing the objects
     * @param downloadPackages
     * an array of download package objects that manage the output of data for an object.
     *
     * @throws ServiceException
     */
    public void downloadObjects(String bucketName, final DownloadPackage[] downloadPackages) throws ServiceException {
        StorageServiceEventAdaptor adaptor = new StorageServiceEventAdaptor();
        (new ThreadedStorageService(service, adaptor)).downloadObjects(bucketName, downloadPackages);
        throwError(adaptor);
    }


}
