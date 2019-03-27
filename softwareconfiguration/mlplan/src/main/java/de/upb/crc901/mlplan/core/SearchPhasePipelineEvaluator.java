package de.upb.crc901.mlplan.core;

import java.util.Arrays;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.upb.crc901.mlpipeline_evaluation.CacheEvaluatorMeasureBridge;
import de.upb.crc901.mlplan.multiclass.wekamlplan.ClassifierFactory;
import hasco.exceptions.ComponentInstantiationFailedException;
import hasco.model.ComponentInstance;
import jaicore.basic.ILoggingCustomizable;
import jaicore.basic.IObjectEvaluator;
<<<<<<< HEAD
import jaicore.basic.IInformedObjectEvaluatorExtension;
=======
import jaicore.basic.algorithm.exceptions.AlgorithmTimeoutedException;
>>>>>>> origin/master
import jaicore.basic.algorithm.exceptions.ObjectEvaluationFailedException;
import jaicore.concurrent.TimeoutTimer;
import jaicore.concurrent.TimeoutTimer.TimeoutSubmitter;
import jaicore.interrupt.Interrupter;
import jaicore.ml.evaluation.evaluators.weka.AbstractEvaluatorMeasureBridge;
import jaicore.ml.evaluation.evaluators.weka.ProbabilisticMonteCarloCrossValidationEvaluator;
import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 * Evaluator used in the search phase of mlplan. Uses MCCV by default, but can be configured to use other Benchmarks.
 * 
 * @author fmohr
 * @author jnowack
 */
public class SearchPhasePipelineEvaluator implements IObjectEvaluator<ComponentInstance, Double>, IInformedObjectEvaluatorExtension<Double>, ILoggingCustomizable {

	private Logger logger = LoggerFactory.getLogger(SearchPhasePipelineEvaluator.class);

	private final ClassifierFactory classifierFactory;
	private final AbstractEvaluatorMeasureBridge<Double, Double> evaluationMeasurementBridge;
	private final int seed;
	private final int numMCIterations;
	private final Instances dataShownToSearch;
	private final double trainFoldSize;
	private final IObjectEvaluator<Classifier, Double> searchBenchmark;
	private final int timeoutForSolutionEvaluation;

<<<<<<< HEAD
	private Double bestScore = 1.0;
	
