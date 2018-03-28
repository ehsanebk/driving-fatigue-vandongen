package actr.tasks.drivingPVT;

import java.util.*;
import java.io.File;
import java.io.PrintStream;
import java.text.DecimalFormat;
import actr.model.Event;
import actr.model.Symbol;
import actr.task.*;
import actr.tasks.drivingPVT.SessionPVT.Block;

/**
 * Model of PVT test and Fatigue mechanism
 * 
 * Paper: Efficient driver drowsiness detection at moderate levels of drowsiness
 * 
 * Pia M. Forsmana, Bryan J. Vilaa,b, Robert A. Short c, Christopher G. Mott d,
 * Hans P.A. Van Dongena,
 * 
 * @author Ehsan Khosroshahi
 */

public class DrivingPVTDayPOST extends Task {
	private double PVTduration = 600.0;
	private double[] timesOfPVT = {
			//
			57.0+1, 60.0+1, 63.0+1, 66.0+1, // day2
			81.0+1, 84.0+1, 87.0+1, 90.0+1, // day3
			105.0+1, 108.0+1, 111.0+1, 114.0+1, // day4
			129.0+1, 132.0+1, 135.0+1, 138.0+1, // day5
			153.0+1, 156.0+1, 159.0+1, 162.0+1, // day6

			201.0+1, 204.0+1, 207.0+1, 210.0+1, // day9
			225.0+1, 228.0+1, 231.0+1, 234.0+1, // day10
			249.0+1, 252.0+1, 255.0+1, 258.0+1, // day11
			273.0+1, 276.0+1, 279.0+1, 282.0+1, // day12
			297.0+1, 300.0+1, 303.0+1, 306.0+1 // day13

	};
	
	private TaskLabel label;
	private double lastTime = 0;
	private String stimulus = "\u2588";
	private double interStimulusInterval = 0.0;
	private Boolean stimulusVisibility = false;
	private String response = null;
	private double responseTime = 0;
	private int sleepAttackIndex = 0; // the variable for handling sleep attacks
	Random random;
	
	int sessionNumber = 0; // starts from 0
	private Block currentBlock;
	private SessionPVT currentSession;
	private Vector<SessionPVT> sessions = new Vector<SessionPVT>();

	private PrintStream data;

	public DrivingPVTDayPOST() {
		super();
		label = new TaskLabel("", 200, 150, 40, 20);
		add(label);
		label.setVisible(false);
	}

	@Override
	public void start() {
		random = new Random();
		lastTime = 0;
		currentSession = new SessionPVT();
		currentBlock = currentSession.new Block();
		stimulusVisibility = false;

		getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
		getModel().getFatigue().startFatigueSession();

		interStimulusInterval = random.nextDouble() * 8 + 2; // A random
		addUpdate(interStimulusInterval);

//		try {
//			File dataFile = new File("./model/data.txt");
//			if (!dataFile.exists())
//				dataFile.createNewFile();
//			data = new PrintStream(dataFile);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	}

	@Override
	public void update(double time) {
		currentSession.totalSessionTime = getModel().getTime() - currentSession.startTime;
		currentBlock.totalBlockTime = getModel().getTime() - currentBlock.startTime;
		
		if (currentSession.totalSessionTime <= PVTduration) {
			label.setText(stimulus);
			label.setVisible(true);
			processDisplay();
			stimulusVisibility = true;
			if (getModel().isVerbose())
				getModel().output("!!!!! Stimulus !!!!!");

			lastTime = getModel().getTime(); // when the stimulus has happened

			// Handling the sleep attacks -- adding an event in 30 s to see if
			// the current stimulus is still on
			currentSession.stimulusIndex++;
			addEvent(new Event(getModel().getTime() + 30.0, "task", "update") {
				@Override
				public void action() {
					sleepAttackIndex++;
					if (sleepAttackIndex == currentSession.stimulusIndex && stimulusVisibility == true) {
						label.setVisible(false);
						processDisplay();
						stimulusVisibility = false;
						currentSession.sleepAttacks++;
						currentBlock.sleepAttacks++;
						// when sleep attack happens we add to the number of responses (NOT DOING IT FOR NOW)
						// currentSession.numberOfResponses++; 
						getModel().output("Sleep attack at session time  ==> " + (getModel().getTime() - currentSession.startTime)
								+ " model time :" + getModel().getTime());
						getModel().output("Stimulus index in the session ==> " + currentSession.stimulusIndex );
						
						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						fatigueResetPercentage(); // reseting the system
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
					}
					repaint();
				}
			});

			// Handling the 5-min blocks
			// adding a new block
			if (currentBlock.totalBlockTime >= 300 ) {
				currentSession.blocks.add(currentBlock);
				currentBlock = currentSession.new Block();
				currentBlock.startTime = currentSession.startTime + currentSession.blockIndex * 300.0;
				currentSession.blockIndex++;
			}
		}

		// Starting a new Session
		else {
			currentSession.blocks.add(currentBlock);
			currentSession.bioMathValue = getModel().getFatigue().getBioMathModelValueforHour(timesOfPVT[sessionNumber]);
			currentSession.timeAwake = getModel().getFatigue().getTimeAwake(timesOfPVT[sessionNumber]);
			currentSession.timeOfTheDay = timesOfPVT[sessionNumber] % 24;
			sessions.add(currentSession);
			sessionNumber++;
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("none"));
			// go to the next session or stop the model
			if (sessionNumber < timesOfPVT.length) {
				addEvent(new Event(getModel().getTime() + 60.0, "task", "update") {
					@Override
					public void action() {
						currentSession = new SessionPVT();
						currentBlock = currentSession.new Block();
						stimulusVisibility = false;
						sleepAttackIndex = 0;
						currentSession.startTime = getModel().getTime();
						currentBlock.startTime = getModel().getTime();
						getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
						getModel().getFatigue().startFatigueSession();
						
						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
					}
				});
			} else {
				getModel().stop();
			}
		}
	}

