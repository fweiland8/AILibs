package jaicore.ml.core.dataset.sampling.inmemory;

import java.util.Comparator;
import java.util.Random;

import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.basic.algorithm.exceptions.AlgorithmException;
import jaicore.ml.core.dataset.IDataset;
import jaicore.ml.core.dataset.IInstance;
import jaicore.ml.core.dataset.sampling.SampleElementAddedEvent;

/**
 * Implementation of Systematic Sampling: Sort datapoints and pick every k-th
 * datapoint for the sample.
 * 
 * @author Lukas Brandt
 */
public class SystematicSampling<I extends IInstance> extends ASamplingAlgorithm<I> {

	private Random random;
	private IDataset<I> sortedDataset = null;
	private int k;
	private int startIndex;
	private int index;

	// Default Comparator to sort datapoints by their vector representation.
	private Comparator<I> datapointComparator = (o1, o2) -> {
		double[] v1 = o1.getPoint();
		double[] v2 = o2.getPoint();
		for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
			int c = Double.compare(v1[i], v2[i]);
			if (c != 0) {
				return c;
			}
		}
		return 0;
	};

	/**
	 * Simple constructor that uses the default datapoint comparator.
	 * 
	 * @param random
	 *            Random Object for determining the sampling start point.
	 */
	public SystematicSampling(Random random, IDataset<I> input) {
		super(input);
		this.random = random;
	}

	/**
	 * Constructor for a custom datapoint comparator.
	 * 
	 * @param random
	 *            Random Object for determining the sampling start point.
	 * @param datapointComparator
	 *            Comparator to sort the dataset.
	 */
	public SystematicSampling(Random random, Comparator<I> datapointComparator, IDataset<I> input) {
		super(input);
		this.random = random;
		this.datapointComparator = datapointComparator;
	}

	@Override
	public AlgorithmEvent nextWithException() throws AlgorithmException {
		switch (this.getState()) {
		case created:
			// Initialize variables and sort dataset.
			this.sample = this.getInput().createEmpty();
			if (this.sortedDataset == null) {
				this.sortedDataset = this.getInput().createEmpty();
				this.sortedDataset.addAll(this.getInput());
				this.sortedDataset.sort(this.datapointComparator);
			}
			this.startIndex = this.random.nextInt(this.sortedDataset.size());
			this.k = this.sortedDataset.size() / this.sampleSize;
			this.index = 0;
			return this.activate();
		case active:
			// If the sample size is not reached yet, add the next datapoint from the
			// systematic sampling method.
			if (this.sample.size() < this.sampleSize) {
				int e = (startIndex + (this.index++) * k) % this.sortedDataset.size();
				this.sample.add(this.sortedDataset.get(e));
				return new SampleElementAddedEvent(getId());
			} else {
				return this.terminate();
			}
		case inactive:
			this.doInactiveStep();
		default:
			throw new IllegalStateException("Unknown algorithm state " + this.getState());
		}
	}

	public IDataset<I> getSortedDataset() {
		return sortedDataset;
	}

	public void setSortedDataset(IDataset<I> sortedDataset) {
		this.sortedDataset = sortedDataset;
	}

}
