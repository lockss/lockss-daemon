package org.lockss.truezip.app;

import de.schlichtherle.truezip.file.TFile;
import java.io.IOException;

/**
 * This command line utility recursively copies the first file or directory
 * argument to the second file or directory argument.
 * Instead of a directory, you can name any configured archive file type in
 * the path names, too.
 * If you name any archive files in the destination path name, they get
 * automatically created.
 * <p>
 * For example, if the JAR for the module {@code truezip-driver-zip} is
 * present on the run time class path and the destination path name is
 * {@code archive.zip}, a ZIP file with this name gets created unless it
 * already exists.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class Copy extends Application<IOException> {

    public static void main(String[] args) throws IOException {
        System.exit(new Copy().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        // Setup the file operands.
        TFile src = new TFile(args[0]);
        TFile dst = new TFile(args[1]);

        // TFile  doesn't do path name completion, so we do it manually in
        // order to emulate the behavior of many copy command line utilities.
        if (TFile.isLenient() && dst.isArchive() || dst.isDirectory())
            dst = new TFile(dst, src.getName());

        // If TFile.setLenient(false) is never called in your application,
        // then you might as well shorten this to...
        /*if (dst.isArchive() || dst.isDirectory())
            dst = new TFile(dst, src.getName());*/

        // If you don't like path name completion for non-existent files which
        // just look like archive files according to their path name,
        // then you might even shorten this to...
        /*if (dst.isDirectory())
            dst = new TFile(dst, src.getName());*/

        // Perform a recursive archive copy.
        src.cp_rp(dst);

        return 0;
    }
}
