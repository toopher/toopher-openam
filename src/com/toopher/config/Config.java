package com.toopher.config;

import java.util.ResourceBundle;

public class Config {
	static public ResourceBundle resourceConf;

	public static void getConfigFile(ResourceBundle rb) {
		try {
			resourceConf = rb;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}