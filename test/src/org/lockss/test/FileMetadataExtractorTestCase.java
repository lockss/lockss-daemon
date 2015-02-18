/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.io.*;
import java.util.*;
import java.net.*;
import org.lockss.extractor.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/** Framework for FileMetadataExtractor tests.  Subclassess must implement
 * only {@link #getFactory()} and {@link #getMimeType()}, and tests which
 * call {@link #extractFrom(String)}. */
public abstract class FileMetadataExtractorTestCase extends LockssTestCase {
  public static String URL = "http://www.example.com/";

  public static String MIME_TYPE_XML = "application/xml";
  protected FileMetadataListExtractor extractor = null;
  protected String encoding;
  protected MockArchivalUnit mau;
  protected MockCachedUrl cu;

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    cu = new MockCachedUrl(getUrl(), mau);
    FileMetadataExtractor fme =
      getFactory().createFileMetadataExtractor(getTarget(), getMimeType());
    extractor = new FileMetadataListExtractor(fme);
    encoding = getEncoding();
  }

  /** Return the MIME type of the MetadataExtractor under test. */
  protected abstract String getMimeType();

  /** Return a factory that creates instances of the MetadataExtractor to
   * test. */
  protected abstract FileMetadataExtractorFactory getFactory();

  protected MetadataTarget getTarget() {
    return MetadataTarget.Any;
  }

  protected String getEncoding() {
    return Constants.DEFAULT_ENCODING;
  }

  protected String getUrl() {
    return URL;
  }

  protected void assertRawEmpty(ArticleMetadata md) {
    assertEquals(0, md.rawSize());
  }

  protected void assertRawEquals(String expkey1, String expval1,
				ArticleMetadata md) {
    assertRawEquals(MapUtil.map(expkey1, expval1), md);
  }

  protected void assertRawEquals(String expkey1, String expval1,
				String expkey2, String expval2,
				ArticleMetadata md) {
    assertRawEquals(MapUtil.map(expkey1, expval1, expkey2, expval2), md);
  }

  /** Compare the raw metedata extracted from text with the key-value
   * pairs.  The keys are compared case-independently */
  protected void assertRawEquals(List<String> keyvaluepairs,
				ArticleMetadata md) {
    assertRawEquals(MapUtil.fromList(keyvaluepairs), md);
  }

  /** Compare the raw metedata with the expected map.  The case of the map
   * keys is ignored. */
  protected void assertRawEquals(Map expMap, ArticleMetadata md) {
    Set<String> seen = new HashSet<String>();
    List<String> errors = new ArrayList<String>();
    for (String key : (Collection<String>)expMap.keySet()) {
      seen.add(key);
      Object expval = expMap.get(key);
      if (expval instanceof List) {
	List actual = md.getRawList(key);
	if (!expval.equals(actual)) {
	  errors.add("Key: " + key + " expected:<" + expval + "> but was:<"
		     + actual + ">");
	}
      } else {
	String actual = md.getRaw(key);
	if (expval == null) {
	  if (actual != null) {
	    errors.add("Key: " + key + " expected:<" + expval + "> but was:<"
	         + actual + ">");
	  }
  } else if (!expval.equals(actual)) {
    errors.add("Key: " + key + " expected:<" + expval + "> but was:<"
  	     + actual + ">");
	}
      }
    }
    if (seen.size() != md.rawSize()) {
      for (String key : md.rawKeySet()) {
	if (!seen.contains(key)) {
	  errors.add("Key: " + key + " unexpected, was:<"
		     + md.getRawList(key) + ">");
	}
      }
    }
    if (!errors.isEmpty()) {
      fail("Incorrect raw metdata: " +
	   StringUtil.separatedString(errors, ",    \n"));
    }
  }

  /** Compare the cooked metedata with the expected map.  The case of the
   * map keys is ignored. */
  protected void assertCookedEquals(Map<MetadataField,Object> expMap,
				    ArticleMetadata md) {
    Set<String> seen = new HashSet<String>();
    List<String> errors = new ArrayList<String>();
    for (MetadataField field : expMap.keySet()) {
      String key = field.getKey();
      seen.add(key);
      Object expval = expMap.get(field);
      log.info("field: " + field.getKey() + ", " + field.getCardinality());
      if (expval instanceof List) {
	switch (field.getCardinality()) {
	case Single: {
	  throw new IllegalArgumentException("Field " + field.getKey() +
					     ": multivalue expected for single-valued field");
	}
	case Multi: {
	  List actual = md.getList(field);
	  if (!expval.equals(actual)) {
	    errors.add("Key: " + key + " expected:<" + expval
		       + "> but was:<" + actual + ">");
	  }
	}
	}
      } else {
	String actual = md.get(field);
	if (!expval.equals(actual)) {
	  errors.add("Key: " + key + " expected:<" + expval
		     + "> but was:<" + actual + ">");
	}
      }
    }
    if (seen.size() != md.size()) {
      for (String key : md.keySet()) {
	if (!seen.contains(key)) {
	  errors.add("Key: " + key + " unexpected, was:<"
		     + md.getList(key) + ">");
	}
      }
    }
    if (!errors.isEmpty()) {
      fail("Incorrect cooked metdata: " +
	   StringUtil.separatedString(errors, ",    \n"));
    }
  }

  public void testEmptyFileReturnsEmptyMetadata() throws Exception {
    List<ArticleMetadata> lst = extractor.extract(MetadataTarget.Any, cu);
    assertEquals(1, lst.size());
    ArticleMetadata md = lst.get(0);
    assertEquals(0, md.rawSize());
  }

  public void testThrows() throws IOException, PluginException {
    try {
      extractor.extract(MetadataTarget.Any, null);
      fail("Calling extract with a null InputStream should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  /** Extract metadata from the string and return it */ 
  protected ArticleMetadata extractFrom(String content) {
    try {
      List<ArticleMetadata> lst = extractor.extract(getTarget(),
						    cu.addVersion(content));
      return lst.get(0);
    } catch (Exception e) {
      fail("extract threw " + e);
    }
    return null;			// impossible
  }
      
  /** Extract metadata from the named resource (e.g., a file in the test
   * directory) */ 
  protected ArticleMetadata extractFromResource(String resname) {
    String content;
    try {
      content = resourceContent(resname);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't read file", e);
    }
    return extractFrom(content);
  }
      
  protected String resourceContent(String resname) throws IOException {
    URL url = getResource(resname);
    InputStream istr = UrlUtil.openInputStream(url.toString());
    return StringUtil.fromInputStream(istr);
  }  
}
