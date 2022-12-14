package sqli;

import org.apache.ibatis.io.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * @author qwefgh90
 * Find attacks
 * <br>1. Visit all memebers in objects recursively and insert them into a String list
 * <br>2. Find malicious codes included in every string value with <a href="https://github.com/payloadbox/sql-injection-payload-list">SQL Injection Payload List</a>
 * <br>3. If malicious codes is found, return true. Otherwise, return false.
 */
public class SQLiPatternChecker {

  private static List<String> patterns = new ArrayList();

  static {
    try (BufferedReader reader = new BufferedReader(Resources.getResourceAsReader("sqli/Generic_SQLI.txt"))) {
      String line = reader.readLine();
      while (line != null) {
        patterns.add(line.toLowerCase()); // to lowercase
        line = reader.readLine();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try (BufferedReader reader = new BufferedReader(Resources.getResourceAsReader("sqli/Generic_UnionSelect.txt"))) {
      String line = reader.readLine();
      while (line != null) {
        patterns.add(line.toLowerCase()); // to lowercase
        line = reader.readLine();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Compare operation executes (A number of parameter * A number of pattern) times
   *
   * @param object
   * @return if any parameter matches one of patterns return true. otherwise return false.
   */
  public static String containsSQLInjectionPattern(Object object) {
    List<String> parameterList = new ArrayList<>();
    collectAllStringMembers(object, parameterList);
    for (String parameter : parameterList) {
      var pattern = containsAnyPattern(parameter);
      if (pattern != null)
        return pattern;
    }
    return null;
  }

  /**
   * It collects not only values of String class
   * but also returned values from toString() of members that implements it.
   *
   * @param target
   * @param list
   */
  public static void collectAllStringMembers(Object target, List<String> list) {
    collectAllStringMembers(target, target.getClass(), list, new HashSet<>());
  }

  private static String containsAnyPattern(String parameter) {
    for (String pattern : patterns) {
      var normalizedParameter = parameter.replaceAll("\s+", "");
      var normalizedPattern = pattern.replaceAll("\s+", "");
      if (!normalizedPattern.isEmpty()
        && !normalizedParameter.isEmpty()
        && normalizedParameter.toLowerCase().contains(normalizedPattern))
        return pattern;
    }
    return null;
  }

  private static void collectAllStringMembers(Object target, Class subType, List<String> candidates, HashSet<Object> visitedObjects) {
    if (target == null)
      return;
    Class targetType = subType != null ? subType : target.getClass();
    ObjectAndSubType pair = new ObjectAndSubType(target, targetType);
    if (visitedObjects.contains(pair)        // Skip if an object and a type have been already visited.
      || targetType.equals(Object.class)   // Skip if a type is Object class.
    )
      return;
    else                                                             // Otherwise, put them into the set
      visitedObjects.add(pair);

    if (Map.class.isAssignableFrom(targetType)) {          // Iterable.class - A class that inherits from Iterable
      Map iterable = (Map) target;
      for (Map.Entry element : (Set<Map.Entry>) iterable.entrySet()) {
        collectAllStringMembers(element.getKey(), null, candidates, visitedObjects); // A recursive call
        collectAllStringMembers(element.getValue(), null, candidates, visitedObjects); // A recursive call
      }
    } else if (Iterable.class.isAssignableFrom(targetType)) {          // Iterable.class - A class that inherits from Iterable
      Iterable iterable = (Iterable) target;
      for (Object element : iterable) {
        collectAllStringMembers(element, null, candidates, visitedObjects); // A recursive call
      }
    } else if (targetType.isArray()) {                               // [L*.class - Array class
      for (Object element : (Object[]) target) {
        collectAllStringMembers(element, null, candidates, visitedObjects); // A recursive call
      }
    } else if (targetType.getPackageName().startsWith("java.lang")) {    // Built-in classes
      candidates.add(target.toString());  //update a list
    } else {                                                          // *.class - Other classes
      if (classImplementsToString(targetType)) {                        // If it implements toString()
        try {
          Method method = targetType.getDeclaredMethod("toString");
          candidates.add((String) method.invoke(target));   //update a list
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
          // An unreachable block
        }
      }
      for (Field field : targetType.getDeclaredFields()) {           // Visit all fields in this class
        try {
          field.setAccessible(true);
          collectAllStringMembers(field.get(target), null, candidates, visitedObjects); // A recursive call
        } catch (IllegalAccessException | InaccessibleObjectException e) {
//          System.err.println(e);
        }
      }
      if (targetType.getSuperclass() != null)                         // Visit all fields in the super class
        collectAllStringMembers(target, targetType.getSuperclass(), candidates, visitedObjects); // A recursive call with a super class
    }
  }

  private static boolean classImplementsToString(final Class<?> aClass) {
    try {
      aClass.getDeclaredMethod("toString");
      return true;

    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  static class ObjectAndSubType {
    Object object;
    Class clazz;

    public ObjectAndSubType(Object object, Class clazz) {
      this.object = object;
      this.clazz = clazz;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ObjectAndSubType that = (ObjectAndSubType) o;
      return Objects.equals(object, that.object) && Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
      return Objects.hash(object, clazz);
    }
  }


}
