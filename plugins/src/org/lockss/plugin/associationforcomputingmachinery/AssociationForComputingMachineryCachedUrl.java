package org.lockss.plugin.associationforcomputingmachinery;

import java.io.InputStream;
import java.io.Reader;

import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.filter.StringFilter;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchiveMemberSpec;
import org.lockss.plugin.CachedUrl;
import org.lockss.repository.LockssRepository;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.util.CIProperties;
import org.lockss.util.HashedInputStream.Hasher;

public class AssociationForComputingMachineryCachedUrl implements org.lockss.plugin.CachedUrl
{
    CachedUrl cu;
	/*public AssociationForComputingMachineryCachedUrl(ArchivalUnit owner,
			String url) {
		super(owner, url);
	}*/
	public AssociationForComputingMachineryCachedUrl(CachedUrl cu) {
	  this.cu = cu;
   }
	/*
	 * Filters out ampersands from the InputStream to avoid bad XML
	 */
	@Override
	public Reader openForReading() {
	    try {
		    String[][] filterRules = {{"&#", "$%$amp;#"}, {"&", ""}, {"$%$amp;#", "&amp;#"}};
		    return StringFilter.makeNestedFilter(cu.openForReading(), filterRules, true);
	    } catch (Exception e) {
	      //logger.error("Creating InputStreamReader for '" + getUrl() + "'", e);
	      throw new LockssRepository.RepositoryStateException("Couldn't create InputStreamReader:" + e.toString());
	    }
	}
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrlSetNode#getUrl()
   */
  @Override
  public String getUrl() {
    // 
    return cu.getUrl();
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrlSetNode#getType()
   */
  @Override
  public int getType() {
    // TODO Auto-generated method stub
    return 0;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrlSetNode#hasContent()
   */
  @Override
  public boolean hasContent() {
    return cu.hasContent();
    //return false;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrlSetNode#isLeaf()
   */
  @Override
  public boolean isLeaf() {
    // TODO Auto-generated method stub
    return false;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getCuVersion(int)
   */
  @Override
  public CachedUrl getCuVersion(int version) {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getCuVersions()
   */
  @Override
  public CachedUrl[] getCuVersions() {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getCuVersions(int)
   */
  @Override
  public CachedUrl[] getCuVersions(int maxVersions) {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getVersion()
   */
  @Override
  public int getVersion() {
    // TODO Auto-generated method stub
    return 0;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getUnfilteredInputStream()
   */
  @Override
  public InputStream getUnfilteredInputStream() {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getUnfilteredInputStream(org.lockss.util.HashedInputStream.Hasher)
   */
  @Override
  public InputStream getUnfilteredInputStream(Hasher hasher) {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#openForHashing()
   */
  @Override
  public InputStream openForHashing() {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#openForHashing(org.lockss.util.HashedInputStream.Hasher)
   */
  @Override
  public InputStream openForHashing(Hasher hasher) {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getLinkRewriterFactory()
   */
  @Override
  public LinkRewriterFactory getLinkRewriterFactory() {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getProperties()
   */
  @Override
  public CIProperties getProperties() {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#addProperty(java.lang.String, java.lang.String)
   */
  @Override
  public void addProperty(String key, String value) {
    // TODO Auto-generated method stub
    
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getContentSize()
   */
  @Override
  public long getContentSize() {
    // TODO Auto-generated method stub
    return 0;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getContentType()
   */
  @Override
  public String getContentType() {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getEncoding()
   */
  @Override
  public String getEncoding() {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getArchivalUnit()
   */
  @Override
  public ArchivalUnit getArchivalUnit() {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#release()
   */
  @Override
  public void release() {
    // TODO Auto-generated method stub
    
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getFileMetadataExtractor(org.lockss.extractor.MetadataTarget)
   */
  @Override
  public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target) {
    // TODO Auto-generated method stub
    return null;
  }
  /* (non-Javadoc)
   * @see org.lockss.plugin.CachedUrl#getArchiveMemberCu(org.lockss.plugin.ArchiveMemberSpec)
   */
  @Override
  public CachedUrl getArchiveMemberCu(ArchiveMemberSpec ams) {
    // TODO Auto-generated method stub
    return null;
  }
}
