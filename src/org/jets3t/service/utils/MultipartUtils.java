/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2011 James Murty
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
package org.jets3t.service.utils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.io.SegmentedRepeatableFileInputStream;
import org.jets3t.service.model.MultipartUpload;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.multi.StorageServiceEventAdaptor;
import org.jets3t.service.multi.event.ServiceEvent;
import org.jets3t.service.multi.s3.MultipartStartsEvent;
import org.jets3t.service.multi.s3.MultipartUploadAndParts;
import org.jets3t.service.multi.s3.S3ServiceEventAdaptor;
import org.jets3t.service.multi.s3.S3ServiceEventListener;
import org.jets3t.service.multi.s3.ThreadedS3Service;

/**
 * Tool to simplify working with the multipart uploads feature offered by
 * Amazon S3.
 *
 * @author jmurty
 */
public class MultipartUtils {
    private static final Log log = LogFactory.getLog(MultipartUtils.class);

    /**
     * Minimum multipart upload part size supported by S3: 5 MB.
     * NOTE: This minimum size does not apply to the last part in a
     * multipart upload, which may be 1 byte or larger.
     */
    public static final long MIN_PART_SIZE = 5 * (1024 * 1024);

    /**
     * Maximum object size supported by S3: 5 GB
     */
    public static final long MAX_OBJECT_SIZE = 5 * (1024 * 1024 * 1024);


    protected long maxPartSize = MAX_OBJECT_SIZE;


    /**
     * @param maxPartSize
     * the maximum size of objects that will be generated or upload by this instance,
     * must be between {@link #MIN_PART_SIZE} and {@link #MAX_OBJECT_SIZE}.
     */
    public MultipartUtils(long maxPartSize) {
        if (maxPartSize < MIN_PART_SIZE) {
            throw new IllegalArgumentException("Maximum part size parameter " + maxPartSize
                + " is less than the minimum legal part size " + MIN_PART_SIZE);
        }
        if (maxPartSize > MAX_OBJECT_SIZE) {
            throw new IllegalArgumentException("Maximum part size parameter " + maxPartSize
                + " is greater than the maximum legal upload object size " + MAX_OBJECT_SIZE);
        }
        this.maxPartSize = maxPartSize;
    }

    /**
     * Use default value for maximum part size: {@link #MAX_OBJECT_SIZE}.
     */
    public MultipartUtils() {
    }

    /**
     * @return
     * maximum part size as set in constructor.
     */
    public long getMaxPartSize() {
        return maxPartSize;
    }

    /**
     * @param file
     * @return
     * true if the given file is larger than the maximum part size defined in this instances.
     */
    public boolean isFileLargerThanMaxPartSize(File file) {
        return file.length() > maxPartSize;
    }

