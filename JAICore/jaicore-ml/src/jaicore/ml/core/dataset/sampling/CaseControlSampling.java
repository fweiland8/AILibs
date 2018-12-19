package jaicore.ml.core.dataset.sampling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import jaicore.ml.core.dataset.*;

import jaicore.basic.algorithm.AlgorithmEvent;
import jaicore.basic.algorithm.AlgorithmFinishedEvent;
import jaicore.basic.algorithm.AlgorithmInitializedEvent;
import jaicore.basic.algorithm.AlgorithmState;
import jaicore.basic.sets.SetUtil.Pair;

/**
 * Case control sampling. Might be used as sampling algorithm or as subroutine for Local Case Control Sampling
 * 
 * @author Nino Schnitker
 *
 */
public class CaseControlSampling extends CaseControlLikeSampling {
	
	/**
	 * Constructor
	 * @param rand RandomObject for reproducibility
	 */
	public CaseControlSampling(Random rand) {
		this.rand = rand;
	}
	
	@Override
	public AlgorithmEvent nextWithException() throws Exception {
		switch(this.getState()) {
		case created:
			this.sample = this.createEmptyDatasetFromInputSchema();
			
			HashMap<Object, Integer> classOccurrences = countClassOccurrences(this.getInput());
			
			// Count number of classes
			int numberOfClasses = classOccurrences.keySet().size();
			
			// Calculate Boundaries that define which Instances is choose for which random number
			probabilityBoundaries = calculateInstanceBoundaries(classOccurrences, numberOfClasses);
			this.setState(AlgorithmState.active);
			return new AlgorithmInitializedEvent();
		case active:
			if(this.sample.size() < this.sampleSize) {
				double r = this.rand.nextDouble();
				IInstance choosenInstance = null;
				for(int i = 0; i < probabilityBoundaries.size(); i++) {
					if(probabilityBoundaries.get(i).getY().doubleValue() > r) {
						choosenInstance = probabilityBoundaries.get(i).getX();
					}
				}
				if(choosenInstance == null) {
					choosenInstance = probabilityBoundaries.get(probabilityBoundaries.size() - 1).getX();
				}
				this.sample.add(choosenInstance);
				return new SampleElementAddedEvent();
			}
			else {
				this.setState(AlgorithmState.inactive);
				return new AlgorithmFinishedEvent();
			}
		case inactive:
			if (this.sample.size() < this.sampleSize) {
				throw new RuntimeException("Expected sample size was not reached before termination");
			} else {
				return new AlgorithmFinishedEvent();
			}
		default:
			throw new IllegalStateException("Unknown algorithm state "+ this.getState());	
		}	
	}	
}
