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
public class DrivingDayA_10segments extends Task {
	// --- Task Code ---//

	private Simulation currentSimulation;
	private JLabel nearLabel, carLabel, keypad;

	private final double scale = .40; // .85;
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
			57.0, 60.0, 63.0, 66.0, // day2
			81.0, 84.0, 87.0, 90.0, // day3
			105.0, 108.0, 111.0, 114.0, // day4
			129.0, 132.0, 135.0, 138.0, // day5
			153.0, 156.0, 159.0, 162.0, // day6

			201.0, 204.0, 207.0, 210.0, // day9
			225.0, 228.0, 231.0, 234.0, // day10
			249.0, 252.0, 255.0, 258.0, // day11
			273.0, 276.0, 279.0, 282.0, // day12
			297.0, 300.0, 303.0, 306.0 // day13
	};

	int simulationNumber = 0;
	double simulationStartTime = 0;
	private Vector<Results> results = new Vector<Results>();
	boolean completed;
	
	public DrivingDayA_10segments() {
		super();
		nearLabel = new JLabel(".");
		carLabel = new JLabel("X");
		keypad = new JLabel("*");
	}

	@Override
	public void start() {
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
				
				
				// in case the car position is out of lane
				if (currentSimulation.samples.lastElement().getSimcarLanePosition()<3.5
						|| currentSimulation.samples.lastElement().getSimcarLanePosition()>5.5)
				{
					System.out.println("car out of lane !!!");
					completed = false;
					results.add(currentSimulation.getResults());
					getModel().stop();
				}

				if (simulator != null)
					simulator.repaint();

			} else {

				results.add(currentSimulation.getResults());
				simulationNumber++;
				System.out.println(simulationNumber);
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

	// calling percentage reset after any new task presentation (audio or
	// visual)
	void fatigueResetPercentage() {
		getModel().getFatigue().fatigueResetPercentages();
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
		double f = 2.5;
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
		URL url = DrivingDayA_10segments.class.getResource("images/" + name);
		return Toolkit.getDefaultToolkit().getImage(url);
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		
		File out = new File("/Users/ehsanebk/OneDrive - drexel.edu/Driving Data(Van Dongen)/Results_Model_TimePoints_Day_Cumulative.csv");
		//File out = new File("/Users/Ehsan/OneDrive - drexel.edu/Driving Data(Van Dongen)/Results_Model_TimePoints_Day_Cumulative.csv");
		PrintWriter outputCSV = null;
		try {
			outputCSV = new PrintWriter(out);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		getModel().output("******** Results of Day A **********");
		outputCSV.print("******** Results of Day A **********");
		try {
			int numberOfSimulations = results.size();
			
			for (int i = 0; i < numberOfSimulations; i++) {
			
			}

			DecimalFormat df2 = new DecimalFormat("#.00");
			
			getModel().output("\n******* index ********** \n");
			outputCSV.print("\n******* index ********** \n");
			for (Task taskCast : tasks) {
				DrivingDayA_10segments task = (DrivingDayA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("–\t");
					outputCSV.print("–,");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					Results result = task.results.elementAt(i);
					getModel().outputInLine(String.valueOf(df2.format(result.lastIndex) +"\t"));
					getModel().outputInLine("\t");
					outputCSV.print(String.valueOf(df2.format(result.lastIndex) +","));
					outputCSV.print(",");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
				
			getModel().output("\n******* taskLatDev_10Segments ********** \n");
			outputCSV.print("\n******* taskLatDev_10Segments ********** \n");
			for (Task taskCast : tasks) {
				DrivingDayA_10segments task = (DrivingDayA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("–\t");
					outputCSV.print(",");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					Results result = task.results.elementAt(i);
					for (int j = 0; j < 10; j++) {
						getModel().outputInLine(df2.format(result.taskLatDev_10Segments[j]) +" ");
						outputCSV.print(df2.format(result.taskLatDev_10Segments[j]) +",");
					}
					getModel().outputInLine("\t");
					outputCSV.print(",,");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
			
			getModel().output("\n*******STEX3_10Segments ********** \n");
			outputCSV.print("\n*******STEX3_10Segments ********** \n");
			for (Task taskCast : tasks) {
				DrivingDayA_10segments task = (DrivingDayA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("–\t");
					outputCSV.print("–,");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					Results result = task.results.elementAt(i);
					for (int j = 0; j < 10; j++) {
						getModel().outputInLine(df2.format(result.STEX3_10Segments[j]) +" ");
						outputCSV.print(df2.format(result.STEX3_10Segments[j]) +",");
					}
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
			
			getModel().output("\n******* taskSpeedDev_10Segments ********** \n");
			outputCSV.print("\n******* taskSpeedDev_10Segments ********** \n");
			for (Task taskCast : tasks) {
				DrivingDayA_10segments task = (DrivingDayA_10segments) taskCast;
				if (!task.completed){
					getModel().outputInLine("–\t");
					outputCSV.print("–,");
				}
				else{
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				
				for (int i = 0; i < numberOfSimulations; i++) {
					Results result = task.results.elementAt(i);
					for (int j = 0; j < 10; j++) {
						getModel().outputInLine(df2.format(result.taskSpeedDev_10Segments[j]) +" ");
						outputCSV.print(df2.format(result.taskSpeedDev_10Segments[j]) +",");
					}
					getModel().outputInLine("\t");
					outputCSV.print(",");
				}
				getModel().outputInLine("\n\n");
				outputCSV.print("\n\n");
			}
			outputCSV.flush();
			outputCSV.close();

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
