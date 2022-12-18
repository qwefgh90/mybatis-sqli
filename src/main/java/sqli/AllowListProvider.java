package sqli;

public interface AllowListProvider {
  boolean isValid(String param);
}
