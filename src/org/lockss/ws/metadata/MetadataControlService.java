/*

 Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.metadata;

import javax.jws.WebMethod;
import javax.jws.WebService;
import org.lockss.ws.entities.LockssWebServicesFault;
import org.lockss.ws.entities.MetadataControlResult;

/**
 * The MetadataMonitor web service interface.
 */
@WebService
public interface MetadataControlService {
  /**
   * Deletes an ISSN linked to a publication.
   * 
   * @param mdItemSeq
   *          A Long with the publication metadata identifier.
   * @param issn
   *          A String with the ISSN.
   * @param issnType
   *          A String with the ISSN type.
   * @return a MetadataControlResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  MetadataControlResult deletePublicationIssn(Long mdItemSeq, String issn,
      String issnType) throws LockssWebServicesFault;

  /**
   * Deletes an Archival Unit and its metadata.
   * 
   * @param auSeq
   *          A Long with the Archival Unit database identifier.
   * @param auKey
   *          A String with the Archival Unit key identifier.
   * @return a MetadataControlResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  MetadataControlResult deleteAu(Long auSeq, String auKey)
      throws LockssWebServicesFault;
}
