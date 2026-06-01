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

double callLLMAgent(double rlOrder,String rlReasoning,double rlConfidence)
{/*ALCODESTART::1772751311292*/
// ── Write state.json for Python ───────────────────────────

traceln("[" + tierName + "] ====== callLLMAgent START =====");
    traceln("[" + tierName + "] DEBUG: rlOrder received = " + rlOrder);
    traceln("[" + tierName + "] DEBUG: rlConfidence received = " + rlConfidence);
    traceln("[" + tierName + "] DEBUG: rlReasoning received = " + rlReasoning);
    
    
String stateDir = agentScriptPath.replace("agent.py", "");
String responsePath = stateDir + "response_" + tierName + ".json";
String logPath = stateDir + "log_LLM" + tierName + ".txt";

try {

    // ========== WRITE RL POLICY FILE (ONLY IF RL ORDER > 0) ==========
    if (rlOrder > 0) {
        String rlPolicyPath = stateDir + "rl_policy_" + tierName + ".json";        
        String rlPolicyJson = "{"
		    + "\"rl_order\":" + rlOrder + ","
		    + "\"rl_confidence\":" + rlConfidence + ","
		    + "\"rl_reasoning\":\"" + rlReasoning + "\""
		    + "}";
        java.io.FileWriter rlFw = new java.io.FileWriter(rlPolicyPath);
        rlFw.write(rlPolicyJson);
        rlFw.close();
        traceln("[" + tierName + "] Wrote RL policy file: " + rlPolicyPath);
    }
    
    // Call Python script FIRST (without writing state)
    ProcessBuilder pb = new ProcessBuilder("python", agentScriptPath, tierName);
    pb.redirectOutput(new java.io.File(logPath));
    pb.redirectErrorStream(true);
    Process process = pb.start();

   
    try {
    java.nio.file.Path lp = java.nio.file.Paths.get(logPath);
    String logContent = new String(java.nio.file.Files.readAllBytes(lp));
	    if (logContent.trim().length() > 0) {
	        traceln("[" + tierName + "] Python: " + logContent.trim());
	    }
	} catch (Exception logEx) {
	    /* not critical */
	}

    // Wait up to 10 seconds for response
    boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

    // Always print Python output for debugging
    //if (pythonOutput.length() > 0) {
        //traceln("[" + tierName + "] Python: " + pythonOutput.toString().trim());
    //}

    if (!finished) {
        traceln("[" + tierName + "] WARNING: Python timed out — rule-based fallback");
        process.destroyForcibly();
        double inventoryPosition = inventory - backlog;
        return Math.max(0, orderUpTo - inventoryPosition);
    }

    // Check response file exists
    java.io.File responseFile = new java.io.File(responsePath);
    if (!responseFile.exists()) {
        traceln("[" + tierName + "] WARNING: No response file — rule-based fallback");
        double inventoryPosition = inventory - backlog;
        return Math.max(0, orderUpTo - inventoryPosition);
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
String rlStatePath = stateDir + "state_rl_" + tierName + ".json";
String rlRespPath = stateDir + "response_rl_" + tierName + ".json";
String rlLogPath = stateDir + "log_rl_" + tierName + ".txt";

if (time() < 1.0) {
	new java.io.File(
			stateDir + "prev_rl_" + tierName + ".json")
			.delete();
}
		
String rlStateJson = "{"
    + "\"tier\":\"" + tierName + "\","
    + "\"inventory\":" + inventory + ","
    + "\"backlog\":" + backlog + ","
    + "\"downstreamInventory\":" + downstreamInventory + ","
    + "\"downstreamBacklog\":" + downstreamBacklog + ","
    + "\"retailerInventory\":" + ((Main)getOwner()).retailer.inventory + ","
    + "\"retailerBacklog\":" + ((Main)getOwner()).retailer.backlog + ","
    + "\"distributorInventory\":" + ((Main)getOwner()).distributor.inventory + ","
    + "\"distributorBacklog\":" + ((Main)getOwner()).distributor.backlog + ","
    + "\"manufacturerInventory\":" + ((Main)getOwner()).manufacturer.inventory + ","
    + "\"manufacturerBacklog\":" + ((Main)getOwner()).manufacturer.backlog + ","
    + "\"demandScenario\":" + ((Main)getOwner()).demandScenario + ","
    + "\"simTime\":" + time()
    + "}";

try {
	// Write RL state file
	java.io.FileWriter fw = new java.io.FileWriter(rlStatePath);
	fw.write(rlStateJson);
	fw.close();

	// Call RL agent script
	ProcessBuilder pb = new ProcessBuilder("python", agentScriptPath.replace("agent.py",
					"rl_inference_file.py"),tierName);
	pb.redirectOutput(new java.io.File(rlLogPath));
	pb.redirectErrorStream(true);
	Process process = pb.start();

	boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);

	if (!finished) {
		traceln("[" + tierName+ "] RL agent timed out — rule-based fallback");
		process.destroyForcibly();
		double inventoryPosition = inventory - backlog;
		return Math.max(0, orderUpTo - inventoryPosition);
	}

	// Read log
	try {
		java.nio.file.Path lp = java.nio.file.Paths.get(rlLogPath);
		String logContent = new String(java.nio.file.Files.readAllBytes(lp));
		if (logContent.trim().length() > 0) {
			traceln("[" + tierName + "] " + logContent.trim());
		}
	} catch (Exception logEx) {
		/* not critical */ }

	// Check response file
	if (!new java.io.File(rlRespPath).exists()) {
		traceln("[" + tierName
				+ "] RL response not found — rule-based fallback");
		double inventoryPosition = inventory - backlog;
		return Math.max(0, orderUpTo - inventoryPosition);
	}

	// Read response
	java.nio.file.Path rp = java.nio.file.Paths.get(rlRespPath);
	String responseJson = new String(java.nio.file.Files.readAllBytes(rp));

	agentConfidence = parseJsonDouble(responseJson,"confidence");
	agentReasoning = parseJsonString(responseJson,"reasoning");
	double orderQty = parseJsonDouble(responseJson,"order_quantity");

	traceln("[" + tierName + "] t=" + time()
			+ " -> RL order=" + orderQty + " confidence="
			+ agentConfidence + " | " + agentReasoning);

	return Math.max(0, orderQty);

} catch (Exception e) {
	traceln("[" + tierName + "] ERROR calling RL agent: " + e.getMessage());
	double ip = inventory - backlog;
	return Math.max(0, orderUpTo - ip);
}
/*ALCODEEND*/}

