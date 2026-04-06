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
    outgoingOrder = callLLMAgent();
    outgoingOrder = Math.min(outgoingOrder, 24.0);

   if (agentConfidence < 0.85) {
        double inventoryPosition = inventory - backlog;
       outgoingOrder = Math.max(0, orderUpTo - inventoryPosition);
        outgoingOrder = Math.min(outgoingOrder, 24.0);
      traceln("[" + tierName + "] LOW CONFIDENCE (" + agentConfidence +
               ") — reverting to rule-based order: " + outgoingOrder);
   }
   
   // HITL — pause for human review if enabled
   boolean crisisDetected = (backlog > main.crisisBacklogThreshold && inventory == 0);
    if (hitlEnabled && (agentConfidence < 0.85 || crisisDetected)) {
        pendingOrder        = outgoingOrder;
        awaitingHumanInput  = true;

        // Pass state to Main dashboard
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

        // Pause simulation — human must respond via dashboard
        getEngine().pause();
        return; // order placed by resumeFromHITL()
    }

} else {
    double inventoryPosition = inventory - backlog;
    outgoingOrder = Math.max(0, orderUpTo - inventoryPosition);
    outgoingOrder = Math.min(outgoingOrder, 24.0);
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

