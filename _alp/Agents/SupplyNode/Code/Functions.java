double receiveOrder(double amount)
{/*ALCODESTART::1772352191162*/
incomingOrder = amount;


/*ALCODEEND*/}

double receiveShipment(double amount)
{/*ALCODESTART::1772353228374*/
// Receive incoming stock
inventory += amount;

// Calculate total demand to fulfil
double demand = incomingOrder + backlog;

// Ship what you can
double shipped = Math.min(demand, inventory);
inventory -= shipped;

// Whatever couldn't be shipped becomes backlog
backlog = Math.max(0, demand - shipped);

incomingOrder = 0;

// Forward shipment downstream
if (downstreamNode != null) {
    downstreamNode.receiveShipment(shipped);
}
/*ALCODEEND*/}

double callLLMAgent()
{/*ALCODESTART::1772751311292*/
// ── Write state.json for Python ───────────────────────────
String stateDir     = agentScriptPath.replace("agent.py", "");
String responsePath = stateDir + "response_" + tierName + ".json";

try {
    // Call Python script FIRST (without writing state)
    ProcessBuilder pb = new ProcessBuilder("python", agentScriptPath, tierName);
    pb.redirectErrorStream(true);
    Process process = pb.start();

    // ── Capture Python output ──────────────────────────────
    java.io.BufferedReader reader = new java.io.BufferedReader(
        new java.io.InputStreamReader(process.getInputStream()));
    StringBuilder pythonOutput = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
        pythonOutput.append(line).append("\n");
    }

    // Wait up to 10 seconds for response
    boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

    // Always print Python output for debugging
    if (pythonOutput.length() > 0) {
        traceln("[" + tierName + "] Python: " + pythonOutput.toString().trim());
    }

    if (!finished) {
        traceln("[" + tierName + "] WARNING: Python timed out — rule-based fallback");
        process.destroyForcibly();
        return Math.max(0, orderUpTo - (inventory - backlog));
    }

    // Check response file exists
    java.io.File responseFile = new java.io.File(responsePath);
    if (!responseFile.exists()) {
        traceln("[" + tierName + "] WARNING: No response file — rule-based fallback");
        return Math.max(0, orderUpTo - (inventory - backlog));
    }

    // Read response file
    java.nio.file.Path rPath = java.nio.file.Paths.get(responsePath);
    String responseJson = new String(java.nio.file.Files.readAllBytes(rPath));

    // Parse response
    agentConfidence = parseJsonDouble(responseJson, "confidence");
    agentReasoning  = parseJsonString(responseJson, "reasoning");
    double orderQty = parseJsonDouble(responseJson, "order_quantity");

    traceln("[" + tierName + "] t=" + time() +
            " → LLM order=" + orderQty +
            " confidence=" + agentConfidence +
            " | " + agentReasoning);

    return Math.max(0, orderQty);

} catch (Exception e) {
    traceln("[" + tierName + "] ERROR: " + e.getMessage());
    return Math.max(0, orderUpTo - (inventory - backlog));
}
/*ALCODEEND*/}

double parseJsonDouble(String json,String key)
{/*ALCODESTART::1772751764897*/
try {
    String search = "\"" + key + "\":";
    int idx = json.indexOf(search);
    if (idx == -1) return 0.0;
    int start = idx + search.length();
    int end = json.indexOf(",", start);
    if (end == -1) end = json.indexOf("}", start);
    return Double.parseDouble(json.substring(start, end).trim());
} catch (Exception e) {
    return 0.0;
}
/*ALCODEEND*/}

String parseJsonString(String json,String key)
{/*ALCODESTART::1772751770055*/
try {
    String search = "\"" + key + "\":\"";
    int idx = json.indexOf(search);
    if (idx == -1) return "";
    int start = idx + search.length();
    int end = json.indexOf("\"", start);
    return json.substring(start, end);
} catch (Exception e) {
    return "";
}
/*ALCODEEND*/}

