/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 Zmanda Inc.
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
package org.jets3t.service.model;

import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import org.jets3t.service.Constants;

/**
 * Class to contain information about an Amazon Web Services (AWS) S3 DevPay product.
 *
 * @author Nikolas Coukouma
 */
public class AWSDevPayProduct implements Serializable, Comparable {
    private static final long serialVersionUID = 7581378683354747125L;

    private String productName = null;
    private String productToken = null;

    public AWSDevPayProduct(String productToken) {
        this.productToken = productToken;
    }

    public AWSDevPayProduct(String productToken, String productName) {
        this(productToken);
        this.productName = productName;
    }

    /**
     * @return
     * the name of the DevPay product
     */
    public String getProductName() {
        return this.productName;
    }

    /**
     * @return
     * the product token of the DevPay product
     */
    public String getProductToken() {
        return this.productToken;
    }

    /**
     * @return
     * the name of the DevPay product
     */
    public String toString() {
        return getProductName();
    }

    /**
     * Compare two products by their names (using string comparision)
     */
    public int compareTo(Object o) {
        return getProductName().compareTo(((AWSDevPayProduct) o).getProductName());
    }

    /**
     * Loads the products listed in
     * {@link Constants#DEVPAY_PRODUCTS_PROPERTIES_FILENAME}
     *
     * @return the Vector of <code>AWSDevPayProduct</code>s
     */
    public static Vector load() throws IOException {
        InputStream pin = AWSDevPayProduct.class.getResourceAsStream("/" + Constants.DEVPAY_PRODUCTS_PROPERTIES_FILENAME);
        Vector ret = new Vector();
        if (pin != null) {
            try {
                ret = load(pin);
            } finally {
                pin.close();
            }
        }
        return ret;
    }

    /**
     * Loads the products listed in the {@link java.util.Properties} file
     * represented by the input stream.
     *
     * @param pin the input stream
     *
     * @return the Vector of <code>AWSDevPayProduct</code>s
     */
    public static Vector load(InputStream pin) throws IOException {
        if (pin == null) {
            return new Vector();
        } else {
            Properties prodProps = new Properties();
            prodProps.load(pin);
            return load(prodProps);
        }
    }

    /**
     * Loads the products listed in the {@link java.util.Properties}.
     * Specifically, any properties ending in {@link Constants#DEVPAY_PRODUCT_NAME_PROP_SUFFIX}
     * (the product's name)
     * have that ending removed and replaced with {@link Constants#DEVPAY_PRODUCT_NAME_PROP_SUFFIX}
     * (to form name of the property for the product's token).
     * If the token exists, then a <code>AWSDevPayProduct</code> is constructed
     * with that name and token, and then is added to the Vector. For example,
     * (with the current constants) "foo.name" would become "foo.token";
     * if both properties exist, then a product is constructed with the values of
     * the "foo.name" and "foo.token" properties (e.g. "Foo" and "{ProductToken}AAA...").
     *
     * @param prodProps the properties
     *
     * @return the Vector of <code>AWSDevPayProduct</code>s, sorted by name
     */
    public static Vector load(Properties prodProps) {
        Vector ret = new Vector();
        Enumeration propEnum = prodProps.propertyNames();
        while (propEnum.hasMoreElements()) {
            String propName = (String) propEnum.nextElement();
            if (propName.endsWith(Constants.DEVPAY_PRODUCT_NAME_PROP_SUFFIX)) {
                String tokenPropName = propName.substring(0,
                    propName.length()-Constants.DEVPAY_PRODUCT_NAME_PROP_SUFFIX.length()) +
                    Constants.DEVPAY_PRODUCT_TOKEN_PROP_SUFFIX;
                String prodName = prodProps.getProperty(propName);
                String prodToken = prodProps.getProperty(tokenPropName);
                if (prodToken != null) {
                    ret.add(new AWSDevPayProduct(prodToken, prodName));
                }
            }
        }
        Collections.sort(ret);
        return ret;
    }
}
