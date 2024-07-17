package eu.dissco.orchestration.backend.utils;

import static eu.dissco.orchestration.backend.configuration.ApplicationConfiguration.HANDLE_PROXY;

public class HandleUtils {

  private HandleUtils() {
    // Utility class not meant to be instantiated
  }

  public static String removeProxy(String id) {
    return id.replace(HANDLE_PROXY, "");
  }
}
