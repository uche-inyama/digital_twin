double initialiseChain()
{/*ALCODESTART::1772354473818*/
// Initialise inventory
retailer.inventory      = retailer.initialInventory;
distributor.inventory   = distributor.initialInventory;
manufacturer.inventory  = manufacturer.initialInventory;

// Set order up to levels
retailer.orderUpTo     = 24;
distributor.orderUpTo  = 24;
manufacturer.orderUpTo = 24;

// Wire upstream nodes — who to order from
retailer.upstreamNode     = distributor;
distributor.upstreamNode  = manufacturer;
manufacturer.upstreamNode = null;  // Manufacturer has no upstream

// Wire downstream nodes — who to ship to
retailer.downstreamNode     = null;  // Retailer ships to customer directly
distributor.downstreamNode  = retailer;
manufacturer.downstreamNode = distributor;

// Pre-fill pipeline — simulate shipments already in transit at t=0
retailer.create_deliveryShipment(1, 8.0);
retailer.create_deliveryShipment(2, 8.0);

distributor.create_deliveryShipment(1, 8.0);
distributor.create_deliveryShipment(2, 8.0);

manufacturer.create_deliveryShipment(1, 8.0);
manufacturer.create_deliveryShipment(2, 8.0);
/*ALCODEEND*/}

double calcVariance(DataSet ds)
{/*ALCODESTART::1772534572250*/
// Get number of samples
int n = ds.size();
if (n < 2) return 0;

// Calculate mean
double sum = 0;
for (int i = 0; i < n; i++) {
    sum += ds.getY(i);
}
double mean = sum / n;

// Calculate variance
double sumSq = 0;
for (int i = 0; i < n; i++) {
    double diff = ds.getY(i) - mean;
    sumSq += diff * diff;
}
return sumSq / n;
/*ALCODEEND*/}

double calculateBullwhip()
{/*ALCODESTART::1772535083227*/
// Get demand variance (denominator — same for all tiers)
double demandVar = calcVariance(customerDemandData);

if (demandVar == 0) {
    traceln("WARNING: Demand variance is zero — cannot compute BWR");
    return;
}

// Compute BWR for each tier
bwrRetailer     = calcVariance(retailer.orderData)      / demandVar;
bwrDistributor  = calcVariance(distributor.orderData)   / demandVar;
bwrManufacturer = calcVariance(manufacturer.orderData)  / demandVar;

// Print results to console
traceln("=== BULLWHIP RATIOS (end of run) ===");
traceln("Customer Demand Variance : " + demandVar);
traceln("Retailer     BWR : " + bwrRetailer);
traceln("Distributor  BWR : " + bwrDistributor);
traceln("Manufacturer BWR : " + bwrManufacturer);
traceln("====================================");
/*ALCODEEND*/}

double resumeFromHITL()
{/*ALCODESTART::1775364336579*/
SupplyNode waitingTier = null;
if (retailer.awaitingHumanInput)          waitingTier = retailer;
else if (distributor.awaitingHumanInput)  waitingTier = distributor;
else if (manufacturer.awaitingHumanInput) waitingTier = manufacturer;

if (waitingTier == null) return;

// Apply human decision
if (humanDecisionAccept) {
    waitingTier.outgoingOrder = waitingTier.pendingOrder;
    traceln("[HITL] Human ACCEPTED order: " + waitingTier.outgoingOrder);
} else {
    waitingTier.outgoingOrder = hitlHumanOrder;
    traceln("[HITL] Human OVERRODE to: " + waitingTier.outgoingOrder);
}

// Place order upstream
if (waitingTier.upstreamNode != null) {
    waitingTier.upstreamNode.receiveOrder(waitingTier.outgoingOrder);
} else {
    waitingTier.create_deliveryShipment(
        waitingTier.leadTime, waitingTier.outgoingOrder);
    traceln("[" + waitingTier.tierName + "] Ordered "
        + waitingTier.outgoingOrder
        + " from external source — arriving in "
        + waitingTier.leadTime + " weeks");
}

// Log cost and datasets for the paused week
waitingTier.totalCost += 0.5 * Math.max(0, waitingTier.inventory)
                       + 1.0 * Math.max(0, waitingTier.backlog);
waitingTier.inventoryData.add(waitingTier.time(), waitingTier.inventory);
waitingTier.backlogData.add(waitingTier.time(), waitingTier.backlog);
waitingTier.orderData.add(waitingTier.time(), waitingTier.outgoingOrder);
waitingTier.shippedData.add(waitingTier.time(), waitingTier.shipped);
waitingTier.incomingOrderData.add(waitingTier.time(), waitingTier.incomingOrder);

// Clear HITL state
waitingTier.awaitingHumanInput = false;
waitingTier.pendingOrder       = 0;
hitlTierName       = "";
hitlInventory      = 0;
hitlBacklog        = 0;
hitlSuggestedOrder = 0;
hitlConfidence     = 0;
hitlReasoning      = "";
hitlWeek           = 0;
humanDecisionAccept = false;
interventionCount++;

getEngine().run();
/*ALCODEEND*/}

double[] getState()
{/*ALCODESTART::1775403861537*/
double[] state = new double[6];
    
    // Retailer
    state[0] = retailer.inventory;
    state[1] = retailer.backlog;
    
    // Distributor
    state[2] = distributor.inventory;
    state[3] = distributor.backlog;
    
    // Manufacturer
    state[4] = manufacturer.inventory;
    state[5] = manufacturer.backlog;
    
    traceln("[STATE] " + retailer.inventory + "," + retailer.backlog + ","
      + distributor.inventory + "," + distributor.backlog + ","
      + (retailer.inventory + distributor.inventory + manufacturer.inventory) + ","
      + (retailer.backlog + distributor.backlog + manufacturer.backlog));
    
    return state;  // MUST return the array
/*ALCODEEND*/}

double getReward()
{/*ALCODESTART::1775403868999*/
double holdingCost = 0.5 * (
  Math.max(0, retailer.inventory) +
  Math.max(0, distributor.inventory) +
  Math.max(0, manufacturer.inventory)
);

double backlogCost = 1.0 * (
  Math.max(0, retailer.backlog) +
  Math.max(0, distributor.backlog) +
  Math.max(0, manufacturer.backlog)
);

double reward = -(holdingCost + backlogCost);

 return reward;
/*ALCODEEND*/}

double runOneStep()
{/*ALCODESTART::1775403874914*/
getEngine().step();
/*ALCODEEND*/}

double resetModel()
{/*ALCODESTART::1775403879999*/
  // DO NOT reset inventory - let it continue naturally
    // Only reset if you need to track episode boundaries for other reasons
    
    traceln("Episode boundary reached. Current time: " + getEngine().getTime() + 
            ", Inventory: R=" + retailer.inventory + 
            ", D=" + distributor.inventory + 
            ", M=" + manufacturer.inventory);
    
    // Optional: Reset step counter or episode-specific tracking
    // But keep inventory values as they are!
/*ALCODEEND*/}

int getDemandScenario()
{/*ALCODESTART::1775416113436*/
return demandScenario; 
/*ALCODEEND*/}

double setDemandScenario(int scenario)
{/*ALCODESTART::1775416118787*/
// Update your existing demand scenario parameter
    this.demandScenario = scenario;
   
    traceln("📊 Demand scenario changed to: " + scenario);
/*ALCODEEND*/}

