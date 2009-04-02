package jkit.bytecode;

import java.io.*;
import java.io.OutputStream;

public class BinaryOutputStream extends OutputStream {
	protected Writer output;
	
	public BinaryOutputStream(Writer output) {
		this.output = output;
	}
	
	public BinaryOutputStream(OutputStream output) {
		this.output = new OutputStreamWriter(output);
	}
	
	public void write(int i) throws IOException {
		output.write(i);
	}		
	
	protected void write_u1(int w) throws IOException {
		output.write(w & 0xFF);
	}

	protected void write_u2(int w) throws IOException {
		output.write((w >> 8) & 0xFF);
		output.write(w & 0xFF);
	}

	protected void write_u4(int w) throws IOException {
		output.write((w >> 24) & 0xFF);
		output.write((w >> 16) & 0xFF);
		output.write((w >> 8) & 0xFF);
		output.write(w & 0xFF);
	}
}
