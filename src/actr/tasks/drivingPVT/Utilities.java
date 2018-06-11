package actr.tasks.drivingPVT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import actr.tasks.driving.Values;

public class Utilities {
	public static DecimalFormat df0 = new DecimalFormat("0");
	public static DecimalFormat df1 = new DecimalFormat("#.0");
	public static DecimalFormat df2 = new DecimalFormat("#.00");
	public static DecimalFormat df3 = new DecimalFormat("#.000");
	public static DecimalFormat df4 = new DecimalFormat("#.0000");
	public static DecimalFormat df5 = new DecimalFormat("#.00000");
	public static DecimalFormat df8 = new DecimalFormat("#.00000000");

	public static int sign(double x) {
		return (x >= 0) ? 1 : -1;
	}

	public static double sqr(double x) {
		return (x * x);
	}

	public static double rotationAngle(double hx, double hz) {
		return (-180 * (Math.atan2(hz, hx)) / Math.PI);
	}

	public static double deg2rad(double x) {
		return x * (Math.PI / 180.0);
	}

	public static double rad2deg(double x) {
		return x * (180.0 / Math.PI);
	}

	public static double mps2mph(double x) {
		return x * 2.237;
	}

	public static double mph2mps(double x) {
		return x / 2.237;
	}

	public static double in2cm(double x) {
		return x * 2.54;
	}

	static double cm2in(double x) {
		return x / 2.54;
	}

	public static double minsigned(double x, double m) {
		return (x < 0) ? Math.max(x, -m) : Math.min(x, m);
	}

	public static double maxsigned(double x, double m) {
		return (x < 0) ? Math.min(x, -m) : Math.max(x, m);
	}

	public static String repeatString(String s, int n) {
		String res = "";
		for (int i = 0; i < n; i++)
			res += s;
		return res;
	}

	public static int count(char c, String s) {
		int res = 0;
		for (int i = 0; i < s.length(); i++)
			if (s.charAt(i) == c)
				res++;
		return res;
	}

