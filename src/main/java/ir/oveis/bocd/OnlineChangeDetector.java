package ir.oveis.bocd;

import ir.oveis.bocd.util.CollectorsUtil;
import ir.oveis.bocd.util.HazardFunction;

import ir.oveis.bocd.util.StudentTDistribution;
import ir.oveis.bocd.util.state_management.*;
import org.apache.flink.api.common.functions.RuntimeContext;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class OnlineChangeDetector {
    private static final double DEFAULT_LAMBDA = 25;
    private static final double DEFAULT_ALPHA = 0.1f;
    private static final double DEFAULT_BETA = 1;
    private static final double DEFAULT_KAPPA = 1;
    private static final double DEFAULT_MU = 0;
    private static final double DEFAULT_GROWTH_PROB = 1;
    private static final int DEFAULT_MAX_SIZE = 500;
    private final HazardFunction hazardFunction;
    private final StoredList<Double> growthProbabilities;
    private final StoredValue<Integer> t;
    private final StoredValue<Integer> t0;
    private final StoredList<Double> alphasList;
    private final StoredList<Double> betasList;
    private final StoredList<Double> kappasList;
    private final StoredList<Double> musList;
    private final double lambda;
    private final int maxSize;

    public OnlineChangeDetector(RuntimeContext context) {
        growthProbabilities =
                new ManagedListState<>(context, "growth-probs", Double.class, DEFAULT_GROWTH_PROB);
        alphasList = new ManagedListState<>(context, "alphas", Double.class, DEFAULT_ALPHA);
        betasList = new ManagedListState<>(context, "betas", Double.class, DEFAULT_BETA);
        kappasList = new ManagedListState<>(context, "kappas", Double.class, DEFAULT_KAPPA);
        musList = new ManagedListState<>(context, "mus", Double.class, DEFAULT_MU);
        t0 = new ManagedValueState<>(context, "t0", Integer.class, 0);
        t = new ManagedValueState<>(context, "t0", Integer.class, -1);
        lambda = DEFAULT_LAMBDA;
        hazardFunction = HazardFunction.ConstantHazardFunction;
        maxSize = DEFAULT_MAX_SIZE;
    }

    public OnlineChangeDetector(
            double alpha,
            double lambda,
            double growthProb,
            double beta,
            double kappa,
            double mu,
            int maxSize) {
        this.lambda = lambda;
        growthProbabilities = new InMemListState<>(Collections.singletonList(growthProb));
        alphasList = new InMemListState<>(Collections.singletonList(alpha));
        betasList = new InMemListState<>(Collections.singletonList(beta));
        musList = new InMemListState<>(Collections.singletonList(mu));
        kappasList = new InMemListState<>(Collections.singletonList(kappa));
        hazardFunction = HazardFunction.ConstantHazardFunction;
        t0 = new InMemValueState<>(0);
        t = new InMemValueState<>(-1);
        this.maxSize = maxSize;
    }

    public int getCurrentT() throws IOException {
        return t.getValue();
    }

    public double growthProbability(int delay) throws Exception {
        List<Double> probs = growthProbabilities.getValues();
        return probs.size() > delay + 1 ? growthProbabilities.getValues().get(delay) : 0;
    }

    public List<Double> getGrowthProbabilities() throws Exception {
        return growthProbabilities.getValues();
    }

    private double changePointProbability(
            List<Double> growthProbs, List<Double> predictiveProbs, List<Double> hazardProbs) {
        return IntStream.range(0, predictiveProbs.size())
                .mapToDouble(i -> growthProbs.get(i) * predictiveProbs.get(i) * hazardProbs.get(i))
                .sum();
    }

    private List<Double> getNewGrowthProbs(
            List<Double> growthProbs, List<Double> predictiveProbs, List<Double> hazardProbs) {
    /*
        Evaluate  the  growth  probabilities  -  shift  the  probabilities  down  and  to
         the  right,  scaled  by  the  hazard  function  and  the  predictive
         probabilities.

    */
        double changePointProb = changePointProbability(growthProbs, predictiveProbs, hazardProbs);
        return IntStream.range(0, predictiveProbs.size() + 1)
                .mapToObj(
                        i ->
                                i == 0
                                        ? changePointProb
                                        : growthProbs.get(i - 1)
                                        * predictiveProbs.get(i - 1)
                                        * (1 - hazardProbs.get(i - 1)))
                .collect(CollectorsUtil.toList(predictiveProbs.size() + 1));
    }

    public void normalizeProbs(List<Double> probs) {
        double sum = probs.stream().mapToDouble(i -> i).sum();
        IntStream.range(0, probs.size()).forEach(i -> probs.set(i, probs.get(i) / sum));
    }

    public void update(double data) throws Exception {

        int currentT = t.getValue() + 1;
        t.update(currentT);
        int currentT0 = t0.getValue();
        currentT -= currentT0;
        List<Double> currentgrowthProbabilities = growthProbabilities.getValues();
        List<Double> predictiveProbabilities = pdf(data);
        List<Double> hazardProbabilities = hazardFunction.apply(lambda, currentT + 1);
        List<Double> newGrowothProbs =
                getNewGrowthProbs(currentgrowthProbabilities, predictiveProbabilities, hazardProbabilities);
        normalizeProbs(newGrowothProbs);
        growthProbabilities.update(newGrowothProbs);
        updateBayesianParams(data);
    }

    public void updateBayesianParams(double data) throws Exception {
        List<Double> mus = musList.getValues();
        List<Double> kappas = kappasList.getValues();
        List<Double> alphas = alphasList.getValues();
        List<Double> betas = betasList.getValues();

        List<Double> nextMuList =
                IntStream.range(0, musList.getValues().size() + 1)
                        .mapToDouble(
                                i ->
                                        i == 0
                                                ? mus.get(0)
                                                : (kappas.get(i - 1) * mus.get(i - 1) + data) / (kappas.get(i - 1) + 1))
                        .boxed()
                        .collect(CollectorsUtil.toList(musList.getValues().size() + 1));
        musList.update(nextMuList);
        List<Double> nextKappaList =
                IntStream.range(0, kappasList.getValues().size() + 1)
                        .mapToObj(i -> i == 0 ? kappas.get(0) : kappas.get(i - 1) + 1)
                        .collect(CollectorsUtil.toList(kappasList.getValues().size() + 1));
        kappasList.update(nextKappaList);
        List<Double> nextAlphaList =
                IntStream.range(0, alphasList.getValues().size() + 1)
                        .mapToObj(i -> i == 0 ? alphas.get(0) : alphas.get(i - 1) + 0.5f)
                        .collect(CollectorsUtil.toList(alphasList.getValues().size() + 1));
        alphasList.update(nextAlphaList);
        List<Double> nextBetaList =
                IntStream.range(0, betasList.getValues().size() + 1)
                        .mapToObj(
                                i ->
                                        i == 0
                                                ? betas.get(0)
                                                : betas.get(i - 1)
                                                + (kappas.get(i - 1) * (float) Math.pow(data - mus.get(i - 1), 2))
                                                / (2 * (kappas.get(i - 1) + 1)))
                        .collect(CollectorsUtil.toList(betasList.getValues().size() + 1));
        betasList.update(nextBetaList);
    }

    public List<Double> pdf(double data) throws Exception {
        List<Double> alphas = alphasList.getValues();
        List<Double> betas = betasList.getValues();
        List<Double> kappas = kappasList.getValues();
        List<Double> mus = musList.getValues();
        return IntStream.range(0, alphas.size())
                .mapToDouble(
                        i ->
                                StudentTDistribution.builder()
                                        .scale(betas.get(i) * (kappas.get(i) + 1) / (alphas.get(i) * kappas.get(i)))
                                        .degreeOfFreedom(alphas.get(i) * 2)
                                        .loc(mus.get(i))
                                        .build()
                                        .pdf(data))
                .boxed()
                .collect(CollectorsUtil.toList(alphas.size() + 1));
    }

    public void prune(int newT0) throws Exception {
        t0.update(newT0);
        int paramsLastIndex = t.getValue() - newT0 + 1;
        musList.pruneAfter(paramsLastIndex);
        kappasList.pruneAfter(paramsLastIndex);
        alphasList.pruneAfter(paramsLastIndex);
        betasList.pruneAfter(paramsLastIndex);
        growthProbabilities.pruneAfter(paramsLastIndex);
    }

    public void trim() throws Exception {
        if (getCurrentT() - t0.getValue() > maxSize + 1) {
            prune(getCurrentT() - maxSize / 2);
        }
    }
}
