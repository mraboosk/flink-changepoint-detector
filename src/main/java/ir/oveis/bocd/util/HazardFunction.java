package ir.oveis.bocd.util;

import java.util.Collections;
import java.util.List;

@FunctionalInterface
public interface HazardFunction {
    HazardFunction ConstantHazardFunction = (lambda, dim) -> Collections.nCopies(dim, 1 / lambda);

    List<Double> apply(double lambda, int dim);
}