	public static PrintStream uniqueOutputFile(String name) {
		int num = 1;
		File file;
		String filename;
		do {
			filename = name + num + ".txt";
			file = new File(filename);
			num++;
		} while (file.exists());
		PrintStream stream = null;
		try {
			stream = new PrintStream(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return stream;
	}
	
	public static boolean arrayContains (int [] array, int element){
		for ( int a: array){
			if ( a == element)
				return true;
		}
		return false;
	}
	
	public static boolean arrayContains (String [] array, String element){
		return Arrays.asList(array).contains(element);
	}

	public static double toDouble(String s){
		try {
			return Double.valueOf(s).doubleValue();
		} catch (Exception e) {
			return 0;
		}
	}
	public static int toInt(String s){
		try {
			return Integer.valueOf(s).intValue();
		} catch (Exception e) {
			return 0;
		}
	}
	public static long toLong(String s){
		try {
			return Long.valueOf(s).longValue();
		} catch (Exception e) {
			return 0;
		}
	}
	
	/**
	 * @param values  An array of numbers that will be modified in place
	 * @param smoothing  the strength of the smoothing filter; 1=no change, larger values smoothes more
	 *  since the invalid values are -100 there are if statements which check how the values are before and after
	 * @return Smooth Array
	 */
	public static Values smoothArray( Values values, int smoothing ){
	  
		Values output = new Values();
		double  value = values.get(0); // start with the first input
		output.add(value);
		for (int i =1; i < values.size(); ++i){
			if (!(values.get(i-1) == -100) && !(values.get(i) == -100) ){
				double currentValue = values.get(i);
				value += (currentValue - value) / smoothing;
				output.add(value);
			}
			else if ((values.get(i) == -100) ){
				output.add(-100.0);
			}
			else if ((values.get(i-1) == -100) && !(values.get(i) == -100) ){
				value = values.get(i); // start again
				output.add(value);
			}
		}
		return output;
	}

	
	/**
	 * @param values
	 * @param CUTOFF
	 * @param SAMPLE_RATE
	 * @return Return RC low-pass filter output samples
	 * given input samples,
	 * time interval dt, and time constant RC
	 */
	public static Values lowpass( Values values,double CUTOFF, double SAMPLE_RATE){
		  
		Values output = new Values();
		double  value = values.get(0); // start with the first input
		output.add(value);
		
		double RC = 1.0/(CUTOFF*2*3.14); 
	    double dt = 1.0/SAMPLE_RATE; 
	    double alpha = dt/(RC+dt);
		
		for (int i =1; i < values.size(); ++i){
			if (!(values.get(i-1) == -100) && !(values.get(i) == -100) ){
				value = output.get(i-1) + (alpha*(values.get(i) - output.get(i-1)));
				output.add(value);
			}
			else if ((values.get(i) == -100) ){
				output.add(-100.0);
			}
			else if ((values.get(i-1) == -100) && !(values.get(i) == -100) ){
				value = values.get(i); // start again
				output.add(value);
			}
		}
		return output;
	}
	
	/**
	 * @param values
	 * @param alpha value of alpha is between 0 and 1. the curve will be smoother if alpha is closer to 0 
	 * @return Return RC low-pass filter output samples, given input samples
	 */
	public static Values lowpass( Values values,double alpha){

		Values output = new Values();
		if (values.size() ==0)
			return output;
		double  value = values.get(0); // start with the first input
		output.add(value);

		for (int i =1; i < values.size(); ++i){
			if (!(values.get(i-1) == -100) && !(values.get(i) == -100) ){
				value = output.get(i-1) + (alpha*(values.get(i) - output.get(i-1)));
				output.add(value);
			}
			else if ((values.get(i) == -100) ){
				output.add(-100.0);
			}
			else if ((values.get(i-1) == -100) && !(values.get(i) == -100) ){
				value = values.get(i); // start again
				output.add(value);
			}
		}
		return output;
	}

	/**
	 * This function finds the local Min and Max based on the valid intervals in
	 * the LanePositions values which is an array
	 * In the LanePos Values the invalid values have the value of -100
	 * @param LanePositions
	 * @param window size by frame
	 * @return  a list of integers that indicates the frame numbers
	 */
	public static List<Integer> MaxMinValues(Values LanePositions,int window) {

		List<Integer> MaxMin = new ArrayList<Integer>();
		// the values which are not available are represented with -100
		boolean max;
		boolean min;

		// adding extra for capturing the end min and max
		for (int j = 0; j < 20; j++) {
			LanePositions.add(-100);
		}

		for (int i = (window / 2); i < LanePositions.size() - (window / 2); i++) {
			if (LanePositions.get(i) > -100) {
				// local max with the specified interval
				if ((LanePositions.get(i - (window / 2)) < LanePositions.get(i)
						|| LanePositions.get(i - (window / 2)) == -100)
						&& (LanePositions.get(i) > LanePositions.get(i + (window / 2))
								|| LanePositions.get(i + (window / 2)) == -100)) {
					max = true;
					for (int j = i - (window / 2); j <= i + (window / 2); j++) {
						if (LanePositions.get(j) > LanePositions.get(i) && LanePositions.get(j) > -100) {
							max = false;
							break;
						}
					}
					if (max) {
						MaxMin.add(i);
						i += (window / 2);
					}
				}
				// local min with the specified interval
				else if ((LanePositions.get(i - (window / 2)) > LanePositions.get(i)
						|| LanePositions.get(i - (window / 2)) == -100)
						&& (LanePositions.get(i) < LanePositions.get(i + (window / 2))
								|| LanePositions.get(i + (window / 2)) == -100)) {
					min = true;
					for (int j = i - (window / 2); j <= i + (window / 2); j++) {
						if (LanePositions.get(j) < LanePositions.get(i) && LanePositions.get(j) > -100) {
							min = false;
							break;
						}
					}
					if (min) {
						MaxMin.add(i);
						i += (window / 2);
					}
				}
			}
		}
		// return numberMax+numberMin;
		return MaxMin;
	}

	/**
	 * Using values of the previous three time steps, 
	 * a second order Taylor expansion on time n-1 is performed 
	 * to obtain the predicted value at time n.
	 * 
	 * @return prediction error value
	 */
	public static Values predictionError(Values v){
		Values e = new Values();
		for (int i = 3; i < v.size(); i++) {
			double predicted = v.get(i-1) +(v.get(i-1)-v.get(i-2)) +
					(1/2)*((v.get(i-1)-v.get(i-2))- (v.get(i-2)-v.get(i-3)));
			e.add(v.get(i) - predicted);
		}
		
		
		return e;
	}
	
	public static double steeringEntropy (Values v){
		Values smoothV = new Values();
		for (int i = 0; i < v.size()-5; i= i+5) {
			smoothV.add((v.get(i)+v.get(i+1)+v.get(i+2)+v.get(i+3)+v.get(i+4))/5);
		}
		Values predictionError = predictionError(smoothV);
		double alpha = predictionError.stddev() * 1.645;
		int frequency[] = new int [9];
		double mean = predictionError.mean();
		
		for (int i = 0; i < predictionError.size(); i++) {
			if (predictionError.get(i) <= mean-5*alpha)
				frequency[0]++;
			else if (predictionError.get(i)> mean-5*alpha && predictionError.get(i)<= mean-2.5*alpha)
				frequency[1]++;
			else if (predictionError.get(i)> mean-2.5*alpha && predictionError.get(i)<= mean-alpha)
				frequency[2]++;
			else if (predictionError.get(i)> mean-alpha && predictionError.get(i)<= mean-0.5*alpha)
				frequency[3]++;
			else if (predictionError.get(i)> mean-0.5*alpha && predictionError.get(i)< mean+0.5*alpha)
				frequency[4]++;
			else if (predictionError.get(i)>= mean+0.5*alpha && predictionError.get(i)< mean+alpha)
				frequency[5]++;
			else if (predictionError.get(i)>= mean+alpha && predictionError.get(i)< mean+2.5*alpha)
				frequency[6]++;
			else if (predictionError.get(i)>= mean+2.5*alpha && predictionError.get(i)< mean+5*alpha)
				frequency[7]++;
			else if (predictionError.get(i)>= mean+5*alpha)
				frequency[8]++;
		}
		double P[] = new double [9];
		for (int i = 0; i < frequency.length; i++) {
			
			P[i]  = (double)frequency[i] / predictionError.size(); 
		}
		
		// Calculating the log base 9 of the different partitions
		double entropy= 0;
		for (int i = 0; i < P.length; i++) {
			if ( !Double.isNaN(-P[i]*(Math.log(P[i])/Math.log(9)))){
				entropy  +=  -P[i]*(Math.log(P[i])/Math.log(9));
			}
		}
		return entropy;
	}
	
}
