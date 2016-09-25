/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.network;

public interface LoginResultListener {
  public enum Result {
    LOGGED_IN,
    CONNECTION_FAILED,
    SERVER_INCOMPATIBLE,
    NO_CREDENTIALS_PRESENT,
    INVALID_CREDENTIALS,
    CONNECTION_CLOSED,
    UNKNOWN
  }

  public void onLoginDone(Result result);
}
