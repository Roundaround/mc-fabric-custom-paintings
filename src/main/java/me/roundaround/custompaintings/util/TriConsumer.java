package me.roundaround.custompaintings.util;

import java.util.Objects;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
  void accept(T t, U u, V v);

  default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
    Objects.requireNonNull(after);

    return (t, u, v) -> {
      this.accept(t, u, v);
      after.accept(t, u, v);
    };
  }
}
