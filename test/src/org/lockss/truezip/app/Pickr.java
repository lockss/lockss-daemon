package org.lockss.truezip.app;

import de.schlichtherle.truezip.file.swing.TFileChooser;
import javax.swing.SwingUtilities;

/**
 * This command line utility lets you pick a file using a {@link TFileChooser}
 * and prints it's path.
 * Of course, {@code TFileChooser} can browse archive files, too.
 * <p>
 * For example, if the JAR for the module {@code truezip-driver-zip} is present
 * on the run time class path and a ZIP file {@code archive.zip} exists, then
 * you can double click it to browse its entries.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class Pickr extends Application<Exception> {

    /** Equivalent to {@code System.exit(new Pickr().run(args));}. */
    public static void main(String[] args) throws Exception {
        System.exit(new Pickr().run(args));
    }

    @Override
    protected int work(final String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                final TFileChooser pickr = new TFileChooser();
                if (TFileChooser.APPROVE_OPTION == pickr.showDialog(
                        null, "Pick this!"))
                    System.out.println(pickr.getSelectedFile());
            }
        });
        return 0;
    }
}
