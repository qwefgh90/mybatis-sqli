package sqli;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class AllowListTest {
  @Test
  void dynamicColumnList1(){
    var candidateColumns = new ExactWordAllowListProvider(true, "blog_id","title","date","writer_email");
    AllowListProviderRegistry.register("POST_COLUMN", candidateColumns);
    Assertions.assertTrue(AllowListProviderRegistry.isValid("blog_id", "POST_COLUMN"));
    Assertions.assertTrue(AllowListProviderRegistry.isValid("title", "POST_COLUMN"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid( "post_password", "POST_COLUMN"));
  }

  void dynamicColumnList2(){
    var candidateColumns = new ExactWordAllowListProvider(true, "author_name","id","address","author_email");
    AllowListProviderRegistry.register("AUTHOR_COLUMN", candidateColumns);
    Assertions.assertTrue(AllowListProviderRegistry.isValid("author_name", "AUTHOR_COLUMN"));
    Assertions.assertTrue(AllowListProviderRegistry.isValid("address", "AUTHOR_COLUMN"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid("password", "AUTHOR_COLUMN"));
  }

  @Test
  void dynamicTable1(){
    var provider = RegexAllowListProvider.create(Pattern.compile("Log_\\d\\d\\d\\d\\d\\d"));
    AllowListProviderRegistry.register("TABLE_PATTERN", provider);
    Assertions.assertTrue(AllowListProviderRegistry.isValid( "Log_200102", "TABLE_PATTERN"));
    Assertions.assertTrue(AllowListProviderRegistry.isValid( "Log_210304", "TABLE_PATTERN"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid( "User", "TABLE_PATTERN"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid( "Account", "TABLE_PATTERN"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid( "Document", "TABLE_PATTERN"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid( "Card", "TABLE_PATTERN"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid( "Payment", "TABLE_PATTERN"));
  }

  void dynamicTable2(){
    var candidateColumns = new ExactWordAllowListProvider(true, "Blog","Post","Comment");
    var provider = RegexAllowListProvider.create(Pattern.compile("Board_\\w+"));
    AllowListProviderRegistry.register("TABLE_NAMES", candidateColumns);
    AllowListProviderRegistry.register("BOARD_TABLE_NAME", provider);
    Assertions.assertTrue(AllowListProviderRegistry.isValid("Board_200102", "TABLE_NAMES", "BOARD_TABLE_NAME"));
    Assertions.assertTrue(AllowListProviderRegistry.isValid("Blog", "TABLE_NAMES", "BOARD_TABLE_NAME"));
    Assertions.assertTrue(AllowListProviderRegistry.isValid("Post", "TABLE_NAMES", "BOARD_TABLE_NAME"));
    Assertions.assertTrue(AllowListProviderRegistry.isValid("Comment", "TABLE_NAMES", "BOARD_TABLE_NAME"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid("Card", "TABLE_NAMES", "BOARD_TABLE_NAME"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid("User", "TABLE_NAMES", "BOARD_TABLE_NAME"));
  }

  @Test
  void sort(){
    var candidateColumns = new ExactWordAllowListProvider(false, "ASC", "DESC");
    AllowListProviderRegistry.register("SORT", candidateColumns);
    Assertions.assertTrue(AllowListProviderRegistry.isValid("ASC", "SORT"));
    Assertions.assertTrue(AllowListProviderRegistry.isValid("DESC", "SORT"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid("10 UNION SELECT 1,null,null—", "SORT"));
    Assertions.assertFalse(AllowListProviderRegistry.isValid("10 ORDER BY 10", "SORT"));
  }
}
