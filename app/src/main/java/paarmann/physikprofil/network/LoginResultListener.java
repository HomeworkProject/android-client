/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil.network;

public interface LoginResultListener {
  public enum Error {
    CONNECTION_FAILED,
    SERVER_INCOMPATIBLE,
    NO_CREDENTIALS_PRESENT,
    INVALID_CREDENTIALS,
    UNKNOWN
  }

  public void onLoginSuccessful();
  public void onLoginFailed(Error error);
}
