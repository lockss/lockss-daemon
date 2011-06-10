/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2009 Doug MacEachern
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class S3ServiceExceptionMx implements DynamicMBean {

    private static final int E_NAME = 0, E_DESCR = 1;
    private static S3ServiceExceptionMx instance;
    private MBeanInfo info;
    private Map counters = Collections.synchronizedMap(new HashMap());

    public static void increment(String code) {
        try {
            getInstance().incrementCounter(code);
        } catch (Exception e) {
            e.printStackTrace(); //XXX
        }
    }

    //"ServiceUnavailable" - connection failures, etc.
    public static void increment() {
        increment(S3ServiceErrorCodeTable.TABLE[0][E_NAME]);
    }

    public static void registerMBean() {
        getInstance();
    }

    public static S3ServiceExceptionMx getInstance() {
        if (instance == null) {
            ObjectName name =
                S3ServiceMx.getObjectName("Type=S3ServiceException");
            instance = new S3ServiceExceptionMx();
            try {
                S3ServiceMx.registerMBean(instance, name);
            } catch (Exception e) {
                e.printStackTrace(); //XXX
            }
        }

        return instance;
    }

    public S3ServiceExceptionMx() {
        String[][] errors = S3ServiceErrorCodeTable.TABLE;
        for (int i=0; i<errors.length; i++) {
            counters.put(errors[i][E_NAME], new LongCounter());
        }
    }

    public void incrementCounter(String code) {
        LongCounter counter = getCounter(code);
        if (counter == null) {
            counter = new LongCounter();
            this.counters.put(code, counter);
        }
        counter.increment();
    }

    private LongCounter getCounter(String code) {
        return (LongCounter)this.counters.get(code);
    }

    public Object getAttribute(String name)
        throws AttributeNotFoundException,
               MBeanException, ReflectionException {
        LongCounter counter = getCounter(name);
        if (counter == null) {
            throw new AttributeNotFoundException(name);
        }
        return new Long(counter.getValue());
    }

    public AttributeList getAttributes(String[] attributes) {
        AttributeList list = new AttributeList();
        for (int i=0; i<attributes.length; i++) {
            String name = attributes[i];
            long value;
            LongCounter counter = getCounter(name);
            if (counter == null) {
                value = -1;
            }
            else {
                value = counter.getValue();
            }
            list.add(new Attribute(name, new Long(value)));
        }
        return list;
    }

    public MBeanInfo getMBeanInfo() {
        if (this.info != null) {
            return this.info;
        }
        String[][] errors = S3ServiceErrorCodeTable.TABLE;
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[errors.length];
        for (int i=0; i<errors.length; i++) {
            String[] error = errors[i];
            attrs[i] =
                new MBeanAttributeInfo(error[E_NAME],
                                       Long.class.getName(),
                                       error[E_DESCR],
                                       true,   // isReadable
                                       false,  // isWritable
                                       false); // isIs
        }
        this.info =
            new MBeanInfo(this.getClass().getName(),
                          "S3ServiceException MBean",
                          attrs, // attributes
                          null,  // constructors
                          null,  // operations
                          null); // notifications

        return this.info;
    }

    public Object invoke(String actionName, Object[] params, String[] signature)
        throws MBeanException, ReflectionException {
        throw new IllegalArgumentException();
    }

    public void setAttribute(Attribute attribute)
        throws AttributeNotFoundException, InvalidAttributeValueException,
               MBeanException, ReflectionException {
        throw new IllegalArgumentException();
    }

    public AttributeList setAttributes(AttributeList attributes) {
        throw new IllegalArgumentException();
    }
}
