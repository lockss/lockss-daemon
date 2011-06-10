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
package org.jets3t.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class to load and store JetS3t-specific properties.
 * <p>
 * Properties are initially loaded via <tt>getInstance</tt> methods from a named properties file,
 * which must be available at the root of the classpath, or from an input stream.
 * In either case the properties are cached according to a name, such that subsequent calls to
 * get a properties instance with the same name will return the same properties object.
 * </p>
 * <p>
 * For more information about JetS3t properties please see:
 * <a href="http://www.jets3t.org/toolkit/configuration.html">JetS3t Configuration</a>
 *
 * @author James Murty
 */
public class Jets3tProperties implements Serializable {
    private static final long serialVersionUID = -822234326095333142L;

    private static final Log log = LogFactory.getLog(Jets3tProperties.class);

    /**
     * Stores the jets3t properties.
     */
    private static final Hashtable<String, Jets3tProperties> propertiesHashtable =
        new Hashtable<String, Jets3tProperties>();

    private Properties properties = new Properties();
    private boolean loaded = false;

    /**
     * Return a properties instance based on properties read from an input stream, and stores
     * the properties object in a cache referenced by the propertiesIdentifier.
     *
     * @param inputStream
     * an input stream containing property name/value pairs in a format that can be read by
     * {@link Properties#load(InputStream)}.
     * @param propertiesIdentifer
     * the name under which the properties are cached
     *
     * @return
     * a properties object initialised with property values from the input stream
     *
     * @throws IOException
     */
    public static Jets3tProperties getInstance(InputStream inputStream, String propertiesIdentifer)
        throws IOException
    {
        Jets3tProperties jets3tProperties = null;

        // Keep static references to properties classes by propertiesIdentifer.
        if (propertiesHashtable.containsKey(propertiesIdentifer)) {
            jets3tProperties = propertiesHashtable.get(propertiesIdentifer);
        } else {
            jets3tProperties = new Jets3tProperties();
            propertiesHashtable.put(propertiesIdentifer, jets3tProperties);
        }
        jets3tProperties.loadAndReplaceProperties(inputStream, propertiesIdentifer);
        return jets3tProperties;
    }

