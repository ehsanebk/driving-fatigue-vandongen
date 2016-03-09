package actr.tasks;

import java.io.File;
import actr.env.Core;

public class Starter implements actr.env.Starter {
	
	public void startup(Core core) {
		//core.openFrame(new File("model/Driving.actr"));
		core.openFrame(new File("model/PVT_driver.actr"));
	}
}
