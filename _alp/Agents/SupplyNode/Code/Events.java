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
    outgoingOrder = callLLMAgent();
    outgoingOrder = Math.min(outgoingOrder, 24.0);

   if (agentConfidence < 0.7) {
        double inventoryPosition = inventory - backlog;
       outgoingOrder = Math.max(0, orderUpTo - inventoryPosition);
        outgoingOrder = Math.min(outgoingOrder, 24.0);
      traceln("[" + tierName + "] LOW CONFIDENCE (" + agentConfidence +
               ") — reverting to rule-based order: " + outgoingOrder);
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

