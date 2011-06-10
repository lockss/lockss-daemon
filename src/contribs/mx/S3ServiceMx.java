/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2009 Doug MacEachern
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

import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

public class S3ServiceMx implements S3ServiceMxMBean {
    static final String DOMAIN = "jets3t";
    S3BucketMx bucketCounter;
    private static S3ServiceMx instance;

    private S3ServiceMx() {
        this.bucketCounter = new S3BucketMx();
    }

    static ObjectName getObjectName(String props) {
        try {
            return new ObjectName(DOMAIN + ":" + props);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(props);
        }
    }

    private static MBeanServer getMBeanServer() {
        List servers =
            MBeanServerFactory.findMBeanServer(null);
        if (servers.size() == 0) {
            return null;
        }
        return (MBeanServer)servers.get(0);
    }

    public static ObjectInstance registerMBean(Object object, ObjectName name)
        throws InstanceAlreadyExistsException,
               MBeanRegistrationException,
               NotCompliantMBeanException
   {
        MBeanServer server = getMBeanServer();
        if (server == null) {
            return null;
        }
        return server.registerMBean(object, name);
    }

    public static void registerMBean() {
        getInstance();
    }

    static S3ServiceMx getInstance() {
        if (instance == null) {
            String props = "Type=S3Service";

            instance = new S3ServiceMx();
            ObjectName name = getObjectName(props);
            try {
                registerMBean(instance, name);
            } catch (Exception e) {
                e.printStackTrace(); //XXX
            }
        }
        return instance;
    }

    public long getTotalListRequests() {
        return this.bucketCounter.getTotalListRequests();
    }

    public long getTotalObjectCopyRequests() {
        return this.bucketCounter.getTotalObjectCopyRequests();
    }

    public long getTotalObjectDeleteRequests() {
        return this.bucketCounter.getTotalObjectDeleteRequests();
    }

    public long getTotalObjectGetRequests() {
        return this.bucketCounter.getTotalObjectGetRequests();
    }

    public long getTotalObjectHeadRequests() {
        return this.bucketCounter.getTotalObjectHeadRequests();
    }

    public long getTotalObjectPutRequests() {
        return this.bucketCounter.getTotalObjectPutRequests();
    }

    public long getTotalRequests() {
        return this.bucketCounter.getTotalRequests();
    }
}
