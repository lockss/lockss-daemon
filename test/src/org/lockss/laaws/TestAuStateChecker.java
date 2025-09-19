/*
 * 2022, Board of Trustees of Leland Stanford Jr. University,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.lockss.laaws;

import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import com.google.gson.Gson;
import org.lockss.util.*;
import org.lockss.hasher.*;
import org.lockss.repository.AuSuspectUrlVersions;
import org.lockss.repository.AuSuspectUrlVersions.SuspectUrlVersion;
import org.lockss.repository.AuSuspectUrlVersionsBean;
import org.lockss.test.LockssTestCase;
import org.lockss.laaws.client.*;

/**
 * AuStateChecker Tester.
 *
 * @version 1.0
 */
public class TestAuStateChecker extends LockssTestCase {
  private Logger log = Logger.getLogger("TestAuStateChecker");

  public TestAuStateChecker(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
*
   * Method: run()
*
   */
  public void testRun() throws Exception {
//TODO: Test goes here... 
  }

  /**
*
   * Method: createInstance(final Type type)
*
   */
  public void testCreateInstance() throws Exception {
//TODO: Test goes here... 
  }


  /**
*
   * Method: checkAuAgreements(ArchivalUnit au)
*
   */
  public void testCheckAuAgreements() throws Exception {
//TODO: Test goes here... 
/* 
try { 
   Method method = AuStateChecker.getClass().getMethod("checkAuAgreements", ArchivalUnit.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/
  }

  public void testCheckAuSuspectUrlVersions() throws Exception {
    AuSuspectUrlVersions asuv = new TestableAuSuspectUrlVersions();
    TimeBase.setSimulated(12345);
    asuv.markAsSuspect("https://www.ajtmh.org/assets/vendor/za34738ba/respond.min.js", 2,
                       HashResult.make("SHA-1:deadbeef"),
                       HashResult.make("SHA-1:8888777766665555"));
    TimeBase.setSimulated(123456);
    asuv.markAsSuspect("url2", 1,
                       HashResult.make("SHA-1:beeffeed"),
                       HashResult.make("SHA-1:01234567"));

    String expJson = "{\"auid\":\"org|lockss|plugin|DirTreePlugin&base_url~https%3A%2F%2Fwww%2Elockss%2Eorg%2Ffoo\",\"suspectVersions\":[{\"url\":\"https://www.ajtmh.org/assets/vendor/za34738ba/respond.min.js\",\"version\":2,\"created\":12345,\"computedHash\":{\"bytes\":\"3q2+7w==\",\"algorithm\":\"SHA-1\"},\"storedHash\":{\"bytes\":\"iIh3d2ZmVVU=\",\"algorithm\":\"SHA-1\"}},{\"url\":\"url2\",\"version\":1,\"created\":123456,\"computedHash\":{\"bytes\":\"vu/+7Q==\",\"algorithm\":\"SHA-1\"},\"storedHash\":{\"bytes\":\"ASNFZw==\",\"algorithm\":\"SHA-1\"}}]}";

    AuSuspectUrlVersionsBean expBean = asuv.getBean("org|lockss|plugin|DirTreePlugin&base_url~https%3A%2F%2Fwww%2Elockss%2Eorg%2Ffoo");
    String genJson = JSON.getGson().toJson(expBean);
    assertEquals(expJson, genJson);

    AuSuspectUrlVersionsBean deserBean = JSON.getGson().fromJson(genJson, AuSuspectUrlVersionsBean.class);
    assertEquals(expBean, deserBean);
  }

  /**
*
   * Method: checkNoAuPeerSet(ArchivalUnit au)
*
   */
  public void testCheckNoAuPeerSet() throws Exception {
//TODO: Test goes here... 
/* 
try { 
   Method method = AuStateChecker.getClass().getMethod("checkNoAuPeerSet", ArchivalUnit.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/
  }

  /**
*
   * Method: checkAuConfig(ArchivalUnit au)
*
   */
  public void testCheckAuConfig() throws Exception {
//TODO: Test goes here... 
/* 
try { 
   Method method = AuStateChecker.getClass().getMethod("checkAuConfig", ArchivalUnit.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/
  }

  /**
*
   * Method: checkAuState(ArchivalUnit au)
*
   */
  public void testCheckAuState() throws Exception {
//TODO: Test goes here... 
/* 
try { 
   Method method = AuStateChecker.getClass().getMethod("checkAuState", ArchivalUnit.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/
  }

  static class TestableAuSuspectUrlVersions extends AuSuspectUrlVersions {
    protected Set<SuspectUrlVersion> makeSuspectVersionsSet() {
      return new LinkedHashSet<SuspectUrlVersion>();
    }
  }



  public static Test suite() {
    return new TestSuite(TestAuStateChecker.class);
  }
} 
