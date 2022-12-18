package sqli;

import java.util.regex.Pattern;

public class RegexAllowListProvider implements AllowListProvider {

  final Pattern pattern;

  public static RegexAllowListProvider create(Pattern pattern) {
    return new RegexAllowListProvider(pattern);
  }

  public RegexAllowListProvider(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean isValid(String param) {
    return pattern.matcher(param).matches();
  }
}
