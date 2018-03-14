package actr.tasks.drivingPVT;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import actr.model.Event;
import actr.model.Symbol;
import actr.task.*;
import actr.tasks.test.fatigue.Values;

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

public class DrivingPVTNightA_OLD extends Task {
	private TaskLabel label;
	private double lastTime = 0;
	private String stimulus = "\u2588";
	private double interStimulusInterval = 0.0;
	private Boolean stimulusVisibility = false;
	private String response = null;
	private double responseTime = 0;
	// the following two variable are for handling sleep attacks
	private int sleepAttackIndex = 0;
	private double PVTduration = 600.0;
	Random random;
	
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
	int sessionNumber = 0; // starts from 0
	private Session currentSession;
	private Vector<Session> sessions = new Vector<Session>();

	@SuppressWarnings("unused")
	private PrintStream data;

	class Session {
		double startTime = 0;
		int falseStarts = 0;
		int alertRosponses = 0;
		Values reactionTimes = new Values();
		// Alert responses (150-500 ms,10 ms intervals )
		int alertResponseSpread[] = new int[35]; 
		double totalSessionTime = 0;
		int lapses = 0;
		int sleepAttacks = 0;
		int stimulusIndex = 0;
		int responses = 0; // number of responses, this can be diff from the
		// stimulusIndex because of false resonces
		double responseTotalTime = 0;
	}

	public DrivingPVTNightA_OLD() {
		super();
		label = new TaskLabel("", 200, 150, 40, 20);
		add(label);
		label.setVisible(false);
	}

