package actr.tasks.driving;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
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
public class DrivingNightB extends Task {
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

	private double simulationDurarion = 60*30; // the driving sessions are 30 min 
	private double accelBrake = 0, speed = 0;

	private static final int minX = 174, maxX = (238 + 24), minY = 94, maxY = (262 + 32);
	static final int centerX = (minX + maxX) / 2, centerY = (minY + maxY) / 2;

	private static Simulator simulator = null;
	
	private double [] timesOfPVT = {
			/*	 The protocol for Study B was equivalent to that of the night shift condition in Study A
			 * , except for an extra baseline day (added between days 1 and 2) 
			 * and an extra restart day (added between days 7 and 8). 
			 * So 24 is added to the first 5 days of Study A and 48 (for 2 days delay) is added to the second 5 days.  
			 * */
			45.0 +24, 48.0 +24, 51.0 +24, 54.0 +24, //day3 
			69.0 +24, 72.0 +24, 75.0 +24, 78.0 +24, //day4
			93.0 +24, 96.0 +24, 99.0 +24, 102.0+24, //day5
			117.0+24, 120.0+24, 123.0+24, 126.0+24, //day6
			141.0+24, 144.0+24, 147.0+24, 150.0+24, //day7
			                                        
			189.0+48, 192.0+48, 195.0+48, 198.0+48, //day11
			213.0+48, 216.0+48, 219.0+48, 222.0+48, //day12
			237.0+48, 240.0+48, 243.0+48, 246.0+48, //day13
			261.0+48, 264.0+48, 267.0+48, 270.0+48, //day14
			285.0+48, 288.0+48, 291.0+48, 294.0+48  //day15
			
	};
	
	int simulationNumber = 0;
	double simulationStartTime =0;
	private Vector<Simulation> simulations  = new Vector<Simulation>();
	private int numberOfSessions;
	

	public DrivingNightB() {
		super();
		nearLabel = new JLabel(".");
		carLabel = new JLabel("X");
		keypad = new JLabel("*");
	}

