package actr.tasks.drivingPVT;

import java.text.DecimalFormat;
import java.util.Vector;

public class Values {
	private Vector<Double> v;

	public Values() {
		v = new Vector<Double>();
	}

	public void add(double d) {
		v.add(d);
	}

	public void add(Values vals2) {
		for (int i = 0; i < vals2.size(); i++)
			v.add(vals2.get(i));
	}

	public double get(int i) {
		return v.elementAt(i);
	}

	public void removeLast() {
		if (v.size() > 0)
			v.removeElementAt(v.size() - 1);
	}

	public int size() {
		return v.size();
	}

	public double min() {
		if (v.size() == 0)
			return 0;
		double min = v.elementAt(0);
		for (int i = 1; i < v.size(); i++)
			if (v.elementAt(i) < min)
				min = v.elementAt(i);
		return min;
	}

	public double max() {
		if (v.size() == 0)
			return 0;
		double max = v.elementAt(0);
		for (int i = 1; i < v.size(); i++)
			if (v.elementAt(i) > max)
				max = v.elementAt(i);
		return max;
	}

	public double mean() {
		if (v.size() == 0)
			return 0;
		double sum = 0;
		for (int i = 0; i < v.size(); i++)
			sum += v.elementAt(i);
		return sum / (1.0 * v.size());
	}
	
	public double meanInRange(double min, double max) {
		if (v.size() == 0)
			return 0;
		double sum = 0;
		int counter = 0;
		for (int i = 0; i < v.size(); i++)
			if (inRange(v.elementAt(i), min, max)){
				sum += v.elementAt(i);
				counter ++;
			}
		return sum / (1.0 * counter);
	}

	public double average() {
		return mean();
	}
	
	public double averageInRange(double min, double max){
		return meanInRange(min, max);
	}

	public boolean inRange(double x, double min, double max) {
		return (x >= min && x <= max);
	}

	public String meanDF3() {
		if (v.size() == 0)
			return "0";
		double sum = 0;
		for (int i = 0; i < v.size(); i++)
			sum += v.elementAt(i);
		DecimalFormat df3 = new DecimalFormat("#.000");
		return df3.format(sum / (1.0 * v.size()));
	}


	public double stddev() {
		if (v.size() < 2)
			return 0;
		double mean = mean();
		double sum = 0;
		for (int i = 0; i < v.size(); i++)
			sum += Math.pow(v.elementAt(i) - mean, 2);
		return Math.sqrt(sum / (1.0 * (v.size() - 1)));
	}

	public double stderr() {
		if (v.size() < 2)
			return 0;
		return stddev() / Math.sqrt(1.0 * v.size());
	}

	public double confint() {
		return 1.96 * stderr();
	}

	public double rmse(double expected) {
		if (v.size() == 0)
			return 0;
		double sum = 0;
		for (int i = 0; i < v.size(); i++)
			sum += Math.pow(v.elementAt(i) - expected, 2);
		return Math.sqrt(sum / (1.0 * v.size()));
	}

	public double rmse() {
		return rmse(0);
	}

	public String toString(DecimalFormat df) {
		if (v.size() == 0)
			return "";
		String s = "";
		s += df.format(v.elementAt(0));
		for (int i = 1; i < v.size(); i++)
			s += "\t" + df.format(v.elementAt(i));
		return s;
	}

	private final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("#.000");

	@Override
	public String toString() {
		return toString(DEFAULT_FORMAT);
	}
}
