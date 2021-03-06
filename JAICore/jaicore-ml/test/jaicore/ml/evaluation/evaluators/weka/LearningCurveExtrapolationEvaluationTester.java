package jaicore.ml.evaluation.evaluators.weka;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.DataSetDescription;

import jaicore.ml.core.dataset.IDataset;
import jaicore.ml.core.dataset.IInstance;
import jaicore.ml.core.dataset.sampling.inmemory.SubsamplingMethod;
import jaicore.ml.core.dataset.sampling.inmemory.WekaInstancesUtil;
import jaicore.ml.learningcurve.extrapolation.ipl.InversePowerLawExtrapolationMethod;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class LearningCurveExtrapolationEvaluationTester {

	@Test
	public void testClassifierEvaluationWithLearningCurveExtrapolation() throws Exception {
		// Load dataset from OpenML
		Instances dataset = null;
		OpenmlConnector client = new OpenmlConnector();
		DataSetDescription description = client.dataGet(42);
		File file = description.getDataset("4350e421cdc16404033ef1812ea38c01");
		DataSource source = new DataSource(file.getCanonicalPath());
		dataset = source.getDataSet();
		dataset.setClassIndex(dataset.numAttributes() - 1);
		Attribute targetAttribute = dataset.attribute(description.getDefault_target_attribute());
		dataset.setClassIndex(targetAttribute.index());
		IDataset<IInstance> simpleDataset = WekaInstancesUtil.wekaInstancesToDataset(dataset);

		// Test classifier evaluation by learning curve extrapolation
		LearningCurveExtrapolationEvaluator evaluator = new LearningCurveExtrapolationEvaluator(
				new int[] { 8, 16, 64, 128 }, SubsamplingMethod.SYSTEMATIC_SAMPLING, simpleDataset, 0.8d,
				new InversePowerLawExtrapolationMethod(), 123l);
		double evaluationResult = evaluator.evaluate(new SMO());
		Assert.assertTrue(evaluationResult > 0 && evaluationResult <= 100);
	}

}
