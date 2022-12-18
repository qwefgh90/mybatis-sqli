package sqli;

import java.util.Arrays;

public class AllowListChecker {
  private AllowListProvider[] allowedList;
  public AllowListChecker(AllowListProvider... providers) {
    this.allowedList = providers;
  }

  public boolean isValid(String param){
    return Arrays.stream(this.allowedList).allMatch(p -> p.isValid(param));
  }
}
