package actr.tasks.drivingPVT;

import java.util.*;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import actr.model.Event;
import actr.model.Symbol;
import actr.task.*;
import actr.tasks.driving.Values;

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

public class DrivingPVTNightPRE extends Task {
	private double PVTduration = 600.0;
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
	private SessionPVT currentSession;
	private Vector<SessionPVT> sessions = new Vector<SessionPVT>();
	
	public DrivingPVTNightPRE() {
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
		stimulusVisibility = false;

		getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
		getModel().getFatigue().startFatigueSession();

		interStimulusInterval = random.nextDouble() * 8 + 2; // A random
		addUpdate(interStimulusInterval);
	}

	@Override
	public void update(double time) {
		currentSession.totalSessionTime = getModel().getTime() - currentSession.startTime;
		
		
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
			addEvent(new Event(getModel().getTime() + 30.0, "task", "update") {
				@Override
				public void action() {
					sleepAttackIndex++;
					if (sleepAttackIndex == currentSession.stimulusIndex && stimulusVisibility == true) {
						label.setVisible(false);
						processDisplay();
						stimulusVisibility = false;
						
						currentSession.RT.add(30000);
						currentSession.timeOfReactionsFromStart.add(getModel().getTime() - currentSession.startTime);
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
		}

		// Starting a new Session
		else {
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
						stimulusVisibility = false;
						sleepAttackIndex = 0;
						currentSession.startTime = getModel().getTime();
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
				currentSession.RT.add(responseTime);
				currentSession.timeOfReactionsFromStart.add(getModel().getTime() - currentSession.startTime);
			}

			label.setVisible(false);
			processDisplay();
			
			interStimulusInterval = random.nextDouble() * 8 + 2; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;
		} else {   // False start situation
			currentSession.RT.add(1);
			currentSession.timeOfReactionsFromStart.add(getModel().getTime() - currentSession.startTime);
			if (getModel().isVerbose())
				getModel().output("False alert happened " + "- Session: " + sessionNumber 
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
			Values[] totallSessionLapsesValues = new Values[numberOfSessions];
			Values[] totallSessionLSNR_apx = new Values[numberOfSessions];

			// allocating memory to the vectors
			for (int i = 0; i < numberOfSessions; i++) {
				totallSessionLapsesValues[i] = new Values();
				totallSessionLSNR_apx[i] = new Values();
			}

			for (Task taskCast : tasks) {
				DrivingPVTNightPRE task = (DrivingPVTNightPRE) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					totallSessionLapsesValues[i].add(task.sessions.get(i).getSessionNumberOfLapses());
					totallSessionLSNR_apx[i].add(task.sessions.get(i).getSessionLSNR_apx());
				}
			}

			DecimalFormat df3 = new DecimalFormat("#.000");

			
			getModel().output("\nAverage Number of lapses in the time points \n");
			getModel().output("Day\t09:00\t12:00\t15:00\t18:00 ");
			for (int i = 0; i < 5; i++) {
				getModel().output((i + 2) + "\t" + totallSessionLapsesValues[i * 4].meanDF3() + "\t"
						+ totallSessionLapsesValues[i * 4 + 1].meanDF3() + "\t" + totallSessionLapsesValues[i * 4 + 2].meanDF3() + "\t"
						+ totallSessionLapsesValues[i * 4 + 3].meanDF3());
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) {
				getModel().output((i + 4) + "\t" + totallSessionLapsesValues[i * 4].meanDF3() + "\t"
						+ totallSessionLapsesValues[i * 4 + 1].meanDF3() + "\t" + totallSessionLapsesValues[i * 4 + 2].meanDF3() + "\t"
						+ totallSessionLapsesValues[i * 4 + 3].meanDF3());
			}
			getModel().output("\n*******************************************\n");
			
			getModel().output("\nAverage LSNR_apx in the time points \n");
			getModel().output("Day\t09:00\t12:00\t15:00\t18:00 ");
			for (int i = 0; i < 5; i++) {
				getModel().output((i + 2) + "\t" + totallSessionLSNR_apx[i * 4].meanDF3() + "\t"
						+ totallSessionLSNR_apx[i * 4 + 1].meanDF3() + "\t" + totallSessionLSNR_apx[i * 4 + 2].meanDF3() + "\t"
						+ totallSessionLSNR_apx[i * 4 + 3].meanDF3());
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) {
				getModel().output((i + 4) + "\t" + totallSessionLSNR_apx[i * 4].meanDF3() + "\t"
						+ totallSessionLSNR_apx[i * 4 + 1].meanDF3() + "\t" + totallSessionLSNR_apx[i * 4 + 2].meanDF3() + "\t"
						+ totallSessionLSNR_apx[i * 4 + 3].meanDF3());
			}
			getModel().output("\n*******************************************\n");
			
			/////  Outputting the blocks ////
			Values[][] totallBlockLapsesValues = new Values[numberOfSessions][2];
			Values[][] totallBlockLSNR_apx = new Values[numberOfSessions][2];
			
			// allocating memory to the vectors
			for (int i = 0; i < numberOfSessions; i++) {
				for (int j = 0; j < 2; j++) {
					totallBlockLapsesValues[i][j] = new Values();
					totallBlockLSNR_apx[i][j] = new Values();
					
				}
			}

			for (Task taskCast : tasks) {
				DrivingPVTNightPRE task = (DrivingPVTNightPRE) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					for (int j = 0; j < 2; j++) {
						totallBlockLapsesValues[i][j].add(task.sessions.elementAt(i).getBlockLapses(j));
						totallBlockLSNR_apx[i][j].add(task.sessions.elementAt(i).getBlockLSNR_apx(j));
						
					}
				}
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

			
			getModel().output("\n*******************************************\n");
			
			getModel().output("\nAverage Number of block LSNR_apx in the time points \n");
			getModel().output("Day\t09:00\t\t12:00\t\t15:00\t\t18:00\t ");
			getModel().output("   \t1of2\t2of2\t1of2\t2of2\t1of2\t2of2\t1of2\t2of2 ");
			for (int i = 0; i < 5; i++) { getModel().output((i + 2) + "\t" 
					+ totallBlockLSNR_apx[i * 4    ][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 1][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 1][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 2][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 2][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 3][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 3][1].meanDF3() +"\t"
					);
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) { getModel().output((i + 4) + "\t" 
					+ totallBlockLSNR_apx[i * 4    ][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 1][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 1][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 2][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 2][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 3][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 3][1].meanDF3() +"\t"
					);
			}

			/// Outputting the raw data to a file
			
			File dataFile = new File("/Users/Ehsan/OneDrive - Drexel University/Driving Data(Van Dongen)/Result_PVT/RawData/Model_PVT_NightPRE.csv");
			if (!dataFile.exists())
				dataFile.createNewFile();
			PrintWriter fileOut = new PrintWriter(dataFile);
			
			fileOut.println("\n Night PRE \n");
			
			for (Task taskCast : tasks) {
				DrivingPVTNightPRE task = (DrivingPVTNightPRE) taskCast;
				fileOut.print("Session Ave Lapses,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getSessionNumberOfLapses() + ",");
				}
				fileOut.print("\n");
				fileOut.flush();
				
				fileOut.print("B1 Ave Lapses,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getBlockLapses(0) + ",");
				}
				fileOut.print("\n");
				fileOut.flush();
				
				fileOut.print("B2 Ave Lapses,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getBlockLapses(1) + ",");
				}
				fileOut.print("\n");
				fileOut.flush();
				
				fileOut.print("Session Ave LSNR_apx,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getSessionLSNR_apx() + ",");
				}
				fileOut.print("\n");
				fileOut.flush();
				
				fileOut.print("B1 Ave LSNR_apx,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getBlockLSNR_apx(0) + ",");
				}
				fileOut.print("\n");
				fileOut.flush();
				
				fileOut.print("B2 Ave LSNR_apx,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getBlockLSNR_apx(1) + ",");
				}
				
//				fileOut.print("Session Ave RT,");
//				for (int i = 0; i < numberOfSessions; i++) {
//					fileOut.print(task.sessions.get(i).getSessionAveRT() + ",");
//				}
//				
//				fileOut.print("Session Ave Alert RT,");
//				for (int i = 0; i < numberOfSessions; i++) {
//					fileOut.print(task.sessions.get(i).getSessionAveAlertRT() + ",");
//				}
				
				fileOut.print("\n****\n");
				fileOut.flush();
			}
			
			fileOut.close();

			// Writing raw data to file based on sessions
			File rawDataOut = new File("/Users/Ehsan/OneDrive - Drexel University/Driving Data(Van Dongen)/Result_PVT/RawData/Model_PVT_NightPRE(Raw).csv");
			PrintWriter rawOutputCSV = null;
			rawOutputCSV = new PrintWriter(rawDataOut);

			rawOutputCSV.println("\n Night PRE \n");
			for (Task taskCast : tasks) {
				DrivingPVTNightPRE task = (DrivingPVTNightPRE) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					rawOutputCSV.print("session #"+ i+1 + ",");
					for (int j = 0; j < task.sessions.get(i).RT.size(); j++) {
						rawOutputCSV.print((int)task.sessions.get(i).RT.get(j) + ",");
					}
					rawOutputCSV.print("\n");
				}
				rawOutputCSV.print("\n****\n");
				rawOutputCSV.flush();

			}
			rawOutputCSV.close();



			// Writing Numbers to the file based on sessions
			File dataSessionFile = new File("./resultPVT/Night_PRE.txt");
			if (!dataSessionFile.exists())
				dataSessionFile.createNewFile();
			PrintStream dataSession = new PrintStream(dataSessionFile);
			DrivingPVTNightPRE task = (DrivingPVTNightPRE) tasks[0];
			dataSession.println("Night PRE");

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