	public SearchPhasePipelineEvaluator(ClassifierFactory classifierFactory, AbstractEvaluatorMeasureBridge<Double, Double> evaluationMeasurementBridge, int numMCIterations, Instances dataShownToSearch, double trainFoldSize, int seed,
			int timeoutForSolutionEvaluation) {
=======
	public SearchPhasePipelineEvaluator(final ClassifierFactory classifierFactory, final AbstractEvaluatorMeasureBridge<Double, Double> evaluationMeasurementBridge, final int numMCIterations, final Instances dataShownToSearch,
			final double trainFoldSize, final int seed, final int timeoutForSolutionEvaluation) {
>>>>>>> origin/master
		super();
		this.classifierFactory = classifierFactory;
		this.evaluationMeasurementBridge = evaluationMeasurementBridge;
		this.seed = seed;
		this.dataShownToSearch = dataShownToSearch;
		this.numMCIterations = numMCIterations;
		this.trainFoldSize = trainFoldSize;
		this.searchBenchmark = new ProbabilisticMonteCarloCrossValidationEvaluator(this.evaluationMeasurementBridge, numMCIterations, 0.0, dataShownToSearch, trainFoldSize, seed);
		this.timeoutForSolutionEvaluation = timeoutForSolutionEvaluation;
	}
	
	public SearchPhasePipelineEvaluator(ClassifierFactory classifierFactory, AbstractEvaluatorMeasureBridge<Double, Double> evaluationMeasurementBridge, int numMCIterations, Instances dataShownToSearch, double trainFoldSize, int seed,
			int timeoutForSolutionEvaluation, IObjectEvaluator<Classifier, Double> searchBenchmark) {
		super();
		this.classifierFactory = classifierFactory;
		this.evaluationMeasurementBridge = evaluationMeasurementBridge;
		this.seed = seed;
		this.dataShownToSearch = dataShownToSearch;
		this.numMCIterations = numMCIterations;
		this.trainFoldSize = trainFoldSize;
		this.searchBenchmark = searchBenchmark;
		this.timeoutForSolutionEvaluation = timeoutForSolutionEvaluation;
	}

	@Override
	public String getLoggerName() {
		return this.logger.getName();
	}

	@Override
	public void setLoggerName(final String name) {
		this.logger.info("Switching logger name from {} to {}", this.logger.getName(), name);
		this.logger = LoggerFactory.getLogger(name);
		if (this.searchBenchmark instanceof ILoggingCustomizable) {
			this.logger.info("Setting logger name of actual benchmark {} to {}.benchmark", this.searchBenchmark.getClass().getName(), name);
			((ILoggingCustomizable) this.searchBenchmark).setLoggerName(name + ".benchmark");
		} else {
			this.logger.info("Benchmark {} does not implement ILoggingCustomizable, not customizing its logger.", this.searchBenchmark.getClass().getName());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Double evaluate(final ComponentInstance c) throws AlgorithmTimeoutedException, InterruptedException, ObjectEvaluationFailedException {
		TimeoutSubmitter sub = TimeoutTimer.getInstance().getSubmitter();
		TimerTask task = sub.interruptMeAfterMS(this.timeoutForSolutionEvaluation, "Timeout for pipeline in search phase.");
		try {
			if (this.evaluationMeasurementBridge instanceof CacheEvaluatorMeasureBridge) {
				CacheEvaluatorMeasureBridge bridge = ((CacheEvaluatorMeasureBridge) this.evaluationMeasurementBridge).getShallowCopy(c);
<<<<<<< HEAD
				long seed = this.seed + c.hashCode();
				IObjectEvaluator<Classifier, Double> copiedSearchBenchmark = new ProbabilisticMonteCarloCrossValidationEvaluator(bridge, numMCIterations, bestScore, this.dataShownToSearch, trainFoldSize, seed);
				return copiedSearchBenchmark.evaluate(classifierFactory.getComponentInstantiation(c));
			}
			if(searchBenchmark instanceof IInformedObjectEvaluatorExtension) {
				((IInformedObjectEvaluatorExtension<Double>)searchBenchmark).updateBestScore(bestScore);
			}
			return searchBenchmark.evaluate(classifierFactory.getComponentInstantiation(c));
=======
				int subSeed = this.seed + c.hashCode();
				IObjectEvaluator<Classifier, Double> copiedSearchBenchmark = new MonteCarloCrossValidationEvaluator(bridge, this.numMCIterations, this.dataShownToSearch, this.trainFoldSize, subSeed);
				return copiedSearchBenchmark.evaluate(this.classifierFactory.getComponentInstantiation(c));
			}
			return this.searchBenchmark.evaluate(this.classifierFactory.getComponentInstantiation(c));
>>>>>>> origin/master
		} catch (InterruptedException e) {
			this.logger.info("Received InterruptedException!");
			assert !Thread.currentThread().isInterrupted() : "The interrupt-flag should not be true when an InterruptedException is thrown! Stack trace of the InterruptedException is \n\t" + Arrays.asList(e.getStackTrace()).stream().map(StackTraceElement::toString).collect(Collectors.joining("\n\t"));
			this.logger.info("Checking whether interrupt is triggered by task {}", task);
			if (Interrupter.get().hasCurrentThreadBeenInterruptedWithReason(task)) {
				this.logger.debug("This is a controlled interrupt of ourselves for task {}.", task);
				Thread.interrupted(); // reset thread interruption flag, because the thread is not really interrupted but should only stop the evaluation
				Interrupter.get().markInterruptOnCurrentThreadAsResolved(task);
				assert !Interrupter.get().hasCurrentThreadOpenInterrupts() : "There are still open interrupts!";
				throw new ObjectEvaluationFailedException("Evaluation of composition failed since the timeout was hit.");
			}
			this.logger.info("Recognized uncontrolled interrupt. Forwarding this exception.");
			throw e;
		} catch (ComponentInstantiationFailedException e) {
			throw new ObjectEvaluationFailedException(e, "Evaluation of composition failed as the component instantiation could not be built.");
		} finally {
			task.cancel();
			this.logger.debug("Canceled timeout job {}", task);
		}
	}
<<<<<<< HEAD

	@Override
	public void updateBestScore(Double bestScore) {
		this.bestScore = bestScore;
	}

=======
>>>>>>> origin/master
}
