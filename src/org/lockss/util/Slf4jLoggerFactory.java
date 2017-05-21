/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.concurrent.*;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * Factory to create SLF4J logger that logs to LOCKSS logger.
 */
public class Slf4jLoggerFactory implements ILoggerFactory {

  // key: name (String), value: a Slf4jLoggerAdapter;
  final ConcurrentMap<String, org.slf4j.Logger> loggerMap;

  public Slf4jLoggerFactory() {
    loggerMap = new ConcurrentHashMap<String, org.slf4j.Logger>();
  }

  @Override
  public org.slf4j.Logger getLogger(String name) {
    Logger slf4jLogger = loggerMap.get(name);
    if (slf4jLogger != null) {
      return slf4jLogger;
    } else {
      org.lockss.util.Logger log = org.lockss.util.Logger.getLogger(name);
      org.slf4j.Logger newInstance = new Slf4jLoggerAdapter(log, name);
      org.slf4j.Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
      return oldInstance == null ? newInstance : oldInstance;
    }
  }
}
