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
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Section;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.*;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.googlecode.catchexception.apis.BDDCatchException.caughtException;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.*;

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
  void runStatement() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      try {
        assertThrows(PersistenceException.class, () -> {
          Mapper mapper = session.getMapper(Mapper.class);
          Author author = new Author(-1, "cbegin", "******", "cbegin@nowhere.com", "N/A", Section.NEWS);
          mapper.getUsersAndGroups(1, "'");
        });
      } finally {
        session.rollback();
      }
    }
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
    for (String s : list) {
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

  @Test
  void testContainsSQLInjectionPattern7() throws IllegalAccessException {
    List<String> list = new ArrayList<>();
    Set map = Set.of("role1", "10 or 1=1");
    boolean result = SQLiPatternChecker.containsSQLInjectionPattern(map);
    print(map);
    Assertions.assertTrue(result);
  }


  @Nested
  @DisplayName("4 types of SQLIA")
  class SQLIA {
    @Test
    void errorBased1() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("1' or '1' = '1");

      user.setGroups(List.of("grp1", "or 1=1 /*"));
      user.setRoles(List.of("role1"));
      boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertTrue(result);
      print(user);
    }

    @Test
    void errorBased2() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("10 AND 1=2");

      user.setGroups(List.of("grp1", "or 1=1 /*"));
      user.setRoles(List.of("role1"));
      boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertTrue(result);
      print(user);
    }

    @Test
    void errorBased3() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("10; INSERT INTO users …");

      user.setGroups(List.of("grp1", "or 1=1 /*"));
      user.setRoles(List.of("role1"));
      boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertTrue(result);
      print(user);
    }

    @Test
    void errorBased4() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("10 ORDER BY 10");

      user.setGroups(List.of("grp1", "or 1=1 /*"));
      user.setRoles(List.of("role1"));
      boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertTrue(result);
      print(user);
    }

    @Test
    void unionBased1() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("1 UNION ALL SELECT creditCardNumber,1,1 FROM CreditCardTable");

      user.setGroups(List.of("grp1", "or 1=1 /*"));
      user.setRoles(List.of("role1"));
      boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertTrue(result);
      print(user);
    }

    @Test
    void unionBased2() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("10 UNION SELECT 1,null,null—");

      user.setGroups(List.of("grp1", "or 1=1 /*"));
      user.setRoles(List.of("role1"));
      boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertTrue(result);
      print(user);
    }

    @Test
    void blind1() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("1' AND ASCII(SUBSTRING(username,1,1))=97 AND '1'='1");

      user.setGroups(List.of("grp1", "or 1=1 /*"));
      user.setRoles(List.of("role1"));
      boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertTrue(result);
      print(user);
    }

    @Test
    void blind2() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("1' AND LENGTH(username)=4 AND '1' = '1");

      user.setGroups(List.of("grp1", "or 1=1 /*"));
      user.setRoles(List.of("role1"));
      boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertTrue(result);
      print(user);
    }

    @Test
    void timeBased1() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("10 AND IF(version() like ‘5%’, sleep(10), ‘false’))--");

      user.setGroups(List.of("grp1", "or 1=1 /*"));
      user.setRoles(List.of("role1"));
      boolean result = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertTrue(result);
      print(user);
    }

  }

  private void print(Object object) throws IllegalAccessException {
    List<String> list = new ArrayList<>();
    SQLiPatternChecker.collectAllStringMembers(object, list);
    for (String s : list) {
      System.out.println(s);
    }
  }
}
