package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
public class SamplingConfig extends AbstractConfig {
    /** Probability in the range from 0.0 to 1.0 that a trace will be sampled */
    private Float probability = DefaultConfig.DEFAULT_SAMPLING_PROBABILITY;

    public SamplingConfig probability(Float probability) {
        this.probability = probability;
        return this;
    }

    //setter && getter
    public Float getProbability() {
        return probability;
    }

    public void setProbability(Float probability) {
        this.probability = probability;
    }

    @Override
    public String toString() {
        return "{" +
                "probability=" + probability +
                "}";
    }
}
