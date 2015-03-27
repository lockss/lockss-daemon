/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.jws.WebService;
import org.lockss.app.LockssDaemon;
import org.lockss.metadata.Isbn;
import org.lockss.metadata.Issn;
import org.lockss.metadata.MetadataManager;
import org.lockss.util.Logger;
import org.lockss.ws.entities.KeyIdNamePairListPair;
import org.lockss.ws.entities.KeyValueListPair;
import org.lockss.ws.entities.IdNamePair;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * The MetadataMonitor web service implementation.
 */
@WebService
public class MetadataMonitorServiceImpl implements MetadataMonitorService {
  private static Logger log =
      Logger.getLogger(MetadataMonitorServiceImpl.class);

  /**
   * Provides the names of the publishers in the database.
   * 
   * @return a List<String> with the publisher names.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<String> getPublisherNames() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPublisherNames(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<String> results =
	  (List<String>)(getMetadataManager().getPublisherNames());

      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "results.size() = " + results.size());
      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the DOI prefixes for the publishers in the database with multiple
   * DOI prefixes.
   * 
   * @return a List<KeyValueListPair> with the DOI prefixes keyed by the
   *         publisher name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getPublishersWithMultipleDoiPrefixes()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPublishersWithMultipleDoiPrefixes(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the DOI prefixes linked to the publishers.
      Map<String, Collection<String>> publishersDoiPrefixes =
	  getMetadataManager().getPublishersWithMultipleDoiPrefixes();

      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "publishersDoiPrefixes.size() = " + publishersDoiPrefixes.size());

      // Check whether there are results to display.
      if (publishersDoiPrefixes.size() > 0) {
        // Yes: Loop through the publishers.
        for (String publisherName : publishersDoiPrefixes.keySet()) {
          if (log.isDebug3())
            log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

          ArrayList<String> prefixes = new ArrayList<String>();

          for (String prefix : publishersDoiPrefixes.get(publisherName)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);
            prefixes.add(prefix);
          }

          results.add(new KeyValueListPair(publisherName, prefixes));
        }
      }

      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "results.size() = " + results.size());
      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the publisher names linked to DOI prefixes in the database that
   * are linked to multiple publishers.
   * 
   * @return a List<KeyValueListPair> with the publisher names keyed by the DOI
   *         prefixes to which they are linked.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getDoiPrefixesWithMultiplePublishers()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getDoiPrefixesWithMultiplePublishers(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the publishers linked to the DOI prefixes.
      Map<String, Collection<String>> doiPrefixesPublishers =
	  ((MetadataManager) LockssDaemon
	      .getManager(LockssDaemon.METADATA_MANAGER))
	      .getDoiPrefixesWithMultiplePublishers();

      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "doiPrefixesPublishers.size() = " + doiPrefixesPublishers.size());

      // Check whether there are results to display.
      if (doiPrefixesPublishers.size() > 0) {
        // Yes: Loop through the prefixes.
        for (String prefix : doiPrefixesPublishers.keySet()) {
          if (log.isDebug3())
            log.debug3(DEBUG_HEADER + "prefix = " + prefix);

          ArrayList<String> publisherNames = new ArrayList<String>();

          for (String publisherName : doiPrefixesPublishers.get(prefix)) {
            if (log.isDebug3())
              log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);
            publisherNames.add(publisherName);
          }

          results.add(new KeyValueListPair(prefix, publisherNames));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the DOI prefixes for the Archival Units in the database with
   * multiple DOI prefixes.
   * 
   * @return a List<KeyValueListPair> with the DOI prefixes keyed by the
   *         Archival Unit name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getAuNamesWithMultipleDoiPrefixes()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuNamesWithMultipleDoiPrefixes(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the DOI prefixes linked to the Archival Units.
      Map<String, Collection<String>> ausDoiPrefixes =
	  ((MetadataManager) LockssDaemon
	      .getManager(LockssDaemon.METADATA_MANAGER))
	      .getAuNamesWithMultipleDoiPrefixes();

      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "ausDoiPrefixes.size() = " + ausDoiPrefixes.size());

      // Check whether there are results to display.
      if (ausDoiPrefixes.size() > 0) {
        // Yes: Loop through the Archival Unit names.
        for (String auName : ausDoiPrefixes.keySet()) {
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auName = " + auName);

          ArrayList<String> prefixes = new ArrayList<String>();

          for (String prefix : ausDoiPrefixes.get(auName)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);
            prefixes.add(prefix);
          }

          results.add(new KeyValueListPair(auName, prefixes));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the ISBNs for the publications in the database with more than two
   * ISBNS.
   * 
   * @return a List<KeyIdNamePairListPair> with the ISBNs keyed by the
   *         publication name. The IdNamePair objects contain the ISBN as the
   *         identifier and the ISBN type as the name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyIdNamePairListPair> getPublicationsWithMoreThan2Isbns()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPublicationsWithMoreThan2Isbns(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyIdNamePairListPair> results =
	  new ArrayList<KeyIdNamePairListPair>();

      Map<String, Collection<Isbn>> publicationsIsbns = ((MetadataManager)
	  LockssDaemon.getManager(LockssDaemon.METADATA_MANAGER))
	  .getPublicationsWithMoreThan2Isbns();

      for (String publication : publicationsIsbns.keySet()) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publication = " + publication);

	ArrayList<IdNamePair> isbnResults = new ArrayList<IdNamePair>();

	for (Isbn isbn : publicationsIsbns.get(publication)) {
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbn = " + isbn);
	  isbnResults.add(new IdNamePair(isbn.getValue(), isbn.getType()));
	}

        results.add(new KeyIdNamePairListPair(publication, isbnResults));
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the ISSNs for the publications in the database with more than two
   * ISSNS.
   * 
   * @return a List<KeyIdNamePairListPair> with the ISSNs keyed by the
   *         publication name. The IdNamePair objects contain the ISSN as the
   *         identifier and the ISSN type as the name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyIdNamePairListPair> getPublicationsWithMoreThan2Issns()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPublicationsWithMoreThan2Issns(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyIdNamePairListPair> results =
	  new ArrayList<KeyIdNamePairListPair>();

      Map<String, Collection<Issn>> publicationsIssns = ((MetadataManager)
	  LockssDaemon.getManager(LockssDaemon.METADATA_MANAGER))
	  .getPublicationsWithMoreThan2Issns();

      for (String publication : publicationsIssns.keySet()) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publication = " + publication);

	ArrayList<IdNamePair> issnResults = new ArrayList<IdNamePair>();

	for (Issn issn : publicationsIssns.get(publication)) {
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = " + issn);
	  issnResults.add(new IdNamePair(issn.getValue(), issn.getType()));
	}

        results.add(new KeyIdNamePairListPair(publication, issnResults));
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the publication names linked to ISBNs in the database that are
   * linked to multiple publications.
   * 
   * @return a List<KeyValueListPair> with the publication names keyed by the
   *         ISBNs to which they are linked.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getIsbnsWithMultiplePublications()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getIsbnsWithMultiplePublications(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the publications linked to the ISBNs.
      Map<String, Collection<String>> isbnsPublications = ((MetadataManager)
	  LockssDaemon.getManager(LockssDaemon.METADATA_MANAGER))
	  .getIsbnsWithMultiplePublications();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "isbnsPublications.size() = " + isbnsPublications.size());

      // Check whether there are results to display.
      if (isbnsPublications.size() > 0) {
        // Yes: Loop through the ISBNs. 
        for (String isbn : isbnsPublications.keySet()) {
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbn = " + isbn);

          ArrayList<String> prefixes = new ArrayList<String>();

          for (String prefix : isbnsPublications.get(isbn)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);
            prefixes.add(prefix);
          }

          results.add(new KeyValueListPair(isbn, prefixes));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the publication names linked to ISSNs in the database that are
   * linked to multiple publications.
   * 
   * @return a List<KeyValueListPair> with the publication names keyed by the
   *         ISSNs to which they are linked.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getIssnsWithMultiplePublications()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getIssnsWithMultiplePublications(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the publications linked to the ISSNs.
      Map<String, Collection<String>> issnsPublications = ((MetadataManager)
	  LockssDaemon.getManager(LockssDaemon.METADATA_MANAGER))
	  .getIssnsWithMultiplePublications();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "issnsPublications.size() = " + issnsPublications.size());

      // Check whether there are results to display.
      if (issnsPublications.size() > 0) {
        // Yes: Loop through the ISSNs. 
        for (String issn : issnsPublications.keySet()) {
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = " + issn);

          ArrayList<String> prefixes = new ArrayList<String>();

          for (String prefix : issnsPublications.get(issn)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "prefix = " + prefix);
            prefixes.add(prefix);
          }

          results.add(new KeyValueListPair(issn, prefixes));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the ISSNs for books in the database.
   * 
   * @return a List<KeyValueListPair> with the ISSNs keyed by the publication
   *         name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getBooksWithIssns()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getBooksWithIssns(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the ISSNs linked to the books.
      Map<String, Collection<String>> booksWithIssns = ((MetadataManager)
	  LockssDaemon.getManager(LockssDaemon.METADATA_MANAGER))
	  .getBooksWithIssns();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "booksWithIssns.size() = " + booksWithIssns.size());

      // Check whether there are results to display.
      if (booksWithIssns.size() > 0) {
        // Yes: Loop through the books.
        for (String bookName : booksWithIssns.keySet()) {
          if (log.isDebug3())
            log.debug3(DEBUG_HEADER + "bookName = " + bookName);

          ArrayList<String> issns = new ArrayList<String>();

          for (String issn : booksWithIssns.get(bookName)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = " + issn);
            issns.add(issn);
          }

          results.add(new KeyValueListPair(bookName, issns));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the ISBNs for periodicals in the database.
   * 
   * @return a List<KeyValueListPair> with the ISBNs keyed by the publication
   *         name.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<KeyValueListPair> getPeriodicalsWithIsbns()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getPeriodicalsWithIsbns(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      List<KeyValueListPair> results = new ArrayList<KeyValueListPair>();

      // Get the ISBNs linked to the periodicals.
      Map<String, Collection<String>> periodicalsWithIsbns = ((MetadataManager)
	  LockssDaemon.getManager(LockssDaemon.METADATA_MANAGER))
	  .getPeriodicalsWithIsbns();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
  	+ "periodicalsWithIsbns.size() = " + periodicalsWithIsbns.size());

      // Check whether there are results to display.
      if (periodicalsWithIsbns.size() > 0) {
        // Yes: Loop through the periodicals.
        for (String periodicalName : periodicalsWithIsbns.keySet()) {
          if (log.isDebug3())
            log.debug3(DEBUG_HEADER + "periodicalName = " + periodicalName);

          ArrayList<String> isbns = new ArrayList<String>();

          for (String isbn : periodicalsWithIsbns.get(periodicalName)) {
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbn = " + isbn);
            isbns.add(isbn);
          }

          results.add(new KeyValueListPair(periodicalName, isbns));
        }
      }

      return results;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the Archival Units in the database with an unknown provider.
   * 
   * @return a List<String> with the sorted Archival Unit names.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<String> getUnknownProviderAuIds() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getUnknownProviderAuIds(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
      return ((List<String>)((MetadataManager) LockssDaemon
	  .getManager(LockssDaemon.METADATA_MANAGER))
	  .getUnknownProviderAuIds());
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the metadata manager.
   * 
   * @return a MetadataManager with the metadata manager.
   */
  private MetadataManager getMetadataManager() {
    return (MetadataManager) LockssDaemon
	.getManager(LockssDaemon.METADATA_MANAGER);
  }
}
