package jaicore.ml.core.dataset.sampling;

import java.util.Random;

import jaicore.basic.algorithm.AlgorithmProblemTransformer;
import jaicore.basic.algorithm.IAlgorithm;
import jaicore.basic.algorithm.IAlgorithmFactory;
import jaicore.ml.core.dataset.IDataset;
import jaicore.ml.core.dataset.IInstance;

public class SimpleRandomSamplingTester<I extends IInstance> extends GeneralSamplingTester<I> {

	private static final long RANDOM_SEED = 1;

	private static final double DEFAULT_SAMPLE_FRACTION = 0.1;

	@Override
	public IAlgorithmFactory<IDataset<I>, IDataset<I>> getFactory() {
		return new IAlgorithmFactory<IDataset<I>, IDataset<I>>() {

			private IDataset<I> input;

			@Override
			public void setProblemInput(IDataset<I> problemInput) {
				this.input = problemInput;
			}

			@Override
			public <P> void setProblemInput(P problemInput, AlgorithmProblemTransformer<P, IDataset<I>> reducer) {
				throw new UnsupportedOperationException("Problem input not applicable for subsampling algorithms!");
			}

			@Override
			public IAlgorithm<IDataset<I>, IDataset<I>> getAlgorithm() {
				Random r = new Random(RANDOM_SEED);
				ASamplingAlgorithm<I> algorithm = new SimpleRandomSampling<I>(r);
				if (this.input != null) {
					algorithm.setInput(input);
					algorithm.setSampleSize((int) DEFAULT_SAMPLE_FRACTION * input.size());
				}
				return algorithm;
			}
		};
	}

}
