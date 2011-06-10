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
package org.jets3t.service.multi.s3;

import org.jets3t.service.multi.StorageServiceEventAdaptor;
import org.jets3t.service.multi.ThreadedStorageService;


/**
 * Simple implementation of {@link S3ServiceEventListener} to listen for events produced by
 * {@link ThreadedS3Service} and {@link ThreadedStorageService}.
 * <p>
 * By default this adaptor does nothing but store the first Error event it comes across, if any,
 * and make it available through {@link #getErrorThrown()}.
 * </p>
 * <p>
 * The behaviour of this class can be specialised by over-riding the appropriate
 * <tt>event</tt> methods, though always be sure to call the <b>super</b>
 * version of these methods if you are relying on the default error-trapping functions of this
 * class.
 * </p>
 *
 * @author James Murty
 */
public class S3ServiceEventAdaptor extends StorageServiceEventAdaptor
                                implements S3ServiceEventListener
{

    public void event(MultipartUploadsEvent event) {
        storeThrowable(event);
    }

    public void event(MultipartStartsEvent event) {
        storeThrowable(event);
    }

    public void event(MultipartCompletesEvent event) {
        storeThrowable(event);
    }

}
