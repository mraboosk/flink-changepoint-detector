package ir.oveis.bocd.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CollectorsUtil {
    public static <T> Collector<T, ?, List<T>> toList(int size) {
        return Collectors.toCollection(() -> new ArrayList<>(size));
    }

}
