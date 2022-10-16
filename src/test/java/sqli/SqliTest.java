/*
 *    Copyright 2009-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package sqli;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class SqliTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    // create a SqlSessionFactory
    try (Reader reader = Resources.getResourceAsReader("sqli/mybatis-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }

    // populate in-memory database
    BaseDataTest.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
        "sqli/CreateDB.sql");
  }

  @Test
  void visitAllMembers() throws IllegalAccessException {
    List<String> list = new ArrayList<>();
    User user = new User();
    user.setId(1);
    user.setName("User 1");
    user.setGroups(List.of("grp1", "grp2"));
    user.setRoles(List.of("role1"));
    SQLiPatternChecker.collectAllStringMembers(user, list);
    Map map = Map.of("role1", "'%20or%20'x'='x");
    SQLiPatternChecker.collectAllStringMembers(map, list);
    for(String s : list){
      System.out.println(s);
    }
  }

  @Test
  void testContainsSQLInjectionPattern1() throws IllegalAccessException {
    List<String> list = new ArrayList<>();
    User user = new User();
    user.setId(1);
    user.setName("User 1");
    user.setGroups(List.of("grp1", "grp2"));
    user.setRoles(List.of("role1"));
    boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
    Assertions.assertFalse(result);
    print(user);
  }

  @Test
  void testContainsSQLInjectionPattern2() throws IllegalAccessException {
    List<String> list = new ArrayList<>();
    User user = new User();
    user.setId(1);
    user.setName("User 1");
    user.setGroups(List.of("or 1=1", "grp2"));
    user.setRoles(List.of("role1"));
    boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
    Assertions.assertTrue(result);
    print(user);
  }
  @Test
  void testContainsSQLInjectionPattern3() throws IllegalAccessException {
    List<String> list = new ArrayList<>();
    User user = new User();
    user.setId(1);
    user.setName("User 1");
    user.setGroups(List.of("grp1", "or 1=1 /*"));
    user.setRoles(List.of("role1"));
    boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
    Assertions.assertTrue(result);
    print(user);
  }

  @Test
  void testContainsSQLInjectionPattern4() throws IllegalAccessException {
    List<String> list = new ArrayList<>();
    Map map = Map.of("role1", "'%20or%20'x'='x");
    boolean result = SQLiPatternChecker.containsSQLInjectionPattern(map);
    print(map);
    Assertions.assertTrue(result);
  }
  @Test
  void testContainsSQLInjectionPattern5() throws IllegalAccessException {
    List<String> list = new ArrayList<>();
    Map map = Map.of("role1", "<>\"'%;)(&+");
    boolean result = SQLiPatternChecker.containsSQLInjectionPattern(map);
    print(map);
    Assertions.assertTrue(result);
  }
  @Test
  void testContainsSQLInjectionPattern6() throws IllegalAccessException {
    List<String> list = new ArrayList<>();
    Set map = Set.of("role1", "' or 3=3 --");
    boolean result = SQLiPatternChecker.containsSQLInjectionPattern(map);
    print(map);
    Assertions.assertTrue(result);
  }
//  @Test
//  void shouldMapListOfStrings() {
//    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
//      Mapper mapper = sqlSession.getMapper(Mapper.class);
//      List<User> users = mapper.getUsersAndGroups(1);
//      Assertions.assertEquals(1, users.size());
//      Assertions.assertEquals(2, users.get(0).getGroups().size());
//      Assertions.assertEquals(2, users.get(0).getRoles().size());
//    }
//  }
//
//  @Test
//  void shouldMapListOfStringsToMap() {
//    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
//      Mapper mapper = sqlSession.getMapper(Mapper.class);
//      List<Map<String, Object>> results = mapper.getUsersAndGroupsMap(1);
//      Assertions.assertEquals(1, results.size());
//      Assertions.assertEquals(2, ((List<?>) results.get(0).get("groups")).size());
//      Assertions.assertEquals(2, ((List<?>) results.get(0).get("roles")).size());
//    }
//  }
//
//  @Test
//  void shouldFailFastIfCollectionTypeIsAmbiguous() throws Exception {
//    try (Reader reader = Resources
//        .getResourceAsReader("sqli/mybatis-config-invalid.xml")) {
//      new SqlSessionFactoryBuilder().build(reader);
//      fail("Should throw exception when collection type is unresolvable.");
//    } catch (PersistenceException e) {
//      assertTrue(e.getMessage()
//          .contains("Ambiguous collection type for property 'groups'. You must specify 'javaType' or 'resultMap'."));
//    }
//  }


  private void print(Object object) throws IllegalAccessException {
    List<String> list = new ArrayList<>();
    SQLiPatternChecker.collectAllStringMembers(object, list);
    for(String s : list){
      System.out.println(s);
    }
  }
}
