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

