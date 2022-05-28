
public class Launcher {

	public static void main(String[] args) {
		CPU.scheduleProgram("Program 1.txt");
		CPU.scheduleProgram("Program 2.txt");
		CPU.scheduleProgram("Program 3.txt");
		CPU.runCpu();
				
	}

}