    /**
     * Split the given file into objects such that no object has a size greater than
     * the defined maximum part size. Each object uses a
     * {@link SegmentedRepeatableFileInputStream} input stream to manage its own
     * byte range within the underlying file.
     *
     * @param objectKey
     * the object key name to apply to all objects returned by this method.
     * @param file
     * a file to split into multiple parts.
     * @return
     * an ordered list of objects that can be uploaded as multipart parts to S3 to
     * re-constitute the given file in the service.
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public List<S3Object> splitFileIntoObjectsByMaxPartSize(String objectKey, File file)
        throws IOException, NoSuchAlgorithmException
    {
        long fileLength = file.length();
        long partCount = fileLength / maxPartSize + (fileLength % maxPartSize > 0 ? 1 : 0);

        if (log.isDebugEnabled()) {
            log.debug("Splitting file " + file.getAbsolutePath() + " of "
                + fileLength + " bytes into " + partCount
                + " object parts with a maximum part size of " + maxPartSize);
        }

        ArrayList<S3Object> multipartPartList = new ArrayList<S3Object>();
        SegmentedRepeatableFileInputStream segFIS = null;

        for (long offset = 0; offset < partCount; offset++) {
            S3Object object = new S3Object(objectKey);
            if (offset < partCount - 1) {
                object.setContentLength(maxPartSize);
                segFIS = new SegmentedRepeatableFileInputStream(
                    file, offset * maxPartSize, maxPartSize);
            } else {
                // Last part, may not be full size.
                long partLength = fileLength % maxPartSize;
                // Handle edge-case where last part is exactly the size of maxPartSize
                if (partLength == 0) {
                    partLength = maxPartSize;
                }
                object.setContentLength(partLength);
                segFIS = new SegmentedRepeatableFileInputStream(
                    file, offset * maxPartSize, partLength);
            }
            object.setContentLength(segFIS.available());
            object.setDataInputStream(segFIS);

            // Calculate part's MD5 hash and reset stream
            object.setMd5Hash(ServiceUtils.computeMD5Hash(segFIS));
            segFIS.reset();

            multipartPartList.add(object);
        }
        return multipartPartList;
    }

    /**
     * Upload one or more file-based objects to S3 as multipart uploads, where each
     * object's underlying file is split into parts based on the value of
     * {@link #maxPartSize}.
     *
     * Objects are uploaded in parallel using a {@link ThreadedS3Service} class
     * that is created within this method, so uploads will take place using as
     * many connections and threads as are configured in your service's
     * {@link Jets3tProperties}.
     *
     * This method can upload small files that don't need to be split into parts,
     * but because there is extra overhead in performing unnecessary multipart upload
     * operations you should avoid doing so unless it's really necessary.
     *
     * @param bucketName
     * the target bucket name
     * @param s3Service
     * the S3 service that will perform the work
     * @param objectsForMultipartUpload
     * a list of one or more objects that will be uploaded, potentially in multiple
     * parts if the object's underlying file is larger than {@link #maxPartSize}
     * @param eventListener
     * an event listener to monitor progress event notifications, which should
     * recognize and handle error events. May be null, in which case a standard
     * {@link S3ServiceEventAdaptor} is used which won't report on events but will
     * throw an exception if there is a failure.
     *
     * @throws Exception
     */
    public void uploadObjects(String bucketName, S3Service s3Service,
        List<StorageObject> objectsForMultipartUpload,
        S3ServiceEventListener eventListener) throws Exception
    {
        if (objectsForMultipartUpload == null || objectsForMultipartUpload.size() < 1) {
            return;
        }

        final List<MultipartUpload> multipartUploadList =
            new ArrayList<MultipartUpload>();
        final List<MultipartUploadAndParts> uploadAndPartsList =
            new ArrayList<MultipartUploadAndParts>();

        if (eventListener == null) {
            eventListener = new S3ServiceEventAdaptor();
        }

        // Adaptor solely to capture newly-created MultipartUpload objects, which we
        // will need when it comes time to upload parts or complete the uploads.
        StorageServiceEventAdaptor captureMultipartUploadObjectsEventAdaptor =
            new S3ServiceEventAdaptor() {
                @Override
                public void event(MultipartStartsEvent event) {
                    if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                        for (MultipartUpload upload: event.getStartedUploads()) {
                            multipartUploadList.add(upload);
                        }
                    }
                }
            };

        try {
            ThreadedS3Service threadedS3Service =
                new ThreadedS3Service(s3Service, eventListener);
            threadedS3Service.addServiceEventListener(
                captureMultipartUploadObjectsEventAdaptor);

            // Build map from object key to storage object
            final Map<String, StorageObject> objectsByKey =
                new HashMap<String, StorageObject>();
            for (StorageObject object: objectsForMultipartUpload) {
                objectsByKey.put(object.getKey(), object);
            }

            // Start all multipart uploads
            threadedS3Service.multipartStartUploads(bucketName, objectsForMultipartUpload);
            throwServiceEventAdaptorErrorIfPresent(eventListener);

            // Build upload and part lists from new multipart uploads, where new
            // MultipartUpload objects were captured by this method's
            // captureMultipartUploadObjectsEventAdaptor)
            for (MultipartUpload upload: multipartUploadList) {
                StorageObject object = objectsByKey.get(upload.getObjectKey());
                if (object.getDataInputFile() == null) {
                    throw new ServiceException();
                }
                List<S3Object> partObjects = splitFileIntoObjectsByMaxPartSize(
                    upload.getObjectKey(),
                    object.getDataInputFile());
                uploadAndPartsList.add(
                    new MultipartUploadAndParts(upload, partObjects));
            }

            // Upload all parts for all multipart uploads
            threadedS3Service.multipartUploadParts(uploadAndPartsList);
            throwServiceEventAdaptorErrorIfPresent(eventListener);

            // Complete all multipart uploads
            threadedS3Service.multipartCompleteUploads(multipartUploadList);
            throwServiceEventAdaptorErrorIfPresent(eventListener);
        } catch (Exception e) {
            throw new Exception("Multipart upload failed", e);
        }
    }

    protected void throwServiceEventAdaptorErrorIfPresent(
        S3ServiceEventListener eventListener) throws Exception
    {
        if (eventListener instanceof S3ServiceEventAdaptor) {
            ((S3ServiceEventAdaptor)eventListener).throwErrorIfPresent();
        }
    }

}
