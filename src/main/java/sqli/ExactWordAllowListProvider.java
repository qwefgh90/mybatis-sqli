package sqli;

import java.util.Arrays;
import java.util.List;

public class ExactWordAllowListProvider implements AllowListProvider{

  final List<String> candidates;
  final boolean caseSensitive;
  public ExactWordAllowListProvider(boolean caseSensitive, List<String> candidates) {
    this.candidates = candidates;
    this.caseSensitive = caseSensitive;
  }
  public ExactWordAllowListProvider(boolean caseSensitive, String... candidates) {
    this.candidates = Arrays.stream(candidates).toList();
    this.caseSensitive = caseSensitive;
  }

  @Override
  public boolean isValid(String param) {
    return candidates.stream().anyMatch(candidate -> caseSensitive ? param.equals(candidate) : param.equalsIgnoreCase(candidate));
  }
}