    /**
     * Return a properties instance based on properties read from a properties file, and stores
     * the properties object in a cache referenced by the properties file name.
     *
     * @param propertiesFileName
     * the name of a properties file that exists in the root of the classpath, such that it can
     * be loaded with the code <tt>getClass().getResourceAsStream("/" + propertiesFileName);</tt>.
     *
     * @return
     * a properties object initialised with property values from the properties file
     */
    public static Jets3tProperties getInstance(String propertiesFileName) {
        Jets3tProperties jets3tProperties = null;

        // Keep static references to properties classes by filename.
        if (propertiesHashtable.containsKey(propertiesFileName)) {
            jets3tProperties = propertiesHashtable.get(propertiesFileName);
            return jets3tProperties;
        } else {
            jets3tProperties = new Jets3tProperties();
            propertiesHashtable.put(propertiesFileName, jets3tProperties);
        }

        // Load properties from classpath.
        InputStream cpIS = jets3tProperties.getClass().getResourceAsStream("/" + propertiesFileName);
        if (cpIS != null) {
            if (log.isDebugEnabled()) {
                log.debug("Loading properties from resource in the classpath: " +
                    propertiesFileName);
            }
            try {
                jets3tProperties.loadAndReplaceProperties(cpIS,
                    "Resource '" + propertiesFileName + "' in classpath");
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Failed to load properties from resource in classpath: "
                        + propertiesFileName, e);
                }
            } finally {
                try {
                    cpIS.close();
                } catch (Exception ignored) {}
            }
        }
        return jets3tProperties;
    }

    /**
     * Sets or removes a property value.
     *
     * @param propertyName
     * the name of the property to set or remove.
     * @param propertyValue
     * a new value for the property. If this value is null, the named property
     * will be removed.
     */
    public void setProperty(String propertyName, String propertyValue) {
        if (propertyValue == null) {
            this.clearProperty(propertyName);
        } else {
            this.properties.put(propertyName, trim(propertyValue));
        }
    }

    /**
     * Removes a property name and value.
     *
     * @param propertyName
     * the name of the property to remove.
     */
    public void clearProperty(String propertyName) {
        this.properties.remove(propertyName);
    }

    /**
     * Clears (removes) all the property names and values used internally by
     * this object. Use this method in combination with
     * {@link #loadAndReplaceProperties(Properties, String)} to directly
     * manage the properties encapsulated in this class.
     */
    public void clearAllProperties() {
        this.properties.clear();
    }

    /**
     * Reads properties from an InputStream and stores them in this class's properties object.
     * If a new property already exists, the property value is replaced.
     *
     * @param inputStream
     * an input stream containing property name/value pairs in a format that can be read by
     * {@link Properties#load(InputStream)}.
     * @param propertiesSource
     * a name for the source of the properties, such as a properties file name or identifier. This
     * is only used to generate meaningful debug messages when properties are updated, so it is
     * possible to tell where the updated property value came from.
     *
     * @throws IOException
     */
    public void loadAndReplaceProperties(InputStream inputStream, String propertiesSource)
        throws IOException
    {
        Properties newProperties = new Properties();
        newProperties.load(inputStream);
        loadAndReplaceProperties(newProperties, propertiesSource);
    }

    /**
     * Merges properties from another JetS3tProperties object into this instance.
     * If a new property already exists, the property value is replaced.
     *
     * @param properties
     * the object containing properties that will be merged into this set of properties.
     * @param propertiesSource
     * a name for the source of the properties, such as a properties file name or identifier. This
     * is only used to generate meaningful debug messages when properties are updated, so it is
     * possible to tell where the updated property value came from.
     */
    public void loadAndReplaceProperties(Jets3tProperties properties, String propertiesSource) {
        Properties newProperties = properties.getProperties();
        loadAndReplaceProperties(newProperties, propertiesSource);
    }

    /**
     * Merges properties from another Properties object into this instance.
     * If a new property already exists, the property value is replaced.
     *
     * @param newProperties
     * the object containing properties that will be merged into this set of properties.
     * @param propertiesSource
     * a name for the source of the properties, such as a properties file name or identifier. This
     * is only used to generate meaningful debug messages when properties are updated, so it is
     * possible to tell where the updated property value came from.
     */
    public void loadAndReplaceProperties(Properties newProperties, String propertiesSource) {
        Iterator<Map.Entry<Object, Object>> propsIter = newProperties.entrySet().iterator();
        while (propsIter.hasNext()) {
            Map.Entry<Object, Object> entry = propsIter.next();
            String propertyName = (String) entry.getKey();
            String propertyValue = (String) entry.getValue();
            if (properties.containsKey(propertyName) && !properties.getProperty(propertyName).equals(propertyValue)) {
                if (log.isDebugEnabled()) {
                    log.debug("Over-riding jets3t property [" + propertyName + "=" + propertyValue
                        + "] with value from properties source " + propertiesSource
                        + ". New value: [" + propertyName + "=" + trim(propertyValue) + "]");
                }
            }
            properties.put(propertyName, trim(propertyValue));
        }

        loaded = true;
    }

    /**
     * @return
     * a properties object containing all this object's properties, but cloned so changes to the
     * returned properties object are not reflected in this object.
     */
    public Properties getProperties() {
        return (Properties) properties.clone();
    }

    /**
     * @param propertyName
     * @param defaultValue
     * @return
     * the named Property value as a string if the property is set, otherwise returns the default value.
     */
    public String getStringProperty(String propertyName, String defaultValue) {
        String stringValue = trim(properties.getProperty(propertyName, defaultValue));
        if (log.isDebugEnabled()) {
            log.debug(propertyName + "=" + stringValue);
        }
        return stringValue;
    }

    /**
     *
     * @param propertyName
     * @param defaultValue
     * @return
     * the named Property value as a long if the property is set, otherwise returns the default value.
     * @throws NumberFormatException
     */
    public long getLongProperty(String propertyName, long defaultValue)
        throws NumberFormatException
    {
        String longValue = trim(properties.getProperty(propertyName, String.valueOf(defaultValue)));
        if (log.isDebugEnabled()) {
            log.debug(propertyName + "=" + longValue);
        }
        return Long.parseLong(longValue);
    }

    /**
     *
     * @param propertyName
     * @param defaultValue
     * @return
     * the named Property value as an int if the property is set, otherwise returns the default value.
     * @throws NumberFormatException
     */
    public int getIntProperty(String propertyName, int defaultValue)
        throws NumberFormatException
    {
        String intValue = trim(properties.getProperty(propertyName, String.valueOf(defaultValue)));
        if (log.isDebugEnabled()) {
            log.debug(propertyName + "=" + intValue);
        }
        return Integer.parseInt(intValue);
    }

    /**
     *
     * @param propertyName
     * @param defaultValue
     * @return
     * the named Property value as a boolean if the property is set, otherwise returns the default value.
     * @throws IllegalArgumentException
     */
    public boolean getBoolProperty(String propertyName, boolean defaultValue)
        throws IllegalArgumentException
    {
        String boolValue = trim(properties.getProperty(propertyName, String.valueOf(defaultValue)));
        if (log.isDebugEnabled()) {
            log.debug(propertyName + "=" + boolValue);
        }
        if ("true".equalsIgnoreCase(boolValue)) {
            return true;
        } else if ("false".equalsIgnoreCase(boolValue)) {
            return false;
        } else {
            throw new IllegalArgumentException("Boolean value '" + boolValue + "' for jets3t property '"
                + propertyName + "' must be 'true' or 'false' (case-insensitive)");
        }
    }

    /**
     * @param propertyName
     * the property name to check for.
     * @return
     * true if the property exists, false otherwise.
     */
    public boolean containsKey(String propertyName) {
        return properties.containsKey(propertyName);
    }

    /**
     * @return
     * true if this properties object was successfully loaded from an input stream or a named
     * properties file, false otherwise.
     */
    public boolean isLoaded() {
        return loaded;
    }

    private static String trim(String str) {
        if (str != null) {
            return str.trim();
        } else {
            return null;
        }
    }

}
