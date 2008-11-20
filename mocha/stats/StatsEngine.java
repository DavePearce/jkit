package mocha.stats;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import mocha.util.Utils;
import jkit.core.*;
import jkit.core.FlowGraph.*;

public class StatsEngine {
	
	private int infLocalVars;
	private int totalLocalVars;
	private int infFields;
	private int totalFields;
	private String className;
	private PrintStream out;
	
	public StatsEngine(boolean toFile) {
		if(toFile) {
			File f = new File("/home/chris/results.csv");
			try {
				boolean writeHeader = f.exists();
				out = new PrintStream(new FileOutputStream(f, true));
				if(!writeHeader) {
					out.println("ClassName,inflocalvars,totallocalvars," +
					"perclocinferred,inffields,totalfields,percfieldinferred");
				}
				out.flush();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		} else {
			out = null;
		}
	}
	
	public void apply(Clazz owner) {
		reset();
		className = owner.type().toString();
		for(Field f : owner.fields()) {
			if(f.type() instanceof Type.Void) {
				infFields++;
			}
			totalFields++;
		}
		for(Method m : owner.methods()) {
			if(m.code() != null) {
				apply(m, owner);
			}
		}
	}

	private void apply(Method m, Clazz owner) {
		FlowGraph cfg = m.code();
		List<LocalVarDef> lvds = cfg.localVariables();
		for(LocalVarDef lvd : lvds) {
			String name = lvd.name();
			if(!isTempVar(name)) {
				if(!lvd.isParameter()) {
					if(lvd.type() instanceof Type.Void) {
						infLocalVars++;
					} else {
						System.out.println("Uninferred " + Utils.formatMethod(m) + " "+ lvd.name());
					}
					totalLocalVars++;
				}
			}
		}
	}
	
	private boolean isTempVar(String name) {
		return name.startsWith("$") || name.endsWith("$iterator");
	}
	
	public void reset() {
		infLocalVars = 0;
		infFields = 0;
		totalLocalVars = 0;
		totalFields = 0;
	}
	
	public void write() {
		if(out == null) {
			System.out.println(this.toString());
		} else {
			double percInfLocal = (double) infLocalVars / (double) totalLocalVars;
			double percInfField = (double) infFields / (double) totalFields;
			out.print(className + ",");
			out.print(infLocalVars + ",");
			out.print(totalLocalVars + ",");
			if(infLocalVars == 0) {
				out.print(0 + ",");
			} else {
				out.print(percInfLocal + ",");
			}
			out.print(infFields + ",");
			out.print(totalFields + ",");
			if(infFields == 0) {
				out.println(0);
			} else {
				out.println(percInfField);
			}
			out.flush();
		}

	}
	
	public String toString() {
		String s = "";
		double percInfLocal = (double) infLocalVars / (double) totalLocalVars;
		double percInfField = (double) infFields / (double) totalFields;
		s += "== Compilation Results for " + className + " ==\n";
		s += "Inferred Local Vars: " + infLocalVars + "\n";
		s += "Total Local Vars: " + totalLocalVars + "\n";
		s += "Percent Inferred: " + percInfLocal + "\n"; 
		s += "Inferred Fields: " + infFields + "\n";
		s += "Total Fields: " + totalFields + "\n";
		s += "Percent Inferred: " + percInfField;
		return s;
	}

}
