package jaicore.ml.learningcurve.extrapolation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import jaicore.basic.algorithm.exceptions.AlgorithmException;
import jaicore.ml.core.dataset.IDataset;
import jaicore.ml.core.dataset.IInstance;
import jaicore.ml.core.dataset.sampling.inmemory.ASamplingAlgorithm;
import jaicore.ml.core.dataset.sampling.inmemory.WekaInstancesUtil;
import jaicore.ml.core.dataset.sampling.inmemory.factories.interfaces.IRerunnableSamplingAlgorithmFactory;
import jaicore.ml.core.dataset.sampling.inmemory.factories.interfaces.ISamplingAlgorithmFactory;
import jaicore.ml.interfaces.LearningCurve;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.UnsupportedAttributeTypeException;

/**
 * Abstract class for implementing a learning curve extrapolation method with
 * some anchor points. For each of this anchorpoints a subsample will be drawn
 * and a classifier will be trained with this sample. Based on the points
 * (subsample size, learner accuracy) a custom method of learning curve
 * extrapolation can be applied.
 * 
 * @author Lukas Brandt
 */
public class LearningCurveExtrapolator {

	protected Classifier learner;
	protected IDataset<IInstance> dataset;
	protected IDataset<IInstance> train;
	protected IDataset<IInstance> test;
	protected ISamplingAlgorithmFactory<IInstance, ? extends ASamplingAlgorithm<IInstance>> samplingAlgorithmFactory;
	protected ASamplingAlgorithm<IInstance> samplingAlgorithm;
	protected Random random;
	protected LearningCurveExtrapolationMethod extrapolationMethod;

	/**
	 * Create a learning curve extrapolator with a subsampling factory.
	 * 
	 * @param extrapolationMethod
	 *            Method for extrapolating a learning curve from anchorpoints.
	 * @param learner
	 *            Learning model to predict the learning curve of.
	 * @param dataset
	 *            Dataset to measure evaluate the learner on.
	 * @param trainsplit
	 *            Portion of the dataset, which shall be used to sample from for
	 *            training.
	 * @param samplingAlgorithmFactory
	 *            Subsampling algorithm factory to create a configured subsampler
	 *            with.
	 * @param seed
	 *            Random seed.
	 */
	public LearningCurveExtrapolator(LearningCurveExtrapolationMethod extrapolationMethod, Classifier learner,
			IDataset<IInstance> dataset, double trainsplit,
			ISamplingAlgorithmFactory<IInstance, ? extends ASamplingAlgorithm<IInstance>> samplingAlgorithmFactory,
			long seed) {
		this.extrapolationMethod = extrapolationMethod;
		this.learner = learner;
		this.dataset = dataset;
		this.samplingAlgorithmFactory = samplingAlgorithmFactory;
		this.samplingAlgorithm = null;
		this.random = new Random(seed);
		this.createSplit(trainsplit, seed);
	}

