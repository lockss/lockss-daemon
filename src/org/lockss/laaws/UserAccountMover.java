/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.laaws;

import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.app.LockssDaemon;
import org.lockss.util.Logger;

import java.util.Collection;
import java.util.stream.Collectors;

public class UserAccountMover extends Worker {
  private static final Logger log = Logger.getLogger(UserAccountMover.class);

  private Collection<UserAccount> acctsToMove;
  private int nMoved = 0;

  public UserAccountMover(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
  }

  public void run() {
    moveUserAccounts(getAccountsToMove());
  }

    // Only transfer editable (i.e., non-static) user accounts
  public Collection<UserAccount> getAccountsToMove() {
    if (acctsToMove == null) {
      AccountManager acctsMgr =
        LockssDaemon.getLockssDaemon().getAccountManager();
      acctsToMove = acctsMgr.getUsers()
        .stream()
        .filter(UserAccount::isEditable)
        .collect(Collectors.toList());
    }
    return acctsToMove;
  }



  private void moveUserAccounts(Collection<UserAccount> accts) {
    if (accts == null || accts.isEmpty()) {
      log.warning("No user accounts to move");
      return;
    }

    try {
      cfgUsersApiClient.postUsers(accts);
      log.info("Successfully moved user accounts");
      auMover.logReport("Moved user accounts");
      nMoved = accts.size();
    } catch (Exception e) {
      String err = "Attempt to move user accounts failed: " + e.getMessage();
      log.error(err, e);
      auMover.addError(err);
    }
  }
}
