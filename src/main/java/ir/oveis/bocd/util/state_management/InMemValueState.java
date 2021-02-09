package ir.oveis.bocd.util.state_management;

import java.io.IOException;

public class InMemValueState<T> implements StoredValue<T> {
  T inMemValue;

  @Override
  public void setDefault() throws IOException {}

  public InMemValueState(T inMemValue) {
    this.inMemValue = inMemValue;
  }

  @Override
  public void update(T newValue) throws IOException {
    inMemValue = newValue;
  }

  @Override
  public T getValue() throws IOException {
    return inMemValue;
  }
}
