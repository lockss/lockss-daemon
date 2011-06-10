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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ObjectsChunk;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.StorageObjectsChunk;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.io.BytesProgressWatcher;
import org.jets3t.service.io.InterruptableInputStream;
import org.jets3t.service.io.ProgressMonitoredInputStream;
import org.jets3t.service.io.TempFile;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.S3Version;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.ServiceUtils;
import org.jets3t.service.utils.signedurl.SignedUrlAndObject;
import org.jets3t.service.utils.signedurl.SignedUrlHandler;

/**
 * S3 service wrapper that performs multiple S3 requests at a time using multi-threading and an
 * underlying thread-safe {@link S3Service} implementation.
 * <p>
 * This service is designed to be run in non-blocking threads that therefore communicates
 * information about its progress by firing {@link ServiceEvent} events. It is the responsibility
 * of applications using this service to correctly handle these events - see the JetS3t application
 * {@link org.jets3t.apps.synchronize.Synchronize} for examples of how an application can use these
 * events.
 * </p>
 * <p>
 * For cases where the full power, and complexity, of the event notification mechanism is not required
 * the simplified multi-threaded service {@link S3ServiceSimpleMulti} can be used.
 * </p>
 * <p>
 * This class uses properties obtained through {@link Jets3tProperties}. For more information on
 * these properties please refer to
 * <a href="http://www.jets3t.org/toolkit/configuration.html">JetS3t Configuration</a>
 * </p>
 *
 * @deprecated 0.8.0 use {@link org.jets3t.service.multi.ThreadedStorageService} instead.
 *
 * @author James Murty
 */
@Deprecated
public class S3ServiceMulti {

    private static final Log log = LogFactory.getLog(S3ServiceMulti.class);

    private S3Service s3Service = null;
    private final boolean[] isShutdown = new boolean[] { false };

    private final ArrayList<S3ServiceEventListener> serviceEventListeners =
        new ArrayList<S3ServiceEventListener>();
    private final long sleepTime;

    /**
     * Construct a multi-threaded service based on an S3Service and which sends event notifications
     * to an event listening class. EVENT_IN_PROGRESS events are sent at the default time interval
     * of 500ms.
     *
     * @param s3Service
     *        an S3Service implementation that will be used to perform S3 requests. This implementation
     *        <b>must</b> be thread-safe.
     * @param listener
     *        the event listener which will handle event notifications.
     */
    public S3ServiceMulti(S3Service s3Service, S3ServiceEventListener listener) {
        this(s3Service, listener, 500);
    }

    /**
     * Construct a multi-threaded service based on an S3Service and which sends event notifications
     * to an event listening class, and which will send EVENT_IN_PROGRESS events at the specified
     * time interval.
     *
     * @param s3Service
     *        an S3Service implementation that will be used to perform S3 requests. This implementation
     *        <b>must</b> be thread-safe.
     * @param listener
     *        the event listener which will handle event notifications.
     * @param threadSleepTimeMS
     *        how many milliseconds to wait before sending each EVENT_IN_PROGRESS notification event.
     */
    public S3ServiceMulti(
        S3Service s3Service, S3ServiceEventListener listener, long threadSleepTimeMS)
    {
        this.s3Service = s3Service;
        addServiceEventListener(listener);
        this.sleepTime = threadSleepTimeMS;

        // Sanity-check the maximum thread and connection settings to ensure the maximum number
        // of connections is at least equal to the largest of the maximum thread counts, and warn
        // the use of potential problems.
        int adminMaxThreadCount = this.s3Service.getJetS3tProperties()
            .getIntProperty("s3service.admin-max-thread-count", 20);
        int maxThreadCount = this.s3Service.getJetS3tProperties()
            .getIntProperty("s3service.max-thread-count", 2);
        int maxConnectionCount = this.s3Service.getJetS3tProperties()
            .getIntProperty("httpclient.max-connections", 20);
        if (maxConnectionCount < maxThreadCount) {
            if (log.isWarnEnabled()) {
                log.warn("Insufficient connections available (httpclient.max-connections="
                    + maxConnectionCount + ") to run " + maxThreadCount
                    + " simultaneous threads (s3service.max-thread-count) - please adjust JetS3t settings");
            }
        }
        if (maxConnectionCount < adminMaxThreadCount) {
            if (log.isWarnEnabled()) {
                log.warn("Insufficient connections available (httpclient.max-connections="
                    + maxConnectionCount + ") to run " + adminMaxThreadCount
                    + " simultaneous admin threads (s3service.admin-max-thread-count) - please adjust JetS3t settings");
            }
        }
    }

