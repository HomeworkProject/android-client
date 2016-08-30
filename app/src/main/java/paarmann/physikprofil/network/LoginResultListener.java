/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil.network;

public interface LoginResultListener {
  public enum Result {
    LOGGED_IN,
    CONNECTION_FAILED,
    SERVER_INCOMPATIBLE,
    NO_CREDENTIALS_PRESENT,
    INVALID_CREDENTIALS,
    UNKNOWN
  }

  public void onLoginDone(Result result);
}
