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

import org.jets3t.service.multi.event.CopyObjectsEvent;
import org.jets3t.service.multi.event.CreateBucketsEvent;
import org.jets3t.service.multi.event.CreateObjectsEvent;
import org.jets3t.service.multi.event.DeleteObjectsEvent;
import org.jets3t.service.multi.event.DownloadObjectsEvent;
import org.jets3t.service.multi.event.GetObjectHeadsEvent;
import org.jets3t.service.multi.event.GetObjectsEvent;
import org.jets3t.service.multi.event.ListObjectsEvent;
import org.jets3t.service.multi.event.LookupACLEvent;
import org.jets3t.service.multi.event.UpdateACLEvent;

/**
 * Listener for events produced by {@link ThreadedStorageService}.
 *
 * @author James Murty
 */
public interface StorageServiceEventListener {

    public void event(ListObjectsEvent event);

    public void event(CreateObjectsEvent event);

    public void event(CopyObjectsEvent event);

    public void event(CreateBucketsEvent event);

    public void event(DeleteObjectsEvent event);

    public void event(GetObjectsEvent event);

    public void event(GetObjectHeadsEvent event);

    public void event(LookupACLEvent event);

    public void event(UpdateACLEvent event);

    public void event(DownloadObjectsEvent event);

}
