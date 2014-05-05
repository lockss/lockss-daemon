package org.lockss.plugin.oaipmh;

import java.io.Reader;

import org.lockss.daemon.Crawler.PermissionHelper;
import org.lockss.daemon.PermissionChecker;

public class OaiPmhTestPermissionChecker implements PermissionChecker {

	@Override
	public boolean checkPermission(PermissionHelper pHelper,
			Reader inputReader, String url) {
		return true;
	}

}
