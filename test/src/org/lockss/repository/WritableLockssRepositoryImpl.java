package org.lockss.repository;

import java.net.MalformedURLException;

import org.lockss.app.LockssDaemon;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.Plugin;

public class WritableLockssRepositoryImpl extends LockssRepositoryImpl {
    boolean dontNormalize = false;
    void setDontNormalize(boolean val) {
      dontNormalize = val;
    }

    WritableLockssRepositoryImpl(String rootPath) {
      super(rootPath);
    }

    public String canonicalizePath(String url)
    throws MalformedURLException {
      if (dontNormalize) return url;
      return super.canonicalizePath(url);
    }

    public static LockssRepository createNewLockssRepository(String root, ArchivalUnit au) {
//      String root = getRepositoryRoot(au);
//      if (root == null) {
//        throw new LockssRepository.RepositoryStateException("null root");
//      }
      String auDir = LockssRepositoryImpl.mapAuToFileLocation(root, au);
      // staticCacheLocation = extendCacheLocation(root);
      LockssRepositoryImpl repo = new WritableLockssRepositoryImpl(auDir);
      Plugin plugin = au.getPlugin();
      if (plugin != null) {
        LockssDaemon daemon = plugin.getDaemon();
        if (daemon != null) {
          RepositoryManager mgr = daemon.getRepositoryManager();
          if (mgr != null) {
            mgr.setRepositoryForPath(auDir, repo);
          }
        }
      }
      return repo;
    }
}
