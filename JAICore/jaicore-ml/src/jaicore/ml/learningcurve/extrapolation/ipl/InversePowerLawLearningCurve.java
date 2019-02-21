package jaicore.ml.learningcurve.extrapolation.ipl;

import java.math.BigDecimal;

import jaicore.ml.interfaces.LearningCurve;

/**
 * Representation of a learning curve with the Inverse Power Law function, which
 * has three parameters named a, b and c. The function is f(x) = (1-a) - b *
 * x^c.
 * 
 * @author Lukas Brandt
 *
 */
public class InversePowerLawLearningCurve implements LearningCurve {

	private double a, b, c;

	public InversePowerLawLearningCurve(double a, double b, double c) {
		assert a > 0 && a < 1;
		assert c > -1 && c < 0;
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public InversePowerLawLearningCurve(InversePowerLawConfiguration configuration) {
		assert configuration.getA() > 0 && configuration.getA() < 1;
		assert configuration.getC() > -1 && configuration.getC() < 0;
		this.a = configuration.getA();
		this.b = configuration.getB();
		this.c = configuration.getC();
	}

	@Override
	public double getSaturationPoint(double epsilon) {
		assert epsilon > 0;
		double n = this.c - 1.0d;
		double base = -(epsilon / (this.b * this.c));
		double result = Math.pow(Math.E, Math.log(base) / n);
		return result;
	}

	@Override
	public double getCurveValue(double x) {
		return (1.0d - this.a) - this.b * Math.pow(x, this.c);
	}

	@Override
	public double getDerivativeCurveValue(double x) {
		return (-this.b) * this.c * Math.pow(x, this.c - 1.0d);
	}

	@Override
	public String toString() {
		return "(1 - " + new BigDecimal(this.a).toPlainString() + ") - " + new BigDecimal(this.b).toPlainString()
				+ " * x ^ " + new BigDecimal(this.c).toPlainString();
	}

}