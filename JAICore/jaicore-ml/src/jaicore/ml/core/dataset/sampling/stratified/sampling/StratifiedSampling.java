package jaicore.ml.core.dataset.sampling.stratified.sampling;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.basic.algorithm.exceptions.AlgorithmException;
import jaicore.ml.core.dataset.IDataset;
import jaicore.ml.core.dataset.IInstance;
import jaicore.ml.core.dataset.sampling.ASamplingAlgorithm;
import jaicore.ml.core.dataset.sampling.SampleElementAddedEvent;
import jaicore.ml.core.dataset.sampling.SimpleRandomSampling;
import jaicore.ml.core.dataset.sampling.WaitForSamplingStepEvent;

/**
 * Implementation of Stratified Sampling: Divide dataset into strati and sample
 * from each of these.
 * 
 * @author Lukas Brandt
 */
public class StratifiedSampling<I extends IInstance> extends ASamplingAlgorithm<I> {

	private IStratiAmountSelector<I> stratiAmountSelector;
	private IStratiAssigner<I> stratiAssigner;
	private Random random;
	private IDataset<I>[] strati;
	private IDataset<I> datasetCopy;
	private ExecutorService executorService;
	private boolean simpleRandomSamplingStarted;

	/**
	 * Constructor for Stratified Sampling.
	 * 
	 * @param stratiAmountSelector
	 *            The custom selector for the used amount of strati.
	 * @param stratiAssigner
	 *            Custom logic to assign datapoints into strati.
	 * @param random
	 *            Random object for sampling inside of the strati.
	 */
	public StratifiedSampling(IStratiAmountSelector<I> stratiAmountSelector, IStratiAssigner<I> stratiAssigner,
			Random random) {
		this.stratiAmountSelector = stratiAmountSelector;
		this.stratiAssigner = stratiAssigner;
		this.random = random;
	}

	@Override
	public AlgorithmEvent nextWithException() throws InterruptedException, AlgorithmException {
		switch (this.getState()) {
		case created:
			this.sample = getInput().createEmpty();
			this.datasetCopy = getInput().createEmpty();
			this.datasetCopy.addAll(this.getInput());
			this.stratiAmountSelector.setNumCPUs(this.getNumCPUs());
			this.stratiAssigner.setNumCPUs(this.getNumCPUs());
			this.strati = new IDataset[this.stratiAmountSelector.selectStratiAmount(this.datasetCopy)];
			for (int i = 0; i < this.strati.length; i++) {
				this.strati[i] = getInput().createEmpty();
			}
			this.simpleRandomSamplingStarted = false;
			this.stratiAssigner.init(this.datasetCopy, this.strati.length);
			this.executorService = Executors.newCachedThreadPool();
			return this.activate();
		case active:
			if (this.sample.size() < this.sampleSize) {
				if (this.datasetCopy.size() >= 1) {
					// Stratify the datapoints one by one.
					I datapoint = this.datasetCopy.remove(0);
					int assignedStrati = this.stratiAssigner.assignToStrati(datapoint);
					if (assignedStrati < 0 || assignedStrati >= this.strati.length) {
						throw new AlgorithmException("No existing strati for index " + assignedStrati);
					} else {
						this.strati[assignedStrati].add(datapoint);
					}
					return new SampleElementAddedEvent();
				} else {
					if (!simpleRandomSamplingStarted) {
						// Simple Random Sampling has not started yet -> Initialize one sampling thread
						// per stratum.
						this.startSimpleRandomSamplingForStrati();
						this.simpleRandomSamplingStarted = true;
						return new WaitForSamplingStepEvent();
					} else {
						// Check if all threads are finished. If yes finish Stratified Sampling, wait
						// shortly in this step otherwise.
						if (this.executorService.isTerminated()) {
							return this.terminate();
						} else {
							synchronized (Thread.currentThread()) {
								Thread.currentThread().wait(100);
								return new WaitForSamplingStepEvent();
							}
						}
					}
				}
			} else {
				return this.terminate();
			}
		case inactive: {
			if (this.sample.size() < this.sampleSize) {
				throw new AlgorithmException("Expected sample size was not reached before termination");
			} else {
				return this.terminate();
			}
		}
		default:
			throw new IllegalStateException("Unknown algorithm state " + this.getState());
		}
	}

	/**
	 * Calculates the necessary sample sizes and start a Simple Random Sampling
	 * Thread for each stratum.
	 */
	private void startSimpleRandomSamplingForStrati() {
		// Calculate the amount of datapoints that will be used from each strati
		int[] sampleSizeForStrati = new int[this.strati.length];
		// Calculate for each stratum the sample size by StratiSize / DatasetSize
		for (int i = 0; i < this.strati.length; i++) {
			sampleSizeForStrati[i] = Math.round(
					(float) (this.sampleSize * ((double) this.strati[i].size() / (double) this.getInput().size())));
			System.out.println("Strati size: " + this.strati[i].size() + " sample amount " + sampleSizeForStrati[i]);
		}

		// Start a Simple Random Sampling thread for each stratum
		for (int i = 0; i < this.strati.length; i++) {
			int index = i;
			this.executorService.execute(new Runnable() {
				@Override
				public void run() {
					SimpleRandomSampling<I> simpleRandomSampling = new SimpleRandomSampling<I>(random);
					simpleRandomSampling.setInput(strati[index]);
					simpleRandomSampling.setSampleSize(sampleSizeForStrati[index]);
					try {
						synchronized (sample) {
							sample.addAll(simpleRandomSampling.call());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			});
		}
		// Prevent executor service from more threads being added.
		this.executorService.shutdown();
	}

}