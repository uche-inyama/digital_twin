void orderCycle()
{/*ALCODESTART::1772359623117*/
// Step 1: Fulfil what was ordered last period
double demand = incomingOrder + backlog;

shipped = Math.min(demand, inventory);

inventory -= shipped;
backlog = Math.max(0, demand - shipped);

// Step 2: Forward shipment downstream with lead time delay
traceln("[" + tierName + "] Step 2 started. shipped = " + shipped +
        ", downstreamNode = " + (downstreamNode != null ? "EXISTS" : "NULL"));

if (downstreamNode != null) {
    double actualShipped;

    if (tierName.equals("Manufacturer") &&
        ((Main)getOwner()).demandScenario == 2 &&
        time() >= 40 && time() <= 50) {
        actualShipped = shipped * 0.2;
        traceln("[Manufacturer] DISRUPTION ACTIVE — shipping only " +
                actualShipped + " of " + shipped + " requested");
    } else {
        actualShipped = shipped;
    }

    traceln("[" + tierName + "] Calling create_deliveryShipment with " +
            actualShipped + " units, leadTime = " + leadTime);
    traceln("[" + tierName + "] leadTime = " + leadTime);

    downstreamNode.create_deliveryShipment(leadTime, actualShipped);
    traceln("[" + tierName + "] Shipment scheduled successfully");

} else {
    traceln("[" + tierName + "] WARNING: downstreamNode is NULL — cannot ship");
}

// Step 3: Refresh downstream state for information sharing
if (downstreamNode != null) {
    downstreamInventory = downstreamNode.inventory;
    downstreamBacklog   = downstreamNode.backlog;
} else {
    downstreamInventory = 0;
    downstreamBacklog   = 0;
}

// Step 4: Calculate and place replenishment order upstream

// Step 4: Calculate and place replenishment order upstream
if (agentEnabled) {

    try {
        String stateDir = agentScriptPath.replace("agent.py", "");
        String statePath = stateDir + "state_" + tierName + ".json";
        
        String stateJson = "{"
            + "\"tier\":\"" + tierName + "\","
            + "\"inventory\":" + inventory + ","
            + "\"backlog\":" + backlog + ","
            + "\"orderUpTo\":" + orderUpTo + ","
            + "\"simTime\":" + time() + ","
            + "\"lastDemand\":" + lastDemand + ","
            + "\"leadTime\":" + leadTime + ","
            + "\"downstreamInventory\":" + downstreamInventory + ","
            + "\"downstreamBacklog\":" + downstreamBacklog + ","
            + "\"informationSharing\":" + informationSharing
            + "}";
        
        java.io.FileWriter fw = new java.io.FileWriter(statePath);
        fw.write(stateJson);
        fw.close();
        traceln("[" + tierName + "] Wrote state: inv=" + inventory + " back=" + backlog + " time=" + time());
    } catch (Exception e) {
        traceln("[" + tierName + "] Error writing state: " + e.getMessage());
    }
    
   // ========== HYBRID MODE ==========
	if (rlEnabled) {
	    double rlOrder = 0;
	    double rlConfidence = 0;
	    String rlReasoning = "";
	
	    try {
	        rlOrder      = callRLAgent();
	        rlConfidence = agentConfidence;
	        rlReasoning  = agentReasoning;
	        traceln("[" + tierName + "] RL DECIDES: order=" + rlOrder +
	                " conf=" + rlConfidence);
	    } catch (Exception e) {
	        traceln("[" + tierName + "] RL not available: " + e.getMessage());
	        rlOrder = Math.max(0, orderUpTo - (inventory - backlog));
	    }
	
	    // RL order capped at tier-specific orderUpTo
	    outgoingOrder = Math.min(rlOrder, orderUpTo);
	
	    // LLM provides reasoning only
	    try {
	        callLLMAgent(rlOrder, rlReasoning, rlConfidence);
	        traceln("[" + tierName + "] LLM EXPLAINS: " + agentReasoning);
	    } catch (Exception e) {
	        traceln("[" + tierName + "] LLM explanation unavailable: " + e.getMessage());
	    }
	
	} else {
	    // LLM only (A2/A3)
	    outgoingOrder = callLLMAgent(0, "", 0);
	    outgoingOrder = Math.min(outgoingOrder, orderUpTo);
	}
	// ========== END HYBRID MODE ==========
	
	// Remove the low confidence fallback entirely for A4
	// Keep only for A2/A3 (non-RL)
	if (!rlEnabled && agentConfidence < 0.70) {
	    double inventoryPosition = inventory - backlog;
	    outgoingOrder = Math.max(0, orderUpTo - inventoryPosition);
	    outgoingOrder = Math.min(outgoingOrder, orderUpTo);
	    traceln("[" + tierName + "] LOW CONFIDENCE (" + agentConfidence +
	            ") — rule-based fallback: " + outgoingOrder);
	}
    
    // HITL — pause for human review if enabled
    boolean crisisDetected = (backlog > main.crisisBacklogThreshold && inventory == 0);
    if (hitlEnabled && (agentConfidence < 0.85 || crisisDetected)) {
        pendingOrder        = outgoingOrder;
        awaitingHumanInput  = true;

        main.hitlTierName      = tierName;
        main.hitlInventory     = inventory;
        main.hitlBacklog       = backlog;
        main.hitlSuggestedOrder = outgoingOrder;
        main.hitlConfidence    = agentConfidence;
        main.hitlReasoning     = agentReasoning;
        main.hitlWeek          = time();
        main.hitlHumanOrder    = outgoingOrder;
        main.humanDecisionAccept = false;

        traceln("[HITL] Pausing at " + tierName +
                " | week=" + (int)time() +
                " | confidence=" + agentConfidence +
                " | crisis=" + crisisDetected +
                " | suggested=" + outgoingOrder);
                
		main.humanDecisionAccept = false;
        getEngine().pause();
        return;
    }

} else {
    // Step 3.5: Apply information sharing to rule-based order
   if (!agentEnabled && informationSharing && downstreamNode != null) {
        // NEW LOGIC: Backlog is PRIORITY
        double excess = Math.max(0, downstreamInventory - orderUpTo);
        double baseOrder = Math.max(0, orderUpTo - inventory);
        
        // Order = MAX(backlog, baseOrder - excess)
        outgoingOrder = Math.max(backlog, baseOrder - excess);
        outgoingOrder = Math.min(outgoingOrder, orderUpTo);
        
        traceln("[" + tierName + "] NEW Info Sharing: inv=" + inventory 
                + " back=" + backlog
                + " downInv=" + downstreamInventory
                + " excess=" + excess
                + " baseOrder=" + baseOrder
                + " order=" + outgoingOrder);
        
    } else if (!agentEnabled) {
        // Original rule-based (no information sharing)
        double inventoryPosition = inventory - backlog;
        outgoingOrder = Math.max(0, orderUpTo - inventoryPosition);
        outgoingOrder = Math.min(outgoingOrder, orderUpTo);
    }
}

if (upstreamNode != null) {
    upstreamNode.receiveOrder(outgoingOrder);
}

if (upstreamNode == null) {
    create_deliveryShipment(leadTime, outgoingOrder);
    traceln("[" + tierName + "] Ordered " + outgoingOrder +
            " from external source — arriving in " + leadTime + " weeks");
}

// Step 4: Accumulate cost
totalCost += 0.5 * Math.max(0, inventory) + 1.0 * Math.max(0, backlog);

// Step 5: Log datasets
inventoryData.add(time(), inventory);
backlogData.add(time(), backlog);
orderData.add(time(), outgoingOrder);
shippedData.add(time(), shipped);
incomingOrderData.add(time(), incomingOrder);
/*ALCODEEND*/}

