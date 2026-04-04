void customerDemand()
{/*ALCODESTART::1772354370924*/
double baseDemand = Math.max(4, normal(10, 2));
double demand;

if (demandScenario == 0) {
    demand = baseDemand;
} else if (demandScenario == 1) {
    // S2 — Demand Shock at week 30, lasts 5 weeks
    if (time() >= 30 && time() <= 35) {
        demand = baseDemand * 3.0;
    } else {
        demand = baseDemand;
    }
} else {
    // S3 — Supply Disruption (demand normal)
    demand = baseDemand;
}

retailer.receiveOrder(demand);
customerDemandData.add(time(), demand);
traceln("Week " + time() + " — Demand: " + demand);
/*ALCODEEND*/}

void endOfRun()
{/*ALCODESTART::1772535155690*/
// ── Calculate Bullwhip Ratios ──
calculateBullwhip();

// ── Print summary to console ──
traceln("=== END OF RUN RESULTS ===");
traceln("Condition: " + autonomyCondition + " | Scenario: " + demandScenario + " | Rep: " + replicationNumber);
traceln("Retailer BWR:      " + bwrRetailer);
traceln("Distributor BWR:   " + bwrDistributor);
traceln("Manufacturer BWR:  " + bwrManufacturer);
traceln("Retailer Cost:     " + retailer.totalCost);
traceln("Distributor Cost:  " + distributor.totalCost);
traceln("Manufacturer Cost: " + manufacturer.totalCost);
traceln("Total Cost:        " + (retailer.totalCost + distributor.totalCost + manufacturer.totalCost));
traceln("==========================");

// ── File paths ──
String folder = "C:/Users/okech/Models/SupplyChainDigitalTwin/results/";
String summaryFile = folder + "summary_results.csv";
String tsFile = folder + "timeseries_A" + autonomyCondition +
                "_S" + demandScenario +
                "_R" + replicationNumber + ".csv";

// ── Export summary header (only if file does not exist yet) ──
java.io.File summaryFileCheck = new java.io.File(summaryFile);
if (!summaryFileCheck.exists()) {
    try {
        java.io.FileWriter fw = new java.io.FileWriter(summaryFile, false);
        fw.write("Condition,Scenario,Replication," +
                 "RetailerBWR,DistributorBWR,ManufacturerBWR," +
                 "RetailerCost,DistributorCost,ManufacturerCost,TotalCost\n");
        fw.close();
        traceln("Summary file created with header");
    } catch (Exception e) { traceln("Summary header error: " + e); }
}

// ── Export summary data row ──
try {
    java.io.FileWriter fw = new java.io.FileWriter(summaryFile, true);
    fw.write(autonomyCondition + "," +
             demandScenario + "," +
             replicationNumber + "," +
             bwrRetailer + "," +
             bwrDistributor + "," +
             bwrManufacturer + "," +
             retailer.totalCost + "," +
             distributor.totalCost + "," +
             manufacturer.totalCost + "," +
             (retailer.totalCost + distributor.totalCost + manufacturer.totalCost) + "\n");
    fw.close();
} catch (Exception e) { traceln("Summary data error: " + e); }

// ── Export weekly time series (only if file does not exist yet) ──
java.io.File tsFileCheck = new java.io.File(tsFile);
if (!tsFileCheck.exists()) {
    try {
        java.io.FileWriter tsfw = new java.io.FileWriter(tsFile, false);
        tsfw.write("Week,Tier,Inventory,Backlog,Order,Shipped,IncomingOrder\n");

        for (int i = 0; i < retailer.inventoryData.size(); i++) {
            tsfw.write(retailer.inventoryData.getX(i) + ",Retailer," +
                       retailer.inventoryData.getY(i) + "," +
                       retailer.backlogData.getY(i) + "," +
                       retailer.orderData.getY(i) + "," +
                       retailer.shippedData.getY(i) + "," +
                       retailer.incomingOrderData.getY(i) + "\n");
        }

        for (int i = 0; i < distributor.inventoryData.size(); i++) {
            tsfw.write(distributor.inventoryData.getX(i) + ",Distributor," +
                       distributor.inventoryData.getY(i) + "," +
                       distributor.backlogData.getY(i) + "," +
                       distributor.orderData.getY(i) + "," +
                       distributor.shippedData.getY(i) + "," +
                       distributor.incomingOrderData.getY(i) + "\n");
        }

        for (int i = 0; i < manufacturer.inventoryData.size(); i++) {
            tsfw.write(manufacturer.inventoryData.getX(i) + ",Manufacturer," +
                       manufacturer.inventoryData.getY(i) + "," +
                       manufacturer.backlogData.getY(i) + "," +
                       manufacturer.orderData.getY(i) + "," +
                       manufacturer.shippedData.getY(i) + "," +
                       manufacturer.incomingOrderData.getY(i) + "\n");
        }

        tsfw.close();
        traceln("Time series exported: " + tsFile);
    } catch (Exception e) { traceln("TS write error: " + e); }
} else {
    traceln("WARNING: " + tsFile + " already exists — not overwritten");
    traceln(">>> Change replicationNumber before running again");
}

// ── Reminder: run just completed  ──
traceln(">>> CURRENT RUN: A" + autonomyCondition + 
        " x S" + demandScenario + 
        " x Rep" + replicationNumber + " COMPLETE");
traceln(">>> Remember: only increment replicationNumber after all 9 cells are done");
/*ALCODEEND*/}

