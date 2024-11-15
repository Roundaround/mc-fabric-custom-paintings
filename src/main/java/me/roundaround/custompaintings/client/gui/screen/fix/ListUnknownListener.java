package me.roundaround.custompaintings.client.gui.screen.fix;

import me.roundaround.custompaintings.util.CustomId;

import java.util.Map;

public interface ListUnknownListener {
  void onListUnknownResponse(Map<CustomId, Integer> counts);
}
