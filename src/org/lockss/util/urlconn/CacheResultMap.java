/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util.urlconn;

import org.lockss.plugin.ArchivalUnit;


/**
 * Maps the result of a cache (fetch and store locally) operation, to
 * success (null) or an exception understood by LOCKSS, usually one under
 * CacheException
 */
public interface CacheResultMap {

  @Deprecated
  public CacheException getMalformedURLException(Exception nestedException);

  public CacheException getMalformedURLException(ArchivalUnit au,
                                                 String url,
                                                 Exception nestedException,
                                                 String message);

  public CacheException getRepositoryException(Exception nestedException);

  public CacheException checkResult(ArchivalUnit au,
				    LockssUrlConnection connection);

  public CacheException mapException(ArchivalUnit au,
				     LockssUrlConnection connection,
				     Exception fetchException,
				     String message);
  
  public CacheException mapException(ArchivalUnit au,
      String url,
      Exception fetchException,
      String message);

  public CacheException mapException(ArchivalUnit au,
				     LockssUrlConnection connection,
				     int responseCode,
				     String message);
  
  public CacheException mapException(ArchivalUnit au,
      String url,
      int responseCode,
      String message);

  public CacheException triggerAction(ArchivalUnit au,
                                      String url,
                                      CacheEvent evt,
                                      ResultAction ra,
                                      String message);
}
