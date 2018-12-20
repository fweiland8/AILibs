package jaicore.ml.core.dataset.sampling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import jaicore.basic.algorithm.AlgorithmEvent;
import jaicore.basic.algorithm.AlgorithmFinishedEvent;
import jaicore.basic.algorithm.AlgorithmInitializedEvent;
import jaicore.basic.algorithm.AlgorithmState;
import jaicore.basic.sets.SetUtil.Pair;
import jaicore.ml.core.dataset.IDataset;
import jaicore.ml.core.dataset.IInstance;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

public class OSMAC <I extends IInstance> extends PilotEstimateSampling <I> {
	
	public OSMAC(Random rand, int preSampleSize) {
		this.rand = rand;
		this.preSampleSize = preSampleSize;
	}
	
	protected ArrayList<Pair<I, Double>> calculateFinalInstanceBoundaries(Instances instances, Classifier pilotEstimator) throws Exception {
		double boundaryOfCurrentInstance = 0.0;
		ArrayList<Pair<I, Double>> probabilityBoundaries = new ArrayList<Pair<I, Double>>();
		ArrayList<Pair<Instance, Double>> instanceProbabilityBoundaries = new ArrayList<Pair<Instance, Double>>();
		double sumOfDistributionLosses = 0;
		int vectorLength;
		for(Instance instance: instances) {
			vectorLength = 0;
			for(double dimensionLength: instance.toDoubleArray()) {
				vectorLength += dimensionLength;
			}
			sumOfDistributionLosses += (1 - pilotEstimator.distributionForInstance(instance)[instance.classIndex()]) //TODO need Class
					* vectorLength;
		}
		for(Instance instance: instances) {
			vectorLength = 0;
			for(double dimensionLength: instance.toDoubleArray()) {
				vectorLength += dimensionLength;
			}
			boundaryOfCurrentInstance += (1 - pilotEstimator.distributionForInstance(instance)[instance.classIndex()]) //TODO need Class
					 * vectorLength / sumOfDistributionLosses;
			instanceProbabilityBoundaries.add(new Pair<Instance, Double>(instance, new Double(boundaryOfCurrentInstance)));
		}
		IDataset<IInstance> dataset = WekaInstancesUtil.wekaInstancesToDataset(instances);
		int iterator = 0;
		for(IInstance instance: dataset) {
			probabilityBoundaries.add(new Pair<I, Double>((I) instance, instanceProbabilityBoundaries.get(iterator).getY()));
		}
		return probabilityBoundaries;
	}
}
