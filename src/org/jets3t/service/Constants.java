/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2010 James Murty, 2008 Zmanda Inc.
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

import java.io.File;

/**
 * Constants used by the S3Service and its implementation classes.
 *
 * @author James Murty
 * @author Nikolas Coukouma
 */
public class Constants {

    /**
     * The JetS3t suite version number implemented by this service.
     */
    public static final String JETS3T_VERSION = "0.8.1";

    public static String S3_DEFAULT_HOSTNAME = "s3.amazonaws.com";
    public static String GS_DEFAULT_HOSTNAME = "commondatastorage.googleapis.com";

    ////////////////////////////////////
    // Default file names and locations.
    ////////////////////////////////////

    /**
     * The name of the <a href="http://www.jets3t.org/toolkit/configuration.html#jets3t">JetS3t properties</a>
     * file: jets3t.properties
     */
    public static String JETS3T_PROPERTIES_FILENAME = "jets3t.properties";

    /**
     * The file containing local Cockpit preferences.
     */
    public static String COCKPIT_PROPERTIES_FILENAME = "jets3t-cockpit.properties";
    /**
     * The file containing the list of AWS DevPay Products
     */
    public static String DEVPAY_PRODUCTS_PROPERTIES_FILENAME = "devpay_products.properties";
    /**
     * The property name suffix for the names of products
     */
    public static String DEVPAY_PRODUCT_NAME_PROP_SUFFIX = ".name";
    /**
     * The property name suffix for the tokens of products
     */
    public static String DEVPAY_PRODUCT_TOKEN_PROP_SUFFIX = ".token";

    /**
     * The name of the <a href="http://www.jets3t.org/toolkit/configuration.html#ignore">JetS3t ignore</a>
     * file: .jets3t-ignore
     */
    public static String JETS3T_IGNORE_FILENAME = ".jets3t-ignore";

    /**
     * The default preferences directory: &lt;user.home&gt;/.jets3t
     */
    public static File DEFAULT_PREFERENCES_DIRECTORY = new File(System.getProperty("user.home") + "/.jets3t");

    /**
     * The file delimiter used by JetS3t is the '/' character, which is compatible with standard
     * browser access to S3 files.
     */
    public static String FILE_PATH_DELIM = "/";

    /**
     * The default encoding used for text data: UTF-8
     */
    public static String DEFAULT_ENCODING = "UTF-8";

    /**
     * HMAC/SHA1 Algorithm per RFC 2104, used when generating S3 signatures.
     */
    public static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    ///////////////////////////////////////
    // JetS3t-specific metadata item names.
    ///////////////////////////////////////

    /**
     * Metadata header for storing the original date of a local file uploaded to S3, so it can
     * be used subsequently to compare files instead of relying on the S3 upload date.
     */
    public static final String METADATA_JETS3T_LOCAL_FILE_DATE = "jets3t-original-file-date-iso8601";

    /**
     * Metadata header for storing information about the data encryption algorithm applied by JetS3t tools.
     */
    public static final String METADATA_JETS3T_CRYPTO_ALGORITHM = "jets3t-crypto-alg";

    /**
     * Metadata header for storing information about the JetS3t version of encryption applied
     * (to keep encryption compatibility between versions).
     */
    public static final String METADATA_JETS3T_CRYPTO_VERSION = "jets3t-crypto-ver";

    /**
     * Metadata header for storing information about data compression applied by jets3t tools.
     */
    public static final String METADATA_JETS3T_COMPRESSED = "jets3t-compression";

    ///////////////////////////////////
    // Settings used by all S3 Services
    ///////////////////////////////////

    /**
     * Default number of objects to include in each chunk of an object listing.
     */
    public static final long DEFAULT_OBJECT_LIST_CHUNK_SIZE = 1000;

    ///////////////////////////////////
    // Headers used by REST S3 Services
    ///////////////////////////////////

    /**
     * Header prefix for general Amazon headers: x-amz-
     */
    public static final String REST_HEADER_PREFIX = "x-amz-";
    /**
     * Header prefix for Amazon metadata headers: x-amz-meta-
     */
    public static final String REST_METADATA_PREFIX = "x-amz-meta-";
    /**
     * Header prefix for Amazon's alternative date header: x-amz-date
     */
    public static final String REST_METADATA_ALTERNATE_DATE = "x-amz-date";
    /**
     * XML namespace URL used when generating S3-compatible XML documents:
     * http://s3.amazonaws.com/doc/2006-03-01/
     */
    public static final String XML_NAMESPACE = "http://s3.amazonaws.com/doc/2006-03-01/";

    /**
     * A flag used to indicate that the sender is willing to accept any Requester Pays
     * bucket fees imposed by the request. This flag may be used in request Headers,
     * or as a parameter.
     */
    public static final String REQUESTER_PAYS_BUCKET_FLAG = "x-amz-request-payer=requester";

    public static final String AMZ_REQUEST_ID_1 = "x-amz-request-id";
    public static final String AMZ_REQUEST_ID_2 = "x-amz-id-2";
    public static final String AMZ_SECURITY_TOKEN = "x-amz-security-token";
    public static final String AMZ_VERSION_ID = "x-amz-version-id";
    public static final String AMZ_DELETE_MARKER = "x-amz-delete-marker";
    public static final String AMZ_MULTI_FACTOR_AUTH_CODE = "x-amz-mfa";

}
