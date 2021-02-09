package ir.oveis.bocd.util.state_management;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;

import java.io.IOException;

public class ManagedValueState<T> implements StoredValue<T> {
  private ValueState<T> state;
  private T localState;
  private final T defaultValue;

  public void setDefault() throws IOException {
    refresh();
    if (localState == null) update(defaultValue);
  }

  public void refresh() throws IOException {
    localState = state.value();
  }

  public void update() throws IOException {
    state.update(localState);
  }

  public void update(T newValue) throws IOException {
    localState = newValue;
    update();
  }

  public T getValue() throws IOException {
    if (localState != null) return localState;
    setDefault();
    return localState;
  }

  public ManagedValueState(RuntimeContext context, String name, Class<T> tClass, T defaultValue) {
    ValueStateDescriptor<T> descriptor = new ValueStateDescriptor<>(name, tClass);
    state = context.getState(descriptor);
    this.defaultValue = defaultValue;
  }
}
