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
package org.jets3t.service.utils;

import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.text.NumberFormatter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Formats numeric byte values into human-readable strings.
 *
 * @author James Murty
 */
public class ByteFormatter {
    private static final Log log = LogFactory.getLog(ByteFormatter.class);

    private String gigabyteSuffix = null;
    private String megabyteSuffix = null;
    private String kilobyteSuffix = null;
    private String byteSuffix = null;
    private NumberFormatter nf = null;

    /**
     * Constructs a formatter that will use default text to represent byte amounts.
     * Default values used:
     * <ul>
     * <li>Gigabyte suffix: " GB"</li>
     * <li>Megabyte suffix: " MB"</li>
     * <li>Kilobyte suffix: " KB"</li>
     * <li>Byte suffix: " B"</li>
     * <li>Precision: 2 decimal places</li>
     * </ul>
     *
     */
    public ByteFormatter() {
        this(" GB", " MB", " KB", " B", 2);
    }

    /**
     * Constructs a formatter that will use the given values when formatting byte values.
     *
     * @param gigabyteSuffix
     * string to display at the end of gigabyte values.
     * @param megabyteSuffix
     * string to display at the end of megabyte values.
     * @param kilobyteSuffix
     * string to display at the end of kilobyte values.
     * @param byteSuffix
     * string to display at the end of byte values.
     * @param decimalPlaces
     * the number of decimal places to use when converting byte amounts into kilo, mega or giga
     * byte values.
     */
    public ByteFormatter(String gigabyteSuffix, String megabyteSuffix, String kilobyteSuffix,
        String byteSuffix, int decimalPlaces)
    {
        this.gigabyteSuffix = gigabyteSuffix;
        this.megabyteSuffix = megabyteSuffix;
        this.kilobyteSuffix = kilobyteSuffix;
        this.byteSuffix = byteSuffix;

        StringBuffer numberFormatString = new StringBuffer();
        numberFormatString.append("0").append((decimalPlaces > 0? "." : ""));
        for (int i = 0; i < decimalPlaces; i++) {
            numberFormatString.append("0");
        }
        nf = new NumberFormatter(new DecimalFormat(numberFormatString.toString()));
    }

    /**
     * Converts a byte size into a human-readable string, such as "1.43 MB" or "27 KB".
     * The values used are based on powers of 1024, ie 1 KB = 1024 bytes, not 1000 bytes.
     *
     * @param byteSize
     * the byte size of some item
     * @return
     * a human-readable description of the byte size
     */
    public String formatByteSize(long byteSize) {
        String result = null;
        try {
            if (byteSize > Math.pow(1024,3)) {
                // Report gigabytes
                result = nf.valueToString(new Double(byteSize / Math.pow(1024,3))) + gigabyteSuffix;
            } else if (byteSize > Math.pow(1024,2)) {
                // Report megabytes
                result = nf.valueToString(new Double(byteSize / Math.pow(1024,2))) + megabyteSuffix;
            } else if (byteSize > 1024) {
                // Report kilobytes
                result = nf.valueToString(new Double(byteSize / Math.pow(1024,1))) + kilobyteSuffix;
            } else if (byteSize >= 0) {
                // Report bytes
                result = byteSize + byteSuffix;
            }
        } catch (ParseException e) {
            if (log.isErrorEnabled()) {
                log.error("Unable to format byte size " + byteSize, e);
            }
            return byteSize + byteSuffix;
        }
        return result;
    }

}
