package jaicore.ml.core.dataset.sampling.inmemory.casecontrol;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jaicore.basic.algorithm.events.AlgorithmEvent;
import jaicore.basic.algorithm.exceptions.AlgorithmException;
import jaicore.basic.sets.SetUtil.Pair;
import jaicore.ml.core.dataset.IDataset;
import jaicore.ml.core.dataset.IInstance;
import jaicore.ml.core.dataset.sampling.SampleElementAddedEvent;
import jaicore.ml.core.dataset.sampling.inmemory.WekaInstancesUtil;
import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.UnsupportedAttributeTypeException;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;

public abstract class PilotEstimateSampling<I extends IInstance> extends CaseControlLikeSampling<I> {

	private Logger logger = LoggerFactory.getLogger(PilotEstimateSampling.class);

	protected int preSampleSize;
	private I chosenInstance = null;

	protected PilotEstimateSampling(IDataset<I> input) {
		super(input);
	}

	public I getChosenInstance() {
		return chosenInstance;
	}

	public void setChosenInstance(I chosenInstance) {
		this.chosenInstance = chosenInstance;
	}

	@Override
	public AlgorithmEvent nextWithException() throws AlgorithmException {
		switch (this.getState()) {
		case created:
			this.doInitStep();
		case active:
			if (this.sample.size() < this.sampleSize) {
				do {
					double r = this.rand.nextDouble();
					chosenInstance = null;
					for (int i = 0; i < probabilityBoundaries.size(); i++) {
						if (probabilityBoundaries.get(i).getY().doubleValue() > r) {
							chosenInstance = probabilityBoundaries.get(i).getX();
							break;
						}
					}
					if (chosenInstance == null) {
						chosenInstance = probabilityBoundaries.get(probabilityBoundaries.size() - 1).getX();
					}
				} while (this.sample.contains(chosenInstance));
				this.sample.add(chosenInstance);
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

	private AlgorithmEvent doInitStep() {
		this.sample = this.getInput().createEmpty();
		if (probabilityBoundaries == null || chosenInstance == null) {
			Classifier pilotEstimator = new Logistic();
			// set preSampleSize to |Dataset|/2 as default value, if preSampleSize would be
			// smaller than 1
			if (this.preSampleSize < 1) {
				this.preSampleSize = this.getInput().size() / 2;
			}
			IDataset<I> pilotEstimateSample = this.getInput().createEmpty();
			IDataset<I> sampleCopy = this.getInput().createEmpty();

			for (I instance : this.getInput()) {
				sampleCopy.add(instance);
			}

			HashMap<Object, Integer> classOccurrences = countClassOccurrences(sampleCopy);

			// Count number of classes
			int numberOfClasses = classOccurrences.keySet().size();

			// Calculate Boundaries that define which Instances is choose for which random
			// number
			probabilityBoundaries = calculateInstanceBoundaries(classOccurrences, numberOfClasses);

			double r;
			I choosenInstance;
			for (int i = 0; i < this.preSampleSize; i++) {
				do {
					r = this.rand.nextDouble();
					choosenInstance = null;
					for (int j = 0; j < probabilityBoundaries.size(); j++) {
						if (probabilityBoundaries.get(j).getY().doubleValue() > r) {
							choosenInstance = probabilityBoundaries.get(j).getX();
							break;
						}
					}
					if (choosenInstance == null) {
						choosenInstance = probabilityBoundaries.get(probabilityBoundaries.size() - 1).getX();
					}
				} while (pilotEstimateSample.contains(choosenInstance));
				pilotEstimateSample.add(choosenInstance);
			}
			Instances pilotEstimateInstances = null;
			try {
				pilotEstimateInstances = WekaInstancesUtil.datasetToWekaInstances(pilotEstimateSample);
			} catch (UnsupportedAttributeTypeException e) {
				logger.error("Error while converting dataset to WEKA dataset", e);
				this.terminate();
			}

			NumericToNominal numericToNominal = new NumericToNominal();
			String[] options = new String[2];
			options[0] = "-R";
			options[1] = "last";
			try {
				numericToNominal.setOptions(options);
				numericToNominal.setInputFormat(pilotEstimateInstances);
			} catch (Exception e) {
				logger.error("Unexpected error", e);
				this.terminate();
			}

			try {
				pilotEstimateInstances = Filter.useFilter(pilotEstimateInstances, numericToNominal);
			} catch (Exception e) {
				logger.error("Cannot apply filter", e);
				this.terminate();
			}

			ArrayList<Pair<Double, Double>> classMapping = new ArrayList<>();
			boolean classNotInMapping;
			for (Instance in : pilotEstimateInstances) {
				classNotInMapping = true;
				for (Pair<Double, Double> classPair : classMapping) {
					if (in.classValue() == classPair.getX().doubleValue()) {
						classNotInMapping = false;
					}
				}
				if (classNotInMapping) {
					classMapping.add(new Pair<Double, Double>(in.classValue(), (double) classMapping.size()));
				}
			}

			try {
				pilotEstimator.buildClassifier(pilotEstimateInstances);
			} catch (Exception e) {
				logger.error("Cannot build classifier", e);
				this.terminate();
			}

			try {
				probabilityBoundaries = calculateFinalInstanceBoundaries(
						WekaInstancesUtil.datasetToWekaInstances(sampleCopy), pilotEstimator);
			} catch (UnsupportedAttributeTypeException e) {
				logger.error("Error while converting dataset to WEKA dataset", e);
				this.terminate();
			}
		}
		return this.activate();
	}

	abstract ArrayList<Pair<I, Double>> calculateFinalInstanceBoundaries(Instances instances,
			Classifier pilotEstimator);
}
