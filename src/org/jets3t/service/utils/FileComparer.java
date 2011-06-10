/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2008 James Murty
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jets3t.service.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.ServiceException;
import org.jets3t.service.StorageObjectsChunk;
import org.jets3t.service.StorageService;
import org.jets3t.service.io.BytesProgressWatcher;
import org.jets3t.service.io.ProgressMonitoredInputStream;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.multi.StorageServiceEventAdaptor;
import org.jets3t.service.multi.StorageServiceEventListener;
import org.jets3t.service.multi.ThreadedStorageService;
import org.jets3t.service.multi.event.GetObjectHeadsEvent;
import org.jets3t.service.multi.event.ListObjectsEvent;

/**
 * File comparison utility to compare files on the local computer with objects present in a service
 * account and determine whether there are any differences. This utility contains methods to
 * build maps of the contents of the local file system or service account for comparison, and
 * methods to find differences in these maps.
 * <p>
 * File comparisons are based primarily on MD5 hashes of the files' contents. If a local file does
 * not match an object in the service with the same name, this utility determine which of the items is
 * newer by comparing the last modified dates.
 *
 * @author James Murty
 */
public class FileComparer {
    private static final Log log = LogFactory.getLog(FileComparer.class);

    private Jets3tProperties jets3tProperties = null;

    /**
     * Constructs the class.
     *
     * @param jets3tProperties
     * the object containing the properties that will be applied in this class.
     */
    public FileComparer(Jets3tProperties jets3tProperties) {
        this.jets3tProperties = jets3tProperties;
    }

    /**
     * @param jets3tProperties
     * the object containing the properties that will be applied in the instance.
     * @return
     * a FileComparer instance.
     */
    public static FileComparer getInstance(Jets3tProperties jets3tProperties) {
        return new FileComparer(jets3tProperties);
    }

