/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer;

import java.util.*;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class SpringerLinkUrlNormalizer implements UrlNormalizer {

  protected static final Logger logger = Logger.getLogger(SpringerLinkUrlNormalizer.class);
  
  /*
</title><meta http-equiv="X-UA-Compatible" content="IE=8" /><link href="/images/favicon.ico" type="image/x-icon" rel="Shortcut Icon" /><meta http-equiv="Content-Type" content="text/html; charset=UTF-8" /><meta name="audience" content="all" /><meta name="distribution" content="GLOBAL" /><meta name="copyright" content="2011 - Springer" /><meta name="author" content="MetaPress" /><link href="/content/1573-3882/2/1?MUD=MP&export=rss" type="application/rss+xml" rel="alternate" title="Metabolomics (Browse Results)" /><link href="/dynamic-file.axd?id=80623e25-67f3-4182-9ee3-9a76475bea6e&m=True" type="text/css" rel="stylesheet" /></head>
   */
  protected static final String CURRENT_ID_CSS = "80623e25-67f3-4182-9ee3-9a76475bea6e";

  /*
<div>
<input type="hidden" name="__VIEWSTATE" id="__VIEWSTATE" value="/wEPDwUKLTQyNzgyODUxNmQYAQUeX19Db250cm9sc1JlcXVpcmVQb3N0QmFja0tleV9fFg0FNmN0bDAwJGN0bDE0JFNlYXJjaENvbnRyb2wkQWR2YW5jZWRTZWFyY2hXaXRoaW5GdWxsVGV4dAU2Y3RsMDAkY3RsMTQkU2VhcmNoQ29udHJvbCRBZHZhbmNlZFNlYXJjaFdpdGhpbkFic3RyYWN0BTZjdGwwMCRjdGwxNCRTZWFyY2hDb250cm9sJEFkdmFuY2VkU2VhcmNoV2l0aGluQWJzdHJhY3QFM2N0bDAwJGN0bDE0JFNlYXJjaENvbnRyb2wkQWR2YW5jZWRTZWFyY2hXaXRoaW5UaXRsZQUzY3RsMDAkY3RsMTQkU2VhcmNoQ29udHJvbCRBZHZhbmNlZFNlYXJjaFdpdGhpblRpdGxlBTVjdGwwMCRjdGwxNCRTZWFyY2hDb250cm9sJEFkdmFuY2VkUHVibGljYXRpb25EYXRlc0FsbAU5Y3RsMDAkY3RsMTQkU2VhcmNoQ29udHJvbCRBZHZhbmNlZFB1YmxpY2F0aW9uRGF0ZXNCZXR3ZWVuBTljdGwwMCRjdGwxNCRTZWFyY2hDb250cm9sJEFkdmFuY2VkUHVibGljYXRpb25EYXRlc0JldHdlZW4FOWN0bDAwJGN0bDE0JFNlYXJjaENvbnRyb2wkQWR2YW5jZWRPcmRlck9mUmVzdWx0c1JlbGV2YW5jZQU/Y3RsMDAkY3RsMTQkU2VhcmNoQ29udHJvbCRBZHZhbmNlZE9yZGVyT2ZSZXN1bHRzUHVibGljYXRpb25EYXRlBT9jdGwwMCRjdGwxNCRTZWFyY2hDb250cm9sJEFkdmFuY2VkT3JkZXJPZlJlc3VsdHNQdWJsaWNhdGlvbkRhdGUFNGN0bDAwJGN0bDE0JFNlYXJjaENvbnRyb2wkQWR2YW5jZWRPcmRlck9mUmVzdWx0c05hbWUFNGN0bDAwJGN0bDE0JFNlYXJjaENvbnRyb2wkQWR2YW5jZWRPcmRlck9mUmVzdWx0c05hbWXHWKqNAJMl4reJKjNnvNzBSIHj3w==" />
</div>

<script type="text/javascript">
      (function () {
        var 
        src = ('_protocol_\/\/www.google.com/jsapi').replace('_protocol_', location.protocol),
        tag = ('<script type=\"text/javascript\" src=\"_src_\"><\/script>').replace('_src_', src);
        document.write(tag);
      })();
    </script><script type="text/javascript">google.load('jquery', '1.4.2');</script><script type="text/javascript">google.load('jqueryui', '1.8.5');</script><script type="text/javascript">google.load("language", "1");</script><script type="text/javascript" src="/dynamic-file.axd?id=f0b3b809-2b0c-4996-b73e-3b855bcbdafa&m=True"></script>
      <script type="text/javascript">
        document.body.className = document.body.className.replace('scriptDisabled', 'scriptEnabled');
        METAPRESS.setAccessIndicatorsClass();
      </script>
   */
  protected static final String CURRENT_ID_JAVASCRIPT_1 = "f6fcb095-3d49-4291-984c-1fa1626f3a8a";
  // 695015e5-1327-447b-b711-8224b68bd201
  
  /*
<div>

        <input type="hidden" name="__EVENTVALIDATION" id="__EVENTVALIDATION" value="/wEWLwL0/vmDDALsg9nFCwKonpOxDwLRn6eIAgLalpOrCQLJlNvIBwKFmo+HBQKimoPcDgKwlfelBQLalueDDgKqlOOADgK3mPPBBAKC5LazDAKJjf25CgLg8uTrCwK8yaHwBgKb2tPtCwKZ2LyxCALFqK2IBgLt9+OaBwLRn8ctAsT2uboJAr/JgOkPAo/Q4Q4Cttfd3w8C0P7MhAsCrrW5hA4C8cvMmg8Cs8CSmwgC05LS6wcCndyglAECpraPzAwC+tzx+AwC64WbhwECo4Drrw0C3vib+g8CgPza+gcC5YinhgQC5pCnhgQCt+fdjQcChq36hg8Cse+qmAsCm/a20g8C5ov3qAYCmZy1lg8C7P3SnQIC3YyOnQlhsaVAOgg+XXAyylg2F+fdjvAQSQ==" />
</div>
<script type="text/javascript">var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));</script><script type="text/javascript">var pageTracker = _gat._getTracker("UA-6028168-1");_gat._anonymizeIp();pageTracker._trackPageview();</script><script type="text/javascript" src="/dynamic-file.axd?id=88d24f97-0be5-46cc-ba7a-c97002f465c0&m=True"></script></form>
    
  </body>
   */
  protected static final String CURRENT_ID_JAVASCRIPT_2 = "88d24f97-0be5-46cc-ba7a-c97002f465c0";  
  
  protected static final Map<String, String> idMapping; // See static initializer
  
  /*
   * STATIC INITIALIZER
   */
  static {
    idMapping = new HashMap<String, String>();
    Map<String, String> m = idMapping; // shorter name for repetitive code below
    // CSS
    // No such links: 00:51:43 11/11/10 - 01:30:26 05/20/11
    m.put("a209de6f-81f0-43df-b579-003b2a6c2c3b", CURRENT_ID_CSS); // 12:06:11 09/26/11 - 07:10:22 12/07/11
    m.put("7e8f2781-87c9-48ff-ac64-b7521267eb3d", CURRENT_ID_CSS); // 08:13:21 12/18/11 - 05:17:00 01/17/12
    m.put("d25b4bf4-02b3-47f8-8cb5-9bdebf5da452", CURRENT_ID_CSS); // 00:51:44 02/06/12 - 07:21:29 03/04/12
    m.put("b4a7f886-a7e7-4dfa-8942-6c13eacc7c38", CURRENT_ID_CSS); // 07:04:14 03/06/12 - 12:02:03 04/18/12
    m.put("555b8adc-82b5-49e4-a40b-5cae3355208c", CURRENT_ID_CSS); // 16:06:47 04/19/12 - 11:45:02 05/03/12
    m.put("be38f38c-6e9c-41c8-8f24-616ce62a7f94", CURRENT_ID_CSS); // 11:13:14 05/08/12 - 10:39:22 06/12/12
    m.put(CURRENT_ID_CSS, CURRENT_ID_CSS); // 07:55:36 06/19/12 - 14:48:20 03/07/13
    // Javascript 1
    // No such links: 00:51:43 11/11/10 - 01:30:26 05/20/11
    m.put("505cc7ec-c441-4d6a-a89c-4634a00eba34", CURRENT_ID_JAVASCRIPT_1); // 07:55:05 09/08/11
    m.put("1108e1d4-97ac-4875-80c8-37bf1b132c8b", CURRENT_ID_JAVASCRIPT_1); // 02:22:15 09/13/11 - 01:00:39 09/18/11
    m.put("71cb314e-b50a-4dc7-bc4d-fe244c65d4f2", CURRENT_ID_JAVASCRIPT_1); // 12:06:11 09/26/11
    m.put("deafc402-eb3e-48e7-b0bb-2baa40586f62", CURRENT_ID_JAVASCRIPT_1); // 15:13:37 10/08/11
    m.put("89fb321a-e8ea-40c3-ac9a-b24727113d7f", CURRENT_ID_JAVASCRIPT_1); // 07:13:31 11/02/11 - 07:10:22 12/07/11
    m.put("e462af41-20fd-4966-bf4f-113610bfd1ec", CURRENT_ID_JAVASCRIPT_1); // 08:13:21 12/18/11
    m.put("cf0e16f3-cd2d-4cbc-b366-efdc9ec5c6f2", CURRENT_ID_JAVASCRIPT_1); // 10:14:29 01/03/12 - 05:17:00 01/17/12
    m.put("f1acae59-9f16-4b8c-85cc-c3eaacae39f2", CURRENT_ID_JAVASCRIPT_1); // 00:51:44 02/06/12
    m.put("55d69b78-3e0a-43ba-a150-18ce8a3ceeba", CURRENT_ID_JAVASCRIPT_1); // 16:31:53 02/09/12
    m.put("5d34e280-1ff2-4079-9190-7fc639a99a57", CURRENT_ID_JAVASCRIPT_1); // 20:33:59 02/12/12
    m.put("22c40097-4145-4d58-8cf7-63c1d4439399", CURRENT_ID_JAVASCRIPT_1); // 02:13:01 02/19/12 - 07:21:29 03/04/12
    m.put("74466dca-978b-4c51-bbb6-02378a4905e1", CURRENT_ID_JAVASCRIPT_1); // 07:04:14 03/06/12 - 21:00:47 04/04/12
    m.put("99381f0b-4327-49b7-afa4-de3fc33ad947", CURRENT_ID_JAVASCRIPT_1); // 15:00:33 04/06/12 - 13:24:42 04/18/12
    m.put("48e4d99e-0f75-417e-8e54-611005af3a10", CURRENT_ID_JAVASCRIPT_1); // 16:06:47 04/19/12
    m.put("17d72427-be38-4c61-a23c-d723395cac64", CURRENT_ID_JAVASCRIPT_1); // 22:00:23 04/21/12
    m.put("f7790a4f-b0eb-4bfe-93e8-70470325fb27", CURRENT_ID_JAVASCRIPT_1); // 12:58:11 04/22/12 - 09:30:16 04/23/12
    m.put("36b4f92d-dee2-442e-992a-36dceaccb4c6", CURRENT_ID_JAVASCRIPT_1); // 05:38:03 04/28/12 - 11:45:02 05/03/12
    m.put("801d651f-f3be-48b6-9d5c-f6f33b9366c0", CURRENT_ID_JAVASCRIPT_1); // 11:13:14 05/08/12
    m.put("23c7edb8-0e91-47b4-8385-5728f0b0e2d5", CURRENT_ID_JAVASCRIPT_1); // 22:45:26 05/10/12
    m.put("d149bcb2-d576-4cb0-9761-ac538aef6037", CURRENT_ID_JAVASCRIPT_1); // 21:10:20 05/15/12 - 15:38:55 05/17/12
    m.put("e907c3f2-74ac-4f1f-8529-e9497606323b", CURRENT_ID_JAVASCRIPT_1); // 07:43:37 05/29/12
    m.put("50bd5442-ad0a-4578-9372-c92b928fe728", CURRENT_ID_JAVASCRIPT_1); // 23:09:11 06/07/12 - 09:05:32 06/08/12
    m.put("d7e19a36-2ce1-4691-95a4-866a4381977e", CURRENT_ID_JAVASCRIPT_1); // 10:39:22 06/12/12
    m.put("3975407c-2da1-455a-99ae-a066f8d78a8c", CURRENT_ID_JAVASCRIPT_1); // 23:06:42 06/18/12 - 07:55:36 06/19/12
    m.put("e408ae52-e310-4f94-839d-e1dad4df509f", CURRENT_ID_JAVASCRIPT_1); // 14:17:03 07/03/12 - 13:25:16 08/10/12
    m.put("8272bab5-29b3-4aea-8c03-d6be1e65a7c5", CURRENT_ID_JAVASCRIPT_1); // 19:24:39 09/17/12 - 12:10:57 09/24/12
    m.put("448d74db-3a49-47cd-a22e-30f88ad429c7", CURRENT_ID_JAVASCRIPT_1); // 00:55:48 10/16/12 - 04:36:15 10/25/12
    m.put("f0b3b809-2b0c-4996-b73e-3b855bcbdafa", CURRENT_ID_JAVASCRIPT_1); // 22:02:30 10/28/12 - 05:31:26 12/13/12 
    m.put("695015e5-1327-447b-b711-8224b68bd201", CURRENT_ID_JAVASCRIPT_1); // 03:31:11 12/21/12 - 09:23:01 01/12/13
    m.put("93bd6382-c886-4f5d-a75f-9ef517e06cbb", CURRENT_ID_JAVASCRIPT_1); // 06:04:43 01/24/13 - 09:33:59 02/09/13
    m.put("76bb4537-0fae-43b1-9c0c-71f06031a6b9", CURRENT_ID_JAVASCRIPT_1); // 14:40:36 02/20/13 - 09:37:49 02/23/13 
    m.put(CURRENT_ID_JAVASCRIPT_1, CURRENT_ID_JAVASCRIPT_1); // 14:46:54 03/06/13 - 14:48:20 03/07/13
    // Javascript 2
    // No such links: 00:51:43 11/11/10 - 01:30:26 05/20/11
    m.put("40594b26-7aa9-4e22-b8b8-c1d26d21b52a", CURRENT_ID_JAVASCRIPT_2); // 12:06:11 09/26/11 - 07:10:22 12/07/11
    m.put("26cc528d-424b-4f70-93ec-00d6a5ebd0c7", CURRENT_ID_JAVASCRIPT_2); // 08:13:21 12/18/11 - 16:31:53 02/09/12
    m.put("b0622fd9-cdd1-41f5-9b63-533c6c2421c6", CURRENT_ID_JAVASCRIPT_2); // 20:33:59 02/12/12 - 07:21:29 03/04/12
    m.put("5120b55b-99e7-41ca-9657-bd9fd6eec7e6", CURRENT_ID_JAVASCRIPT_2); // 07:04:14 03/06/12 - 12:02:03 04/18/12
    m.put("0f9d5a7b-28ef-4e86-98e4-e5fafc71dd75", CURRENT_ID_JAVASCRIPT_2); // 16:06:47 04/19/12 - 10:39:22 06/12/12
    m.put("54b6894c-8544-45e8-9ca9-333567e5add3", CURRENT_ID_JAVASCRIPT_2); // 07:55:36 06/19/12
    m.put(CURRENT_ID_JAVASCRIPT_2, CURRENT_ID_JAVASCRIPT_2); // 14:17:03 07/03/12 - 14:48:20 03/07/13
  }
  
  /*
At least these IDs are still unaccounted for.

0c7570d1-00e3-44ab-abfb-30a6ef0a0b92
6e203a02-a9a3-4ec4-9f41-d2110ca514b6
f04d1c36-c4c9-47c9-ab67-95b7e14ca9df
f0bf38b6-66f3-4c9d-b3f9-ec4e16885d3b
   */
  
  // Prefix part
  public static final String STARTS_WITH = "http://www.springerlink.com/dynamic-file.axd?id=";
  public static final int STARTS_WITH_LENGTH = STARTS_WITH.length();

  // ID part
  public static final int ID_LENGTH = CURRENT_ID_CSS.length();

  // Hyphens within the ID part (relative to the beginning of the ID)
  public static final int HYPHEN1 = CURRENT_ID_CSS.indexOf('-');
  public static final int HYPHEN2 = CURRENT_ID_CSS.indexOf('-', HYPHEN1 + 1);
  public static final int HYPHEN3 = CURRENT_ID_CSS.indexOf('-', HYPHEN2 + 1);
  public static final int HYPHEN4 = CURRENT_ID_CSS.indexOf('-', HYPHEN3 + 1);
  
  // Suffix part
  public static final String ENDS_WITH = "&m=True";
  public static final int ENDS_WITH_LENGTH = ENDS_WITH.length();

  // Total length
  public static final int TOTAL_LENGTH = STARTS_WITH_LENGTH + ID_LENGTH + ENDS_WITH_LENGTH;
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    if (   url.length() != TOTAL_LENGTH
        || !url.startsWith(STARTS_WITH)
        || !url.endsWith(ENDS_WITH)) {
      if (logger.isDebug3()) {
        logger.debug3(String.format("Non-matching: %s in %s", url, au.getName()));
      }
      return url;
    }
    String actualId = url.substring(STARTS_WITH_LENGTH, STARTS_WITH_LENGTH + ID_LENGTH);
    if (   actualId.charAt(HYPHEN1) != '-'
        || actualId.charAt(HYPHEN2) != '-'
        || actualId.charAt(HYPHEN3) != '-'
        || actualId.charAt(HYPHEN4) != '-') {
      if (logger.isDebug2()) {
        logger.debug2(String.format("Irregular: %s in %s", url, au.getName()));
      }
      return url;
    }
    String canonicalId = idMapping.get(actualId);
    if (canonicalId == null) {
      if (logger.isDebug2()) {
        logger.debug2(String.format("Unknown: %s in %s", url, au.getName()));
      }
      return url;
    }
    if (actualId.equals(canonicalId)) {
      if (logger.isDebug3()) {
        logger.debug3(String.format("Canonical: %s in %s", url, au.getName()));
      }
      return url;
    }
    String ret = STARTS_WITH + canonicalId + ENDS_WITH;
    if (logger.isDebug3()) {
      logger.debug3(String.format("Matching: %s -> %s in %s", url, ret, au.getName()));
    }
    return ret;
  }
  
}
