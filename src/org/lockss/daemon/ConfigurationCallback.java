/*
 * $Id: ConfigurationCallback.java,v 1.1 2002-08-31 06:26:35 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;
import java.util.*;

/**
 * The <code>ConfigurationCallback</code> interface defines the
 * callback registered by clients of <code>Configuration</code>
 * who want to know when the configuration has changed.
 */
public interface ConfigurationCallback {
    /**
     * Called to indicate that something in the configuration has changed.
     * It is called after the new config is installed as current.
     * @param newConfig  the new (just installed) <code>Configuration</code>.
     * @param oldConfig  the previous <code>Configuration</code>.
     * @param changedKeys  the set of keys whose value has changed.
     */
    public void configurationChanged(Configuration oldConfig,
				     Configuration newConfig,
				     Set changedKeys);
}
