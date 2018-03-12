package it.tredi.msa.test;

import java.security.AccessControlException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ExecutorTest implements Runnable {

	private String command;
	private boolean shutdown = false;

	public ExecutorTest (String s) {

		this.command = s;
	}

	@Override

	public void run() {

		if (!shutdown) {
			System.out.println(Thread.currentThread().getName() + " start. Command = " + command);
			//processCommand();
			System.out.println(Thread.currentThread().getName()+" End.");
		}
	}


	private void processCommand() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void shutdown() {
		shutdown = true;
		Thread.currentThread().interrupt();
	}

	@Override
	public String toString(){
		return this.command;
	}
}
