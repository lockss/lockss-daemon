/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository;

/**
 * <p>
 * As part of the LAAWS (LOCKSS Architected as Web Services) initiative, a new
 * {@code LockssRepository} interface is being defined. The old one, formerly
 * found here, has been renamed {@link OldLockssRepository}, but this interface
 * is being kept so that plugins that reference it can transition to the new
 * name after a daemon release debuting the new name.
 * </p>
 * 
 * @deprecated Use {@link OldLockssRepository} instead.
 * @since 1.74
 * @see OldLockssRepository
 */
@Deprecated
public interface LockssRepository extends OldLockssRepository {

  /**
   * <p>
   * As part of the LAAWS (LOCKSS Architected as Web Services) initiative, a new
   * {@code LockssRepository} interface is being defined. The old one, formerly
   * found here, has been renamed {@link OldLockssRepository}, but this class
   * is being kept so that plugins that reference it can transition to the new
   * name after a daemon release debuting the new name.
   * </p>
   * 
   * @deprecated Use {@link OldLockssRepository.RepositoryStateException} instead.
   * @since 1.74
   * @see OldLockssRepository.RepositoryStateException
   */
  @Deprecated
  public class RepositoryStateException extends OldLockssRepository.RepositoryStateException {
    
    /**
     * @deprecated Use {@link OldLockssRepository.RepositoryStateException#RepositoryStateException()} instead.
     */
    @Deprecated
    public RepositoryStateException() {
      super();
    }
    
    /**
     * @deprecated Use {@link OldLockssRepository.RepositoryStateException#RepositoryStateException(String)} instead.
     */
    @Deprecated
    public RepositoryStateException(String msg) {
      super(msg);
    }
    
    /**
     * @deprecated Use {@link OldLockssRepository.RepositoryStateException#RepositoryStateException(Throwable)} instead.
     */
    @Deprecated
    public RepositoryStateException(Throwable cause) {
      super(cause);
    }

    /**
     * @deprecated Use {@link OldLockssRepository.RepositoryStateException#RepositoryStateException(String, Throwable)} instead.
     */
    @Deprecated
    public RepositoryStateException(String msg, Throwable cause) {
      super(msg, cause);
    }

  }

}
