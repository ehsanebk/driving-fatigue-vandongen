package actr.tasks;

import java.io.File;
import actr.env.Core;

public class Starter implements actr.env.Starter {
	
	public void startup(Core core) {
		//core.openFrame(new File("model/DrivingNightA.actr"));
		core.openFrame(new File("model/DrivingNightB.actr"));
		//core.openFrame(new File("model/PVT_DriverNightA.actr"));
		//core.openFrame(new File("model/PVT_DriverNightB.actr"));
	}
}
