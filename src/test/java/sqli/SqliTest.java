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

  @Nested
  @DisplayName("Nested SQLiA")
  class NestedSQLIA {
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
    void nowhere() throws IllegalAccessException {
      User user = new User();
      user.setId(1);
      user.setName("My Name");
      user.setGroups(List.of("group1", "group2"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNull(matchedPattern);
    }

    @Test
    void list() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("User 1");
      user.setGroups(List.of("or 1=1", "grp2"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print("✔ SQLIA's been found in an item in List");
    }

    @Test
    void map() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      Map map = Map.of("role1", "'%20or%20'x'='x");
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(map);
      Assertions.assertNotNull(matchedPattern);
      print("✔ SQLIA's been found in a entry in Map");
    }

    @Test
    void set() throws IllegalAccessException {
      Set<String> set = Set.of("role1", "1' or 3=3 --");
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(set);
      Assertions.assertNotNull(matchedPattern);
      print("✔ SQLIA's been found in a entry in Set");
    }

    @Test
    void array() throws IllegalAccessException {
      String[] arr = {"role1", "10 or 1=1"};
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(arr);
      Assertions.assertNotNull(matchedPattern);
      print("✔ SQLIA's been found in an element in Array");
    }

    @Test
    void memberVariableOfClazz() throws IllegalAccessException {
      User user = new User();
      user.setId(1);
      user.setName("or 1=1");
      user.setGroups(List.of("", "grp2"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print("✔ SQLIA's been found in a member variable in User class");
    }


    @Test
    void memberVariableOfParent() throws IllegalAccessException {
      class Parent {
        String parentName;

        public void setParentName(String parentName) {
          this.parentName = parentName;
        }
      }
      class Child extends Parent {
        String childName;

        void setChildName(String childName) {
          this.childName = childName;
        }
      }
      var child = new Child();
      child.setParentName("or 1=1");
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(child);
      Assertions.assertNotNull(matchedPattern);
      print("✔ SQLIA's been found in a member variable in the parent class");
    }

    @Test
    void returnFromToString() throws IllegalAccessException {
      class Parent {
        String parentName;

        public void setParentName(String parentName) {
          this.parentName = parentName;
        }

        @Override
        public String toString() {
          return "or 1=1";
        }
      }
      class Child extends Parent {
        String childName;

        void setChildName(String childName) {
          this.childName = childName;
        }
      }
      var child = new Child();
      child.setParentName("no name");
      child.setChildName("no name");
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(child);
      Assertions.assertNotNull(matchedPattern);
      print("✔ SQLIA's been found in the value from toString()");
    }
  }

  @Nested
  @DisplayName("Four types of SQLIA")
  class FourTypesOfSQLIA {
    @Test
    void errorBased1() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("1' or '1' = '1");
      user.setGroups(List.of("grp1"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print(String.format("✔ Type1-1 Attack's found. A pattern is [ %s ] matched.", matchedPattern));
    }

    @Test
    void errorBased2() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("10 AND 1=2");
      user.setGroups(List.of("grp1"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print(String.format("✔ Type1-2 Attack's found. A pattern is [ %s ] matched.", matchedPattern));
    }

    @Test
    void errorBased3() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("10; INSERT INTO users …");
      user.setGroups(List.of("grp1"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print(String.format("✔ Type1-3 Attack's found. A pattern is [ %s ] matched.", matchedPattern));
    }

    @Test
    void errorBased4() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("10 ORDER BY 10");

      user.setGroups(List.of("grp1"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print(String.format("✔ Type1-4 Attack's found. A pattern is [ %s ] matched.", matchedPattern));
    }

    @Test
    void unionBased1() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("1 UNION ALL SELECT creditCardNumber,1,1 FROM CreditCardTable");

      user.setGroups(List.of("grp1"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print(String.format("✔ Type2-1 Attack's found. A pattern is [ %s ] matched.", matchedPattern));
    }

    @Test
    void unionBased2() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("10 UNION SELECT 1,null,null—");

      user.setGroups(List.of("grp1"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print(String.format("✔ Type2-2 Attack's found. A pattern is [ %s ] matched.", matchedPattern));
    }

    @Test
    void blind1() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("1' AND ASCII(SUBSTRING(username,1,1))=97 AND '1'='1");

      user.setGroups(List.of("grp1"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print(String.format("✔ Type3-1 Attack's found. A pattern is [ %s ] matched.", matchedPattern));
    }

    @Test
    void blind2() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("1' AND LENGTH(username)=4 AND '1' = '1");

      user.setGroups(List.of("grp1"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print(String.format("✔ Type3-2 Attack's found. A pattern is [ %s ] matched.", matchedPattern));
    }

    @Test
    void timeBased1() throws IllegalAccessException {
      List<String> list = new ArrayList<>();
      User user = new User();
      user.setId(1);
      user.setName("10 AND IF(version() like ‘5%’, sleep(10), ‘false’))--");

      user.setGroups(List.of("grp1"));
      user.setRoles(List.of("role1"));
      String matchedPattern = SQLiPatternChecker.containsSQLInjectionPattern(user);
      Assertions.assertNotNull(matchedPattern);
      print(String.format("✔ Type4-1 Attack's found. A pattern is [ %s ] matched.", matchedPattern));
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
