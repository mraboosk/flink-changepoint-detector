package ir.oveis.bocd.util.state_management;

import java.io.IOException;

public interface StoredValue<T> {
  public void setDefault() throws IOException;

  public void update(T newValue) throws IOException;

  public T getValue() throws IOException;
}
