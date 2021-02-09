package ir.oveis.bocd.util.state_management;

import java.util.ArrayList;
import java.util.List;

public class InMemListState<T> implements StoredList<T> {
  List<T> inMemState;

  public InMemListState(List<T> inMemState) {
    this.inMemState = inMemState;
  }

  @Override
  public List<T> getValues() throws Exception {
    return inMemState;
  }

  @Override
  public void setDefault() throws Exception {
    if (inMemState.isEmpty()) inMemState = new ArrayList<>();
  }

  @Override
  public void add(T newValue) throws Exception {
    inMemState.add(newValue);
  }

  @Override
  public void update(List<T> newState) throws Exception {
    inMemState = newState;
  }

  @Override
  public void pruneAfter(int t) throws Exception {
    inMemState = inMemState.subList(0, t + 1);
  }

  public int getLength() throws Exception {
    return inMemState.size();
  }
}
