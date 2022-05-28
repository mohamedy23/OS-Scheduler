import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class CPU {
	private static Object[] memory = new Object[2048];
	private static Queue<PCB> RRQueue = new LinkedList<PCB>();
	private static int blockPointer = 0;// points to beginning of the line codes of the next process
	private static int nProcess = 0;// count number of processes
	private static int quanta = 2;

	public static void scheduleProgram(String fileName) {
		String line = "";
		String output = "";
		int index = 0;
		nProcess++;// number of Process
		// instructionPointer=nProcess*10-10;//start location of the process
		System.out.println("process " + nProcess);
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			int length = 0;// length of the process
			while ((line = bufferedReader.readLine()) != null) {

				System.out.println(line);
				memory[blockPointer + 1 + length] = line;
				length++;
			}

			memory[blockPointer] = new PCB(nProcess, 0, blockPointer + 1, blockPointer,
					(2 * length) + 1 + blockPointer);
			System.out.println("PCB " + memory[blockPointer]);
			RRQueue.add((PCB) memory[blockPointer]);// add PCB to Ready queue
			blockPointer = (2 * length) + blockPointer + 2; // next process code location
			bufferedReader.close();

		} catch (IOException ex) {
			System.out.println("balabezo");
		}

	}

	private static boolean finished(PCB currentProcess) {
		boolean f = currentProcess.PC < currentProcess.start || currentProcess.PC > currentProcess.end
				|| memory[currentProcess.PC] == null || !(memory[currentProcess.PC] instanceof String);
		if (f) {
			currentProcess.state = 3;
		}
		return f;
	}

	public static void runCpu() {
		while (!RRQueue.isEmpty()) {
			PCB currentProcess = RRQueue.peek();
			for (int i = 1; i <= quanta && !finished(currentProcess); i++) {
				if (currentProcess.state == 0) {
					System.out.println("process running " + currentProcess.PID);
					excuteLine((String) memory[currentProcess.PC], currentProcess);
					currentProcess.PC++;
					currentProcess.state = 1;
				} else if (currentProcess.state == 1) {
					excuteLine((String) memory[currentProcess.PC], currentProcess);
					currentProcess.PC++;

				}
			}
			
			if (currentProcess.state == 3||finished(currentProcess)) {
				RRQueue.remove();
				System.out.println(currentProcess.PID + " finished in " + ++currentProcess.numberOfQ + " quanta");
			} else {
				currentProcess.numberOfQ++;
				currentProcess.state = 0;
				RRQueue.remove();
				RRQueue.add(currentProcess);
			}
			System.out.println("------------------------------------------");

		}

	}

	private static void excuteLine(String line, PCB currentProcess) {
		String output = "";
		int index = 0;
		String[] ins = line.split(" ");
		if (ins[0].equals("assign")) {
			if (ins[2].equals("readFile")) {
				String temp = readFile(ins[3], currentProcess);
				assign(ins[1], temp, currentProcess);
			} else {
				assign(ins[1], ins[2], currentProcess);
			}
		} else if (ins[0].equals("readFile")) {
			String temp = readFile(ins[1], currentProcess);
		} else if (ins[0].equals("writeFile")) {
			if (ins[1].equals("readFile")) {
				if (ins[3].equals("readFile")) {
					writeFile(readFile(ins[2], currentProcess), readFile(ins[4], currentProcess), currentProcess);
				} else {
					writeFile(readFile(ins[2], currentProcess), ins[3], currentProcess);
				}
			} else {
				if (ins[2].equals("readFile")) {
					writeFile(ins[1], readFile(ins[3], currentProcess), currentProcess);
				} else {
					writeFile(ins[1], ins[2], currentProcess);
				}
			}
		} else if (ins[0].equals("print")) {
			print(ins[1], currentProcess);
		} else if (ins[0].equals("add")) {

			if (ins[1].equals("readFile")) {
				if (ins[3].equals("readFile")) {
					add(readFile(ins[2], currentProcess), readFile(ins[4], currentProcess), currentProcess);
				} else {
					add(readFile(ins[2], currentProcess), ins[3], currentProcess);
				}
			} else {
				if (ins[2].equals("readFile")) {
					add(ins[1], readFile(ins[3], currentProcess), currentProcess);
				} else {
					add(ins[1], ins[2], currentProcess);
				}
			}

		}

	}

	private static void putVar(String name, Object value, PCB currentProcess) {
		for (int i = currentProcess.end; i >= currentProcess.start; i--) {

			if (memory[i] instanceof Variable) {
				Variable v = (Variable) memory[i];
				if (v.name.equals(name)) {
					v.value = value;
					System.out.println("Write memory index " + i + " be " + memory[i]);
					break;
				}
			} else if (memory[i] == null) {
				memory[i] = new Variable(name, value);
				System.out.println("Write memory index " + i + " be " + memory[i]);
				break;
			}
		}
	}

	private static void assign(String x, String y, PCB currentProcess) {
		if (y.equals("input")) {
			Scanner sc = new Scanner(System.in);
			String temp = sc.nextLine();
			try {
				int t = Integer.parseInt(temp);
				putVar(x, t, currentProcess);
			} catch (NumberFormatException e) {
				putVar(x, temp, currentProcess);
			}
		} else {
			String temp = y;
			try {
				int t = Integer.parseInt(temp);
				putVar(x, t, currentProcess);
			} catch (NumberFormatException e) {
				putVar(x, temp, currentProcess);
			}
		}
	}

	private static boolean containVar(String name, PCB currentProcess) {
		for (int i = currentProcess.end; i >= currentProcess.start; i--) {
			if (memory[i] instanceof Variable) {
				Variable v = (Variable) memory[i];
				if (v.name.equals(name)) {
					return true;
				}
			} else {
				break;
			}
		}
		return false;
	}

	private static Object getVar(String name, PCB currentProcess) {
		if (containVar(name, currentProcess)) {
			for (int i = currentProcess.end; i >= currentProcess.start; i--) {
				if (memory[i] instanceof Variable) {
					Variable v = (Variable) memory[i];
					if (v.name.equals(name)) {
						System.out.println("read memory index " + i + " " + memory[i]);
						return v.value;
					}
				} else {
					break;
				}
			}
		}
		return null;
	}

	private static String readFile(String dis, PCB currentProcess) {
		String fileName = dis;
		Object temp = getVar(dis, currentProcess);
		if (temp != null && temp instanceof String) {
			fileName = (String) temp;
		} else if (dis.equals("input")) {
			Scanner sc = new Scanner(System.in);
			fileName = sc.nextLine();
		}
		String line = "";
		String output = "";
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				output += line + " ";
			}
			bufferedReader.close();
		} catch (IOException ex) {
			System.out.println(fileName + " fi");
			// ex.printStackTrace();
		}
		return output;
	}

	private static void writeFile(String dis, String value, PCB currentProcess) {

		String fileName = dis;
		Object temp = getVar(dis, currentProcess);
		if (temp != null && temp instanceof String) {
			fileName = (String) temp;
		} else if (dis.equals("input")) {
			Scanner sc = new Scanner(System.in);
			fileName = sc.nextLine();
		}

		String data = value;
		Object tempDate = getVar(value, currentProcess);
		if (tempDate != null && tempDate instanceof Integer) {
			data = (Integer) tempDate + "";
		} else if (tempDate != null && tempDate instanceof String) {
			data = (String) tempDate;
		} else if (value.equals("input")) {
			Scanner sc = new Scanner(System.in);
			data = sc.nextLine();
		}

		try {

			FileWriter fileWriter = new FileWriter(fileName, true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.append(" " + data);
			bufferedWriter.close();

		} catch (IOException ex) {
			System.out.println("Error");
		}
	}

	private static void add(String x, String y, PCB currentProcess) {
		Scanner sc = new Scanner(System.in);
		int a = 0;
		int b = 0;
		Object tempA = getVar(x, currentProcess);
		if (tempA != null && tempA instanceof Integer) {
			a = (Integer) tempA;
		} else if (x.equals("input")) {
			a = sc.nextInt();
		} else {
			a = Integer.parseInt(x);
		}
		Object tempB = getVar(y, currentProcess);
		if (tempB != null && tempB instanceof Integer) {
			b = (Integer) tempB;
		} else if (y.equals("input")) {
			b = sc.nextInt();
		} else {
			b = Integer.parseInt(y);
		}
		putVar(x, a + b, currentProcess);

	}

	private static void print(String x, PCB currentProcess) {
		Object temp = getVar(x, currentProcess);
		if (temp != null)
			System.out.println(temp);
		else
			System.out.println(x);
	}

}

class PCB {
	int PID;
	int state;
	int PC;
	int start;
	int end;
	int numberOfQ = 0;

	public PCB(int pID, int state, int pC, int start, int end) {
		PID = pID;
		this.state = state;
		PC = pC;
		this.start = start;
		this.end = end;
	}

	@Override
	public String toString() {
		return "PCB [PID=" + PID + ", state=" + state + ", PC=" + PC + ", start=" + start + ", end=" + end + "]";
	}

}

class Variable {
	String name;
	Object value;

	public Variable(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return "Variable [name=" + name + ", value=" + value + "]";
	}

}
