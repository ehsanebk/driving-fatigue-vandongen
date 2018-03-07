package actr.tasks.driving;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JLabel;
import actr.task.Result;
import actr.task.Task;
import actr.tasks.drivingPVT.Values;

/**
 * The main Driving task class that sets up the simulation and starts periodic
 * updates.
 * 
 * @author Dario Salvucci
 */
public class DrivingNightA_10segments extends Task {
	// --- Task Code ---//

	private Simulation currentSimulation;
	private JLabel nearLabel, carLabel, keypad;

	private final double scale = .45;
	private final double steerFactor_dfa = (16 * scale);
	private final double steerFactor_dna = (4 * scale);
	private final double steerFactor_na = (3 * scale);
	private final double accelFactor_thw = (2 * .40); // 1 orig, 3?
	private final double accelFactor_dthw = (4 * .40); // 3 orig, 5?
	private final double steerNaMax = .07;
	private final double thwFollow = 1.0; // 1.0 orig

	private final double simulationDurarion = 60 * 30; // the driving sessions are 30
													// min (30 * 60sec)
	private final double simulationDistance = 45061.6;  // equal to 28 miles

	private double accelBrake = 0, speed = 0;

	private static final int minX = 174, maxX = (238 + 24), minY = 94, maxY = (262 + 32);
	static final int centerX = (minX + maxX) / 2, centerY = (minY + maxY) / 2;

	private static Simulator simulator = null;

	private double[] timesOfPVT = {
			//
			45.0, 48.0, 51.0, 54.0, // day2
			69.0, 72.0, 75.0, 78.0, // day3
			93.0, 96.0, 99.0, 102.0, // day4
			117.0, 120.0, 123.0, 126.0, // day5
			141.0, 144.0, 147.0, 150.0, // day6

			189.0, 192.0, 195.0, 198.0, // day9
			213.0, 216.0, 219.0, 222.0, // day10
			237.0, 240.0, 243.0, 246.0, // day11
			261.0, 264.0, 267.0, 270.0, // day12
			285.0, 288.0, 291.0, 294.0 // day13

	};

	int simulationNumber = 0;
	double simulationStartTime = 0;
	private Vector<Results> results = new Vector<Results>();
	private boolean completed;
	private Vector<int []> microLapses = new Vector<int []>(); // [ number of mnicro lapses , number of total productions ] 
	private int currentSimulation_NumberOf_MicroLapses=0;
	private int currentSimulation_NumberOf_Productions=0;
	
	File out; // output file 
	File outPara;  // for fatigue parameter output
	PrintWriter outputPara = null;
	PrintWriter outputCSV = null;
	
	int c=0;
	
	public DrivingNightA_10segments() {
		super();
		nearLabel = new JLabel(".");
		carLabel = new JLabel("X");
		keypad = new JLabel("*");	
	}

