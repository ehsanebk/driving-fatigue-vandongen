package actr.tasks;

import java.io.File;

import actr.env.Core;

public class Starter implements actr.env.Starter {

	private final String MODEL_PATH = "model/PVT_driver.actr";

	public void startup(Core core) {
		core.openFrame(new File(MODEL_PATH));
	}
}
