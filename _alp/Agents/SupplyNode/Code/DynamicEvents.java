void deliveryShipment(double amount)
{/*ALCODESTART::1772353248589*/
// Debug — see when deliveries actually arrive
traceln("🚚 DELIVERY ARRIVING at " + this.tierName + 
        " time=" + time() + " amount=" + amount);

// Actually deliver the goods
receiveShipment(amount);

// Show result
traceln("   After: inventory=" + inventory + " backlog=" + backlog);
/*ALCODEEND*/}