    /**
     * @return
     * a FileComparer instance initialized with the default JetS3tProperties
     * object.
     */
    public static FileComparer getInstance() {
        return new FileComparer(
            Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME));
    }

    /**
     * If a <code>.jets3t-ignore</code> file is present in the given directory, the file is read
     * and all the paths contained in it are coverted to regular expression Pattern objects.
     * If the parent directory's list of patterns is provided, any relevant patterns are also
     * added to the ignore listing. Relevant parent patterns are those with a directory prefix
     * that matches the current directory, or with the wildcard depth pattern (*.*./).
     *
     * @param directory
     * a directory that may contain a <code>.jets3t-ignore</code> file. If this parameter is null
     * or is actually a file and not a directory, an empty list will be returned.
     * @param parentIgnorePatternList
     * a list of Patterns that were applied to the parent directory of the given directory. If this
     * parameter is null, no parent ignore patterns are applied.
     *
     * @return
     * a list of Pattern objects representing the paths in the ignore file. If there is no ignore
     * file, or if it has no contents, the list returned will be empty.
     */
    protected List<Pattern> buildIgnoreRegexpList(File directory, List<Pattern> parentIgnorePatternList) {
        List<Pattern> ignorePatternList = new ArrayList<Pattern>();

        // Add any applicable ignore patterns found in ancestor directories
        if (parentIgnorePatternList != null) {
            Iterator<Pattern> parentIgnorePatternIter = parentIgnorePatternList.iterator();
            while (parentIgnorePatternIter.hasNext()) {
                Pattern parentPattern = parentIgnorePatternIter.next();
                String parentIgnorePatternString = parentPattern.pattern();

                // If parent ignore pattern contains a slash, it is eligible for inclusion.
                int slashOffset = parentIgnorePatternString.indexOf(Constants.FILE_PATH_DELIM);
                if (slashOffset >= 0 && parentIgnorePatternString.length() > (slashOffset + 1)) { // Ensure there is at least 1 char after slash
                    // Chop pattern into header and tail around first slash character.
                    String patternHeader = parentIgnorePatternString.substring(0, slashOffset);
                    String patternTail = parentIgnorePatternString.substring(slashOffset + 1);

                    if (".*.*".equals(patternHeader)) {
                        // ** patterns are special and apply to any directory depth, so add both the
                        // pattern's tail to match in this directory, and the original pattern to match
                        // again in descendent directories.
                        ignorePatternList.add(Pattern.compile(patternTail));
                        ignorePatternList.add(parentPattern);
                    } else if (Pattern.compile(patternHeader).matcher(directory.getName()).matches()) {
                        // Adds pattern's tail section to ignore list for this directory, provided
                        // the pre-slash pattern matches the current directory's name.
                        ignorePatternList.add(Pattern.compile(patternTail));
                    }
                }
            }
        }

        if (directory == null || !directory.isDirectory()) {
            return ignorePatternList;
        }

        File jets3tIgnoreFile = new File(directory, Constants.JETS3T_IGNORE_FILENAME);
        if (jets3tIgnoreFile.exists() && jets3tIgnoreFile.canRead()) {
            if (log.isDebugEnabled()) {
                log.debug("Found ignore file: " + jets3tIgnoreFile.getPath());
            }
            try {
                String ignorePaths = ServiceUtils.readInputStreamToString(
                    new FileInputStream(jets3tIgnoreFile), null);
                StringTokenizer st = new StringTokenizer(ignorePaths.trim(), "\n");
                while (st.hasMoreTokens()) {
                    String ignorePath = st.nextToken();

                    // Convert path to RegExp.
                    String ignoreRegexp = ignorePath;
                    ignoreRegexp = ignoreRegexp.replaceAll("\\.", "\\\\.");
                    ignoreRegexp = ignoreRegexp.replaceAll("\\*", ".*");
                    ignoreRegexp = ignoreRegexp.replaceAll("\\?", ".");

                    Pattern pattern = Pattern.compile(ignoreRegexp);
                    if (log.isDebugEnabled()) {
                        log.debug("Ignore path '" + ignorePath + "' has become the regexp: "
                        + pattern.pattern());
                    }
                    ignorePatternList.add(pattern);

                    if (pattern.pattern().startsWith(".*.*/") && pattern.pattern().length() > 5) {
                        // **/ patterns are special and apply to any directory depth, including the current
                        // directory. So add the pattern's after-slash tail to match in this directory as well.
                        ignorePatternList.add(Pattern.compile(pattern.pattern().substring(5)));
                    }

                }
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Failed to read contents of ignore file '" + jets3tIgnoreFile.getPath()
                    + "'", e);
                }
            }
        }

        if (isSkipMd5FileUpload()) {
            Pattern pattern = Pattern.compile(".*\\.md5");
            if (log.isDebugEnabled()) {
                log.debug("Skipping upload of pre-computed MD5 files with path '*.md5' using the regexp: "
                + pattern.pattern());
            }
            ignorePatternList.add(pattern);
        }

        return ignorePatternList;
    }

    /**
     * Determines whether a file should be ignored when building a file map. A file may be ignored
     * in two situations: 1) if it matches a regular expression pattern in the given list of
     * ignore patterns, or 2) if it is a symlink/alias and the JetS3tProperties setting
     * "filecomparer.skip-symlinks" is true.
     *
     * @param ignorePatternList
     * a list of Pattern objects representing the file names to ignore.
     * @param file
     * a file that will either be ignored or not, depending on whether it matches an ignore Pattern
     * or is a symlink/alias.
     *
     * @return
     * true if the file should be ignored, false otherwise.
     */
    protected boolean isIgnored(List<Pattern> ignorePatternList, File file) {
        if (isSkipSymlinks()) {
            /*
             * Check whether this file is actually a symlink/alias, and skip it if so.
             * Since Java IO libraries do not provide an official way to determine whether
             * a file is a symlink, we rely on a property of symlinks where the absolute
             * path to the symlink differs from the canonical path. This is hacky, but
             * mostly seems to work...
             */
            try {
                if (!file.getAbsolutePath().equals(file.getCanonicalPath())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Ignoring symlink "
                            + (file.isDirectory() ? "directory" : "file")
                            + ": " + file.getPath());
                    }
                    // Skip symlink.
                    return true;
                }
            } catch (IOException e) {
                log.warn("Unable to determine whether "
                    + (file.isDirectory() ? "directory" : "file")
                    + " '" + file.getAbsolutePath() + "' is a symlink", e);
            }
        }

        // Skip 'special' files that are neither files nor directories
        if (!file.isFile() && !file.isDirectory()) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring special file: " + file.getPath());
            }
            return true;
        }

        Iterator<Pattern> patternIter = ignorePatternList.iterator();
        while (patternIter.hasNext()) {
            Pattern pattern = patternIter.next();

            if (pattern.matcher(file.getName()).matches()) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring " + (file.isDirectory() ? "directory" : "file")
                    + " matching pattern '" + pattern.pattern() + "': " + file.getPath());
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Normalize string into "Normalization Form Canonical Decomposition" (NFD).
     *
     * References:
     * http://stackoverflow.com/questions/3610013
     * http://en.wikipedia.org/wiki/Unicode_equivalence
     *
     * @param str
     * @return string normalized into NFC form.
     */
    protected String normalizeUnicode(String str) {
        Normalizer.Form form = Normalizer.Form.NFD;
        if (!Normalizer.isNormalized(str, form)) {
            return Normalizer.normalize(str, form);
        }
        return str;
    }

    /**
     * Builds a map of files and directories that exist on the local system, where the map
     * keys are the object key names that will be used for the files in a remote storage
     * service, and the map values are absolute paths (Strings) to that file in the local
     * file system. The entire local file hierarchy within the given set of files and
     * directories is traversed (i.e. sub-directories are included.)
     * <p>
     * A file/directory hierarchy is represented using '/' delimiter characters in
     * object key names.
     * <p>
     * Any file or directory matching a path in a <code>.jets3t-ignore</code> file will be ignored.
     *
     * @param fileList
     * the set of files and directories to include in the file map.
     * @param fileKeyPrefix
     * A prefix added to each file path key in the map, e.g. the name of the root directory the
     * files belong to. If provided, a '/' suffix is always added to the end of the prefix. If null
     * or empty, no prefix is used.
     * @param includeDirectories
     * If true all directories, including empty ones, will be included in the Map. These directories
     * will be mere place-holder objects with a trailing slash (/) character in the name and the
     * content type {@link Mimetypes#MIMETYPE_BINARY_OCTET_STREAM}.
     * If this variable is false directory objects will not be included in the Map, and it will not
     * be possible to store empty directories in the service.
     *
     * @return
     * a Map of file path keys to File objects.
     */
    public Map<String, String> buildObjectKeyToFilepathMap(
        File[] fileList, String fileKeyPrefix, boolean includeDirectories)
    {
        if (fileKeyPrefix == null || fileKeyPrefix.trim().length() == 0) {
            fileKeyPrefix = "";
        }

        // Build map of files proposed for upload or download.
        Map<String, String> objectKeyToFilepathMap = new TreeMap<String, String>();
        List<Pattern> ignorePatternList = null;
        List<Pattern> ignorePatternListForCurrentDir = null;

        for (File file: fileList) {
            if (file.getParentFile() == null) {
                // For direct references to a file or dir, look for a .jets3t-ignore file
                // in the current directory - only do this once for the current dir.
                if (ignorePatternListForCurrentDir == null) {
                    ignorePatternListForCurrentDir = buildIgnoreRegexpList(new File("."), null);
                }
                ignorePatternList = ignorePatternListForCurrentDir;
            } else {
                ignorePatternList = buildIgnoreRegexpList(file.getParentFile(), null);
            }

            if (!isIgnored(ignorePatternList, file)) {
                if (!file.exists()) {
                    continue;
                }
                String objectKeyName = normalizeUnicode(file.getName());
                if (!file.isDirectory()) {
                    objectKeyToFilepathMap.put(objectKeyName, file.getAbsolutePath());
                } else {
                    objectKeyName += Constants.FILE_PATH_DELIM;
                    if (includeDirectories) {
                        objectKeyToFilepathMap.put(objectKeyName, file.getAbsolutePath());
                    }
                    buildObjectKeyToFilepathMapForDirectory(
                        file, objectKeyName, objectKeyToFilepathMap,
                        includeDirectories, ignorePatternList);
                }
            }
        }
        return objectKeyToFilepathMap;
    }

    /**
     * Recursively builds a map of object key names to file paths that contains
     * all the files and directories inside the given directory. The map
     * keys are the object key names that will be used for the files in a remote storage
     * service, and the map values are absolute paths (Strings) to that file in the local
     * file system.
     * <p>
     * A file/directory hierarchy is represented using '/' delimiter characters in
     * object key names.
     * <p>
     * Any file or directory matching a path in a <code>.jets3t-ignore</code> file will be ignored.
     *
     * @param directory
     * The directory containing the files/directories of interest. The directory is <b>not</b>
     * included in the result map.
     * @param fileKeyPrefix
     * A prefix added to each file path key in the map, e.g. the name of the root directory the
     * files belong to. This prefix <b>must</b> end with a '/' character.
     * @param objectKeyToFilepathMap
     * map of '/'-delimited object key names to local file absolute paths, to which this method adds items.
     * @param includeDirectories
     * If true all directories, including empty ones, will be included in the Map. These directories
     * will be mere place-holder objects with a trailing slash (/) character in the name and the
     * content type {@link Mimetypes#MIMETYPE_BINARY_OCTET_STREAM}.
     * If this variable is false directory objects will not be included in the Map, and it will not
     * be possible to store empty directories in the service.
     * @param parentIgnorePatternList
     * a list of Patterns that were applied to the parent directory of the given directory. This list
     * will be checked to see if any of the parent's patterns should apply to the current directory.
     * See {@link #buildIgnoreRegexpList(File, List)} for more information.
     * If this parameter is null, no parent ignore patterns are applied.
     */
    protected void buildObjectKeyToFilepathMapForDirectory(File directory, String fileKeyPrefix,
        Map<String, String> objectKeyToFilepathMap, boolean includeDirectories,
        List<Pattern> parentIgnorePatternList)
    {
        List<Pattern> ignorePatternList = buildIgnoreRegexpList(directory, parentIgnorePatternList);

        for (File childFile: directory.listFiles()) {
            if (!isIgnored(ignorePatternList, childFile)) {
                String objectKeyName = normalizeUnicode(fileKeyPrefix + childFile.getName());

                if (!childFile.isDirectory()) {
                    objectKeyToFilepathMap.put(objectKeyName, childFile.getAbsolutePath());
                } else {
                    objectKeyName += Constants.FILE_PATH_DELIM;
                    if (includeDirectories) {
                        objectKeyToFilepathMap.put(objectKeyName, childFile.getAbsolutePath());
                    }
                    buildObjectKeyToFilepathMapForDirectory(
                        childFile, objectKeyName, objectKeyToFilepathMap,
                        includeDirectories, ignorePatternList);
                }
            }
        }
    }

    /**
     * Lists the objects in a bucket using a partitioning technique to divide
     * the object namespace into separate partitions that can be listed by
     * multiple simultaneous threads. This method divides the object namespace
     * using the given delimiter, traverses this space up to the specified
     * depth to identify prefix names for multiple "partitions", and
     * then lists the objects in each partition. It returns the complete list
     * of objects in the bucket path.
     * <p>
     * This partitioning technique will work best for buckets with many objects
     * that are divided into a number of virtual subdirectories of roughly equal
     * size.
     *
     * @param service
     * the service object that will be used to perform listing requests.
     * @param bucketName
     * the name of the bucket whose contents will be listed.
     * @param targetPath
     * a root path within the bucket to be listed. If this parameter is null, all
     * the bucket's objects will be listed. Otherwise, only the objects below the
     * virtual path specified will be listed.
     * @param delimiter
     * the delimiter string used to identify virtual subdirectory partitions
     * in a bucket. If this parameter is null, or it has a value that is not
     * present in your object names, no partitioning will take place.
     * @param toDepth
     * the number of delimiter levels this method will traverse to identify
     * subdirectory partions. If this value is zero, no partitioning will take
     * place.
     *
     * @return
     * the list of objects under the target path in the bucket.
     *
     * @throws ServiceException
     */
    public StorageObject[] listObjectsThreaded(StorageService service,
        final String bucketName, String targetPath, final String delimiter, int toDepth)
        throws ServiceException
    {
        final List<StorageObject> allObjects =
            Collections.synchronizedList(new ArrayList<StorageObject>());
        final List<String> lastCommonPrefixes =
            Collections.synchronizedList(new ArrayList<String>());
        final ServiceException serviceExceptions[] = new ServiceException[1];

        /*
         * Create a ThreadedStorageService object with an event listener that responds to
         * ListObjectsEvent notifications and populates a complete object listing.
         */
        final ThreadedStorageService threadedService = new ThreadedStorageService(service,
            new StorageServiceEventAdaptor() {
            @Override
            public void event(ListObjectsEvent event) {
                if (ListObjectsEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                    Iterator<StorageObjectsChunk> chunkIter = event.getChunkList().iterator();
                    while (chunkIter.hasNext()) {
                        StorageObjectsChunk chunk = chunkIter.next();

                        if (log.isDebugEnabled()) {
                            log.debug("Listed " + chunk.getObjects().length
                            + " objects and " + chunk.getCommonPrefixes().length
                            + " common prefixes in bucket '" + bucketName
                            + "' using prefix=" + chunk.getPrefix()
                            + ", delimiter=" + chunk.getDelimiter());
                        }

                        allObjects.addAll(Arrays.asList(chunk.getObjects()));
                        lastCommonPrefixes.addAll(Arrays.asList(chunk.getCommonPrefixes()));
                    }
                } else if (ListObjectsEvent.EVENT_ERROR == event.getEventCode()) {
                    serviceExceptions[0] = new ServiceException(
                        "Failed to list all objects in bucket",
                        event.getErrorCause());
                }
            }
        });

        // The first listing partition we use as a starting point is the target path.
        String[] prefixesToList = new String[] {targetPath};
        int currentDepth = 0;

        while (currentDepth <= toDepth && prefixesToList.length > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Listing objects in '" + bucketName + "' using "
                + prefixesToList.length + " prefixes: "
                + Arrays.asList(prefixesToList));
            }

            // Initialize the variables that will be used, or populated, by the
            // multi-threaded listing.
            lastCommonPrefixes.clear();
            final String[] finalPrefixes = prefixesToList;
            final String finalDelimiter = (currentDepth < toDepth ? delimiter : null);

            /*
             * Perform a multi-threaded listing, where each prefix string
             * will be used as a unique partition to be listed in a separate thread.
             */
            (new Thread() {
                @Override
                public void run() {
                    threadedService.listObjects(bucketName, finalPrefixes,
                        finalDelimiter, Constants.DEFAULT_OBJECT_LIST_CHUNK_SIZE);
                };
            }).run();
            // Throw any exceptions that occur inside the threads.
            if (serviceExceptions[0] != null) {
                throw serviceExceptions[0];
            }

            // We use the common prefix paths identified in the last listing
            // iteration, if any, to identify partitions for follow-up listings.
            prefixesToList = lastCommonPrefixes
                .toArray(new String[lastCommonPrefixes.size()]);

            currentDepth++;
        }

        return allObjects.toArray(new StorageObject[allObjects.size()]);
    }


    /**
     * Lists the objects in a bucket using a partitioning technique to divide
     * the object namespace into separate partitions that can be listed by
     * multiple simultaneous threads. This method divides the object namespace
     * using the given delimiter, traverses this space up to the specified
     * depth to identify prefix names for multiple "partitions", and
     * then lists the objects in each partition. It returns the complete list
     * of objects in the bucket path.
     * <p>
     * This partitioning technique will work best for buckets with many objects
     * that are divided into a number of virtual subdirectories of roughly equal
     * size.
     * <p>
     * The delimiter and depth properties that define how this method will
     * partition the bucket's namespace are set in the jets3t.properties file
     * with the setting:
     * filecomparer.bucket-listing.&lt;bucketname>=&lt;delim>,&lt;depth><br>
     * For example: <code>filecomparer.bucket-listing.my-bucket=/,2</code>
     *
     * @param service
     * the service object that will be used to perform listing requests.
     * @param bucketName
     * the name of the bucket whose contents will be listed.
     * @param targetPath
     * a root path within the bucket to be listed. If this parameter is null, all
     * the bucket's objects will be listed. Otherwise, only the objects below the
     * virtual path specified will be listed.
     *
     * @return
     * the list of objects under the target path in the bucket.
     *
     * @throws ServiceException
     */
    public StorageObject[] listObjectsThreaded(StorageService service,
        final String bucketName, String targetPath) throws ServiceException
    {
        String delimiter = null;
        int toDepth = 0;

        // Find bucket-specific listing properties, if any.
        String bucketListingProperties = jets3tProperties.getStringProperty(
            "filecomparer.bucket-listing." + bucketName, null);
        if (bucketListingProperties != null) {
            String splits[] = bucketListingProperties.split(",");
            if (splits.length != 2) {
                throw new ServiceException(
                    "Invalid setting for bucket listing property "
                    + "filecomparer.bucket-listing." + bucketName + ": '" +
                    bucketListingProperties + "'");
            }
            delimiter = splits[0].trim();
            toDepth = Integer.parseInt(splits[1]);
        }

        return listObjectsThreaded(service, bucketName, targetPath,
            delimiter, toDepth);
    }

    /**
     * Builds a service Object Map containing all the objects within the given target path,
     * where the map's key for each object is the relative path to the object.
     *
     * @see #lookupObjectMetadataForPotentialClashes(StorageService, String, String, StorageObject[], Map, BytesProgressWatcher, StorageServiceEventListener)
     *
     * @param service
     * @param bucketName
     * @param targetPath
     * @param objectKeyToFilepathMap
     * map of '/'-delimited object key names to local file absolute paths
     * @param progressWatcher
     * watcher to monitor bytes read during comparison operations, may be null.
     * @param eventListener
     * @return
     * mapping of keys to StorageObjects
     * @throws ServiceException
     */
    public Map<String, StorageObject> buildObjectMap(StorageService service, String bucketName,
        String targetPath, Map<String, String> objectKeyToFilepathMap,
        BytesProgressWatcher progressWatcher, StorageServiceEventListener eventListener)
        throws ServiceException
    {
        String prefix = (targetPath.length() > 0 ? targetPath : null);
        StorageObject[] objectsIncomplete = this.listObjectsThreaded(
            service, bucketName, prefix);
        return lookupObjectMetadataForPotentialClashes(
            service, bucketName, targetPath,
            objectsIncomplete, objectKeyToFilepathMap,
            progressWatcher, eventListener);
    }


    /**
     * Builds a service Object Map containing a partial set of objects within the given target path,
     * where the map's key for each object is the relative path to the object.
     * <p>
     * If the method is asked to perform a complete listing, it will use the
     * {@link #listObjectsThreaded(StorageService, String, String)} method to list the objects
     * in the bucket, potentially taking advantage of any bucket name partitioning
     * settings you have applied.
     * <p>
     * If the method is asked to perform only a partial listing, no bucket name
     * partitioning will be applied.
     *
     * @see #lookupObjectMetadataForPotentialClashes(StorageService, String, String, StorageObject[], Map, BytesProgressWatcher, StorageServiceEventListener)
     *
     * @param service
     * @param bucketName
     * @param targetPath
     * @param priorLastKey
     * the prior last key value returned by a prior invocation of this method, if any.
     * @param objectKeyToFilepathMap
     * map of '/'-delimited object key names to local file absolute paths
     * @param completeListing
     * if true, this method will perform a complete listing of a service target.
     * If false, the method will list a partial set of objects commencing from the
     * given prior last key.
     * @param progressWatcher
     * watcher to monitor bytes read during comparison operations, may be null.
     * @param eventListener
     *
     * @return
     * an object containing a mapping of key names to StorageObjects, and the prior last
     * key (if any) that should be used to perform follow-up method calls.
     * @throws ServiceException
     */
    public PartialObjectListing buildObjectMapPartial(StorageService service,
        String bucketName, String targetPath, String priorLastKey,
        Map<String, String> objectKeyToFilepathMap, boolean completeListing,
        BytesProgressWatcher progressWatcher, StorageServiceEventListener eventListener)
        throws ServiceException
    {
        String prefix = (targetPath.length() > 0 ? targetPath : null);
        StorageObject[] objects = null;
        String resultPriorLastKey = null;
        if (completeListing) {
            objects = listObjectsThreaded(service, bucketName, prefix);
        } else {
            StorageObjectsChunk chunk = service.listObjectsChunked(
                bucketName, prefix, null, Constants.DEFAULT_OBJECT_LIST_CHUNK_SIZE,
                priorLastKey, completeListing);
            objects = chunk.getObjects();
            resultPriorLastKey = chunk.getPriorLastKey();
        }

        Map<String, StorageObject> objectsMap = lookupObjectMetadataForPotentialClashes(
            service, bucketName, targetPath, objects, objectKeyToFilepathMap,
            progressWatcher, eventListener);
        return new PartialObjectListing(objectsMap, resultPriorLastKey);
    }

    /**
     * Given a set of storage objects for which only minimal information is available,
     * retrieve metadata information for any objects that potentially clash with
     * local files. An object is considered a potential clash when it has the same
     * object key name as a local file pending upload/download, and when the hash
     * value of the object data contents either differs from the local file's hash
     * or the hash comparison cannot be performed without the metadata information.
     *
     * @see #populateObjectMap(String, StorageObject[])
     *
     * @param service
     * @param bucketName
     * @param targetPath
     * @param objectsWithoutMetadata
     * @param objectKeyToFilepathMap
     * @param progressWatcher
     * watcher to monitor bytes read during comparison operations, may be null.
     * @param eventListener
     * @return
     * mapping of keys to StorageObjects
     * @throws ServiceException
     */
    public Map<String, StorageObject> lookupObjectMetadataForPotentialClashes(
        StorageService service, String bucketName, String targetPath,
        StorageObject[] objectsWithoutMetadata, Map<String, String> objectKeyToFilepathMap,
        BytesProgressWatcher progressWatcher, StorageServiceEventListener eventListener)
        throws ServiceException
    {
        Map<String, StorageObject> objectMap = populateObjectMap(targetPath, objectsWithoutMetadata);

        // Identify objects that might clash with local files
        Set<StorageObject> objectsForMetadataRetrieval = new HashSet<StorageObject>();
        for (StorageObject object: objectsWithoutMetadata) {
            String objectKey = object.getKey();
            if (!ServiceUtils.isEtagAlsoAnMD5Hash(object.getETag())) {
                // Always retrieve metadata for objects whose ETags are
                // not MD5 hash values (e.g. multipart uploads)
                objectsForMetadataRetrieval.add(object);
                continue;
            }
            if (object.isMetadataComplete()) {
                // We already have this object's metadata
                continue;
            }

            String filepath = objectKeyToFilepathMap.get(objectKey);

            // Backwards-compatibility with JetS3t's old directory place-holders
            // key names that do not end with a slash (/).
            if (filepath == null && object.getContentLength() == 0
                && !objectKey.endsWith("/")
                && "d41d8cd98f00b204e9800998ecf8427e".equals(object.getETag()))
            {
                // Reasonable chance this is a directory place-holder, see if
                // there's a matching local directory.
                filepath = objectKeyToFilepathMap.get(objectKey + "/");
                // If not, bail out.
                if (filepath == null || !(new File(filepath).isDirectory())) {
                    continue;
                }
            }

            if (filepath == null) {
                // Give up
                continue;
            }

            // Compare object's minimal ETag value against File's MD5 hash.
            File file = new File(filepath);
            String fileHashAsHex = null;
            try {
                if (file.isDirectory()) {
                    // Dummy value, always retrieve metadata for directory place-holder objects
                    fileHashAsHex = "";
                } else {
                    fileHashAsHex = ServiceUtils.toHex(
                        generateFileMD5Hash(file, objectKey, progressWatcher));
                }
            } catch (Exception e) {
                throw new ServiceException(
                    "Unable to generate MD5 hash for file " + file.getPath(), e);
            }

            if (object.getETag() != null && object.getETag().equals(fileHashAsHex)) {
                // Object's ETag value is available and matches the MD5 hex hash of the file
                continue;
            }
            // Cannot tell whether local file and object are the same,
            // we will need all the object's metadata.
            objectsForMetadataRetrieval.add(object);
        }

        if (objectsForMetadataRetrieval.size() > 0) {
            // Retrieve the complete metadata information for selected objects
            final List<StorageObject> objectsCompleteList =
                new ArrayList<StorageObject>(objectsWithoutMetadata.length);
            final ServiceException serviceExceptions[] = new ServiceException[1];
            ThreadedStorageService threadedService = new ThreadedStorageService(service,
                new StorageServiceEventAdaptor() {
                @Override
                public void event(GetObjectHeadsEvent event) {
                    if (GetObjectHeadsEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                        StorageObject[] finishedObjects = event.getCompletedObjects();
                        if (finishedObjects.length > 0) {
                            objectsCompleteList.addAll(Arrays.asList(finishedObjects));
                        }
                    } else if (GetObjectHeadsEvent.EVENT_ERROR == event.getEventCode()) {
                        serviceExceptions[0] = new ServiceException(
                            "Failed to retrieve detailed information about all objects",
                            event.getErrorCause());
                    }
                }
            });
            if (eventListener != null) {
                threadedService.addServiceEventListener(eventListener);
            }
            threadedService.getObjectsHeads(bucketName,
                objectsForMetadataRetrieval.toArray(new StorageObject[] {}));
            if (serviceExceptions[0] != null) {
                throw serviceExceptions[0];
            }

            StorageObject[] objectsWithMetadata =
                objectsCompleteList.toArray(new StorageObject[objectsCompleteList.size()]);
            objectMap.putAll(populateObjectMap(targetPath, objectsWithMetadata));
        }

        return objectMap;
    }

    /**
     * Builds a map of key/object pairs each object is associated with a key based on its location
     * in the service target path.
     *
     * @param targetPath
     * @param objects
     * @return
     * a map of keys to StorageObjects.
     */
    public Map<String, StorageObject> populateObjectMap(String targetPath, StorageObject[] objects) {
        Map<String, StorageObject> map = new TreeMap<String, StorageObject>();
        for (int i = 0; i < objects.length; i++) {
            String relativeKey = objects[i].getKey();
            if (targetPath.length() > 0) {
                relativeKey = relativeKey.substring(targetPath.length());
                int slashIndex = relativeKey.indexOf(Constants.FILE_PATH_DELIM);
                if (slashIndex == 0) {
                    relativeKey = relativeKey.substring(slashIndex + 1, relativeKey.length());
                } else {
                    // This object is the result of a prefix search, not an explicit directory.
                    // Base the relative key on the last full subdirectory in the
                    // target path if available...
                    slashIndex = targetPath.lastIndexOf(Constants.FILE_PATH_DELIM);
                    if (slashIndex >= 0) {
                        relativeKey = objects[i].getKey().substring(slashIndex + 1);
                    }
                    // ...otherwise, use the full object key name.
                    else {
                        relativeKey = objects[i].getKey();
                    }
                }
            }
            if (relativeKey.length() > 0) {
                map.put(normalizeUnicode(relativeKey), objects[i]);
            }
        }
        return map;
    }

    /**
     *
     * @param file
     * @param relativeFilePath
     * @param progressWatcher
     * watcher to monitor bytes read during comparison operations, may be null.
     * @return
     * MD5 hash as bytes
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public byte[] generateFileMD5Hash(File file, String relativeFilePath,
        BytesProgressWatcher progressWatcher)
        throws IOException, NoSuchAlgorithmException
    {
        byte[] computedHash = null;

        // Check whether a pre-computed MD5 hash file is available
        File computedHashFile = (getMd5FilesRootDirectoryFile() != null
            ? new File(getMd5FilesRootDirectoryFile(), relativeFilePath + ".md5")
            : new File(file.getPath() + ".md5"));
        if (isUseMd5Files()
            && computedHashFile.canRead()
            && computedHashFile.lastModified() > file.lastModified())
        {
            BufferedReader br = null;
            try {
                // A pre-computed MD5 hash file is available, try to read this hash value
                br = new BufferedReader(new FileReader(computedHashFile));
                computedHash = ServiceUtils.fromHex(br.readLine().split("\\s")[0]);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Unable to read hash from computed MD5 file", e);
                }
            } finally {
                if (br != null) {
                    br.close();
                }
            }
        }

        if (computedHash == null) {
            // A pre-computed hash file was not available, or could not be read.
            // Calculate the hash value anew.
            InputStream hashInputStream = null;
            if (progressWatcher != null) {
                hashInputStream = new ProgressMonitoredInputStream( // Report on MD5 hash progress.
                    new FileInputStream(file), progressWatcher);
            } else {
                hashInputStream = new FileInputStream(file);
            }
            computedHash = ServiceUtils.computeMD5Hash(hashInputStream);
        }

        if (isGenerateMd5Files() && !file.getName().endsWith(".md5") &&
            (!computedHashFile.exists()
            || computedHashFile.lastModified() < file.lastModified()))
        {
            // Create parent directory for new hash file if necessary
            File parentDir = computedHashFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Create or update a pre-computed MD5 hash file.
            FileWriter fw = null;
            try {
                fw = new FileWriter(computedHashFile);
                fw.write(ServiceUtils.toHex(computedHash));
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Unable to write computed MD5 hash to a file", e);
                }
            } finally {
                if (fw != null) {
                    fw.close();
                }
            }
        }

        return computedHash;
    }

    /**
     * Compares the contents of a directory on the local file system with the contents of a service
     * resource. This comparison is performed on a map of files and a map of service objects previously
     * generated using other methods in this class.
     *
     * @param objectKeyToFilepathMap
     * map of '/'-delimited object key names to local file absolute paths
     * @param objectsMap
     * a map of keys to StorageObjects.
     * @return
     * an object containing the results of the file comparison.
     *
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ParseException
     */
    public FileComparerResults buildDiscrepancyLists(
        Map<String, String> objectKeyToFilepathMap, Map<String, StorageObject> objectsMap)
        throws NoSuchAlgorithmException, FileNotFoundException, IOException, ParseException
    {
        return buildDiscrepancyLists(objectKeyToFilepathMap, objectsMap, null);
    }

    /**
     * Compares the contents of a directory on the local file system with the contents of a service
     * resource. This comparison is performed on a map of files and a map of service objects previously
     * generated using other methods in this class.
     *
     * @param objectKeyToFilepathMap
     * map of '/'-delimited object key names to local file absolute paths
     * @param objectsMap
     * a map of keys to StorageObjects.
     * @param progressWatcher
     * watcher to monitor bytes read during comparison operations, may be null.
     * @return
     * an object containing the results of the file comparison.
     *
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ParseException
     */
    public FileComparerResults buildDiscrepancyLists(Map<String, String> objectKeyToFilepathMap,
        Map<String, StorageObject> objectsMap, BytesProgressWatcher progressWatcher)
        throws NoSuchAlgorithmException, FileNotFoundException, IOException, ParseException
    {
        List<String> onlyOnServerKeys = new ArrayList<String>();
        List<String> updatedOnServerKeys = new ArrayList<String>();
        List<String> updatedOnClientKeys = new ArrayList<String>();
        List<String> onlyOnClientKeys = new ArrayList<String>();
        List<String> alreadySynchronisedKeys = new ArrayList<String>();
        List<String> alreadySynchronisedLocalPaths = new ArrayList<String>();

        // Start by assuming all items are local to client. Items will be removed
        // from this set as we proceed.
        onlyOnClientKeys.addAll(objectKeyToFilepathMap.keySet());

        // Check files on server against local client files.
        Iterator<Map.Entry<String, StorageObject>> objectsMapIter = objectsMap.entrySet().iterator();
        while (objectsMapIter.hasNext()) {
            Map.Entry<String, StorageObject> entry = objectsMapIter.next();
            String keyPath = entry.getKey();
            StorageObject storageObject = entry.getValue();

            String[] splitPathComponents = splitFilePathIntoDirPaths(
                keyPath, storageObject.isDirectoryPlaceholder());

            int componentCount = 0;
            for (String localPath: splitPathComponents) {
                componentCount += 1;

                String filepath = objectKeyToFilepathMap.get(localPath);

                // Check whether local file is already on server
                if (filepath != null) {
                    // File has been backed up in the past, is it still up-to-date?
                    File file = new File(filepath);

                    // We don't care about directory date changes, as long as it's present.
                    if (file.isDirectory()) {
                        // Only flag key path as already synced if the current localPath
                        // is also equivalent to the *full* path of the object in the storage
                        // service, not just an object's parent directory. (Issue #69)
                        if (componentCount == splitPathComponents.length) {
                            alreadySynchronisedKeys.add(keyPath);
                            alreadySynchronisedLocalPaths.add(localPath);
                            boolean wasRemoved = onlyOnClientKeys.remove(keyPath);

                            // Backwards-compatibility with JetS3t directory place-holders
                            // without trailing slash (/) suffixes
                            if (!wasRemoved && !keyPath.endsWith("/")
                                && storageObject.isDirectoryPlaceholder())
                            {
                                onlyOnClientKeys.remove(keyPath + "/");
                            }
                        }
                    }
                    // Compare file hashes.
                    else {
                        String fileHashAsBase64 = ServiceUtils.toBase64(
                            generateFileMD5Hash(file, localPath, progressWatcher));

                        // Get the service object's Base64 hash.
                        String objectHash = null;
                        if (storageObject.containsMetadata(StorageObject.METADATA_HEADER_ORIGINAL_HASH_MD5)) {
                            // Use the object's *original* hash, as it is an encoded version of a local file.
                            objectHash = (String) storageObject.getMetadata(
                                StorageObject.METADATA_HEADER_ORIGINAL_HASH_MD5);
                            if (log.isDebugEnabled()) {
                                log.debug("Object in service is encoded, using the object's original hash value for: "
                                + storageObject.getKey());
                            }
                        } else {
                            // The object wasn't altered when uploaded, so use its current hash.
                            objectHash = storageObject.getMd5HashAsBase64();
                        }

                        if (fileHashAsBase64.equals(objectHash)) {
                            // Hashes match so file is already synchronised.
                            alreadySynchronisedKeys.add(keyPath);
                            alreadySynchronisedLocalPaths.add(localPath);
                            onlyOnClientKeys.remove(keyPath);
                        } else {
                            // File is out-of-synch. Check which version has the latest date.
                            Date objectLastModified = null;
                            String metadataLocalFileDate = (String) storageObject.getMetadata(
                                Constants.METADATA_JETS3T_LOCAL_FILE_DATE);

                            if (metadataLocalFileDate == null) {
                                // This is risky as local file times and service times don't match!
                                if (!isAssumeLocalLatestInMismatch() && log.isWarnEnabled()) {
                                    log.warn("Using service last modified date as file date. This is not reliable "
                                    + "as the time according to service can differ from your local system time. "
                                    + "Please use the metadata item "
                                    + Constants.METADATA_JETS3T_LOCAL_FILE_DATE);
                                }
                                objectLastModified = storageObject.getLastModifiedDate();
                            } else {
                                objectLastModified = ServiceUtils
                                    .parseIso8601Date(metadataLocalFileDate);
                            }
                            if (objectLastModified.getTime() > file.lastModified()) {
                                updatedOnServerKeys.add(keyPath);
                                onlyOnClientKeys.remove(keyPath);
                            } else if (objectLastModified.getTime() < file.lastModified()) {
                                updatedOnClientKeys.add(keyPath);
                                onlyOnClientKeys.remove(keyPath);
                            } else {
                                // Local file date and service object date values match exactly, yet the
                                // local file has a different hash. This shouldn't ever happen, but
                                // sometimes does with Excel files.
                                if (isAssumeLocalLatestInMismatch()) {
                                    if (log.isWarnEnabled()) {
                                        log.warn("Backed-up object " + storageObject.getKey()
                                        + " and local file " + file.getName()
                                        + " have the same date but different hash values. "
                                        + "Assuming local file is the latest version.");
                                    }
                                    updatedOnClientKeys.add(keyPath);
                                    onlyOnClientKeys.remove(keyPath);
                                } else {
                                    throw new IOException("Backed-up object " + storageObject.getKey()
                                        + " and local file " + file.getName()
                                        + " have the same date but different hash values. "
                                        + "This shouldn't happen!");
                                }

                            }
                        }
                    }
                } else {
                    // File is not in local file system, so it's only on the service.

                    // Only flag key path as already synced if the current localPath
                    // is also equivalent to the *full* path of the object in the storage
                    // service, not just an object's parent directory.
                    if (componentCount == splitPathComponents.length) {
                        onlyOnServerKeys.add(keyPath);
                        onlyOnClientKeys.remove(keyPath);
                    }
                }
            }
        }

        return new FileComparerResults(onlyOnServerKeys, updatedOnServerKeys, updatedOnClientKeys,
            onlyOnClientKeys, alreadySynchronisedKeys, alreadySynchronisedLocalPaths);
    }

    private String[] splitFilePathIntoDirPaths(String path, boolean isDirectoryPlaceholder) {
        String[] pathComponents = path.split(Constants.FILE_PATH_DELIM);
        String[] dirPathsInOrder = new String[pathComponents.length];
        String myPath = "";
        for (int i = 0; i < pathComponents.length; i++) {
            String pathComponent = pathComponents[i];
            myPath = myPath + pathComponent;
            if (i < pathComponents.length - 1 || isDirectoryPlaceholder) {
                myPath += Constants.FILE_PATH_DELIM;
            }
            dirPathsInOrder[i] = myPath;
        }
        return dirPathsInOrder;
    }

    /**
     * @return
     * true if the "filecomparer.skip-symlinks" configuration option is set.
     */
    public boolean isSkipSymlinks() {
        return jets3tProperties.getBoolProperty("filecomparer.skip-symlinks", false);
    }

    /**
     * @return
     * true if the "filecomparer.use-md5-files" configuration option is set.
     */
    public boolean isUseMd5Files() {
        return jets3tProperties.getBoolProperty("filecomparer.use-md5-files", false);
    }

    /**
     * @return
     * true if the "filecomparer.generate-md5-files" configuration option is set.
     */
    public boolean isGenerateMd5Files() {
        return jets3tProperties.getBoolProperty("filecomparer.generate-md5-files", false);
    }

    /**
     * @return
     * true if the "filecomparer.skip-upload-of-md5-files" configuration option is set.
     */
    public boolean isSkipMd5FileUpload() {
        return jets3tProperties.getBoolProperty("filecomparer.skip-upload-of-md5-files", false);
    }

    /**
     * @return
     * true if the "filecomparer.assume-local-latest-in-mismatch" configuration option is set.
     */
    public boolean isAssumeLocalLatestInMismatch() {
        return jets3tProperties.getBoolProperty(
            "filecomparer.assume-local-latest-in-mismatch", false);
    }


    /**
     * @return
     * the file represented by the configuration option "filecomparer.md5-files-root-dir"
     * or null if this option is not specified.
     * @throws FileNotFoundException
     */
    public File getMd5FilesRootDirectoryFile() throws FileNotFoundException {
        String dirPath = jets3tProperties.getStringProperty(
            "filecomparer.md5-files-root-dir", null);
        if (dirPath != null) {
            File dirFile = new File(dirPath);
            if (!dirFile.isDirectory()) {
                throw new FileNotFoundException(
                    "filecomparer.md5-files-root-dir path is not a directory: " + dirPath);
            }
            return dirFile;
        }
        return null;
    }

    public class PartialObjectListing {
        private Map<String, StorageObject> objectsMap = null;
        private String priorLastKey = null;

        public PartialObjectListing(Map<String, StorageObject> objectsMap, String priorLastKey) {
            this.objectsMap = objectsMap;
            this.priorLastKey = priorLastKey;
        }

        public Map<String, StorageObject> getObjectsMap() {
            return objectsMap;
        }

        public String getPriorLastKey() {
            return priorLastKey;
        }
    }

}