	@Override
	public void eval(Iterator<String> it) {
		it.next(); // (
		String cmd = it.next();
		if (cmd.equals("fatigue-reset-percentage")) {
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
	public void typeKey(char c) {
		if (stimulusVisibility == true) {
			response = c + "";
			responseTime = getModel().getTime() - lastTime;
			responseTime *= 1000; //Changing the scale to Millisecond
			
			if (response != null) {
				currentSession.numberOfResponses++;
				currentBlock.numberOfResponses++;
				currentSession.responseTotalTime += responseTime;
				currentSession.reactionTimes.add(responseTime);
				currentBlock.blockReactionTimes.add(responseTime);
			}

			label.setVisible(false);
			processDisplay();
			
			interStimulusInterval = random.nextDouble() * 8 + 2; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;
		} else {   // False start situation
			currentSession.reactionTimes.add(1);
			currentBlock.blockReactionTimes.add(1);
			if (getModel().isVerbose())
				getModel().output("False alert happened " + "- Session: " + sessionNumber + " Block:" + (currentSession.blocks.size() + 1)
						+ "   time of session : " + (getModel().getTime() - currentSession.startTime));
		}
	}

//	@Override
//	public int analysisIterations() {
// 		return 100;
//	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {

		try {

			int numberOfSessions = timesOfPVT.length;
			Values[] totallLapsesValues = new Values[numberOfSessions];
			Values[] totallFalseAlerts = new Values[numberOfSessions];
			Values[] totallSleepAtacks = new Values[numberOfSessions];
			Values[] totallAlertResponces = new Values[numberOfSessions];
			Values[][] totallAlertResponcesSpread = new Values[numberOfSessions][35];

			Values[] totallProportionLapsesValues = new Values[numberOfSessions];
			Values[] totallProportionFalseAlerts = new Values[numberOfSessions];
			Values[] totallProportionSleepAtacks = new Values[numberOfSessions];
			Values[] totallProportionAlertRresponces = new Values[numberOfSessions];
			Values[][] totallProportionAlertResponcesSpread = new Values[numberOfSessions][35];

			// allocating memory to the vectors
			for (int i = 0; i < numberOfSessions; i++) {
				totallLapsesValues[i] = new Values();
				totallFalseAlerts[i] = new Values();
				totallSleepAtacks[i] = new Values();
				totallAlertResponces[i] = new Values();
				
				totallProportionLapsesValues[i] = new Values();
				totallProportionFalseAlerts[i] = new Values();
				totallProportionSleepAtacks[i] = new Values();
				totallProportionAlertRresponces[i] = new Values();
				for (int j = 0; j < 35; j++) {
					totallAlertResponcesSpread[i][j] = new Values();
					totallProportionAlertResponcesSpread[i][j] = new Values();
				}
			}

			for (Task taskCast : tasks) {
				DrivingPVTDayPOST task = (DrivingPVTDayPOST) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					totallFalseAlerts[i].add(task.sessions.elementAt(i).getNumberOfFalseAlerts());
					totallLapsesValues[i].add(task.sessions.get(i).getNumberOfLapses());
					totallSleepAtacks[i].add(task.sessions.get(i).getNumberOfSleepAttacks());
					totallAlertResponces[i].add(task.sessions.get(i).getNumberOfAlertResponses());
					
					totallProportionFalseAlerts[i].add(task.sessions.get(i).getProportionOfFalseAlert());
					totallProportionSleepAtacks[i].add(task.sessions.get(i).getProportionOfSleepAttacks());
					totallProportionLapsesValues[i].add(task.sessions.get(i).getProportionOfLapses());
					totallProportionAlertRresponces[i].add(task.sessions.get(i).getProportionOfAlertResponses());
				}
			}

			DecimalFormat df3 = new DecimalFormat("#.000");

			getModel().output("******* Average Proportion of Responses **********\n");
			getModel().output("#\tFS\t" + "AR\t " + "L\t" + "SA");

			// double[] AlertResponsesProportion = new double[35];
			for (int s = 0; s < numberOfSessions; s++) {
				// for (int i = 0; i < 35; i++)
				// AlertResponsesProportion[i] =
				// totallProportionAlertResponcesSpread[s][i].mean();

				getModel().output(s + "\t" + totallProportionFalseAlerts[s].meanDF3() + "\t"
						// + Utilities.toString(AlertResponsesProportion) + " "
						+ totallProportionAlertRresponces[s].meanDF3() + "\t"
						+ totallProportionLapsesValues[s].meanDF3() + "\t"
						+ totallProportionSleepAtacks[s].meanDF3() );
			}

			getModel().output("\nAverage Proportion of lapses in the time points \n");
			getModel().output("Day\t09:00\t12:00\t15:00\t18:00 ");
			for (int i = 0; i < 5; i++) {
				getModel().output((i + 2) + "\t" + totallProportionLapsesValues[i * 4].meanDF3() + "\t"
						+ totallProportionLapsesValues[i * 4 + 1].meanDF3() + "\t" + totallProportionLapsesValues[i * 4 + 2].meanDF3() + "\t"
						+ totallProportionLapsesValues[i * 4 + 3].meanDF3());
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) {
				getModel().output((i + 4) + "\t" + totallProportionLapsesValues[i * 4].meanDF3() + "\t"
						+ totallProportionLapsesValues[i * 4 + 1].meanDF3() + "\t" + totallProportionLapsesValues[i * 4 + 2].meanDF3() + "\t"
						+ totallProportionLapsesValues[i * 4 + 3].meanDF3());
			}
			getModel().output("\n*******************************************\n");
			
			getModel().output("\nAverage Number of lapses in the time points \n");
			getModel().output("Day\t09:00\t12:00\t15:00\t18:00 ");
			for (int i = 0; i < 5; i++) {
				getModel().output((i + 2) + "\t" + totallLapsesValues[i * 4].meanDF3() + "\t"
						+ totallLapsesValues[i * 4 + 1].meanDF3() + "\t" + totallLapsesValues[i * 4 + 2].meanDF3() + "\t"
						+ totallLapsesValues[i * 4 + 3].meanDF3());
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) {
				getModel().output((i + 4) + "\t" + totallLapsesValues[i * 4].meanDF3() + "\t"
						+ totallLapsesValues[i * 4 + 1].meanDF3() + "\t" + totallLapsesValues[i * 4 + 2].meanDF3() + "\t"
						+ totallLapsesValues[i * 4 + 3].meanDF3());
			}
			getModel().output("\n*******************************************\n");
			
			/////  Outputting the blocks ////
			Values[][] totallBlockLapsesValues = new Values[numberOfSessions][2];
			Values[][] totallBlockFalseAlerts = new Values[numberOfSessions][2];
			Values[][] totallBlockAlertResponces = new Values[numberOfSessions][2];
			
			Values[][] totallBlockProportionLapsesValues = new Values[numberOfSessions][2];
			Values[][] totallBlockProportionFalseAlerts = new Values[numberOfSessions][2];
			
			// allocating memory to the vectors
			for (int i = 0; i < numberOfSessions; i++) {
				for (int j = 0; j < 2; j++) {
					totallBlockLapsesValues[i][j] = new Values();
					totallBlockFalseAlerts[i][j] = new Values();
					totallBlockAlertResponces[i][j] = new Values();
					
					totallBlockProportionLapsesValues[i][j] = new Values();
					totallBlockProportionFalseAlerts[i][j] = new Values();
				}
			}

			for (Task taskCast : tasks) {
				DrivingPVTDayPOST task = (DrivingPVTDayPOST) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					for (int j = 0; j < 2; j++) {
						totallBlockLapsesValues[i][j].add(task.sessions.elementAt(i).blocks.get(j).getNumberOfLapses());
						totallBlockFalseAlerts[i][j].add(task.sessions.get(i).blocks.get(j).getNumberOfFalseAlerts());
						totallBlockAlertResponces[i][j].add(task.sessions.get(i).blocks.get(j).getNumberOfAlertResponses());
						
						totallBlockProportionFalseAlerts[i][j].add(task.sessions.get(i).blocks.get(j).getProportionOfFalseAlert());
						totallBlockProportionLapsesValues[i][j].add(task.sessions.get(i).blocks.get(j).getProportionOfLapses());
					}
				}
			}



			getModel().output("\nAverage Proportion of block lapses in the time points \n");
			getModel().output("Day\t09:00\t\t12:00\t\t15:00\t\t18:00\t ");
			getModel().output("   \t1of2\t2of2\t1of2\t2of2\t1of2\t2of2\t1of2\t2of2 ");
			for (int i = 0; i < 5; i++) { getModel().output((i + 2) + "\t" 
					+ totallBlockProportionLapsesValues[i * 4][0].meanDF3() 	+ "\t" + totallBlockProportionLapsesValues[i * 4][1].meanDF3() +"\t"
					+ totallBlockProportionLapsesValues[i * 4 + 1][0].meanDF3() + "\t" + totallBlockProportionLapsesValues[i * 4 + 1][1].meanDF3() +"\t"
					+ totallBlockProportionLapsesValues[i * 4 + 2][0].meanDF3() + "\t" + totallBlockProportionLapsesValues[i * 4 + 2][1].meanDF3() +"\t"
					+ totallBlockProportionLapsesValues[i * 4 + 3][0].meanDF3() + "\t" + totallBlockProportionLapsesValues[i * 4 + 3][1].meanDF3() +"\t"
					);
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) { getModel().output((i + 4) + "\t" 
					+ totallBlockProportionLapsesValues[i * 4][0].meanDF3() 	+ "\t" + totallBlockProportionLapsesValues[i * 4][1].meanDF3() +"\t"
					+ totallBlockProportionLapsesValues[i * 4 + 1][0].meanDF3() + "\t" + totallBlockProportionLapsesValues[i * 4 + 1][1].meanDF3() +"\t"
					+ totallBlockProportionLapsesValues[i * 4 + 2][0].meanDF3() + "\t" + totallBlockProportionLapsesValues[i * 4 + 2][1].meanDF3() +"\t"
					+ totallBlockProportionLapsesValues[i * 4 + 3][0].meanDF3() + "\t" + totallBlockProportionLapsesValues[i * 4 + 3][1].meanDF3() +"\t"
					);
			}
			getModel().output("\n*******************************************\n");
			
			getModel().output("\nAverage Number of block lapses in the time points \n");
			getModel().output("Day\t09:00\t\t12:00\t\t15:00\t\t18:00\t ");
			getModel().output("   \t1of2\t2of2\t1of2\t2of2\t1of2\t2of2\t1of2\t2of2 ");
			for (int i = 0; i < 5; i++) { getModel().output((i + 2) + "\t" 
					+ totallBlockLapsesValues[i * 4    ][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 1][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 1][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 2][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 2][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 3][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 3][1].meanDF3() +"\t"
					);
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) { getModel().output((i + 4) + "\t" 
					+ totallBlockLapsesValues[i * 4    ][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 1][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 1][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 2][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 2][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 3][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 3][1].meanDF3() +"\t"
					);
			}
			
			/// Outputting the raw data
//			getModel().output("\n*******************************************\n");
//			
//			for (Task taskCast : tasks) {
//				DrivingPVTNightA task = (DrivingPVTNightA) taskCast;
//				for (int i = 0; i < numberOfSessions; i++) {
//					getModel().outputInLine(task.sessions.get(i).reactionTimes.toString() + "\n");
//					for (int j = 0; j < task.sessions.get(i).blocks.size(); j++) {
//						getModel().outputInLine(task.sessions.get(i).blocks.get(j).blockReactionTimes.toString()+ "\n");
//					}
//					getModel().output("***");
//				}
//				getModel().output("\n***********************************\n");
//			}
			
//			File dataFile = new File("./result/BioMathValuesNightA.txt");
//			if (!dataFile.exists())
//				dataFile.createNewFile();
//			PrintStream data = new PrintStream(dataFile);
//
//			for (int h = 0; h < timesOfPVT[timesOfPVT.length - 1]; h++) {
//				data.println(h + "\t" + df3.format(getModel().getFatigue().getBioMathModelValueforHour(h)));
//			}
			//			data.close();

			// Writing Numbers to the file based on sessions
			File dataSessionFile = new File("./resultPVT/dataSessions_Day_POST.txt");
			if (!dataSessionFile.exists())
				dataSessionFile.createNewFile();
			PrintStream dataSession = new PrintStream(dataSessionFile);
			DrivingPVTDayPOST task = (DrivingPVTDayPOST) tasks[0];
			dataSession.println("Day POST");
			
			dataSession.print("TimeOfDay" + ",");
			for (int i = 0; i < numberOfSessions; i++) 
				dataSession.print(task.sessions.get(i).timeOfTheDay + ",");
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("BioMath" + ",");
			for (int i = 0; i < numberOfSessions; i++)
				dataSession.print(task.sessions.get(i).bioMathValue + ",");
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("AwakeTime" + ",");
			for (int i = 0; i < numberOfSessions; i++)
				dataSession.print(task.sessions.get(i).timeAwake + ",");
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		Result result = new Result();
		return result;
	}

}