	@Override
	public void start() {
		random = new Random();
		lastTime = 0;
		currentSession = new Session();
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

		if (currentSession.totalSessionTime <= PVTduration) {
			label.setText(stimulus);
			label.setVisible(true);
			processDisplay();
			stimulusVisibility = true;
			if (getModel().isVerbose())
				getModel().output("!!!!! Stimulus !!!!!");

			lastTime = getModel().getTime();

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
						// when sleep attack happens we add to the number of responses (NOT DOING IT FOR NOW)
						// currentSession.numberOfResponses++; 
						getModel().output("Sleep attack at session time  ==> " + (getModel().getTime() - currentSession.startTime)
								+ " model time :" + getModel().getTime());
						getModel().output("Stimulus index in the session ==> " + currentSession.stimulusIndex );
						
						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						fatigueResetPercentage();
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
					}
					repaint();
				}
			});
		}

		// Starting a new Session
		else {
			sessions.add(currentSession);
			sessionNumber++;
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("none"));
			// go to the next session or stop the model
			if (sessionNumber < timesOfPVT.length) {
				addEvent(new Event(getModel().getTime() + 60.0, "task", "update") {
					@Override
					public void action() {
						currentSession = new Session();
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

			if (response != null) // && response.equals("spc"))
			{
				currentSession.responses++;
				currentSession.responseTotalTime += responseTime;
				currentSession.reactionTimes.add(responseTime);
			}

			label.setVisible(false);
			processDisplay();

			Random random = new Random();
			interStimulusInterval = random.nextDouble() * 8 + 1; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;

			if (responseTime < .150) {
				currentSession.falseStarts++;
			} else if (responseTime > .150 && responseTime <= .500) {
				// making the array for alert reaction times
				currentSession.alertResponseSpread[(int) ((responseTime - .150) * 100)]++; 
				currentSession.alertRosponses++;
			} else if (responseTime > .500 && responseTime < 30.0) {
				currentSession.lapses++;
			}
		} else {
			currentSession.responses++;
			currentSession.falseStarts++;
			currentSession.reactionTimes.add(0);
			
			if (getModel().isVerbose())
				getModel().output("False alert happened " + "- Session: " + sessionNumber //+ " Block:" + (currentSession.blocks.size() + 1)
						+ "   time of session : " + (getModel().getTime() - currentSession.startTime));
		}

	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {

		try {

			int numberOfSessions = timesOfPVT.length;
			Values[] totallLapsesValues = new Values[numberOfSessions];
			Values[] totallFalseAlerts = new Values[numberOfSessions];
			Values[] totallSleepAtacks = new Values[numberOfSessions];
			Values[] totallAlertResponces = new Values[numberOfSessions];
			Values[][] totallAlertResponcesSpread = new Values[numberOfSessions][35];
			Values[] totallResponsesNumber = new Values[numberOfSessions];

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
				totallResponsesNumber[i] = new Values();
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
				DrivingPVTNightA_OLD task = (DrivingPVTNightA_OLD) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					totallFalseAlerts[i].add(task.sessions.elementAt(i).falseStarts);
					totallLapsesValues[i].add(task.sessions.get(i).lapses);
					totallSleepAtacks[i].add(task.sessions.get(i).sleepAttacks);
					totallAlertResponces[i].add(task.sessions.get(i).alertRosponses);
					totallResponsesNumber[i].add(task.sessions.get(i).responses);
					for (int j = 0; j < 35; j++) {
						totallAlertResponcesSpread[i][j].add(task.sessions.get(i).alertResponseSpread[j]);
					}

					totallProportionFalseAlerts[i]
							.add((double) task.sessions.get(i).falseStarts / task.sessions.get(i).responses);
					totallProportionLapsesValues[i]
							.add((double) task.sessions.get(i).lapses / task.sessions.get(i).responses);
					totallProportionSleepAtacks[i]
							.add((double) task.sessions.get(i).sleepAttacks / task.sessions.get(i).responses);
					totallProportionAlertRresponces[i]
							.add((double) task.sessions.get(i).alertRosponses / task.sessions.get(i).responses);
					for (int j = 0; j < 35; j++) {
						totallProportionAlertResponcesSpread[i][j].add(
								(double) task.sessions.get(i).alertResponseSpread[j] / task.sessions.get(i).responses);
					}
				}
			}

			DecimalFormat df3 = new DecimalFormat("#.000");

			// getModel().output("******* Proportion of Responses
			// **********\n");
			// getModel()
			// .output("#\tFS "
			// + " --------------------------- Alert Responses
			// --------------------------- "
			// + " Alert Responses "
			// + " --------------------------- Alert Responses
			// ---------------------------- "
			// + "L SA");

			getModel().output("******* Average Proportion of Responses **********\n");
			getModel().output("#\tFS\t" + "AR\t " + "L\t" + "SA");

			// double[] AlertResponsesProportion = new double[35];
			for (int s = 0; s < numberOfSessions; s++) {
				// for (int i = 0; i < 35; i++)
				// AlertResponsesProportion[i] =
				// totallProportionAlertResponcesSpread[s][i].mean();

				getModel().output(s + "\t" + df3.format(totallProportionFalseAlerts[s].mean()) + "\t"
						// + Utilities.toString(AlertResponsesProportion) + " "
						+ df3.format(totallProportionAlertRresponces[s].mean()) + "\t"
						+ df3.format(totallProportionLapsesValues[s].mean()) + "\t"
						+ df3.format(totallProportionSleepAtacks[s].mean()));
			}

			getModel().output("\nAverage Number of lapses in the time points \n");
			getModel().output("Day\t21:00\t00:00\t03:00\t06:00 ");
			for (int i = 0; i < 5; i++) {
				getModel().output((i + 2) + "\t" + totallLapsesValues[i * 4].mean() + "\t"
						+ totallLapsesValues[i * 4 + 1].mean() + "\t" + totallLapsesValues[i * 4 + 2].mean() + "\t"
						+ totallLapsesValues[i * 4 + 3].mean());
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) {
				getModel().output((i + 4) + "\t" + totallLapsesValues[i * 4].mean() + "\t"
						+ totallLapsesValues[i * 4 + 1].mean() + "\t" + totallLapsesValues[i * 4 + 2].mean() + "\t"
						+ totallLapsesValues[i * 4 + 3].mean());
			}
			getModel().output("\n*******************************************\n");

//			File dataFile = new File("./result/BioMathValuesNightA.txt");
//			if (!dataFile.exists())
//				dataFile.createNewFile();
//			PrintStream data = new PrintStream(dataFile);
//
//			for (int h = 0; h < timesOfPVT[timesOfPVT.length - 1]; h++) {
//				data.println(h + "\t" + df3.format(getModel().getFatigue().getBioMathModelValueforHour(h)));
//			}
//			data.close();

			for (Task taskCast : tasks) {
				DrivingPVTNightA_OLD task = (DrivingPVTNightA_OLD) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					getModel().outputInLine(task.sessions.get(i).reactionTimes.toString() + "\n");
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		Result result = new Result();
		return result;
	}

}
