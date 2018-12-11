package jaicore.ml.core.dataset.sampling.stratified.sampling;

import java.util.Random;

import jaicore.basic.algorithm.AlgorithmEvent;
import jaicore.basic.algorithm.AlgorithmFinishedEvent;
import jaicore.basic.algorithm.AlgorithmInitializedEvent;
import jaicore.basic.algorithm.AlgorithmState;
import jaicore.ml.core.dataset.IDataset;
import jaicore.ml.core.dataset.IInstance;
import jaicore.ml.core.dataset.sampling.ASamplingAlgorithm;
import jaicore.ml.core.dataset.sampling.SampleElementAddedEvent;
import jaicore.ml.core.dataset.sampling.SimpleRandomSampling;

/**
 * Implementation of Stratified Sampling: Divide dataset into strati and sample from each of these.
 * 
 * @author Lukas Brandt
 */
public class StratifiedSampling extends ASamplingAlgorithm{

	private IStratiAmountSelector stratiAmountSelector;
	private IStratiAssigner stratiAssigner;
	private Random random;
	private IDataset[] strati;
	private int stratiIndex;
	private IDataset datasetCopy;
	
	/**
	 * Constructor for Stratified Sampling.
	 * @param stratiAmountSelector The custom selector for the used amount of strati.
	 * @param stratiAssigner Custom logic to assign datapoints into strati.
	 * @param random Random object for sampling inside of the strati.
	 */
	public StratifiedSampling(IStratiAmountSelector stratiAmountSelector, IStratiAssigner stratiAssigner, Random random) {
		this.stratiAmountSelector = stratiAmountSelector;
		this.stratiAssigner = stratiAssigner;
		this.random = random;
		// TODO: create empty dataset
		this.datasetCopy = null;
		this.datasetCopy.addAll(this.getInput());
	}
	
	@Override
	public AlgorithmEvent nextWithException() throws Exception {
		switch (this.getState()) {
		case created:
			// TODO: create empty dataset
			this.sample = null;
			this.strati = new IDataset[this.stratiAmountSelector.selectStratiAmount(this.datasetCopy)];
			// TODO: create emtpy strati dataset
			for (int i = 0; i < this.strati.length; i++) {
				this.strati[i] = null;
			}
			this.stratiAssigner.init(this.datasetCopy, this.strati.length);
			this.stratiIndex = 0;
			this.setState(AlgorithmState.active);
			return new AlgorithmInitializedEvent();			
		case active:
			if (this.sample.size() < this.sampleSize) {
				if (this.datasetCopy.size() >= 1) {
					// Stratify the datapoints one by one.
					IInstance datapoint = this.datasetCopy.remove(0);
					int assignedStrati = this.stratiAssigner.assignToStrati(datapoint);
					if (assignedStrati < 0 || assignedStrati >= this.strati.length) {
						throw new Exception("No existing strati for index " + assignedStrati);
					} else {
						this.strati[assignedStrati].add(datapoint);
					}
					return new SampleElementAddedEvent();
				} else {
					// Sample from the srati one by one.					
					if (this.stratiIndex >= this.strati.length) {
						// All strati were used
						this.setState(AlgorithmState.inactive);
						return new AlgorithmFinishedEvent();
					} else {
						// Sample from the stratum with the current index
						int sizeOfStratiSample = (int)(this.sampleSize * ((double)this.strati[stratiIndex].size() / (double)this.getInput().size()));
						SimpleRandomSampling simpleRandomSampling = new SimpleRandomSampling(random);
						simpleRandomSampling.setInput(this.strati[this.stratiIndex]);
						simpleRandomSampling.setSampleSize(sizeOfStratiSample);
						this.sample.addAll(simpleRandomSampling.call());
						this.stratiIndex++;
						return new SampleElementAddedEvent();
					}
				}
			} else {
				this.setState(AlgorithmState.inactive);
				return new AlgorithmFinishedEvent();
			}
		case inactive: {
			if (this.sample.size() < this.sampleSize) {
				throw new Exception("Expected sample size was not reached before termination");
			} else {
				return new AlgorithmFinishedEvent();
			}
		}
		default:
			throw new IllegalStateException("Unknown algorithm state "+ this.getState());
		}
	}

}
