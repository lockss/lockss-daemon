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

package org.lockss.util;

import java.util.Enumeration;
import java.util.Properties;
import junit.framework.TestCase;
import org.lockss.test.MockLogTarget;


/**
 * This is the test class for org.lockss.util.TestLogger
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class TestLogger extends TestCase{

  //expected debugging levels, in ascending order of seriousness
  private static final String[] levels = {
    "debug",
    "info",
    "warning",
    "error",
    "critical"
  };

  public TestLogger(String msg){
    super(msg);
  }

  public void setUp(){
//     baos = new ByteArrayOutputStream();
//     System.setErr(new PrintStream(baos));
  }

  //test that the severity levels are correctly relative to each other
  public void testSeverityHierarchy(){
    for (int ix=0; ix < levels.length; ix++){
      for (int jx=0; jx < levels.length; jx++){
	if (ix >= jx){ 
	  assertTrue(levels[ix]+" not above "+levels[jx]+" threshold",
		     Logger.isLevelAboveThreshold(levels[ix], levels[jx]));
	}
	else{
	  assertTrue(levels[ix]+" above "+levels[jx]+" threshold",
		     !Logger.isLevelAboveThreshold(levels[ix], levels[jx]));
	  
	}
      }
    }
  }

 //  public void testOutputStringFormat()
//   throws REException{
//     Properties props = System.getProperties();
//     props.setProperty("org.lockss.log.level.default", "debug");
//     ByteArrayOutputStream baos = new ByteArrayOutputStream();
//     PrintStream ps = new PrintStream(baos);
//     Logger.setGlobalErrorStream(ps);
//     String callerId = "caller-id";
//     String errorMessage = "error message";
//     Logger.error(callerId, errorMessage);

//     RE regExp = 
//       new RE("\\d(\\d)?:\\d\\d:\\d\\d (A|P)M: error: "+callerId+" "+errorMessage+"\n");
//     String debugString = baos.toString();
//     assertTrue("Debug string: \""+debugString+"\" not of correct format."+
// 	       " Should be <time>: <error-level>: <caller-id> <error message>",
// 	       regExp.isMatch(debugString));
//   }

  public void testStaticLoggingAboveThreshold(){
    Properties props = System.getProperties();
    props.setProperty("org.lockss.log.level.default", "debug");
    Logger.loadProps();
    MockLogTarget target = new MockLogTarget();
    Logger.addTarget(target);

    String callerId = "caller-id";
    String errorMessage = "error message";
    Logger.error(callerId, errorMessage);

    Enumeration enum = target.getMessages();
    String[] logInfo = (String[]) enum.nextElement();
    
    assertEquals(callerId, logInfo[0]);
    assertEquals(errorMessage, logInfo[1]);
    assertEquals(Logger.ERROR, logInfo[2]);

    assertTrue("More than one message sent to target", !enum.hasMoreElements());
  }

  public void testStaticLoggingBelowThreshold(){
    Properties props = System.getProperties();
    props.setProperty("org.lockss.log.level.default", "critical");
    Logger.loadProps();
    MockLogTarget target = new MockLogTarget();
    Logger.addTarget(target);

    String callerId = "caller-id";
    String errorMessage = "error message";
    Logger.error(callerId, errorMessage);

    Enumeration enum = target.getMessages();
    
    assertTrue("Should have no messages", !enum.hasMoreElements());
  }

  public void testObjectLoggingNullId(){
    Logger logger = Logger.getLogger(null);
    assertNull(logger);
  }

  public void testObjectLoggingBelowThreshold(){
    Properties props = System.getProperties();
    props.setProperty("org.lockss.log.level.test_id", "critical");
    MockLogTarget target = new MockLogTarget();
    Logger.addTarget(target);
    String callerId = "test_id";

    Logger logger = Logger.getLogger(callerId);
    String errorMessage = "error message";
    logger.error(errorMessage);

    Enumeration enum = target.getMessages();
    
    assertTrue("Should have no messages", !enum.hasMoreElements());
  }

  public void testObjectLoggingAboveThreshold(){
    Properties props = System.getProperties();
    props.setProperty("org.lockss.log.level.test_id", "warning");
    MockLogTarget target = new MockLogTarget();
    Logger.addTarget(target);
    String callerId = "test_id";

    Logger logger = Logger.getLogger(callerId);

    String errorMessage = "error message";
    logger.error(errorMessage);

    Enumeration enum = target.getMessages();
    String[] logInfo = (String[]) enum.nextElement();
    assertTrue(logInfo != null);
    assertEquals(callerId, logInfo[0]);
    assertEquals(errorMessage, logInfo[1]);
    assertEquals(Logger.ERROR, logInfo[2]);

    assertTrue("More than one message sent to target", !enum.hasMoreElements());
  }
  //tests to write:
  //tests for stderr target
}

