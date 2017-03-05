/*
 * Copyright (c) 2017  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.network;


import de.mlessmann.common.annotations.NotNull;
import de.homeworkproject.homework.api.provider.IHWProvider;
import de.homeworkproject.homework.internal.providers.HWProvider;

import org.json.JSONException;
import org.json.JSONObject;

public class ManualHWProvider extends HWProvider {
  public ManualHWProvider(@NotNull JSONObject information) {
    super(information);
  }

  public static ManualHWProvider createProvider(String host, int port, boolean ssl) {
    JSONObject js = new JSONObject();
    try {
      js.put("name", "Manuell");
      js.put("postal", "");
      js.put("country", "");
      js.put("state", "");
      js.put("city", "");

      JSONObject conn = new JSONObject();
      conn.put("host", host);
      conn.put("sslPort", ssl ? port : 0);
      conn.put("plainPort", !ssl ? port : 0);
      js.put("connection", conn);
    } catch (JSONException e) {
      throw new RuntimeException("Error creating provider info JSON");
    }

    return new ManualHWProvider(js);
  }
}