	/**
	 * Measure the learner accuracy at the given anchorpoints and extrapolate a
	 * learning curve based the results.
	 * 
	 * @param anchorPoints
	 *            Sample sizes as anchorpoints, where the accuracy shall be
	 *            measured.
	 * @return The extrapolated learning curve.
	 * 
	 * @throws InvalidAnchorPointsException
	 *             The anchorpoints (amount, values, ...) are not suitable for the
	 *             given learning curve extrapolation method.
	 * @throws AlgorithmException
	 *             An error occured during the creation of the specified
	 *             anchorpoints.
	 * @throws InterruptedException
	 */
	public LearningCurve extrapolateLearningCurve(int[] anchorPoints)
			throws InvalidAnchorPointsException, AlgorithmException, InterruptedException {
		double[] yValues = new double[anchorPoints.length];
		try {
			Instances testInstances = WekaInstancesUtil.datasetToWekaInstances(this.test);

			// Create subsamples at the anchorpoints and measure the accuracy there.
			for (int i = 0; i < anchorPoints.length; i++) {
				// If it is a rerunnable factory, set the previous run.
				if (this.samplingAlgorithmFactory instanceof IRerunnableSamplingAlgorithmFactory
						&& this.samplingAlgorithm != null) {
					((IRerunnableSamplingAlgorithmFactory<IInstance, ASamplingAlgorithm<IInstance>>) this.samplingAlgorithmFactory)
							.setPreviousRun(this.samplingAlgorithm);
				}
				this.samplingAlgorithm = this.samplingAlgorithmFactory.getAlgorithm(anchorPoints[i], this.train,
						this.random);
				IDataset<IInstance> subsampledDataset = this.samplingAlgorithm.call();

				// Train classifier on subsample.
				this.learner.buildClassifier(WekaInstancesUtil.datasetToWekaInstances(subsampledDataset));

				// Measure accuracy of the trained learner on test split.
				double correctCounter = 0d;
				for (Instance instance : testInstances) {
					if (this.learner.classifyInstance(instance) == instance.classValue()) {
						correctCounter++;
					}
				}
				yValues[i] = correctCounter / (double) testInstances.size();

			}

			return extrapolationMethod.extrapolateLearningCurveFromAnchorPoints(anchorPoints, yValues,
					this.dataset.size());
		} catch (UnsupportedAttributeTypeException e) {
			throw new AlgorithmException(e, "Error during convertion of the dataset to WEKA instances");
		} catch (AlgorithmExecutionCanceledException | TimeoutException | AlgorithmException e) {
			throw new AlgorithmException(e, "Error during creation of the subsamples for the anchorpoints");
		} catch (ExecutionException e) {
			throw new AlgorithmException(e, "Error during learning curve extrapolation");
		} catch (Exception e) {
			throw new AlgorithmException(e, "Error during training/testing the classifier");
		}

	}

	private void createSplit(double trainsplit, long seed) {
		this.train = this.dataset.createEmpty();
		this.test = this.dataset.createEmpty();
		IDataset<IInstance> data = this.dataset.createEmpty();
		data.addAll(this.dataset);

		// Shuffle the data
		Random r = new Random(seed);
		Collections.shuffle(data, r);

		// Stratify the data by class
		Map<Object, IDataset<IInstance>> classStrati = new HashMap<>();
		this.dataset.forEach(d -> {
			Object c = d.getTargetValue(Object.class).getValue();
			if (!classStrati.containsKey(c)) {
				classStrati.put(c, this.dataset.createEmpty());
			}
			classStrati.get(c).add(d);
		});

		// Retrieve strati sizes
		Map<Object, Integer> classStratiSizes = new HashMap<>(classStrati.size());
		for (Entry<Object, IDataset<IInstance>> entry : classStrati.entrySet()) {
			classStratiSizes.put(entry.getKey(), classStrati.get(entry.getKey()).size());
		}

		// First assign one item of each class to train and test
		for (Entry<Object, IDataset<IInstance>> entry : classStrati.entrySet()) {
			IDataset<IInstance> availableInstances = classStrati.get(entry.getKey());
			if (!availableInstances.isEmpty()) {
				train.add(availableInstances.get(0));
				availableInstances.remove(0);
			}
			if (!availableInstances.isEmpty()) {
				test.add(availableInstances.get(0));
				availableInstances.remove(0);
			}
		}

		// Distribute remaining instances over train test
		for (Entry<Object, IDataset<IInstance>> entry : classStrati.entrySet()) {
			IDataset<IInstance> availableInstances = classStrati.get(entry.getKey());
			int trainItems = (int) Math.min(availableInstances.size(),
					Math.ceil(trainsplit * classStratiSizes.get(entry.getKey())));
			for (int j = 0; j < trainItems; j++) {
				this.train.add(availableInstances.get(0));
				availableInstances.remove(0);
			}
			int testItems = (int) Math.min(availableInstances.size(),
					Math.ceil((1 - trainsplit) * classStratiSizes.get(entry.getKey())));
			for (int j = 0; j < testItems; j++) {
				this.test.add(availableInstances.get(0));
				availableInstances.remove(0);
			}
		}

		// Shuffle train and test
		Collections.shuffle(this.train, r);
		Collections.shuffle(this.test, r);
	}

}
