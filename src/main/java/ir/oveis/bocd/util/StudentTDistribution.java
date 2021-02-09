package ir.oveis.bocd.util;

import org.apache.commons.math3.distribution.TDistribution;

public class StudentTDistribution {
    private double degreeOfFreedom;
    private double scale;
    private double loc;

    public static StudentTDistributionBuilder builder() {
        return new StudentTDistributionBuilder();
    }

    public double pdf(double data) {
        return new TDistribution(degreeOfFreedom).density((data - loc) / scale) / scale;
    }

    public static final class StudentTDistributionBuilder {
        private double degreeOfFreedom;
        private double scale;
        private double loc;

        private StudentTDistributionBuilder() {
        }

        public StudentTDistributionBuilder degreeOfFreedom(double degreeOfFreedom) {
            this.degreeOfFreedom = degreeOfFreedom;
            return this;
        }

        public StudentTDistributionBuilder scale(double scale) {
            this.scale = scale;
            return this;
        }

        public StudentTDistributionBuilder loc(double loc) {
            this.loc = loc;
            return this;
        }

        public StudentTDistribution build() {
            StudentTDistribution studentTDistribution = new StudentTDistribution();
            studentTDistribution.degreeOfFreedom = this.degreeOfFreedom;
            studentTDistribution.loc = this.loc;
            studentTDistribution.scale = this.scale;
            return studentTDistribution;
        }
    }
}
