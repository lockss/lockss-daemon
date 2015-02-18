package org.lockss.truezip.app;

import de.schlichtherle.truezip.key.pbe.swing.HurlingWindowFeedback;
import de.schlichtherle.truezip.key.pbe.swing.InvalidKeyFeedback;
import de.schlichtherle.truezip.file.TApplication;

/**
 * An abstract class which configures the TrueZIP File* module.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
abstract class Application<E extends Exception> extends TApplication<E> {

    /**
     * Runs the setup phase.
     * <p>
     * This method is {@link #run run} only once at the start of the life
     * cycle.
     * Its task is to configure the default behavior of the TrueZIP File* API
     * in order to answer the following questions:
     * <ul>
     * <li>What are the file suffixes which shall be recognized as archive
     *     files and hence as virtual directories?
     * <li>Shall missing archive files and directory entries get automatically
     *     created whenever required?
     * </ul>
     * <p>
     * The implementation in the class {@link Application} configures
     * the type of the feedback when prompting the user for keys for RAES
     * encrypted ZIP alias ZIP.RAES alias TZP files by the Swing based
     * prompting key manager.
     * If this JVM is running in headless mode, then this configuration is
     * ignored and the user is prompted by the console I/O based prompting
     * key manager.
     */
    @Override
    protected void setup() {
        String spec = InvalidKeyFeedback.class.getName();
        String impl = HurlingWindowFeedback.class.getName();
        System.setProperty(spec, System.getProperty(spec, impl));
    }

    /*@Override
    protected void sync() throws FsSyncException {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ex) {
            Logger  .getLogger(Application.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        super.sync();
    }*/
}
