package jkit.bytecode;

import java.io.*;
import java.io.OutputStream;

public class BinaryWriter extends Writer {
	protected Writer output;
	
	public BinaryWriter(Writer output) {
		this.output = output;
	}
	
	public BinaryWriter(OutputStream output) {
		this.output = new OutputStreamWriter(output);
	}
	
	public void close() throws IOException {
		output.close();
	}
	
	public void flush() throws IOException {
		output.flush();
	}
	
	public void write(char[] cs) throws IOException {
		output.write(cs);
	}
	
	public void write(char[] cs, int off, int length) throws IOException {
		output.write(cs,off,length);
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
