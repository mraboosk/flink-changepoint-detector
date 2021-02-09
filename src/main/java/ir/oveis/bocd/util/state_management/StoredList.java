package ir.oveis.bocd.util.state_management;

import java.util.List;

public interface StoredList<T> {
  public List<T> getValues() throws Exception;

  public void setDefault() throws Exception;

  public void add(T newValue) throws Exception;

  public void update(List<T> newState) throws Exception;

  public void pruneAfter(int t) throws Exception;
  public int getLength() throws Exception ;
}
