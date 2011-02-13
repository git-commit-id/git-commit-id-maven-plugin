package pl.project13.maven.git;

import org.fest.assertions.Condition;

import java.util.Map;

/**
* Date: 2/13/11
*
* @author Konrad Malawski
*/
class ContainsKeyCondition extends Condition<Map<?, ?>> {

  private String key;

  public ContainsKeyCondition(String key) {
    this.key = key;
  }

  @Override
  public boolean matches(Map<?, ?> map) {
    return map.containsKey(key);
  }
}
