package ir.oveis.bocd.util.state_management;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;

import java.util.ArrayList;
import java.util.List;

public class ManagedListState<T> implements StoredList<T> {
  private ListState<T> state;
  private List<T> localState;
  private T defaultFirstValue;

  public void refresh() throws Exception {
    List<T> valuesList = new ArrayList<>();
    state.get().forEach(valuesList::add);
    localState = valuesList;
  }

  public void setDefault() throws Exception {
    refresh();
    if (localState.isEmpty()) add(defaultFirstValue);
  }

  public void add(T newValue) throws Exception {
    state.add(newValue);
    localState.add(newValue);
  }

  public void update(List<T> newState) throws Exception {
    localState = newState;
    update();
  }

  public void update() throws Exception {
    state.update(localState);
  }

  public List<T> getValues() throws Exception {
    if (localState != null) {
      return localState;
    }
    setDefault();
    return localState;
  }

  public int getLength() throws Exception {
    return getValues().size();
  }

  public void pruneAfter(int t) throws Exception {
    update(getValues().subList(0, t + 1));
  }

  public ManagedListState(
      RuntimeContext context, String name, Class<T> tClass, T defaultFirstValue) {
    ListStateDescriptor<T> descriptor = new ListStateDescriptor<>(name, tClass);
    state = context.getListState(descriptor);
    this.defaultFirstValue = defaultFirstValue;
  }
}
