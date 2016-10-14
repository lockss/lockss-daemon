/*

 Copyright (c) 2014-2016 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.job;

/**
 * Constants used in SQL code.
 * 
 * @author Fernando García-Loygorri
 */
public class SqlConstants {
  //
  // Database table names.
  //
  /** Name of the job type table. */
  public static final String JOB_TYPE_TABLE = "job_type";

  /** Name of the job status table. */
  public static final String JOB_STATUS_TABLE = "job_status";

  /** Name of the job table. */
  public static final String JOB_TABLE = "job";

  //
  // Database table column names.
  //
  /** Name of plugin_id column. */
  public static final String PLUGIN_ID_COLUMN = "plugin_id";

  /** Name of au_key column. */
  public static final String AU_KEY_COLUMN = "au_key";

  /** Type name column. */
  public static final String TYPE_NAME_COLUMN = "type_name";

  /** Priority column. */
  public static final String PRIORITY_COLUMN = "priority";

  /** Archival Unit creation time column. */
  public static final String CREATION_TIME_COLUMN = "creation_time";

  /** Job type identifier column. */
  public static final String JOB_TYPE_SEQ_COLUMN = "job_type_seq";

  /** Job status identifier column. */
  public static final String JOB_STATUS_SEQ_COLUMN = "job_status_seq";

  /** Status name column. */
  public static final String STATUS_NAME_COLUMN = "status_name";

  /** Job identifier column. */
  public static final String JOB_SEQ_COLUMN = "job_seq";

  /** Description column. */
  public static final String DESCRIPTION_COLUMN = "description";

  /** Job start time column. */
  public static final String START_TIME_COLUMN = "start_time";

  /** Job end time column. */
  public static final String END_TIME_COLUMN = "end_time";

  /** Status message column. */
  public static final String STATUS_MESSAGE_COLUMN = "status_message";

  /** Owner column. */
  public static final String OWNER_COLUMN = "owner";

  //
  // Maximum lengths of variable text length database columns.
  //
  public static final int MAX_PLUGIN_ID_COLUMN = 256;

  /** Length of the AU key column. */
  public static final int MAX_AU_KEY_COLUMN = 512;

  /** Length of the type name column. */
  public static final int MAX_TYPE_NAME_COLUMN = 32;

  /** Length of the status name column. */
  public static final int MAX_STATUS_NAME_COLUMN = 32;

  /** Length of the description column. */
  public static final int MAX_DESCRIPTION_COLUMN = 128;

  /** Length of the status message column. */
  public static final int MAX_STATUS_MESSAGE_COLUMN = 512;

  /** Length of the owner column. */
  public static final int MAX_OWNER_COLUMN = 32;

  /**
   * Types of jobs.
   */
  public static final String JOB_TYPE_DELETE_AU = "delete_au";
  public static final String JOB_TYPE_PUT_AU = "put_au";

  /**
   * Statuses of jobs.
   */
  public static final String JOB_STATUS_CREATED = "created";
  public static final String JOB_STATUS_DELETED = "deleted";
  public static final String JOB_STATUS_DONE = "done";
  public static final String JOB_STATUS_RUNNING = "running";
  public static final String JOB_STATUS_TERMINATED = "terminated";
  public static final String JOB_STATUS_TERMINATING = "terminating";
}
