/*

 Copyright (c) 2019-2024 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.laaws;

import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.lockss.util.*;

public class LockssRestHttpException extends IOException {

  private long timestamp;
  private int status;
  private String error;
  private String exception;
  private String message;
  private String path;
  private String serverErrorType;

  public long getTimestamp() {
    return timestamp;
  }

  public int getStatus() {
    return status;
  }

  public String getError() {
    return error;
  }

  public String getException() {
    return exception;
  }

  public String getMessage() {
    return message;
  }

  public String getPath() {
    return path;
  }

  public String getServererrortype() {
    return serverErrorType;
  }


  @Override
  public String toString() {
    return String.format("[LRHE: timestamp: %s, status: %d, error: %s, message: %s%s%s",
                         timestamp, status, error, message,
                         StringUtil.isNullString(path) ? "" : String.format(" path: %s", path),
                         StringUtil.isNullString(serverErrorType) ? "" : String.format(" serverErrorType: %s", serverErrorType));
  }

}
