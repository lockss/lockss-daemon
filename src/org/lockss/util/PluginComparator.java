/*
 * $Id PluginComparator.java 2012/12/03 14:52:00 rwincewicz $
 */

/*

 Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.util;

import java.util.Comparator;
import org.lockss.plugin.Plugin;

/**
 *
 * @author rwincewicz
 */
public class PluginComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        int res;
        if (o1 == o2) {
            return 0;
        }
        if (!((o1 instanceof Plugin)
                && (o2 instanceof Plugin))) {
            throw new IllegalArgumentException("PluginComparator("
                    + o1.getClass().getName() + ","
                    + o2.getClass().getName() + ")");
        }
        Plugin p1 = (Plugin) o1;
        Plugin p2 = (Plugin) o2;
        if (p1.getPluginName().charAt(0) > p2.getPluginName().charAt(0)) {
            res = 1;
        } else {
            res = -1;
        }
        
        return res;
    }
}
