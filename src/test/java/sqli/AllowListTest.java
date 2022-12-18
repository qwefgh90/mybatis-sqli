package sqli;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class AllowListTest {
  @Test
  void dynamicColumnList1(){
    var candidateColumns = new ExactWordAllowListProvider(true, "blog_id","title","date","writer_email");
    var checker = new AllowListChecker(candidateColumns);
    Assertions.assertTrue(checker.isValid("blog_id"));
    Assertions.assertTrue(checker.isValid("title"));
    Assertions.assertFalse(checker.isValid("created_date"));
    Assertions.assertFalse(checker.isValid("post_password"));
  }

  @Test
  void dynamicColumnList2(){
    var candidateColumns = new ExactWordAllowListProvider(true, "author_name","id","address","author_email");
    var checker = new AllowListChecker(candidateColumns);
    Assertions.assertTrue(checker.isValid("author_name"));
    Assertions.assertTrue(checker.isValid("address"));
    Assertions.assertFalse(checker.isValid("password"));
  }

  @Test
  void dynamicTable1(){
    var provider = RegexAllowListProvider.create(Pattern.compile("Log_\\d\\d\\d\\d\\d\\d"));
    var checker = new AllowListChecker(provider);
    Assertions.assertTrue(checker.isValid("Log_200102"));
    Assertions.assertTrue(checker.isValid("Log_210304"));
    Assertions.assertFalse(checker.isValid("User"));
    Assertions.assertFalse(checker.isValid("Account"));
    Assertions.assertFalse(checker.isValid("Document"));
    Assertions.assertFalse(checker.isValid("Card"));
    Assertions.assertFalse(checker.isValid("Payment"));
  }

  @Test
  void dynamicTable2(){
    var candidateColumns = new ExactWordAllowListProvider(true, "Blog","Post","Comment");
    var provider = RegexAllowListProvider.create(Pattern.compile("Board_\\w+"));
    var checker = new AllowListChecker(provider, candidateColumns);
    Assertions.assertTrue(checker.isValid("Board_200102"));
    Assertions.assertTrue(checker.isValid("Blog"));
    Assertions.assertTrue(checker.isValid("Post"));
    Assertions.assertTrue(checker.isValid("Comment"));
    Assertions.assertFalse(checker.isValid("Card"));
    Assertions.assertFalse(checker.isValid("User"));
  }

  @Test
  void sort(){
    var candidateColumns = new ExactWordAllowListProvider(false, "ASC", "DESC");
    var checker = new AllowListChecker(candidateColumns);
    Assertions.assertTrue(checker.isValid("ASC"));
    Assertions.assertTrue(checker.isValid("DESC"));
    Assertions.assertFalse(checker.isValid("injection!!!"));
    Assertions.assertFalse(checker.isValid("injection222"));
  }
}
