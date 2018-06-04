/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.lang3.tuple.*;

import org.lockss.state.*;
import org.lockss.alert.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

import org.lockss.rewriter.*;
import org.lockss.extractor.*;

/**
 * Runs a ContentValidator on all files in an AU, returns an object
 * describing any validation failures
 */
public class AuValidator {
  protected static Logger log = Logger.getLogger(AuValidator.class);

  protected final ArchivalUnit au;
  
  /** Holds the result of a AU content validation */
  public static class Result {
    private ArchivalUnit au;
    private List<ValidationFailure> validationFailures =
      new ArrayList<ValidationFailure>();
    private String error;
    private int numFiles = 0;
    private int numValidations = 0;

    public Result(ArchivalUnit au) {
      this.au = au;
    }
    
    public ArchivalUnit getAu() {
      return au;
    }

    public boolean hasValidationFailures() {
      return !validationFailures.isEmpty();
    }

    public boolean hasError() {
      return error != null;
    }

    public String getError() {
      return error;
    }

    public void setError(String msg) {
      this.error = msg;
    }

    public Result incrFiles() {
      numFiles++;
      return this;
    }

    public Result incrValidations() {
      numValidations++;
      return this;
    }

    public int numFiles() {
      return numFiles;
    }

    public int numValidations() {
      return numValidations;
    }

    public int numValidationFailures() {
      return validationFailures.size();
    }

    public Result addValidationFailure(ValidationFailure vf) {
      validationFailures.add(vf);
      return this;
    }

    public List<ValidationFailure> getValidationFailures() {
      return validationFailures;
    }
  }

  public static class ValidationFailure {
    private String url;
    private String message;

    public ValidationFailure(String url, String message) {
      this.url = url;
      this.message = message;
    }

    public String getUrl() {
      return url;
    }

    public String getMessage() {
      return message;
    }
  }

  public AuValidator(ArchivalUnit au) {
    this.au = au;
  }

  public Result validateAu() {
    Result res = new Result(au);
    for (CuIterator iter = au.getAuCachedUrlSet().getCuIterator();
	 iter.hasNext(); ) {
      CachedUrl cu = iter.next();
      try {
	processCu(res, cu);
      } finally {
	AuUtil.safeRelease(cu);
      }
    }
    return res;
  }

  void processCu(Result res, CachedUrl cu) {
    res.incrFiles();
    String ctype = cu.getContentType();
    ContentValidatorFactory cvfact = au.getContentValidatorFactory(ctype);
    if (cvfact != null) {
      ContentValidator cv = cvfact.createContentValidator(au, ctype);
      if (cv != null) {
	try {
	  res.incrValidations();
	  cv.validate(cu);
	} catch (ContentValidationException e) {
	  log.debug2("Validation error1", e);
	  res.addValidationFailure(new ValidationFailure(cu.getUrl(),
							 e.toString()));
	} catch (Exception e) {
	  log.debug2("Validation error2", e);
	  res.addValidationFailure(new ValidationFailure(cu.getUrl(),
							 e.toString()));
	}
      }
    }
  }
}