    /**
     * Make a best-possible effort to shutdown and clean up any resources used by this
     * service such as HTTP connections, connection pools, threads etc. After calling
     * this method the service instance will no longer be usable -- a new instance must
     * be created to do more work.
     *
     * @throws S3ServiceException
     */
    public void shutdown() throws S3ServiceException {
        this.isShutdown[0] = true;
        try {
            this.getS3Service().shutdown();
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * @return true if the {@link #shutdown()} method has been used to shut down and
     * clean up this service. If this function returns true this service instance
     * can no longer be used to do work.
     */
    public boolean isShutdown() {
        return this.isShutdown[0];
    }

    /**
     * @return
     * the underlying S3 service implementation.
     */
    public S3Service getS3Service() {
        return s3Service;
    }

    /**
     * Adds a service event listener to the set of listeners that will be notified of events.
     *
     * @param listener
     * an event listener to add to the event notification chain.
     */
    public void addServiceEventListener(S3ServiceEventListener listener) {
        if (listener != null) {
            serviceEventListeners.add(listener);
        }
    }

    /**
     * Removes a service event listener from the set of listeners that will be notified of events.
     *
     * @param listener
     * an event listener to remove from the event notification chain.
     */
    public void removeServiceEventListener(S3ServiceEventListener listener) {
        if (listener != null) {
            serviceEventListeners.remove(listener);
        }
    }

    /**
     * Sends a service event to each of the listeners registered with this service.
     * @param event
     * the event to send to this service's registered event listeners.
     */
    protected void fireServiceEvent(ServiceEvent event) {
        if (serviceEventListeners.size() == 0) {
            if (log.isWarnEnabled()) {
                log.warn("S3ServiceMulti invoked without any S3ServiceEventListener objects, this is dangerous!");
            }
        }
        Iterator<S3ServiceEventListener> listenerIter = serviceEventListeners.iterator();
        while (listenerIter.hasNext()) {
            S3ServiceEventListener listener = listenerIter.next();

            if (event instanceof CreateObjectsEvent) {
                listener.s3ServiceEventPerformed((CreateObjectsEvent) event);
            } else if (event instanceof CopyObjectsEvent) {
                listener.s3ServiceEventPerformed((CopyObjectsEvent) event);
            } else if (event instanceof CreateBucketsEvent) {
                listener.s3ServiceEventPerformed((CreateBucketsEvent) event);
            } else if (event instanceof ListObjectsEvent) {
                listener.s3ServiceEventPerformed((ListObjectsEvent) event);
            } else if (event instanceof DeleteObjectsEvent) {
                listener.s3ServiceEventPerformed((DeleteObjectsEvent) event);
            } else if (event instanceof DeleteVersionedObjectsEvent) {
                listener.s3ServiceEventPerformed((DeleteVersionedObjectsEvent) event);
            } else if (event instanceof GetObjectsEvent) {
                listener.s3ServiceEventPerformed((GetObjectsEvent) event);
            } else if (event instanceof GetObjectHeadsEvent) {
                listener.s3ServiceEventPerformed((GetObjectHeadsEvent) event);
            } else if (event instanceof LookupACLEvent) {
                listener.s3ServiceEventPerformed((LookupACLEvent) event);
            } else if (event instanceof UpdateACLEvent) {
                listener.s3ServiceEventPerformed((UpdateACLEvent) event);
            } else if (event instanceof DownloadObjectsEvent) {
                listener.s3ServiceEventPerformed((DownloadObjectsEvent) event);
            } else {
                throw new IllegalArgumentException("Listener not invoked for event class: " + event.getClass());
            }
        }
    }


    /**
     * @return
     * true if the underlying S3Service implementation is authenticated.
     */
    public boolean isAuthenticatedConnection() {
        return s3Service.isAuthenticatedConnection();
    }

    /**
     * @return
     * the AWS credentials in the underlying S3Service.
     */
    public ProviderCredentials getAWSCredentials() {
        return s3Service.getAWSCredentials();
    }

    /**
     * Lists the objects in a bucket based on an array of prefix strings, and
     * sends {@link ListObjectsEvent} notification events.
     * The objects that match each prefix are listed in a separate background
     * thread, potentially allowing you to list the contents of large buckets more
     * quickly than if you had to list all the objects in sequence.
     * <p>
     * Objects in the bucket that do not match one of the prefixes will not be
     * listed.
     *
     * @param bucketName
     * the name of the bucket in which the objects are stored.
     * @param prefixes
     * an array of prefix strings. A separate listing thread will be run for
     * each of these prefix strings, and the method will only complete once
     * the entire object listing for each prefix has been obtained (unless the
     * operation is cancelled, or an error occurs)
     * @param delimiter
     * an optional delimiter string to apply to each listing operation. This
     * parameter should be null if you do not wish to apply a delimiter.
     * @param maxListingLength
     * the maximum number of objects to list in each iteration. This should be a
     * value between 1 and 1000, where 1000 will be the best choice in almost all
     * circumstances. Regardless of this value, all the objects in the bucket that
     * match the criteria will be returned.
     *
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean listObjects(final String bucketName, final String[] prefixes,
        final String delimiter, final long maxListingLength)
    {
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        ListObjectsRunnable[] runnables = new ListObjectsRunnable[prefixes.length];
        for (int i = 0; i < runnables.length; i++) {
            runnables[i] = new ListObjectsRunnable(bucketName, prefixes[i],
                delimiter, maxListingLength, null);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(ListObjectsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List chunkList) {
                fireServiceEvent(ListObjectsEvent.newInProgressEvent(threadWatcher, chunkList,
                    uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                success[0] = false;
                fireServiceEvent(ListObjectsEvent.newCancelledEvent(uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(ListObjectsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(ListObjectsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(ListObjectsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Creates multiple buckets, and sends {@link CreateBucketsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @param buckets
     * the buckets to create.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean createBuckets(final S3Bucket[] buckets) {
        final List incompletedBucketList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        CreateBucketRunnable[] runnables = new CreateBucketRunnable[buckets.length];
        for (int i = 0; i < runnables.length; i++) {
            incompletedBucketList.add(buckets[i]);

            runnables[i] = new CreateBucketRunnable(buckets[i]);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(CreateBucketsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                incompletedBucketList.removeAll(completedResults);
                S3Bucket[] completedBuckets = (S3Bucket[]) completedResults
                    .toArray(new S3Bucket[completedResults.size()]);
                fireServiceEvent(CreateBucketsEvent.newInProgressEvent(threadWatcher, completedBuckets, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Bucket[] incompletedBuckets = (S3Bucket[]) incompletedBucketList
                    .toArray(new S3Bucket[incompletedBucketList.size()]);
                success[0] = false;
                fireServiceEvent(CreateBucketsEvent.newCancelledEvent(incompletedBuckets, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(CreateBucketsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(CreateBucketsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(CreateBucketsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Copies multiple objects within or between buckets, while sending
     * {@link CopyObjectsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
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
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean copyObjects(final String sourceBucketName, final String destinationBucketName,
        final String[] sourceObjectKeys, final S3Object[] destinationObjects, boolean replaceMetadata)
    {
        final List incompletedObjectsList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        CopyObjectRunnable[] runnables = new CopyObjectRunnable[sourceObjectKeys.length];
        for (int i = 0; i < runnables.length; i++) {
            incompletedObjectsList.add(destinationObjects[i]);
            runnables[i] = new CopyObjectRunnable(sourceBucketName, destinationBucketName,
                sourceObjectKeys[i], destinationObjects[i], replaceMetadata);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(CopyObjectsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                incompletedObjectsList.removeAll(completedResults);
                Map[] copyResults = (Map[]) completedResults
                    .toArray(new Map[completedResults.size()]);
                fireServiceEvent(CopyObjectsEvent.newInProgressEvent(threadWatcher,
                    copyResults, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Object[] incompletedObjects = (S3Object[]) incompletedObjectsList
                    .toArray(new S3Object[incompletedObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(CopyObjectsEvent.newCancelledEvent(incompletedObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(CopyObjectsEvent.newCompletedEvent(uniqueOperationId,
                    sourceObjectKeys, destinationObjects));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(CopyObjectsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(CopyObjectsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Creates multiple objects in a bucket, and sends {@link CreateObjectsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.max-admin-thread-count</tt>.
     *
     * @param bucket
     * the bucket to create the objects in
     * @param objects
     * the objects to create/upload.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean putObjects(final S3Bucket bucket, final S3Object[] objects) {
        final List incompletedObjectsList = new ArrayList();
        final List progressWatchers = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        CreateObjectRunnable[] runnables = new CreateObjectRunnable[objects.length];
        for (int i = 0; i < runnables.length; i++) {
            incompletedObjectsList.add(objects[i]);
            BytesProgressWatcher progressMonitor = new BytesProgressWatcher(objects[i].getContentLength());
            runnables[i] = new CreateObjectRunnable(bucket, objects[i], progressMonitor);
            progressWatchers.add(progressMonitor);
        }

        // Wait for threads to finish, or be cancelled.
        ThreadWatcher threadWatcher = new ThreadWatcher(
            (BytesProgressWatcher[]) progressWatchers.toArray(new BytesProgressWatcher[progressWatchers.size()]));
        (new ThreadGroupManager(runnables, threadWatcher,
            this.s3Service.getJetS3tProperties(), false)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(CreateObjectsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                incompletedObjectsList.removeAll(completedResults);
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                fireServiceEvent(CreateObjectsEvent.newInProgressEvent(threadWatcher,
                    completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Object[] incompletedObjects = (S3Object[]) incompletedObjectsList
                    .toArray(new S3Object[incompletedObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(CreateObjectsEvent.newCancelledEvent(incompletedObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(CreateObjectsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(CreateObjectsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(CreateObjectsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Deletes multiple objects from a bucket, and sends {@link DeleteObjectsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @param bucket
     * the bucket containing the objects to be deleted
     * @param objectKeys
     * key names of objects to delete
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean deleteObjects(final S3Bucket bucket, String[] objectKeys) {
        S3Object objects[] = new S3Object[objectKeys.length];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = new S3Object(objectKeys[i]);
        }
        return this.deleteObjects(bucket, objects);
    }

    /**
     * Deletes multiple objects from a bucket, and sends {@link DeleteObjectsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @param bucket
     * the bucket containing the objects to be deleted
     * @param objects
     * the objects to delete
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean deleteObjects(final S3Bucket bucket, final S3Object[] objects) {
        final List objectsToDeleteList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        DeleteObjectRunnable[] runnables = new DeleteObjectRunnable[objects.length];
        for (int i = 0; i < runnables.length; i++) {
            objectsToDeleteList.add(objects[i]);
            runnables[i] = new DeleteObjectRunnable(bucket, objects[i]);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(DeleteObjectsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                objectsToDeleteList.removeAll(completedResults);
                S3Object[] deletedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                fireServiceEvent(DeleteObjectsEvent.newInProgressEvent(threadWatcher, deletedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Object[] remainingObjects = (S3Object[]) objectsToDeleteList
                    .toArray(new S3Object[objectsToDeleteList.size()]);
                success[0] = false;
                fireServiceEvent(DeleteObjectsEvent.newCancelledEvent(remainingObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(DeleteObjectsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(DeleteObjectsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(DeleteObjectsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Delete multiple object versions from a bucket in S3, and sends
     * {@link DeleteVersionedObjectsEvent} notification events. This will delete only the specific
     * version identified and will not affect any other Version or DeleteMarkers related to
     * the object.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
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
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean deleteVersionsOfObjectWithMFA(final String[] versionIds,
        String multiFactorSerialNumber, String multiFactorAuthCode,
        String bucketName, String objectKey)
    {
        final List versionsToDeleteList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        DeleteVersionedObjectRunnable[] runnables =
            new DeleteVersionedObjectRunnable[versionIds.length];
        for (int i = 0; i < runnables.length; i++) {
            versionsToDeleteList.add(new S3Version(objectKey, versionIds[i]));
            runnables[i] = new DeleteVersionedObjectRunnable(
                versionIds[i], multiFactorSerialNumber, multiFactorAuthCode,
                bucketName, objectKey);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(DeleteVersionedObjectsEvent.newStartedEvent(
                    threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                versionsToDeleteList.removeAll(completedResults);
                S3Version[] deletedVersions = (S3Version[]) completedResults
                    .toArray(new S3Version[completedResults.size()]);
                fireServiceEvent(DeleteVersionedObjectsEvent.newInProgressEvent(
                    threadWatcher, deletedVersions, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Version[] remainingVersions = (S3Version[]) versionsToDeleteList
                    .toArray(new S3Version[versionsToDeleteList.size()]);
                success[0] = false;
                fireServiceEvent(DeleteVersionedObjectsEvent.newCancelledEvent(
                    remainingVersions, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(DeleteVersionedObjectsEvent.newCompletedEvent(
                    uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(DeleteVersionedObjectsEvent.newErrorEvent(
                    throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher,
                Throwable[] ignoredErrors)
            {
                success[0] = false;
                fireServiceEvent(DeleteVersionedObjectsEvent.newIgnoredErrorsEvent(
                    threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Delete multiple object versions from a bucket in S3, and sends
     * {@link DeleteVersionedObjectsEvent} notification events. This will delete only the specific
     * version identified and will not affect any other Version or DeleteMarkers related to
     * the object.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @param versionIds
     * the identifiers of the object versions that will be deleted.
     * @param bucketName
     * the name of the versioned bucket containing the object to be deleted.
     * @param objectKey
     * the key representing the object in S3.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean deleteVersionsOfObject(final String[] versionIds,
        String bucketName, String objectKey)
    {
        return deleteVersionsOfObjectWithMFA(versionIds, null, null, bucketName, objectKey);
    }

    /**
     * Retrieves multiple objects (details and data) from a bucket, and sends
     * {@link GetObjectsEvent} notification events.
     *
     * @param bucket
     * the bucket containing the objects to retrieve.
     * @param objects
     * the objects to retrieve.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean getObjects(S3Bucket bucket, S3Object[] objects) {
        String[] objectKeys = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            objectKeys[i] = objects[i].getKey();
        }
        return getObjects(bucket, objectKeys);
    }

    /**
     * Retrieves multiple objects (details and data) from a bucket, and sends
     * {@link GetObjectsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.max-thread-count</tt>.
     *
     * @param bucket
     * the bucket containing the objects to retrieve.
     * @param objectKeys
     * the key names of the objects to retrieve.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean getObjects(final S3Bucket bucket, final String[] objectKeys) {
        final List pendingObjectKeysList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        GetObjectRunnable[] runnables = new GetObjectRunnable[objectKeys.length];
        for (int i = 0; i < runnables.length; i++) {
            pendingObjectKeysList.add(objectKeys[i]);
            runnables[i] = new GetObjectRunnable(bucket, objectKeys[i], false);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), false)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(GetObjectsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                for (int i = 0; i < completedObjects.length; i++) {
                    pendingObjectKeysList.remove(completedObjects[i].getKey());
                }
                fireServiceEvent(GetObjectsEvent.newInProgressEvent(threadWatcher, completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                List cancelledObjectsList = new ArrayList();
                Iterator iter = pendingObjectKeysList.iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    cancelledObjectsList.add(new S3Object(key));
                }
                S3Object[] cancelledObjects = (S3Object[]) cancelledObjectsList
                    .toArray(new S3Object[cancelledObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(GetObjectsEvent.newCancelledEvent(cancelledObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(GetObjectsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(GetObjectsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(GetObjectsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Retrieves details (but no data) about multiple objects from a bucket, and sends
     * {@link GetObjectHeadsEvent} notification events.
     *
     * @param bucket
     * the bucket containing the objects whose details will be retrieved.
     * @param objects
     * the objects with details to retrieve.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean getObjectsHeads(S3Bucket bucket, S3Object[] objects) {
        String[] objectKeys = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            objectKeys[i] = objects[i].getKey();
        }
        return getObjectsHeads(bucket, objectKeys);
    }

    /**
     * Retrieves details (but no data) about multiple objects from a bucket, and sends
     * {@link GetObjectHeadsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @param bucket
     * the bucket containing the objects whose details will be retrieved.
     * @param objectKeys
     * the key names of the objects with details to retrieve.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean getObjectsHeads(final S3Bucket bucket, final String[] objectKeys) {
        final List pendingObjectKeysList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        GetObjectRunnable[] runnables = new GetObjectRunnable[objectKeys.length];
        for (int i = 0; i < runnables.length; i++) {
            pendingObjectKeysList.add(objectKeys[i]);
            runnables[i] = new GetObjectRunnable(bucket, objectKeys[i], true);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(GetObjectHeadsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                for (int i = 0; i < completedObjects.length; i++) {
                    pendingObjectKeysList.remove(completedObjects[i].getKey());
                }
                fireServiceEvent(GetObjectHeadsEvent.newInProgressEvent(threadWatcher, completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                List cancelledObjectsList = new ArrayList();
                Iterator iter = pendingObjectKeysList.iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    cancelledObjectsList.add(new S3Object(key));
                }
                S3Object[] cancelledObjects = (S3Object[]) cancelledObjectsList
                    .toArray(new S3Object[cancelledObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(GetObjectHeadsEvent.newCancelledEvent(cancelledObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(GetObjectHeadsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(GetObjectHeadsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(GetObjectHeadsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Retrieves Access Control List (ACL) information for multiple objects from a bucket, and sends
     * {@link LookupACLEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @param bucket
     * the bucket containing the objects
     * @param objects
     * the objects to retrieve ACL details for.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean getObjectACLs(final S3Bucket bucket, final S3Object[] objects) {
        final List pendingObjectsList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        GetACLRunnable[] runnables = new GetACLRunnable[objects.length];
        for (int i = 0; i < runnables.length; i++) {
            pendingObjectsList.add(objects[i]);
            runnables[i] = new GetACLRunnable(bucket, objects[i]);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(LookupACLEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                pendingObjectsList.removeAll(completedResults);
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                fireServiceEvent(LookupACLEvent.newInProgressEvent(threadWatcher, completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Object[] cancelledObjects = (S3Object[]) pendingObjectsList
                    .toArray(new S3Object[pendingObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(LookupACLEvent.newCancelledEvent(cancelledObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(LookupACLEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(LookupACLEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(LookupACLEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Updates/sets Access Control List (ACL) information for multiple objects in a bucket, and sends
     * {@link UpdateACLEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @param bucket
     * the bucket containing the objects
     * @param objects
     * the objects to update/set ACL details for.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean putACLs(final S3Bucket bucket, final S3Object[] objects) {
        final List pendingObjectsList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        PutACLRunnable[] runnables = new PutACLRunnable[objects.length];
        for (int i = 0; i < runnables.length; i++) {
            pendingObjectsList.add(objects[i]);
            runnables[i] = new PutACLRunnable(bucket, objects[i]);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(UpdateACLEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                pendingObjectsList.removeAll(completedResults);
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                fireServiceEvent(UpdateACLEvent.newInProgressEvent(threadWatcher, completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Object[] cancelledObjects = (S3Object[]) pendingObjectsList
                    .toArray(new S3Object[pendingObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(UpdateACLEvent.newCancelledEvent(cancelledObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(UpdateACLEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(UpdateACLEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(UpdateACLEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * A convenience method to download multiple objects from S3 to pre-existing
     * output streams, which is particularly useful for downloading objects to files.
     * The S3 objects can be represented as S3Objects or as signed URLs in a
     * {@link DownloadPackage} package. This method sends
     * {@link DownloadObjectsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.max-thread-count</tt>.
     * <p>
     * If the JetS3t configuration property <tt>downloads.restoreLastModifiedDate</tt> is set
     * to true, any files created by this method will have their last modified date set according
     * to the value of the S3 object's {@link Constants#METADATA_JETS3T_LOCAL_FILE_DATE} metadata
     * item.
     *
     * @param bucket
     * the bucket containing the objects
     * @param downloadPackages
     * an array of download packages containing the object to be downloaded, and able to build
     * an output stream where the object's contents will be written to.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     * @throws S3ServiceException
     */
    public boolean downloadObjects(final S3Bucket bucket,
        final DownloadPackage[] downloadPackages) throws S3ServiceException
    {
        final List progressWatchers = new ArrayList();
        final List incompleteObjectDownloadList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        boolean restoreLastModifiedDate = this.s3Service.getJetS3tProperties()
            .getBoolProperty("downloads.restoreLastModifiedDate", false);

        // Start all queries in the background.
        DownloadObjectRunnable[] runnables = new DownloadObjectRunnable[downloadPackages.length];
        final S3Object[] objects = new S3Object[downloadPackages.length];
        for (int i = 0; i < runnables.length; i++) {
            if (downloadPackages[i].getObject() == null) {
                // For signed URL downloads without corresponding object information, we create
                // a surrogate S3Object containing nothing but the object's key name.
                // This will allow the download to work, but total download size will not be known.
                try {
                    URL url = new URL(downloadPackages[i].getSignedUrl());
                    objects[i] = ServiceUtils.buildObjectFromUrl(
                        url.getHost(), url.getPath(), s3Service.getEndpoint());
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new S3ServiceException("Unable to determine S3 Object key name from signed URL: " +
                        downloadPackages[i].getSignedUrl());
                }
            } else {
                objects[i] = downloadPackages[i].getObject();
            }

            BytesProgressWatcher progressMonitor = new BytesProgressWatcher(objects[i].getContentLength());

            incompleteObjectDownloadList.add(objects[i]);
            progressWatchers.add(progressMonitor);

            if (downloadPackages[i].isSignedDownload()) {
                runnables[i] = new DownloadObjectRunnable(
                    downloadPackages[i], progressMonitor, restoreLastModifiedDate);
            } else {
                runnables[i] = new DownloadObjectRunnable(bucket, objects[i].getKey(),
                    downloadPackages[i], progressMonitor, restoreLastModifiedDate);
            }
        }

        // Wait for threads to finish, or be cancelled.
        ThreadWatcher threadWatcher = new ThreadWatcher(
            (BytesProgressWatcher[]) progressWatchers.toArray(new BytesProgressWatcher[progressWatchers.size()]));
        (new ThreadGroupManager(runnables, threadWatcher,
            this.s3Service.getJetS3tProperties(), false)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(DownloadObjectsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                incompleteObjectDownloadList.removeAll(completedResults);
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                fireServiceEvent(DownloadObjectsEvent.newInProgressEvent(threadWatcher, completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Object[] incompleteObjects = (S3Object[]) incompleteObjectDownloadList
                    .toArray(new S3Object[incompleteObjectDownloadList.size()]);
                success[0] = false;
                fireServiceEvent(DownloadObjectsEvent.newCancelledEvent(incompleteObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(DownloadObjectsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(DownloadObjectsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(DownloadObjectsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * A convenience method to download multiple objects from S3 to pre-existing
     * output streams, which is particularly useful for downloading objects to files.
     * This method sends {@link DownloadObjectsEvent} notification events.
     * <p>
     * This method can only download S3 objects represented by {@link DownloadPackage}
     * packages based on signed URL. To download objects when you don't have
     * signed URLs, you must use the method
     * {@link #downloadObjects(S3Bucket, DownloadPackage[])}
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.max-thread-count</tt>.
     * <p>
     * If the JetS3t configuration property <tt>downloads.restoreLastModifiedDate</tt> is set
     * to true, any files created by this method will have their last modified date set according
     * to the value of the S3 object's {@link Constants#METADATA_JETS3T_LOCAL_FILE_DATE} metadata
     * item.
     *
     * @param downloadPackages
     * an array of download packages containing the object to be downloaded, represented
     * with signed URL strings.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     * @throws S3ServiceException
     */
    public boolean downloadObjects(final DownloadPackage[] downloadPackages)
        throws S3ServiceException
    {
        // Sanity check to ensure all packages are based on signed URLs
        for (int i = 0; i < downloadPackages.length; i++) {
            if (!downloadPackages[i].isSignedDownload()) {
                throw new S3ServiceException(
                    "The downloadObjects(DownloadPackage[]) method may only be used with " +
                    "download packages based on signed URLs. Download package " + (i + 1) +
                    " of " + downloadPackages.length + " is not based on a signed URL");
            }
        }
        return downloadObjects(null, downloadPackages);
    }

    /**
     * Retrieves multiple objects (details and data) from a bucket using signed GET URLs corresponding
     * to those objects.
     * <p>
     * Object retrieval using signed GET URLs can be performed without the underlying S3Service knowing
     * the AWSCredentials for the target S3 account, however the underlying service must implement
     * the {@link SignedUrlHandler} interface.
     * <p>
     * This method sends {@link GetObjectHeadsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.max-thread-count</tt>.
     *
     * @param signedGetURLs
     * signed GET URL strings corresponding to the objects to be deleted.
     *
     * @throws IllegalStateException
     * if the underlying S3Service does not implement {@link SignedUrlHandler}
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean getObjects(final String[] signedGetURLs) throws MalformedURLException, UnsupportedEncodingException {
        if (!(s3Service instanceof SignedUrlHandler)) {
            throw new IllegalStateException("S3ServiceMutli's underlying S3Service must implement the"
                + "SignedUrlHandler interface to make the method getObjects(String[] signedGetURLs) available");
        }

        final List pendingObjectKeysList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        GetObjectRunnable[] runnables = new GetObjectRunnable[signedGetURLs.length];
        for (int i = 0; i < runnables.length; i++) {
            URL url = new URL(signedGetURLs[i]);
            S3Object object = ServiceUtils.buildObjectFromUrl(
                url.getHost(), url.getPath(), s3Service.getEndpoint());
            pendingObjectKeysList.add(object);

            runnables[i] = new GetObjectRunnable(signedGetURLs[i], false);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), false)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(GetObjectsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                for (int i = 0; i < completedObjects.length; i++) {
                    pendingObjectKeysList.remove(completedObjects[i].getKey());
                }
                fireServiceEvent(GetObjectsEvent.newInProgressEvent(threadWatcher, completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                List cancelledObjectsList = new ArrayList();
                Iterator iter = pendingObjectKeysList.iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    cancelledObjectsList.add(new S3Object(key));
                }
                S3Object[] cancelledObjects = (S3Object[]) cancelledObjectsList
                    .toArray(new S3Object[cancelledObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(GetObjectsEvent.newCancelledEvent(cancelledObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(GetObjectsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(GetObjectsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(GetObjectsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Retrieves details (but no data) about multiple objects using signed HEAD URLs corresponding
     * to those objects.
     * <p>
     * Detail retrieval using signed HEAD URLs can be performed without the underlying S3Service knowing
     * the AWSCredentials for the target S3 account, however the underlying service must implement
     * the {@link SignedUrlHandler} interface.
     * <p>
     * This method sends {@link GetObjectHeadsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @param signedHeadURLs
     * signed HEAD URL strings corresponding to the objects to be deleted.
     *
     * @throws IllegalStateException
     * if the underlying S3Service does not implement {@link SignedUrlHandler}
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean getObjectsHeads(final String[] signedHeadURLs) throws MalformedURLException, UnsupportedEncodingException {
        if (!(s3Service instanceof SignedUrlHandler)) {
            throw new IllegalStateException("S3ServiceMutli's underlying S3Service must implement the"
                + "SignedUrlHandler interface to make the method getObjectsHeads(String[] signedHeadURLs) available");
        }

        final List pendingObjectKeysList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        GetObjectRunnable[] runnables = new GetObjectRunnable[signedHeadURLs.length];
        for (int i = 0; i < runnables.length; i++) {
            URL url = new URL(signedHeadURLs[i]);
            S3Object object = ServiceUtils.buildObjectFromUrl(
                url.getHost(), url.getPath(), s3Service.getEndpoint());
            pendingObjectKeysList.add(object);

            runnables[i] = new GetObjectRunnable(signedHeadURLs[i], true);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(GetObjectHeadsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                for (int i = 0; i < completedObjects.length; i++) {
                    pendingObjectKeysList.remove(completedObjects[i].getKey());
                }
                fireServiceEvent(GetObjectHeadsEvent.newInProgressEvent(threadWatcher, completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                List cancelledObjectsList = new ArrayList();
                Iterator iter = pendingObjectKeysList.iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    cancelledObjectsList.add(new S3Object(key));
                }
                S3Object[] cancelledObjects = (S3Object[]) cancelledObjectsList
                    .toArray(new S3Object[cancelledObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(GetObjectHeadsEvent.newCancelledEvent(cancelledObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(GetObjectHeadsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(GetObjectHeadsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(GetObjectHeadsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Updates/sets Access Control List (ACL) information for multiple objects in
     * a bucket, and sends {@link UpdateACLEvent} notification events.
     * The S3 objects are represented as signed URLs.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @param signedURLs
     * URL strings that are authenticated and signed to allow a PUT request to
     * be performed for the referenced object.
     * @param acl
     * the access control list settings to apply to the objects.
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean putObjectsACLs(final String[] signedURLs, final AccessControlList acl)
        throws MalformedURLException, UnsupportedEncodingException
    {
        final List pendingObjectsList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        PutACLRunnable[] runnables = new PutACLRunnable[signedURLs.length];
        for (int i = 0; i < runnables.length; i++) {
            URL url = new URL(signedURLs[i]);
            S3Object object = ServiceUtils.buildObjectFromUrl(
                url.getHost(), url.getPath(), s3Service.getEndpoint());
            pendingObjectsList.add(object);
            runnables[i] = new PutACLRunnable(signedURLs[i], acl);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(UpdateACLEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                pendingObjectsList.removeAll(completedResults);
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                fireServiceEvent(UpdateACLEvent.newInProgressEvent(threadWatcher, completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Object[] cancelledObjects = (S3Object[]) pendingObjectsList
                    .toArray(new S3Object[pendingObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(UpdateACLEvent.newCancelledEvent(cancelledObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(UpdateACLEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(UpdateACLEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(UpdateACLEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Deletes multiple objects from a bucket using signed DELETE URLs corresponding to those objects.
     * <p>
     * Deletes using signed DELETE URLs can be performed without the underlying S3Service knowing
     * the AWSCredentials for the target S3 account, however the underlying service must implement
     * the {@link SignedUrlHandler} interface.
     * <p>
     * This method sends {@link DeleteObjectsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.admin-max-thread-count</tt>.
     *
     * @param signedDeleteUrls
     * signed DELETE URL strings corresponding to the objects to be deleted.
     *
     * @throws IllegalStateException
     * if the underlying S3Service does not implement {@link SignedUrlHandler}
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean deleteObjects(final String[] signedDeleteUrls) throws MalformedURLException, UnsupportedEncodingException {
        if (!(s3Service instanceof SignedUrlHandler)) {
            throw new IllegalStateException("S3ServiceMutli's underlying S3Service must implement the"
                + "SignedUrlHandler interface to make the method deleteObjects(String[] signedDeleteURLs) available");
        }

        final List objectsToDeleteList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        DeleteObjectRunnable[] runnables = new DeleteObjectRunnable[signedDeleteUrls.length];
        for (int i = 0; i < runnables.length; i++) {
            URL url = new URL(signedDeleteUrls[i]);
            S3Object object = ServiceUtils.buildObjectFromUrl(
                url.getHost(), url.getPath(), s3Service.getEndpoint());
            objectsToDeleteList.add(object);

            runnables[i] = new DeleteObjectRunnable(signedDeleteUrls[i]);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), true)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(DeleteObjectsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                objectsToDeleteList.removeAll(completedResults);
                S3Object[] deletedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                fireServiceEvent(DeleteObjectsEvent.newInProgressEvent(threadWatcher, deletedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Object[] remainingObjects = (S3Object[]) objectsToDeleteList
                    .toArray(new S3Object[objectsToDeleteList.size()]);
                success[0] = false;
                fireServiceEvent(DeleteObjectsEvent.newCancelledEvent(remainingObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(DeleteObjectsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(DeleteObjectsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(DeleteObjectsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Creates multiple objects in a bucket using a pre-signed PUT URL for each object.
     * <p>
     * Uploads using signed PUT URLs can be performed without the underlying S3Service knowing
     * the AWSCredentials for the target S3 account, however the underlying service must implement
     * the {@link SignedUrlHandler} interface.
     * <p>
     * This method sends {@link CreateObjectsEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.max-thread-count</tt>.
     *
     * @param signedPutUrlAndObjects
     * packages containing the S3Object to upload and the corresponding signed PUT URL.
     *
     * @throws IllegalStateException
     * if the underlying S3Service does not implement {@link SignedUrlHandler}
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean putObjects(final SignedUrlAndObject[] signedPutUrlAndObjects) {
        if (!(s3Service instanceof SignedUrlHandler)) {
            throw new IllegalStateException("S3ServiceMutli's underlying S3Service must implement the"
                + "SignedUrlHandler interface to make the method putObjects(SignedUrlAndObject[] signedPutUrlAndObjects) available");
        }

        final List progressWatchers = new ArrayList();
        final List incompletedObjectsList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Calculate total byte count being transferred.
        S3Object objects[] = new S3Object[signedPutUrlAndObjects.length];
        for (int i = 0; i < signedPutUrlAndObjects.length; i++) {
            objects[i] = signedPutUrlAndObjects[i].getObject();
        }

        // Start all queries in the background.
        SignedPutRunnable[] runnables = new SignedPutRunnable[signedPutUrlAndObjects.length];
        for (int i = 0; i < runnables.length; i++) {
            BytesProgressWatcher progressMonitor = new BytesProgressWatcher(objects[i].getContentLength());
            progressWatchers.add(progressMonitor);
            incompletedObjectsList.add(signedPutUrlAndObjects[i].getObject());
            runnables[i] = new SignedPutRunnable(signedPutUrlAndObjects[i], progressMonitor);
        }

        // Wait for threads to finish, or be cancelled.
        ThreadWatcher threadWatcher = new ThreadWatcher(
            (BytesProgressWatcher[]) progressWatchers.toArray(new BytesProgressWatcher[progressWatchers.size()]));
        (new ThreadGroupManager(runnables, threadWatcher,
            this.s3Service.getJetS3tProperties(), false)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(CreateObjectsEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                incompletedObjectsList.removeAll(completedResults);
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                fireServiceEvent(CreateObjectsEvent.newInProgressEvent(threadWatcher, completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                S3Object[] incompletedObjects = (S3Object[]) incompletedObjectsList
                    .toArray(new S3Object[incompletedObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(CreateObjectsEvent.newCancelledEvent(incompletedObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(CreateObjectsEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(CreateObjectsEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(CreateObjectsEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    /**
     * Retrieves ACL information about multiple objects from a bucket using signed GET ACL URLs
     * corresponding to those objects.
     * The S3 objects are represented as signed URLs.
     * <p>
     * Object retrieval using signed GET URLs can be performed without the underlying S3Service knowing
     * the AWSCredentials for the target S3 account, however the underlying service must implement
     * the {@link SignedUrlHandler} interface.
     * <p>
     * This method sends {@link LookupACLEvent} notification events.
     * <p>
     * The maximum number of threads is controlled by the JetS3t configuration property
     * <tt>s3service.max-thread-count</tt>.
     *
     * @param signedAclURLs
     * signed GET URL strings corresponding to the objects to be queried.
     *
     * @throws IllegalStateException
     * if the underlying S3Service does not implement {@link SignedUrlHandler}
     *
     * @return
     * true if all the threaded tasks completed successfully, false otherwise.
     */
    public boolean getObjectsACLs(final String[] signedAclURLs) throws MalformedURLException, UnsupportedEncodingException {
        if (!(s3Service instanceof SignedUrlHandler)) {
            throw new IllegalStateException("S3ServiceMutli's underlying S3Service must implement the"
                + "SignedUrlHandler interface to make the method getObjects(String[] signedGetURLs) available");
        }

        final List pendingObjectKeysList = new ArrayList();
        final Object uniqueOperationId = new Object(); // Special object used to identify this operation.
        final boolean[] success = new boolean[] {true};

        // Start all queries in the background.
        GetACLRunnable[] runnables = new GetACLRunnable[signedAclURLs.length];
        for (int i = 0; i < runnables.length; i++) {
            URL url = new URL(signedAclURLs[i]);
            S3Object object = ServiceUtils.buildObjectFromUrl(
                url.getHost(), url.getPath(),s3Service.getEndpoint());
            pendingObjectKeysList.add(object);

            runnables[i] = new GetACLRunnable(signedAclURLs[i]);
        }

        // Wait for threads to finish, or be cancelled.
        (new ThreadGroupManager(runnables, new ThreadWatcher(runnables.length),
            this.s3Service.getJetS3tProperties(), false)
        {
            @Override
            public void fireStartEvent(ThreadWatcher threadWatcher) {
                fireServiceEvent(LookupACLEvent.newStartedEvent(threadWatcher, uniqueOperationId));
            }
            @Override
            public void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults) {
                S3Object[] completedObjects = (S3Object[]) completedResults
                    .toArray(new S3Object[completedResults.size()]);
                for (int i = 0; i < completedObjects.length; i++) {
                    pendingObjectKeysList.remove(completedObjects[i].getKey());
                }
                fireServiceEvent(LookupACLEvent.newInProgressEvent(threadWatcher, completedObjects, uniqueOperationId));
            }
            @Override
            public void fireCancelEvent() {
                List cancelledObjectsList = new ArrayList();
                Iterator iter = pendingObjectKeysList.iterator();
                while (iter.hasNext()) {
                    cancelledObjectsList.add(iter.next());
                }
                S3Object[] cancelledObjects = (S3Object[]) cancelledObjectsList
                    .toArray(new S3Object[cancelledObjectsList.size()]);
                success[0] = false;
                fireServiceEvent(LookupACLEvent.newCancelledEvent(cancelledObjects, uniqueOperationId));
            }
            @Override
            public void fireCompletedEvent() {
                fireServiceEvent(LookupACLEvent.newCompletedEvent(uniqueOperationId));
            }
            @Override
            public void fireErrorEvent(Throwable throwable) {
                success[0] = false;
                fireServiceEvent(LookupACLEvent.newErrorEvent(throwable, uniqueOperationId));
            }
            @Override
            public void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors) {
                success[0] = false;
                fireServiceEvent(LookupACLEvent.newIgnoredErrorsEvent(threadWatcher, ignoredErrors, uniqueOperationId));
            }
        }).run();

        return success[0];
    }

    ///////////////////////////////////////////////
    // Private classes used by the methods above //
    ///////////////////////////////////////////////

    /**
     * All the operation threads used by this service extend this class, which provides common
     * methods used to retrieve the result object from a completed thread (via {@link #getResult()}
     * or force a thread to be interrupted (via {@link #forceInterrupt}.
     */
    private abstract class AbstractRunnable implements Runnable {

        public abstract Object getResult();

        public abstract void forceInterruptCalled();

        protected void forceInterrupt() {
            forceInterruptCalled();
        }
    }

    /**
     * Thread for performing the update/set of Access Control List information for an object.
     */
    private class PutACLRunnable extends AbstractRunnable {
        private S3Bucket bucket = null;
        private S3Object s3Object = null;
        private String signedUrl = null;
        private AccessControlList signedUrlAcl = null;
        private Object result = null;

        public PutACLRunnable(S3Bucket bucket, S3Object s3Object) {
            this.bucket = bucket;
            this.s3Object = s3Object;
        }

        public PutACLRunnable(String signedAclUrl, AccessControlList signedUrlAcl) {
            this.signedUrl = signedAclUrl;
            this.signedUrlAcl = signedUrlAcl;
            this.bucket = null;
            this.s3Object = null;
        }

        public void run() {
            try {
                if (signedUrl == null) {
                    if (s3Object == null) {
                        s3Service.putBucketAcl(bucket);
                    } else {
                        s3Service.putObjectAcl(bucket, s3Object);
                    }
                    result = s3Object;
                } else {
                    SignedUrlHandler handler = s3Service;
                    handler.putObjectAclWithSignedUrl(signedUrl, signedUrlAcl);
                    URL url = new URL(signedUrl);
                    S3Object object = ServiceUtils.buildObjectFromUrl(
                        url.getHost(), url.getPath(), s3Service.getEndpoint());
                    object.setAcl(signedUrlAcl);
                    result = object;
                }
            } catch (RuntimeException e) {
                result = e;
                throw e;
            } catch (Exception e) {
                result = e;
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            // This is an atomic operation, cannot interrupt. Ignore.
        }
    }

    /**
     * Thread for retrieving Access Control List information for an object.
     */
    private class GetACLRunnable extends AbstractRunnable {
        private S3Bucket bucket = null;
        private S3Object object = null;
        private String signedAclUrl = null;
        private Object result = null;

        public GetACLRunnable(S3Bucket bucket, S3Object object) {
            this.bucket = bucket;
            this.object = object;
        }

        public GetACLRunnable(String signedAclUrl) {
            this.signedAclUrl = signedAclUrl;
            this.bucket = null;
            this.object = null;
        }

        public void run() {
            try {
                if (signedAclUrl == null) {
                    AccessControlList acl = s3Service.getObjectAcl(bucket, object.getKey());
                    object.setAcl(acl);
                    result = object;
                } else {
                    SignedUrlHandler handler = s3Service;
                    AccessControlList acl = handler.getObjectAclWithSignedUrl(signedAclUrl);
                    URL url = new URL(signedAclUrl);
                    object = ServiceUtils.buildObjectFromUrl(
                        url.getHost(), url.getPath(), s3Service.getEndpoint());
                    object.setAcl(acl);
                    result = object;
                }
            } catch (RuntimeException e) {
                result = e;
                throw e;
            } catch (Exception e) {
                result = e;
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            // This is an atomic operation, cannot interrupt. Ignore.
        }
    }

    /**
     * Thread for deleting an object.
     */
    private class DeleteObjectRunnable extends AbstractRunnable {
        private S3Bucket bucket = null;
        private S3Object object = null;
        private String signedDeleteUrl = null;
        private Object result = null;

        public DeleteObjectRunnable(S3Bucket bucket, S3Object object) {
            this.signedDeleteUrl = null;
            this.bucket = bucket;
            this.object = object;
        }

        public DeleteObjectRunnable(String signedDeleteUrl) {
            this.signedDeleteUrl = signedDeleteUrl;
            this.bucket = null;
            this.object = null;
        }

        public void run() {
            try {
                if (signedDeleteUrl == null) {
                    s3Service.deleteObject(bucket, object.getKey());
                    result = object;
                } else {
                    SignedUrlHandler handler = s3Service;
                    handler.deleteObjectWithSignedUrl(signedDeleteUrl);
                    URL url = new URL(signedDeleteUrl);
                    result = ServiceUtils.buildObjectFromUrl(
                        url.getHost(), url.getPath(), s3Service.getEndpoint());
                }
            } catch (RuntimeException e) {
                result = e;
                throw e;
            } catch (Exception e) {
                result = e;
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            // This is an atomic operation, cannot interrupt. Ignore.
        }
    }

    /**
     * Thread for deleting a versioned object.
     */
    private class DeleteVersionedObjectRunnable extends AbstractRunnable {
        private String versionId = null;
        private String multiFactorSerialNumber = null;
        private String multiFactorAuthCode = null;
        private String bucketName = null;
        private String objectKey = null;
        private Object result = null;

        public DeleteVersionedObjectRunnable(String versionId,
            String multiFactorSerialNumber, String multiFactorAuthCode,
            String bucketName, String objectKey)
        {
            this.versionId = versionId;
            this.multiFactorSerialNumber = multiFactorSerialNumber;
            this.multiFactorAuthCode = multiFactorAuthCode;
            this.bucketName = bucketName;
            this.objectKey = objectKey;
        }

        public void run() {
            try {
                s3Service.deleteVersionedObjectWithMFA(versionId,
                    multiFactorSerialNumber, multiFactorAuthCode,
                    bucketName, objectKey);
                result = new S3Version(objectKey, versionId);
            } catch (RuntimeException e) {
                result = e;
                throw e;
            } catch (Exception e) {
                result = e;
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            // This is an atomic operation, cannot interrupt. Ignore.
        }
    }
    /**
     * Thread for creating a bucket.
     */
    private class CreateBucketRunnable extends AbstractRunnable {
        private S3Bucket bucket = null;
        private Object result = null;

        public CreateBucketRunnable(S3Bucket bucket) {
            this.bucket = bucket;
        }

        public void run() {
            try {
                result = s3Service.createBucket(bucket);
            } catch (S3ServiceException e) {
                result = e;
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            // This is an atomic operation, cannot interrupt. Ignore.
        }
    }

    /**
     * Thread for listing the objects in a bucket.
     */
    private class ListObjectsRunnable extends AbstractRunnable {
        private Object result = null;
        private String bucketName = null;
        private String prefix = null;
        private String delimiter = null;
        private long maxListingLength = 1000;
        private String priorLastKey = null;
        private boolean halted = false;

        public ListObjectsRunnable(String bucketName, String prefix,
            String delimiter, long maxListingLength, String priorLastKey)
        {
            this.bucketName = bucketName;
            this.prefix = prefix;
            this.delimiter = delimiter;
            this.maxListingLength = maxListingLength;
            this.priorLastKey = priorLastKey;
        }

        public void run() {
            try {
                List allObjects = new ArrayList();
                List allCommonPrefixes = new ArrayList();

                do {
                    StorageObjectsChunk chunk = s3Service.listObjectsChunked(
                        bucketName, prefix, delimiter, maxListingLength, priorLastKey);
                    priorLastKey = chunk.getPriorLastKey();

                    allObjects.addAll(Arrays.asList(chunk.getObjects()));
                    allCommonPrefixes.addAll(Arrays.asList(chunk.getCommonPrefixes()));
                } while (!halted && priorLastKey != null);

                result = new S3ObjectsChunk(
                    prefix, delimiter,
                    (S3Object[]) allObjects.toArray(new S3Object[allObjects.size()]),
                    (String[]) allCommonPrefixes.toArray(new String[allCommonPrefixes.size()]),
                    null);
            } catch (ServiceException se) {
                result = new S3ServiceException(se);
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            halted = true;
        }
    }

    /**
     * Thread for creating/uploading an object. The upload of any object data is monitored with a
     * {@link ProgressMonitoredInputStream} and can be can cancelled as the input stream is wrapped in
     * an {@link InterruptableInputStream}.
     */
    private class CreateObjectRunnable extends AbstractRunnable {
        private S3Bucket bucket = null;
        private S3Object s3Object = null;
        private InterruptableInputStream interruptableInputStream = null;
        private BytesProgressWatcher progressMonitor = null;

        private Object result = null;

        public CreateObjectRunnable(S3Bucket bucket, S3Object s3Object, BytesProgressWatcher progressMonitor) {
            this.bucket = bucket;
            this.s3Object = s3Object;
            this.progressMonitor = progressMonitor;
        }

        public void run() {
            try {
                File underlyingFile = s3Object.getDataInputFile();

                if (s3Object.getDataInputStream() != null) {
                    interruptableInputStream = new InterruptableInputStream(s3Object.getDataInputStream());
                    ProgressMonitoredInputStream pmInputStream = new ProgressMonitoredInputStream(
                        interruptableInputStream, progressMonitor);
                    s3Object.setDataInputStream(pmInputStream);
                }
                result = s3Service.putObject(bucket, s3Object);

                if (underlyingFile instanceof TempFile) {
                    underlyingFile.delete();
                }
            } catch (ServiceException se) {
                result = new S3ServiceException(se);
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            if (interruptableInputStream != null) {
                interruptableInputStream.interrupt();
            }
        }
    }

    /**
     * Thread for copying an object.
     */
    private class CopyObjectRunnable extends AbstractRunnable {
        private String sourceBucketName = null;
        private String destinationBucketName = null;
        private String sourceObjectKey = null;
        private S3Object destinationObject = null;
        private boolean replaceMetadata = false;

        private Object result = null;

        public CopyObjectRunnable(String sourceBucketName, String destinationBucketName,
            String sourceObjectKey, S3Object destinationObject, boolean replaceMetadata)
        {
            this.sourceBucketName = sourceBucketName;
            this.destinationBucketName = destinationBucketName;
            this.sourceObjectKey = sourceObjectKey;
            this.destinationObject = destinationObject;
            this.replaceMetadata = replaceMetadata;
        }

        public void run() {
            try {
                result = s3Service.copyObject(sourceBucketName, sourceObjectKey,
                    destinationBucketName, destinationObject, replaceMetadata);
            } catch (ServiceException se) {
                result = new S3ServiceException(se);
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            // This is an atomic operation, cannot interrupt. Ignore.
        }
    }

    /**
     * Thread for retrieving an object.
     */
    private class GetObjectRunnable extends AbstractRunnable {
        private S3Bucket bucket = null;
        private String objectKey = null;
        private String signedGetOrHeadUrl = null;
        private boolean headOnly = false;

        private Object result = null;

        public GetObjectRunnable(S3Bucket bucket, String objectKey, boolean headOnly) {
            this.signedGetOrHeadUrl = null;
            this.bucket = bucket;
            this.objectKey = objectKey;
            this.headOnly = headOnly;
        }

        public GetObjectRunnable(String signedGetOrHeadUrl, boolean headOnly) {
            this.signedGetOrHeadUrl = signedGetOrHeadUrl;
            this.bucket = null;
            this.objectKey = null;
            this.headOnly = headOnly;
        }

        public void run() {
            try {
                if (headOnly) {
                    if (signedGetOrHeadUrl == null) {
                        result = s3Service.getObjectDetails(bucket, objectKey);
                    } else {
                        SignedUrlHandler handler = s3Service;
                        result = handler.getObjectDetailsWithSignedUrl(signedGetOrHeadUrl);
                    }
                } else {
                    if (signedGetOrHeadUrl == null) {
                        result = s3Service.getObject(bucket, objectKey);
                    } else {
                        SignedUrlHandler handler = s3Service;
                        result = handler.getObjectWithSignedUrl(signedGetOrHeadUrl);
                    }
                }
            } catch (ServiceException se) {
                result = new S3ServiceException(se);
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            // This is an atomic operation, cannot interrupt. Ignore.
        }
    }

    /**
     * Thread for downloading an object. The download of any object data is monitored with a
     * {@link ProgressMonitoredInputStream} and can be can cancelled as the input stream is wrapped in
     * an {@link InterruptableInputStream}.
     */
    private class DownloadObjectRunnable extends AbstractRunnable {
        private String objectKey = null;
        private S3Bucket bucket = null;
        private DownloadPackage downloadPackage = null;
        private InterruptableInputStream interruptableInputStream = null;
        private BytesProgressWatcher progressMonitor = null;
        private boolean restoreLastModifiedDate = true;

        private Object result = null;

        public DownloadObjectRunnable(S3Bucket bucket, String objectKey, DownloadPackage downloadPackage,
            BytesProgressWatcher progressMonitor, boolean restoreLastModifiedDate)
        {
            this.bucket = bucket;
            this.objectKey = objectKey;
            this.downloadPackage = downloadPackage;
            this.progressMonitor = progressMonitor;
            this.restoreLastModifiedDate = restoreLastModifiedDate;
        }

        public DownloadObjectRunnable(DownloadPackage downloadPackage, BytesProgressWatcher progressMonitor,
            boolean restoreLastModifiedDate)
        {
            this.downloadPackage = downloadPackage;
            this.progressMonitor = progressMonitor;
            this.restoreLastModifiedDate = restoreLastModifiedDate;
        }

        public void run() {
            BufferedInputStream bufferedInputStream = null;
            BufferedOutputStream bufferedOutputStream = null;
            S3Object object = null;

            try {
                if (!downloadPackage.isSignedDownload()) {
                    object = s3Service.getObject(bucket, objectKey);
                } else {
                    SignedUrlHandler handler = s3Service;
                    object = handler.getObjectWithSignedUrl(downloadPackage.getSignedUrl());
                }

                // Replace the S3 object in the download package with the downloaded version to make metadata available.
                downloadPackage.setObject(object);

                // Setup monitoring of stream bytes transferred.
                interruptableInputStream = new InterruptableInputStream(object.getDataInputStream());
                bufferedInputStream = new BufferedInputStream(
                    new ProgressMonitoredInputStream(interruptableInputStream, progressMonitor));

                bufferedOutputStream = new BufferedOutputStream(
                    downloadPackage.getOutputStream());

                MessageDigest messageDigest = null;
                try {
                    messageDigest = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("Unable to calculate MD5 hash of data received as algorithm is not available", e);
                    }
                }

                try {
                    byte[] buffer = new byte[1024];
                    int byteCount = -1;

                    while ((byteCount = bufferedInputStream.read(buffer)) != -1) {
                        bufferedOutputStream.write(buffer, 0, byteCount);

                        if (messageDigest != null) {
                            messageDigest.update(buffer, 0, byteCount);
                        }
                    }

                    // Check that actual bytes received match expected hash value
                    if (messageDigest != null) {
                        byte[] dataMD5Hash = messageDigest.digest();
                        String hexMD5OfDownloadedData = ServiceUtils.toHex(dataMD5Hash);

                        // Don't check MD5 hash against ETag if ETag doesn't look like an MD5 value
                        if (!ServiceUtils.isEtagAlsoAnMD5Hash(object.getETag())) {
                            // Use JetS3t's own MD5 hash metadata value for comparison, if it's available
                            if (!hexMD5OfDownloadedData.equals(object.getMd5HashAsHex())) {
                                if (log.isWarnEnabled()) {
                                    log.warn("Unable to verify MD5 hash of downloaded data against"
                                        + " ETag returned by service because ETag value \""
                                        + object.getETag() + "\" is not an MD5 hash value"
                                        + ", for object key: " + object.getKey());
                                }
                            }
                        } else {
                            if (!hexMD5OfDownloadedData.equals(object.getETag())) {
                                throw new S3ServiceException("Mismatch between MD5 hash of downloaded data ("
                                    + hexMD5OfDownloadedData + ") and ETag returned by S3 ("
                                    + object.getETag() + ") for object key: "
                                    + object.getKey());
                            } else {
                                if (log.isDebugEnabled()) {
                                    log.debug("Object download was automatically verified, the calculated MD5 hash "+
                                        "value matched the ETag provided by S3: " + object.getKey());
                                }
                            }
                        }
                    }

                } finally {
                    if (bufferedOutputStream != null) {
                        bufferedOutputStream.close();
                    }
                    if (bufferedInputStream != null) {
                        bufferedInputStream.close();
                    }
                }

                object.setDataInputStream(null);
                object.setDataInputFile(downloadPackage.getDataFile());

                // If data was downloaded to a file, set the file's Last Modified date
                // to the original last modified date metadata stored with the object.
                if (restoreLastModifiedDate && downloadPackage.getDataFile() != null) {
                    String metadataLocalFileDate = (String) object.getMetadata(
                        Constants.METADATA_JETS3T_LOCAL_FILE_DATE);

                    if (metadataLocalFileDate != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Restoring original Last Modified date for object '"
                                + object.getKey() + "' to file '" + downloadPackage.getDataFile()
                                + "': " + metadataLocalFileDate);
                        }
                        downloadPackage.getDataFile().setLastModified(
                            ServiceUtils.parseIso8601Date(metadataLocalFileDate).getTime());
                    }
                }

                result = object;
            } catch (Throwable t) {
                result = t;
            } finally {
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("Unable to close Object input stream", e);
                        }
                    }
                }
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("Unable to close download output stream", e);
                        }
                    }
                }
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            if (interruptableInputStream != null) {
                interruptableInputStream.interrupt();
            }
        }
    }

    /**
     * Thread for creating/uploading an object using a pre-signed PUT URL. The upload of any object
     * data is monitored with a {@link ProgressMonitoredInputStream} and can be can cancelled as
     * the input stream is wrapped in an {@link InterruptableInputStream}.
     */
    private class SignedPutRunnable extends AbstractRunnable {
        private SignedUrlAndObject signedUrlAndObject = null;
        private InterruptableInputStream interruptableInputStream = null;
        private BytesProgressWatcher progressMonitor = null;

        private Object result = null;

        public SignedPutRunnable(SignedUrlAndObject signedUrlAndObject, BytesProgressWatcher progressMonitor) {
            this.signedUrlAndObject = signedUrlAndObject;
            this.progressMonitor = progressMonitor;
        }

        public void run() {
            try {
                File underlyingFile = signedUrlAndObject.getObject().getDataInputFile();

                if (signedUrlAndObject.getObject().getDataInputStream() != null) {
                    interruptableInputStream = new InterruptableInputStream(
                        signedUrlAndObject.getObject().getDataInputStream());
                    ProgressMonitoredInputStream pmInputStream = new ProgressMonitoredInputStream(
                        interruptableInputStream, progressMonitor);
                    signedUrlAndObject.getObject().setDataInputStream(pmInputStream);
                }
                SignedUrlHandler signedPutUploader = s3Service;
                result = signedPutUploader.putObjectWithSignedUrl(
                    signedUrlAndObject.getSignedUrl(), signedUrlAndObject.getObject());

                if (underlyingFile instanceof TempFile) {
                    underlyingFile.delete();
                }
            } catch (ServiceException se) {
                result = new S3ServiceException(se);
            } finally {
                try {
                    signedUrlAndObject.getObject().closeDataInputStream();
                } catch (IOException e) {
                    if (log.isErrorEnabled()) {
                        log.error("Unable to close Object's input stream", e);
                    }
                }
            }
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void forceInterruptCalled() {
            if (interruptableInputStream != null) {
                interruptableInputStream.interrupt();
            }
        }
    }



    /**
     * The thread group manager is responsible for starting, running and stopping the set of threads
     * required to perform an S3 operation.
     * <p>
     * The manager starts all the threads, monitors their progress and stops threads when they are
     * cancelled or an error occurs - all the while firing the appropriate {@link ServiceEvent} event
     * notifications.
     */
    private abstract class ThreadGroupManager {
        private final Log log = LogFactory.getLog(ThreadGroupManager.class);
        private int maxThreadCount = 1;

        /**
         * the set of runnable objects to execute.
         */
        private AbstractRunnable[] runnables = null;

        /**
         * Thread objects that are currently running, where the index corresponds to the
         * runnables index. Any AbstractThread runnable that is not started, or has completed,
         * will have a null value in this array.
         */
        private Thread[] threads = null;

        private boolean ignoreExceptions = false;

        /**
         * set of flags indicating which runnable items have been started
         */
        private boolean started[] = null;

        /**
         * set of flags indicating which threads have already had In Progress events fired on
         * their behalf. These threads have finished running.
         */
        private boolean alreadyFired[] = null;

        private ThreadWatcher threadWatcher = null;

        private long lastProgressEventFiredTime = 0;


        public ThreadGroupManager(AbstractRunnable[] runnables,
            ThreadWatcher threadWatcher, Jets3tProperties jets3tProperties,
            boolean isAdminTask)
        {
            this.runnables = runnables;
            this.threadWatcher = threadWatcher;
            if (isAdminTask) {
                this.maxThreadCount = jets3tProperties
                    .getIntProperty("s3service.admin-max-thread-count", 20);
            } else {
                this.maxThreadCount = jets3tProperties
                    .getIntProperty("s3service.max-thread-count", 2);
            }
            this.ignoreExceptions = jets3tProperties
                .getBoolProperty("s3service.ignore-exceptions-in-multi", false);

            this.threads = new Thread[runnables.length];
            started = new boolean[runnables.length]; // All values initialized to false.
            alreadyFired = new boolean[runnables.length]; // All values initialized to false.
        }

        /**
         * Determine which threads, if any, have finished since the last time an In Progress event
         * was fired.
         *
         * @return
         * a list of the threads that finished since the last In Progress event was fired. This list may
         * be empty.
         *
         * @throws Throwable
         */
        private ResultsTuple getNewlyCompletedResults() throws Throwable
        {
            ArrayList completedResults = new ArrayList();
            ArrayList errorResults = new ArrayList();

            for (int i = 0; i < threads.length; i++) {
                if (!alreadyFired[i] && started[i] && !threads[i].isAlive()) {
                    alreadyFired[i] = true;
                    if (log.isDebugEnabled()) {
                        log.debug("Thread " + (i+1) + " of " + threads.length
                            + " has recently completed, releasing resources");
                    }

                    if (runnables[i].getResult() instanceof Throwable) {
                        Throwable throwable = (Throwable) runnables[i].getResult();
                        runnables[i] = null;
                        threads[i] = null;

                        if (ignoreExceptions) {
                            // Ignore exceptions
                            if (log.isWarnEnabled()) {
                                log.warn("Ignoring exception (property " +
                                        "s3service.ignore-exceptions-in-multi is set to true)",
                                        throwable);
                            }
                            errorResults.add(throwable);
                        } else {
                            throw throwable;
                        }
                    } else {
                        completedResults.add(runnables[i].getResult());
                        runnables[i] = null;
                        threads[i] = null;
                    }
                }
            }

            Throwable[] ignoredErrors = new Throwable[] {};
            if (errorResults.size() > 0) {
                ignoredErrors = (Throwable[]) errorResults.toArray(new Throwable[errorResults.size()]);
            }

            return new ResultsTuple(completedResults, ignoredErrors);
        }

        /**
         * Starts pending threads such that the total of running threads never exceeds the
         * maximum count set in the jets3t property <i>s3service.max-thread-count</i>.
         *
         * @throws Throwable
         */
        private void startPendingThreads()
            throws Throwable
        {
            // Count active threads that are running (i.e. have been started but final event not fired)
            int runningThreadCount = 0;
            for (int i = 0; i < runnables.length; i++) {
                if (started[i] && !alreadyFired[i]) {
                    runningThreadCount++;
                }
            }

            // Start threads until we are running the maximum number allowed.
            for (int i = 0; runningThreadCount < maxThreadCount && i < started.length; i++) {
                if (!started[i]) {
                    threads[i] = new Thread(runnables[i]);
                    threads[i].start();
                    started[i] = true;
                    runningThreadCount++;
                    if (log.isDebugEnabled()) {
                        log.debug("Thread " + (i+1) + " of " + runnables.length + " has started");
                    }
                }
            }
        }

        /**
         * @return
         * the number of threads that have not finished running (sum of those currently running, and those awaiting start)
         */
        private int getPendingThreadCount() {
            int pendingThreadCount = 0;
            for (int i = 0; i < runnables.length; i++) {
                if (!alreadyFired[i]) {
                    pendingThreadCount++;
                }
            }
            return pendingThreadCount;
        }

        /**
         * Invokes the {@link AbstractRunnable#forceInterrupt} on all threads being managed.
         *
         */
        private void forceInterruptAllRunnables() {
            if (log.isDebugEnabled()) {
                log.debug("Setting force interrupt flag on all runnables");
            }
            for (int i = 0; i < runnables.length; i++) {
                if (runnables[i] != null) {
                    runnables[i].forceInterrupt();
                    runnables[i] = null;
                }
            }
        }

        /**
         * Runs and manages all the threads involved in an S3 multi-operation.
         *
         */
        public void run() {
            if (log.isDebugEnabled()) {
                log.debug("Started ThreadManager");
            }

            final boolean[] interrupted = new boolean[] { false };

            /*
             * Create a cancel event trigger, so all the managed threads can be cancelled if required.
             */
            final CancelEventTrigger cancelEventTrigger = new CancelEventTrigger() {
                private static final long serialVersionUID = 6328417466929608235L;

                public void cancelTask(Object eventSource) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cancel task invoked on ThreadManager");
                    }

                    // Flag that this ThreadManager class should shutdown.
                    interrupted[0] = true;

                    // Set force interrupt flag for all runnables.
                    forceInterruptAllRunnables();
                }
            };

            // Actual thread management happens in the code block below.
            try {
                // Start some threads
                startPendingThreads();

                threadWatcher.updateThreadsCompletedCount(0, cancelEventTrigger);
                fireStartEvent(threadWatcher);

                // Loop while threads haven't been interrupted/cancelled, and at least one thread is
                // still active (ie hasn't finished its work)
                while (!interrupted[0] && getPendingThreadCount() > 0) {
                    try {
                        // Shut down threads if this service has been shutdown.
                        if (isShutdown[0]) {
                            throw new InterruptedException("S3ServiceMulti#shutdown method invoked");
                        }

                        Thread.sleep(100);

                        if (interrupted[0]) {
                            // Do nothing, we've been interrupted during sleep.
                        } else {
                            if (System.currentTimeMillis() - lastProgressEventFiredTime > sleepTime) {
                                // Fire progress event.
                                int completedThreads = runnables.length - getPendingThreadCount();
                                threadWatcher.updateThreadsCompletedCount(completedThreads, cancelEventTrigger);
                                ResultsTuple results = getNewlyCompletedResults();

                                lastProgressEventFiredTime = System.currentTimeMillis();
                                fireProgressEvent(threadWatcher, results.completedResults);

                                if (results.errorResults.length > 0) {
                                    fireIgnoredErrorsEvent(threadWatcher, results.errorResults);
                                }
                            }

                            // Start more threads.
                            startPendingThreads();
                        }
                    } catch (InterruptedException e) {
                        interrupted[0] = true;
                        forceInterruptAllRunnables();
                    }
                }

                if (interrupted[0]) {
                    fireCancelEvent();
                } else {
                    int completedThreads = runnables.length - getPendingThreadCount();
                    threadWatcher.updateThreadsCompletedCount(completedThreads, cancelEventTrigger);
                    ResultsTuple results = getNewlyCompletedResults();

                    fireProgressEvent(threadWatcher, results.completedResults);
                    if (results.completedResults.size() > 0) {
                        if (log.isDebugEnabled()) {
                            log.debug(results.completedResults.size() + " threads have recently completed");
                        }
                    }

                    if (results.errorResults.length > 0) {
                        fireIgnoredErrorsEvent(threadWatcher, results.errorResults);
                    }

                    fireCompletedEvent();
                }
            } catch (Throwable t) {
                if (log.isErrorEnabled()) {
                    log.error("A thread failed with an exception. Firing ERROR event and cancelling all threads", t);
                }
                // Set force interrupt flag for all runnables.
                forceInterruptAllRunnables();

                fireErrorEvent(t);
            }
        }

        public abstract void fireStartEvent(ThreadWatcher threadWatcher);

        public abstract void fireProgressEvent(ThreadWatcher threadWatcher, List completedResults);

        public abstract void fireCompletedEvent();

        public abstract void fireCancelEvent();

        public abstract void fireErrorEvent(Throwable t);

        public abstract void fireIgnoredErrorsEvent(ThreadWatcher threadWatcher, Throwable[] ignoredErrors);

        private class ResultsTuple {
            public List completedResults = null;
            public Throwable[] errorResults = null;

            public ResultsTuple(List completedResults, Throwable[] errorResults) {
                this.completedResults = completedResults;
                this.errorResults = errorResults;
            }
        }
    }

}
