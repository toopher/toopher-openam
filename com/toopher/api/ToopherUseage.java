package com.toopher.api;

import java.util.Date;
import org.json.JSONObject;

import com.toopher.config.Config;

public class ToopherUseage {
	ToopherAPI api = null;
	long DELAY_TIME = 3 * 1000;

	public static void toopherPrintln(String string) {
		System.out.println("---------------------");
		System.out.println("Toopher:--(" + new Date() + ")" + string);
		System.out.println("---------------------");
	}

	public ToopherUseage() {
		api = new ToopherAPI(Config.resourceConf.getString("TOOPHER_KEY"),
				Config.resourceConf.getString("TOOPHER_SECRATE"));

	}

	public JSONObject toopherPairingVerification(String username,
			String pairingPhrase) {
		JSONObject data = new JSONObject();
		try {
			data.put("pairingId", "-");
			data.put("mgs", "Done");
			data.put("flag", false);
			if (api != null && username != null) {
				try {
					PairingStatus pairingStatus = api.pair(pairingPhrase,
							username);
					data.put("pairingId", pairingStatus.id);
					data.put("flag", true);
					data.put("mgs",
							"Authorize pairing on phone and then press return to continue.");
				} catch (RequestError err) {
					data.put("mgs",
							"The pairing phrase was not accepted (reason: "
									+ err.getMessage());
				} catch (Exception ex) {
					data.put("mgs", ex.getMessage());
				}
			} else {
			}
			toopherPrintln("SampleAuth(Toopher I)" + data.get("mgs"));
			if ((Boolean) data.get("flag")) {
				Integer count = new Integer(0);
				JSONObject tempData = null;
				while (count <= 10) {
					tempData = pairingToopher((String) data.get("pairingId"));
					tempData.put("pairingId", data.get("pairingId"));
					tempData.put("mgs", "Not authorize by user.");
					if ((Boolean) tempData.get("pairingIdValid")) {
						if ((Boolean) tempData.get("pairingStatus")) {
							tempData.put("mgs", "Authorize by user..");
							break;
						} else {
							count++;
							Thread.sleep(DELAY_TIME);// 10 sec
						}
					} else {
						tempData.put("mgs", "Eror occured pair again");
						break;
					}
					toopherPrintln("SampleAuth(Toopher II)"
							+ tempData.get("mgs"));
				}
				data = tempData;
				data.put(
						"flag",
						(((Boolean) tempData.get("pairingIdValid")) && ((Boolean) tempData
								.get("pairingStatus"))));
			}
		} catch (Exception e) {
		}
		return data;
	}

	private JSONObject pairingToopher(String pairingId) {
		JSONObject data = new JSONObject();
		try {
			try {
				PairingStatus pairingStatus = api.getPairingStatus(pairingId);
				data.put("pairingIdValid", true);
				data.put("pairingStatus", pairingStatus.enabled);
			} catch (RequestError err) {
				data.put("pairingIdValid", false);
			}
		} catch (Exception e) {
		}
		return data;
	}

	public JSONObject toopherUserAuth(String terminalName,
			String terminalExtra, String pairingId, String oneTimePassword) {
		JSONObject data = new JSONObject();
		try {
			data.put("flag", false);
			try {
				AuthenticationStatus requestStatus = api.authenticate(
						pairingId, terminalName, terminalExtra);
				data.put("requestId", requestStatus.id);
				data.put("flag", true);
				data.put("pairingIdisValid", true);
			} catch (RequestError err) {
				toopherPrintln("Error initiating authentication (reason:"
						+ err.getMessage());
				data.put("pairingIdisValid", false);
			}
			if ((Boolean) (data.get("flag"))) {
				data.put("flag", false);
				data = authenticatingToopher((String) data.get("requestId"),
						oneTimePassword);
			}
		} catch (Exception e) {
		}
		return data;
	}

	private JSONObject authenticatingToopher(String requestId,
			String oneTimePassword) {
		JSONObject data = new JSONObject();
		try {
			data.put("flag", false);
			if (oneTimePassword == null)
				data.put("deniedByUser", false);
			try {
				AuthenticationStatus requestStatus = null;
				int count = oneTimePassword == null ? 0 : 10;
				while (count <= 10) {
					requestStatus = (oneTimePassword == null) ? api
							.getAuthenticationStatus(requestId) : api
							.getAuthenticationByOTPStatus(requestId,
									oneTimePassword);
					if (requestStatus == null) {
						data.put("requestIdisValid", false);
						break;
					} else {
						data.put("requestIdisValid", true);
						data.put("requestStatusPending", requestStatus.pending);
						data.put("timeout", true);
						if (!requestStatus.pending) {
							String automation = requestStatus.automated ? "automatically "
									: "";
							String result = requestStatus.granted ? "granted"
									: "denied";
							toopherPrintln("The request was " + automation
									+ result + "!");
							data.put(
									"deniedByUser",
									(!requestStatus.granted
											&& requestStatus.reason != null && (requestStatus.reason)
											.contains("Manually denied by user")));
							data.put("automate", requestStatus.automated);
							data.put("isGranted", requestStatus.granted);
							data.put("flag", requestStatus.granted);
							data.put("terminalId", requestStatus.terminalId);
							break;
						} else {
							Thread.sleep(DELAY_TIME);
						}
					}
					count++;
				}
			} catch (RequestError err) {
				toopherPrintln("Could not check authentication status (reason:"
						+ err.getMessage());
				data.put("requestIdisValid", false);
			}
		} catch (Exception e) {
		}
		return data;
	}
}
