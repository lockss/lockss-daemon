package org.lockss.plugin.oaipmh;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.lockss.crawler.CrawlInitializer;
import org.lockss.crawler.CrawlInitializerFactory;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.CrawlSpec;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

import com.lyncode.xoai.model.oaipmh.Granularity;
import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.serviceprovider.*;
import com.lyncode.xoai.serviceprovider.client.HttpOAIClient;
import com.lyncode.xoai.serviceprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.serviceprovider.model.Context;
import com.lyncode.xoai.serviceprovider.parameters.*;

public class OaiPmhCrawlInitializerFactory implements CrawlInitializerFactory {
	@Override
	public CrawlInitializer createCrawlInitializer(ArchivalUnit au, CrawlSpec spec) {
		return new OaiPmhCrawlInitializer(au, spec);
	}
	
	public static class OaiPmhCrawlInitializer 
		implements CrawlInitializer{
		private static Logger logger = Logger.getLogger("OaiPmhCrawlInitializer");
		
		private static final String DEFAULT_METADATA_PREFIX = "oai_dc";
		public static final String GRANULARITY_DAY = "YYYY-MM-DD";
		public static final String GRANULARITY_SECOND = "YYYY-MM-DDThh:mm:ssZ";
		public static final String DATE_FORMAT = "YYYY-MM-DD'T'hh:mm:ss";
		public static final String KEY_AU_OAI_FROM_DATE = "oai_from_date";
		public static final String KEY_AU_OAI_UNTIL_DATE = "oai_until_date";
		public static final String KEY_AU_OAI_SET = "oai_set";	
		private static final String DEFAULT_GRANULARITY = GRANULARITY_SECOND;
		
		private ServiceProvider sp;
		private String url;
		private Date from;
		private Date until;
		private String set;
		private String metadataPrefix;
		
		public OaiPmhCrawlInitializer (ArchivalUnit au, CrawlSpec spec) {
			TypedEntryMap props = au.getProperties();
			if(props.containsKey(ConfigParamDescr.YEAR.getKey())) {
				setDates(props.getInt(ConfigParamDescr.YEAR.getKey()));
			} else if (props.containsKey(KEY_AU_OAI_FROM_DATE) && props.containsKey(KEY_AU_OAI_FROM_DATE)) {
				setDates(props.getString(KEY_AU_OAI_FROM_DATE), props.getString(KEY_AU_OAI_UNTIL_DATE));
			} else {
				//throw new PluginException();
			}
			if(props.containsKey(ConfigParamDescr.BASE_URL.getKey())) {
				this.url = props.getString(ConfigParamDescr.BASE_URL.getKey());
			} else {
				//throw?
			}
			
			/*List<String> urls = spec.getStartingUrls();
			if(urls.size() != 1) {
				//throw new PluginException();
			} else {
				this.url = urls.get(0);
			}*/
			
			if(props.containsKey(KEY_AU_OAI_SET)) {
				this.set = props.getString(KEY_AU_OAI_SET);
			} else {
				set = null;
			}
			setMetadataPrefix(null);
			
			this.sp = new ServiceProvider(buildContext(url));
		}
		
		private void setDates(int year) {
			String postfix = "-01-01T00:00:00";
			setDates(year + postfix, (year + 1) + postfix);
		}
		
		private void setDates(String from, String until) {
			TimeZone utc = TimeZone.getTimeZone("UTC");
			DateFormat df = new SimpleDateFormat(DATE_FORMAT);
			df.setTimeZone(utc);
			try {
				this.from = df.parse(from);
				this.until = df.parse(until);
				logger.critical("From: " + from + ", Until: " + until);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void setMetadataPrefix(String metadataPrefix) {
			if(metadataPrefix == null) {
				this.metadataPrefix = DEFAULT_METADATA_PREFIX;
			} else {
				this.metadataPrefix = metadataPrefix;
			}
		}

		public Context buildContext(String url) {
			Context con = new Context();
			con.withBaseUrl(url + "oai/request");
			con.withGranularity(Granularity.fromRepresentation(DEFAULT_GRANULARITY));
			con.withOAIClient(new HttpOAIClient(con));
			return con;
		}
		
		public ListIdentifiersParameters buildParams(Date from, Date until, String set, String metadataPrefix) {
			ListIdentifiersParameters mip = ListIdentifiersParameters.request();
			mip.withMetadataPrefix(metadataPrefix);
			mip.withFrom(from);
			mip.withUntil(until);
			if(set != null) {
				mip.withSetSpec(set);
			}
			return mip;
		}

		@Override
		public Collection<String> getUrlList(){
			try {
				Collection<String> urlList;
				urlList = new ArrayList<String>();
				for(Iterator<Header> idIter = sp.listIdentifiers(buildParams(this.from, this.until, this.set, this.metadataPrefix)); idIter.hasNext(); ) {
					Header h = idIter.next();
					String id = h.getIdentifier();
					urlList.addAll(idToUrls(id, url));
				}
				return urlList;
			} catch (BadArgumentException e) {
				e.printStackTrace();
				return new ArrayList<String>();
			}
		}
		
		public List<String> idToUrls(String id, String url) {
			if(id.contains(":") && !id.endsWith(":")) {
				String id_num = id.substring(id.lastIndexOf(':') + 1);
				return Arrays.asList(url + "oai/request?verb=GetRecord&identifier=" + id + "&metadataPrefix=" + metadataPrefix, url + "handle/" + id_num);
			} else {
				return Arrays.asList(url + "oai/request?verb=GetRecord&identifier=" + id + "&metadataPrefix=" + metadataPrefix);
			}
		}
	}

}
