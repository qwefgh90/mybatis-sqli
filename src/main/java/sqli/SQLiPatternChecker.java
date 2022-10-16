package sqli;

import org.apache.ibatis.io.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * @author qwefgh90
 * find attack
 * <br>1. Visit all memebers in objects recursively and insert them into a String list
 * <br>2. Find malicious codes included in every string value with <a href="https://github.com/payloadbox/sql-injection-payload-list">SQL Injection Payload List</a>
 * <br>3. If malicious codes is found, return true. Otherwise, return false.
 */
public class SQLiPatternChecker {

  private static List<String> patterns = new ArrayList();

  static{
    try (BufferedReader reader = new BufferedReader(Resources.getResourceAsReader("sqli/Generic_SQLI.txt"))) {
      String line = reader.readLine();
      while(line != null){
        patterns.add(line.toLowerCase()); // to lowercase
        line = reader.readLine();
      }
    } catch(IOException e){
      throw new RuntimeException(e);
    }
  }

  /**
   * Compare operation executes (A number of parameter * A number of pattern) times
   * @param object
   * @return if any parameter matches one of patterns return true. otherwise return false.
   */
  public static boolean containsSQLInjectionPattern(Object object){
    List<String> parameterList = new ArrayList<>();
    collectAllStringMembers(object, parameterList);
    for(String parameter : parameterList){
      if(containsAnyPattern(parameter))
        return true;
    }
    return false;
  }

  /**
   * It collects not only values of String class
   * but also returned values from toString() of members that implements it.
   * @param target
   * @param list
   */
  public static void collectAllStringMembers(Object target, List<String> list) {
    collectAllStringMembers(target, list, null, new HashSet<>());
  }

  private static boolean containsAnyPattern(String parameter){
    for(String pattern : patterns){
      if(parameter.toLowerCase().contains(pattern))
        return true;
    }
    return false;
  }

  private static void collectAllStringMembers(Object target, List<String> list, Class targetTypeInContext, HashSet<Object> visitedObjects) {
    if (target == null)
      return;
    targetTypeInContext = targetTypeInContext != null ? targetTypeInContext : target.getClass();
    ObjectClass pair = new ObjectClass(target, targetTypeInContext);
    if(visitedObjects.contains(pair))                                     // Skip if an object and a type have been already visited.
      return;
    else                                                                  // Otherwise, put them into the set
      visitedObjects.add(pair);

    if (String.class.equals(targetTypeInContext)) {                           // String.class - A String class
      list.add((String) target);  //update a list
    }
    else if (Iterable.class.isAssignableFrom(targetTypeInContext)) {          // Iterable.class - A class that inherits from Iterable
      Iterable iterable = (Iterable) target;
      for (Object element : iterable) {
        collectAllStringMembers(element, list, null, visitedObjects); // A recursive call
      }
    } else if (targetTypeInContext.isArray()) {                               // [L*.class - Array class
      for (Object element : (Object[]) target) {
        collectAllStringMembers(element, list, null, visitedObjects); // A recursive call
      }
    }else {                                                                   // *.class - Other classes
      for (Field field : targetTypeInContext.getDeclaredFields()) {           // Visit all fields in this class
        try {
          field.setAccessible(true);
          collectAllStringMembers(field.get(target), list, null, visitedObjects); // A recursive call
        } catch (IllegalAccessException | InaccessibleObjectException e) {
//          System.err.println(e);
        }
      }
      if(!targetTypeInContext.equals(Object.class)
        && classImplementsToString(targetTypeInContext)){ // If it implements toString()
        try {
          Method method = targetTypeInContext.getDeclaredMethod("toString");
          list.add((String)method.invoke(target));   //update a list
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
          // An unreachable block
        }
      }
      if (targetTypeInContext.getSuperclass() != null)  // Visit all fields in the super class
        collectAllStringMembers(target, list, targetTypeInContext.getSuperclass(), visitedObjects); // A recursive call with a super class
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

  static class ObjectClass{
    Object object;
    Class clazz;

    public ObjectClass(Object object, Class clazz) {
      this.object = object;
      this.clazz = clazz;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ObjectClass that = (ObjectClass) o;
      return Objects.equals(object, that.object) && Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
      return Objects.hash(object, clazz);
    }
  }

}