double callRLAgent()
{/*ALCODESTART::1775404722808*/
// ── callRLAgent() — Add this function to SupplyNode
// ───────────────────────
// Called from orderCycle when autonomyCondition == 4

// Delete prev files at start of new episode

// ── Write tier-specific state file for RL agent
// ───────────────────────────
String stateDir = agentScriptPath.replace("agent.py", "");
String rlStatePath = stateDir + "state_rl_" + tierName
		+ ".json";
String rlRespPath = stateDir + "response_rl_" + tierName
		+ ".json";
String rlLogPath = stateDir + "log_rl_" + tierName + ".txt";

if (time() < 1.0) {
	new java.io.File(
			stateDir + "prev_rl_" + tierName + ".json")
			.delete();
}

// Get system-wide state for SGA
Main main = (Main) getOwner();
double systemTotalBacklog = main.retailer.backlog
		+ main.distributor.backlog
		+ main.manufacturer.backlog;
double systemTotalInventory = main.retailer.inventory
		+ main.distributor.inventory
		+ main.manufacturer.inventory;
double totalShipped = main.retailer.totalShipped
		+ main.distributor.totalShipped
		+ main.manufacturer.totalShipped;
double totalReceived = main.retailer.totalReceived
		+ main.distributor.totalReceived
		+ main.manufacturer.totalReceived;
double systemSL = totalReceived > 0
		? totalShipped / totalReceived
		: 1.0;

String rlStateJson = String.format(
		"{\"tier\":\"%s\",\"inventory\":%.2f,\"backlog\":%.2f,"
				+ "\"lastDemand\":%.2f,\"leadTime\":%d,\"orderUpTo\":%d,"
				+ "\"simTime\":%.2f,"
				+ "\"downstreamInventory\":%.2f,\"downstreamBacklog\":%.2f,"
				+ "\"systemTotalBacklog\":%.2f,\"systemTotalInventory\":%.2f,"
				+ "\"systemServiceLevel\":%.4f,\"shippedLast\":%.2f}",
		tierName, inventory, backlog, lastDemand, leadTime,
		orderUpTo, time(), downstreamInventory,
		downstreamBacklog, systemTotalBacklog,
		systemTotalInventory, systemSL, shipped);

try {
	// Write RL state file
	java.io.FileWriter fw = new java.io.FileWriter(
			rlStatePath);
	fw.write(rlStateJson);
	fw.close();

	// Call RL agent script
	ProcessBuilder pb = new ProcessBuilder("python",
			agentScriptPath.replace("agent.py",
					"train_offline.py"),
			tierName);
	pb.redirectOutput(new java.io.File(rlLogPath));
	pb.redirectErrorStream(true);
	Process process = pb.start();

	boolean finished = process.waitFor(30,
			java.util.concurrent.TimeUnit.SECONDS);

	if (!finished) {
		traceln("[" + tierName
				+ "] RL agent timed out — rule-based fallback");
		process.destroyForcibly();
		double ip = inventory - backlog;
		return Math.max(0, orderUpTo - ip);
	}

	// Read log
	try {
		java.nio.file.Path lp = java.nio.file.Paths
				.get(rlLogPath);
		String logContent = new String(
				java.nio.file.Files.readAllBytes(lp));
		if (logContent.trim().length() > 0) {
			traceln("[" + tierName + "] "
					+ logContent.trim());
		}
	} catch (Exception logEx) {
		/* not critical */ }

	// Check response file
	if (!new java.io.File(rlRespPath).exists()) {
		traceln("[" + tierName
				+ "] RL response not found — rule-based fallback");
		double ip = inventory - backlog;
		return Math.max(0, orderUpTo - ip);
	}

	// Read response
	java.nio.file.Path rp = java.nio.file.Paths
			.get(rlRespPath);
	String responseJson = new String(
			java.nio.file.Files.readAllBytes(rp));

	agentConfidence = parseJsonDouble(responseJson,
			"confidence");
	agentReasoning = parseJsonString(responseJson,
			"reasoning");
	double orderQty = parseJsonDouble(responseJson,
			"order_quantity");

	traceln("[" + tierName + "] t=" + time()
			+ " -> RL order=" + orderQty + " confidence="
			+ agentConfidence + " | " + agentReasoning);

	return Math.max(0, orderQty);

} catch (Exception e) {
	traceln("[" + tierName + "] ERROR calling RL agent: "
			+ e.getMessage());
	double ip = inventory - backlog;
	return Math.max(0, orderUpTo - ip);
}
/*ALCODEEND*/}

