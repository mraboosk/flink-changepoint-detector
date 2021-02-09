package ir.oveis.bocd;

import ir.oveis.bocd.util.CollectorsUtil;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;

class OnlineChangeDetectorTest {
    private final int delay = 4;
    private final int maxSize = 4;
    private final double threshold = 0.5;
    private OnlineChangeDetector changeDetector;
    private List<Double> sampleValues;

    @BeforeEach
    void setUp() {
        int size = 1000;
        Random rnd = new Random();
        rnd.setSeed(0);

        sampleValues =
                IntStream.range(0, size)
                        .mapToDouble(i -> rnd.nextGaussian())
                        .boxed()
                        .collect(CollectorsUtil.toList(size));
        IntStream.range(sampleValues.size() / 4, sampleValues.size() / 2)
                .forEach(i -> sampleValues.set(i, sampleValues.get(i) + 10));
        IntStream.range(sampleValues.size() / 2, 3 * sampleValues.size() / 4)
                .forEach(i -> sampleValues.set(i, sampleValues.get(i) - 10));
        double lambda = 100;
        double alpha = 0.1;
        double beta = 1;
        double kappa = 1;
        double mu = 0.;
        double growthProb = 1;
        changeDetector = new OnlineChangeDetector(alpha, lambda, growthProb, beta, kappa, mu, maxSize);
    }

    @Test
    void changePointDetection_changeHappenedInNormalData_changesDetected() throws Exception {
        List<Integer> changePoints = new ArrayList<>();
        for (int i = 0; i < delay; i++) {
            changeDetector.update(sampleValues.get(i));
        }
        for (int i = delay; i < sampleValues.size(); i++) {
            changeDetector.update(sampleValues.get(i));
            if (changeDetector.growthProbability(delay) >= threshold) {
                changePoints.add(changeDetector.getCurrentT() - delay + 1);
            }
        }
        List<Integer> expectedChangePoints = Arrays.asList(250, 500, 750);
        MatcherAssert.assertThat(changePoints, is(expectedChangePoints));
    }

    @Test
    void prune_changeHappenedThePruned_changesDetected() throws Exception {
        List<Integer> changePoints = new ArrayList<>();
        for (int i = 0; i < delay; i++) {
            changeDetector.update(sampleValues.get(i));
        }
        for (int i = delay; i < sampleValues.size(); i++) {
            changeDetector.update(sampleValues.get(i));
            if (changeDetector.growthProbability(delay) >= threshold) {
                changePoints.add(changeDetector.getCurrentT() - delay + 1);
                changeDetector.prune(changeDetector.getCurrentT() - delay);
            }
        }
        List<Integer> expectedChangePoints = Arrays.asList(250, 500, 750);
        MatcherAssert.assertThat(changePoints, is(expectedChangePoints));
    }

    @Test
    void trim_changeHappenedThePruned_changesDetected() throws Exception {
        for (Double sampleValue : sampleValues) {
            changeDetector.update(sampleValue);
            changeDetector.trim();
        }
        Assertions.assertTrue(changeDetector.getGrowthProbabilities().size() < maxSize + 2);
    }
}
