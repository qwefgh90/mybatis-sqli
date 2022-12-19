package sqli;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AllowListProviderRegistry {
  private static Map<String, AllowListProvider> providerMap = new HashMap<>();

  public static void register(String providerName, AllowListProvider provider){
    providerMap.put(providerName, provider);
  }

  /**
   * Check if the parameter is valid by using providers.
   * @param param a text to check
   * @param providerNames a list of pattern providers
   * @return return true if a parameter is valid. otherwise, return false
   */
  public static boolean isValid(String param, String... providerNames){
    if(providerNames == null || providerNames.length == 0)
      throw new RuntimeException("There are no names provided.");
    for(var providerName : providerNames) {
      if (providerMap.containsKey(providerName) && providerMap.get(providerName).isValid(param))
        return true;
    }
    return false;
  }
}