	@Override
	public void start() {
		currentSimulation = new Simulation();
		
		getModel().getFatigue().setFatigueHour(timesOfPVT[simulationNumber]);
		getModel().getFatigue().startFatigueSession();

//		if (getModel().getRealTime()) {
//			if (simulator == null)
//				simulator = new Simulator();
//			simulator.useSimulation(simulation);
//			setLayout(new BorderLayout());
//			add(simulator, BorderLayout.CENTER);
//			setVisible(false); // trigger OpenGL init
//			setVisible(true);
//		} else{
		
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
		
//	}

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
		if (time-simulationStartTime <= simulationDurarion) {
			currentSimulation.getEnvironment().setTime(time);
			currentSimulation.update();
			updateVisuals();
			if(simulator != null)
				simulator.repaint();
			
		} else{
			
			
			simulationNumber++;
			// go to the next simulation or stop the model
			if (simulationNumber< timesOfPVT.length){
				simulations.add(currentSimulation);
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

				getModel().getVision().addVisual("near", "near", "near", nearLabel.getX(), nearLabel.getY(), 1, 1, 10);
				getModel().getVision().addVisual("car", "car", "car", carLabel.getX(), carLabel.getY(), 1, 1, 100);
				getModel().getVision().addVisual("keypad", "keypad", "keypad", keypad.getX(), keypad.getY(), 1, 1);


			}else{
				simulations.add(currentSimulation);
				getModel().stop();
			}
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

	@Override
	public void finish() {
		simulator.stop();
	}

	public int numberOfSimulations() {
		return 1;
	}

	public static Image getImage(final String name) {
		URL url = DrivingNightB.class.getResource("images/" + name);
		return Toolkit.getDefaultToolkit().getImage(url);
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		
		int numberOfSimulations = timesOfPVT.length;
		Values[] totalLatDev = new Values[numberOfSimulations];
		Values[] totalLatVel = new Values[numberOfSimulations];
		Values[] totalbrakeRT = new Values[numberOfSimulations];
		Values[] totalheadingError = new Values[numberOfSimulations];
		Values[] totalSpeedDev = new Values[numberOfSimulations];
		Values[] totalSTEX3 = new Values[numberOfSimulations];
		Values[] totalSteeringDev = new Values[numberOfSimulations];
		
		for (int i = 0; i < numberOfSimulations; i++) {
			totalLatDev[i] = new Values();
			totalLatVel[i] = new Values();
			totalbrakeRT[i] = new Values();
			totalheadingError[i] = new Values();
			totalSpeedDev[i] = new Values();
			totalSTEX3[i] = new Values();
			totalSteeringDev[i] = new Values();
		}
		
		
		for (Task taskCast : tasks) {
			DrivingNightB task = (DrivingNightB) taskCast;
			for (int i = 0; i < numberOfSessions; i++) {
				Results results =  task.simulations.elementAt(i).getResults();
				totalLatDev[i].add(results.taskLatDev);
				totalLatVel[i].add(results.taskLatVel);
				totalbrakeRT[i].add(results.brakeRT);
				totalheadingError[i].add(results.headingError);
				totalSpeedDev[i].add(results.taskSpeedDev);
				totalSTEX3[i].add(results.STEX3);
				totalSteeringDev[i].add(results.taskSteeringDev);		
			}
		}

		DecimalFormat df2 = new DecimalFormat("#.00");
		DecimalFormat df3 = new DecimalFormat("#.000");
		
		getModel().output("******* Average LatDev for time points **********\n");
		getModel().output("Day\t21:00\t00:00\t03:00\t06:00 " );
		for (int i = 0; i < 5; i++) {	
			getModel().output((i+2)+"\t"+totalLatDev[i*4].mean()+"\t"+totalLatDev[i*4+1].mean()+"\t"
					+totalLatDev[i*4+2].mean()+"\t"+totalLatDev[i*4+3].mean());	
		}
		getModel().output("* 34 h break *");
		for (int i = 5; i < 10; i++) {	
			getModel().output((i+4)+"\t"+totalLatDev[i*4].mean()+"\t"+totalLatDev[i*4+1].mean()+"\t"
					+totalLatDev[i*4+2].mean()+"\t"+totalLatDev[i*4+3].mean());
		}

		getModel().output("******* Average STEX3 for time points **********\n");
		getModel().output("Day\t21:00\t00:00\t03:00\t06:00 " );
		for (int i = 0; i < 5; i++) {	
			getModel().output((i+2)+"\t"+totalSTEX3[i*4].mean()+"\t"+totalSTEX3[i*4+1].mean()+"\t"
					+totalSTEX3[i*4+2].mean()+"\t"+totalSTEX3[i*4+3].mean());	
		}
		getModel().output("* 34 h break *");
		for (int i = 5; i < 10; i++) {	
			getModel().output((i+4)+"\t"+totalSTEX3[i*4].mean()+"\t"+totalSTEX3[i*4+1].mean()+"\t"
					+totalSTEX3[i*4+2].mean()+"\t"+totalSTEX3[i*4+3].mean());
		}
		
		getModel().output("******* Average STEX3 for time points **********\n");
		getModel().output("Day\t21:00\t00:00\t03:00\t06:00 " );
		for (int i = 0; i < 5; i++) {	
			getModel().output((i+2)+"\t"+totalSteeringDev[i*4].mean()+"\t"+totalSteeringDev[i*4+1].mean()+"\t"
					+totalSteeringDev[i*4+2].mean()+"\t"+totalSteeringDev[i*4+3].mean());	
		}
		getModel().output("* 34 h break *");
		for (int i = 5; i < 10; i++) {	
			getModel().output((i+4)+"\t"+totalSteeringDev[i*4].mean()+"\t"+totalSteeringDev[i*4+1].mean()+"\t"
					+totalSteeringDev[i*4+2].mean()+"\t"+totalSteeringDev[i*4+3].mean());
		}
		
		getModel().output("******* Average LatVel for time points **********\n");
		getModel().output("Day\t21:00\t00:00\t03:00\t06:00 " );
		for (int i = 0; i < 5; i++) {	
			getModel().output((i+2)+"\t"+totalLatVel[i*4].mean()+"\t"+totalLatVel[i*4+1].mean()+"\t"
					+totalLatVel[i*4+2].mean()+"\t"+totalLatVel[i*4+3].mean());	
		}
		getModel().output("* 34 h break *");
		for (int i = 5; i < 10; i++) {	
			getModel().output((i+4)+"\t"+totalLatVel[i*4].mean()+"\t"+totalLatVel[i*4+1].mean()+"\t"
					+totalLatVel[i*4+2].mean()+"\t"+totalLatVel[i*4+3].mean());
		}
		
		getModel().output("******* Average brakeRT for time points **********\n");
		
		getModel().output("******* Average headingError for time points **********\n");
		
		getModel().output("******* Average SpeedDev for time points **********\n");
		
		
		
		Result result = new Result();
		return result;
		
	}
}
