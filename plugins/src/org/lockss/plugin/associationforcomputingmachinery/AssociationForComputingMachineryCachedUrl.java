package org.lockss.plugin.associationforcomputingmachinery;

import java.io.*;

import org.lockss.filter.StringFilter;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.repository.LockssRepository;
import org.lockss.util.Logger;


public class AssociationForComputingMachineryCachedUrl extends org.lockss.plugin.base.BaseCachedUrl
{
  CachedUrl cu;
  static Logger log = Logger.getLogger(AssociationForComputingMachineryCachedUrl.class);

  /**
   * @param owner
   * @param url
   */
  public AssociationForComputingMachineryCachedUrl(ArchivalUnit owner,
      String url) {
    super(owner, url);
    this.cu = owner.makeCachedUrl(url);     
  }
  
  @Override
  public boolean hasContent() {
    log.debug3(this.cu+"hasContent? = " +this.cu.hasContent());
    return this.cu.hasContent();
  }
  /*
   * Filters out unprotected ampersands from the InputStream to avoid bad XML
   * note to self: should not override BaseCachedUrl - should be moved 
   * (in the future, when there is time) to somewhere that only affects 
   * the metadata. (right now, no one else uses this, but, future-someone might...)
   */
  @Override
  public Reader openForReading() {
      try {
        log.debug3("  openForReading("+ cu.getUrl() +")");   
              /* The filter changes '&#'XXX into something unique (w/out &), 
               * then removes any remaining '&' chars, then changes the unique 
               * to a slightly different '&amp;#' -- this gets parsed in
               * AssociationForComputingMachineryXmlMetadataExtractorFactory:getValue()
               * and changed in ACMblahblah.fixUnicodeIn()
               */
              String[][] filterRules = {{"&#", "$%$amp;#"}, {"&", ""}, {"$%$amp;#", "&amp;#"}};
              //String[][] filterRules = {{"&#", "$%$amp;#"}, {"&", ""}, {"$%$amp;#", "&#"}};
              return StringFilter.makeNestedFilter(cu.openForReading(), filterRules, true);
      } catch (Exception e) {
        //logger.error("Creating InputStreamReader for '" + getUrl() + "'", e);
        throw new LockssRepository.RepositoryStateException("Couldn't create InputStreamReader:" + e.toString());
      }
  }

}
