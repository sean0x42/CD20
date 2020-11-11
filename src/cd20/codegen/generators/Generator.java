package cd20.codegen.generators;

import java.util.Collection;

public interface Generator<T> {
  public abstract void generate(Collection<T> collection);
  public abstract int getSize();
  public abstract String getBody();
}
