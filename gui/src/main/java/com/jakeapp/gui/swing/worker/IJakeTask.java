package com.jakeapp.gui.swing.worker;

/**
 * @author studpete
 */
// FIXME: add with data later
public interface IJakeTask extends Runnable {

	/**
	 * JakeTasks implement a hashCode, so we can identify them as they run
	 * @return
	 */
	public int hashCode();
}