	@Override
	public void start() {
		out = new File("/Users/ehsanebk/OneDrive - drexel.edu/Driving Data(Van Dongen)/Results_Model_TimePoints_Night_Cumulative.csv");
		outPara = new File("/Users/ehsanebk/OneDrive - drexel.edu/Driving Data(Van Dongen)/Results_Fatigue_Parameters(Night).csv");
//		out = new File("/Users/Ehsan/OneDrive - drexel.edu/Driving Data(Van Dongen)/Results_Model_TimePoints_Night_CumulativeX.csv");
//		outPara = new File("/Users/Ehsan/OneDrive - drexel.edu/Driving Data(Van Dongen)/Results_Fatigue_Parameters(Night)X.csv");

		if (!new File(out.getParent()).exists()){
			getModel().output("The output file path is not valid!!");
			getModel().stop();
		}
		
		// for output fatigue parameters and the data
		try {
			outputPara = new PrintWriter(outPara);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		outputPara.println("time,FP,FinalFP,UT");
		
		completed = true;
		currentSimulation = new Simulation();

		getModel().getFatigue().setFatigueHour(timesOfPVT[simulationNumber]);
		getModel().getFatigue().startFatigueSession();

		if (getModel().getRealTime()) {
			if (simulator == null)
				simulator = new Simulator();
			simulator.useSimulation(currentSimulation);
			setLayout(new BorderLayout());
			add(simulator, BorderLayout.CENTER);
			setVisible(false); // trigger OpenGL init
			setVisible(true);
		} else {

			add(nearLabel);
			nearLabel.setSize(20, 20);
			nearLabel.setLocation(250, 250);
			add(carLabel);
			carLabel.setSize(20, 20);
			carLabel.setLocation(250, 250);
			add(keypad);
			keypad.setSize(20, 20);
			int keypadX = 250 + (int) (actr.model.Utilities.angle2pixels(10.0));
			keypad.setLocation(keypadX, 250);

		}

		getModel().runCommand("(set-visual-frequency near .1)");
		getModel().runCommand("(set-visual-frequency far .1)");
		getModel().runCommand("(set-visual-frequency car .1)");

		accelBrake = 0;
		speed = 0;

		getModel().getVision().addVisual("near", "near", "near", nearLabel.getX(), nearLabel.getY(), 1, 1, 10);
		getModel().getVision().addVisual("car", "car", "car", carLabel.getX(), carLabel.getY(), 1, 1, 100);
		getModel().getVision().addVisual("keypad", "keypad", "keypad", keypad.getX(), keypad.getY(), 1, 1);

		addPeriodicUpdate(Environment.SAMPLE_TIME);
	}

	@Override
	public void update(double time) {
		try {
			if (currentSimulation.getEnvironment().getSimcar().getIndex() <= simulationDistance) {
				currentSimulation.getEnvironment().setTime(time - simulationStartTime);
				currentSimulation.update();
				updateVisuals();
				c++;
				if (getModel().getProcedural().isMicroLapse())
					currentSimulation_NumberOf_MicroLapses++;
				currentSimulation_NumberOf_Productions++;
				if (simulationNumber == timesOfPVT.length-1)
					outputPara.println(currentSimulation.getEnvironment().getTime()+","
							+ getModel().getProcedural().getFatigueUtility() + ","
							+ getModel().getProcedural().getFinalInstUtility() + "," 
							+ getModel().getFatigue().getFatigueUT());
				outputPara.flush();
				// in case the car position is out of lane
				if (currentSimulation.samples.lastElement().getSimcarLanePosition()<3
						|| currentSimulation.samples.lastElement().getSimcarLanePosition()>6)
				{
					System.out.println("car out of lane !!!");
					completed = false;
					results.add(currentSimulation.getResults());
					int [] l = {currentSimulation_NumberOf_MicroLapses,currentSimulation_NumberOf_Productions} ;
					currentSimulation_NumberOf_MicroLapses = 0;
					currentSimulation_NumberOf_Productions = 0;
					microLapses.add(l);
					getModel().stop();
				}

				if (simulator != null)
					simulator.repaint();

			} else {
				System.out.println(simulationNumber);
				results.add(currentSimulation.getResults());
				int [] l = {currentSimulation_NumberOf_MicroLapses,currentSimulation_NumberOf_Productions} ;
				currentSimulation_NumberOf_MicroLapses = 0;
				currentSimulation_NumberOf_Productions = 0;
				microLapses.add(l);
				simulationNumber++;
				// go to the next simulation or stop the model
				if (simulationNumber < timesOfPVT.length) {
					currentSimulation = new Simulation();
					simulationStartTime = time;
					getModel().getFatigue().setFatigueHour(timesOfPVT[simulationNumber]);
					getModel().getFatigue().startFatigueSession();

					removeAll();

					add(nearLabel);
					nearLabel.setSize(20, 20);
					nearLabel.setLocation(250, 250);
					add(carLabel);
					carLabel.setSize(20, 20);
					carLabel.setLocation(250, 250);
					add(keypad);
					keypad.setSize(20, 20);
					int keypadX = 250 + (int) (actr.model.Utilities.angle2pixels(10.0));
					keypad.setLocation(keypadX, 250);

					accelBrake = 0;
					speed = 0;

					getModel().getVision().addVisual("near", "near", "near", nearLabel.getX(), nearLabel.getY(), 1, 1,
							10);
					getModel().getVision().addVisual("car", "car", "car", carLabel.getX(), carLabel.getY(), 1, 1, 100);
					getModel().getVision().addVisual("keypad", "keypad", "keypad", keypad.getX(), keypad.getY(), 1, 1);

				} else {
					getModel().stop();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void updateVisuals() {
		Environment env = currentSimulation.getEnvironment();
		if (env.getSimcar().getNearPoint() != null) {
			Coordinate cn = env.world2image(env.getSimcar().getNearPoint());
			Coordinate cc = env.world2image(env.getSimcar().getCarPoint());
			if (cn == null || cc == null)
				env.setDone(true);
			else {
				nearLabel.setLocation(cn.getX(), cn.getY());
				carLabel.setLocation(cc.getX(), cc.getY());
				getModel().getVision().moveVisual("near", cn.getX() + 5, cn.getY() + 10, cn.getD());
				getModel().getVision().moveVisual("car", cc.getX() + 5, cc.getY() + 10, cc.getD());
				speed = env.getSimcar().getSpeed();
			}
		}
	}

	double minSigned(double x, double y) {
		return (x >= 0) ? Math.min(x, y) : Math.max(x, -y);
	}

	boolean isCarStable(double na, double nva, double fva) {
		double f = 0.5; //2.5;
		return (Math.abs(na) < .025 * f) && (Math.abs(nva) < .0125 * f) && (Math.abs(fva) < .0125 * f);
	}

	double image2angle(double x, double d) {
		Environment env = currentSimulation.getEnvironment();
		double px = env.getSimcar().getPX() + (env.getSimcar().getHX() * d);
		double pz = env.getSimcar().getPZ() + (env.getSimcar().getHZ() * d);
		Coordinate im = env.world2image(new Position(px, pz));
		try {
			return Math.atan2(.5 * (x - im.getX()), 450);
		} catch (Exception e) {
			return 0;
		}
	}

	@Override
	public void eval(Iterator<String> it) {
		it.next(); // (
		String cmd = it.next();
		if (cmd.equals("do-steer")) {
			double na = Double.valueOf(it.next());
			double dna = Double.valueOf(it.next());
			double dfa = Double.valueOf(it.next());
			double dt = Double.valueOf(it.next());
			doSteer(na, dna, dfa, dt);
		} else if (cmd.equals("do-accelerate")) {
			double fthw = Double.valueOf(it.next());
			double dthw = Double.valueOf(it.next());
			double dt = Double.valueOf(it.next());
			doAccelerate(fthw, dthw, dt);
		} else if (cmd.equals("fatigue-reset-percentage")) {
			fatigueResetPercentage();
		}
	}
	
	// calling percentage reset after any new task presentation (audio or visual)
	void fatigueResetPercentage() {
		getModel().getFatigue().fatigueResetPercentages();
		if (getModel().isVerbose())
			getModel().output("!!!! Fatigue Percentage Reset !!!!");
	}

	@Override
	public boolean evalCondition(Iterator<String> it) {
		it.next(); // (
		String cmd = it.next();
		if (cmd.equals("is-car-stable") || cmd.equals("is-car-not-stable")) {
			double na = Double.valueOf(it.next());
			double nva = Double.valueOf(it.next());
			double fva = Double.valueOf(it.next());
			boolean b = isCarStable(na, nva, fva);
			return cmd.equals("is-car-stable") ? b : !b;
		} else
			return false;
	}

	@Override
	public double bind(Iterator<String> it) {
		try {
			it.next(); // (
			String cmd = it.next();
			if (cmd.equals("image->angle")) {
				double x = Double.valueOf(it.next());
				double d = Double.valueOf(it.next());
				return image2angle(x, d);
			} else if (cmd.equals("mp-time"))
				return currentSimulation.getEnvironment().getTime();
			else if (cmd.equals("get-thw")) {
				double fd = Double.valueOf(it.next());
				double v = Double.valueOf(it.next());
				double thw = (v == 0) ? 4.0 : fd / v;
				return Math.min(thw, 4.0);
			} else if (cmd.equals("get-velocity"))
				return speed;
			else
				return 0;

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
			return 0;
		}
	}

	void doSteer(double na, double dna, double dfa, double dt) {
		Simcar simcar = currentSimulation.getEnvironment().getSimcar();
		if (simcar.getSpeed() >= 10.0) {
			double dsteer = (dna * steerFactor_dna) + (dfa * steerFactor_dfa)
					+ (minSigned(na, steerNaMax) * steerFactor_na * dt);
			dsteer *= currentSimulation.getDriver().getSteeringFactor();
			simcar.setSteerAngle(simcar.getSteerAngle() + dsteer);
		} else
			simcar.setSteerAngle(0);
	}

	void doAccelerate(double fthw, double dthw, double dt) {
		Simcar simcar = currentSimulation.getEnvironment().getSimcar();
		if (simcar.getSpeed() >= 10.0) {
			double dacc = (dthw * accelFactor_dthw) + (dt * (fthw - thwFollow) * accelFactor_thw);
			accelBrake += dacc;
			accelBrake = minSigned(accelBrake, 1.0);
		} else {
			accelBrake = .65 * (currentSimulation.getEnvironment().getTime() / 3.0);
			accelBrake = minSigned(accelBrake, .65);
		}
		simcar.setAccelerator((accelBrake >= 0) ? accelBrake : 0);
		simcar.setBrake((accelBrake < 0) ? -accelBrake : 0);
	}

	// @Override
	// public void finish() {
	// simulator.stop();
	// }

	public static Image getImage(final String name) {
		URL url = DrivingNightA_10segments.class.getResource("images/" + name);
		return Toolkit.getDefaultToolkit().getImage(url);
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {

		// for output the data
		try {
			outputCSV = new PrintWriter(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		int numberOfSimulations = ((DrivingNightA_10segments)tasks[0]).results.size();
		Values[][] totalLatDev = new Values[numberOfSimulations][10];
		Values[][] totalSTEX3 = new Values[numberOfSimulations][10];
		Values[][] totalSpeedDev = new Values[numberOfSimulations][10];
		for (int i = 0; i < numberOfSimulations; i++){
			for (int j = 0; j < 10; j++) {
				totalLatDev[i][j] = new Values();
				totalSTEX3[i][j] = new Values();
				totalSpeedDev[i][j] = new Values();
			}
		}
		
		for (Task taskCast : tasks) {
			DrivingNightA_10segments task = (DrivingNightA_10segments) taskCast;
			if (!task.completed)
				continue;
			for (int i = 0; i < numberOfSimulations; i++) {
				Results results = task.results.elementAt(i);
				for (int j = 0; j < 10; j++) {
					totalLatDev[i][j].add(results.taskLatDev_10Segments[j]);
					totalSTEX3[i][j].add(results.STEX3_10Segments[j]);;
					totalSpeedDev[i][j].add(results.taskSpeedDev_10Segments[j]);;
				}
			}
		}
		
		
		
		
		getModel().output("******** Results of Night A **********");
		outputCSV.print("******** Results of Night A **********");
		
		outputCSV.println("\n******* Average LatDev for 10 Segments **********");
		int counter = 0 ;
		while (counter < numberOfSimulations ) {
			if (counter % 4 ==0)
				outputCSV.print("\n");
			if (counter == 20)
				outputCSV.print("\n");
			outputCSV.print(",");
			for (int i = 0; i < 10; i++) {
				outputCSV.print("," + totalLatDev[counter][i].meanDF3());
			}
			counter++;
		}
		
		outputCSV.println("\n******* Average STEX3 for 10 Segments **********");
		counter = 0 ;
		while (counter < numberOfSimulations ) {
			if (counter % 4 ==0)
				outputCSV.print("\n");
			if (counter == 20)
				outputCSV.print("\n");
			outputCSV.print(",");
			for (int i = 0; i < 10; i++) {
				outputCSV.print("," + totalSTEX3[counter][i].meanDF3());
			}
			counter++;
		}
		
		outputCSV.println("\n******* Average SpeedDev for 10 Segments **********");
		counter = 0 ;
		while (counter < numberOfSimulations ) {
			if (counter % 4 ==0)
				outputCSV.print("\n");
			if (counter == 20)
				outputCSV.print("\n");
			outputCSV.print(",");
			for (int i = 0; i < 10; i++) {
				outputCSV.print("," + totalSpeedDev[counter][i].meanDF3());
			}
			counter++;
		}
		
		
		try {
			DecimalFormat df = new DecimalFormat("#.000000");
			
			getModel().output("\n******* index ********** \n");
			outputCSV.print("\n******* index ********** \n");
			for (Task taskCast : tasks) {
				DrivingNightA_10segments task = (DrivingNightA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("*\t");
					outputCSV.print("*,");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					Results result = task.results.elementAt(i);
					getModel().outputInLine(String.valueOf(df.format(result.lastIndex) +"\t"));
					getModel().outputInLine("\t");
					outputCSV.print(String.valueOf(df.format(result.lastIndex) +","));
					outputCSV.print(",");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
			outputCSV.flush();
			
			getModel().output("\n******* Task Time ********** \n");
			outputCSV.print("\n******* Task Time ********** \n");
			for (Task taskCast : tasks) {
				DrivingNightA_10segments task = (DrivingNightA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("*\t");
					outputCSV.print("*,");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					Results result = task.results.elementAt(i);
					getModel().outputInLine(String.valueOf(df.format(result.taskTime) +"\t"));
					getModel().outputInLine("\t");
					outputCSV.print(String.valueOf(df.format(result.taskTime) +","));
					outputCSV.print(",");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
			outputCSV.flush();
			
			getModel().output("\n******* Number of MicroLapses ********** \n");
			outputCSV.print("\n******* Number of MicroLapses ********** \n");
			for (Task taskCast : tasks) {
				DrivingNightA_10segments task = (DrivingNightA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("*\t");
					outputCSV.print("*,");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					getModel().outputInLine(task.microLapses.get(i)[0]+"/"+ task.microLapses.get(i)[1]+"\t");
					getModel().outputInLine("\t");
					outputCSV.print(task.microLapses.get(i)[0]+"/"+ task.microLapses.get(i)[1] +",");
					outputCSV.print(",");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
			outputCSV.flush();
			
			getModel().output("\n******* Index_10Segments ********** \n");
			outputCSV.print("\n******* Index_10Segments ********** \n");
			for (Task taskCast : tasks) {
				DrivingNightA_10segments task = (DrivingNightA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("*\t");
					outputCSV.print("*,");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					Results result = task.results.elementAt(i);
					for (int j = 0; j < 10; j++) {
						getModel().outputInLine(result.startIndex_10Segments[j]
								+"-" + result.endIndex_10Segments[j] +" ");
						outputCSV.print(result.startIndex_10Segments[j]
								+"-" + result.endIndex_10Segments[j] +",");
					}
					getModel().outputInLine("\t");
					outputCSV.print(",,");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
			outputCSV.flush();

			getModel().output("\n******* taskLatDev_10Segments ********** \n");
			outputCSV.print("\n******* taskLatDev_10Segments ********** \n");
			for (Task taskCast : tasks) {
				DrivingNightA_10segments task = (DrivingNightA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("*\t");
					outputCSV.print("*,");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					Results result = task.results.elementAt(i);
					for (int j = 0; j < 10; j++) {
						getModel().outputInLine(df.format(result.taskLatDev_10Segments[j]) +" ");
						outputCSV.print(df.format(result.taskLatDev_10Segments[j]) +",");
					}
					getModel().outputInLine("\t");
					outputCSV.print(",,");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
			outputCSV.flush();

			getModel().output("\n******* STEX3_10Segments ********** \n");
			outputCSV.print("\n******* STEX3_10Segments ********** \n");
			for (Task taskCast : tasks) {
				DrivingNightA_10segments task = (DrivingNightA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("*\t");
					outputCSV.print("*,");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					Results result = task.results.elementAt(i);
					for (int j = 0; j < 10; j++) {
						getModel().outputInLine(df.format(result.STEX3_10Segments[j]) +" ");
						outputCSV.print(df.format(result.STEX3_10Segments[j]) +",");
					}
					getModel().outputInLine("\t");
					outputCSV.print(",,");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
			outputCSV.flush();
			
			getModel().output("\n******* taskSpeedDev_10Segments ********** \n");
			outputCSV.print("\n******* taskSpeedDev_10Segments ********** \n");
			for (Task taskCast : tasks) {
				DrivingNightA_10segments task = (DrivingNightA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("*\t");
					outputCSV.print("*,");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					Results result = task.results.elementAt(i);
					for (int j = 0; j < 10; j++) {
						getModel().outputInLine(df.format(result.taskSpeedDev_10Segments[j]) +" ");
						outputCSV.print(df.format(result.taskSpeedDev_10Segments[j]) +",");
					}
					getModel().outputInLine("\t");
					outputCSV.print(",,");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
			outputCSV.flush();
			outputCSV.close();
			
			outputPara.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		Result result = new Result();
		return result;
	}

	public static String toString(double a[]) {
		String s = "";
		for (int i = 0; i < a.length; i++)
			s += String.format("%.2f", a[i]) + (i < a.length - 1 ? "\t" : "");
		return s;
	}	
}
