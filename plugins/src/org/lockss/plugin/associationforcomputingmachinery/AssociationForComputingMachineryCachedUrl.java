package org.lockss.plugin.associationforcomputingmachinery;

import java.io.Reader;

import org.lockss.filter.StringFilter;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.repository.LockssRepository;

public class AssociationForComputingMachineryCachedUrl extends org.lockss.plugin.base.BaseCachedUrl
{
	public AssociationForComputingMachineryCachedUrl(ArchivalUnit owner,
			String url) {
		super(owner, url);
	}
	
	/*
	 * Filters out ampersands from the InputStream to avoid bad XML
	 */
	@Override
	public Reader openForReading() {
	    try {
		    String[][] filterRules = {{"&#", "$%$amp;#"}, {"&", ""}, {"$%$amp;#", "&amp;#"}};
		    return StringFilter.makeNestedFilter(super.openForReading(), filterRules, true);
	    } catch (Exception e) {
	      logger.error("Creating InputStreamReader for '" + getUrl() + "'", e);
	      throw new LockssRepository.RepositoryStateException("Couldn't create InputStreamReader:" + e.toString());
	    }
	}
}
