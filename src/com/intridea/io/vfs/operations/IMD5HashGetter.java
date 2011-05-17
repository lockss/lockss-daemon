package com.intridea.io.vfs.operations;

import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.operations.FileOperation;

/**
 * Get md5 hash for file.
 *
 * @author <A href="mailto:alexey at abashev dot ru">Alexey Abashev</A>
 * @version $Id: IMD5HashGetter.java,v 1.1.2.1 2011-05-17 21:49:25 dshr Exp $
 */
public interface IMD5HashGetter extends FileOperation {
    /**
     * Get MD5 hash for object.
     *
     * @return
     */
    String getMD5Hash() throws FileSystemException;
}
