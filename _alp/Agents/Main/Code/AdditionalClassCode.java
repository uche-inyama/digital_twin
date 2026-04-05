public void setRLAction(double[] action) {
  retailer.rlOrderInput = action[0];
  distributor.rlOrderInput = action[1];
  manufacturer.rlOrderInput = action[2];
}

public void setRLAction(double r, double d, double m) {
  retailer.rlOrderInput = r;
  distributor.rlOrderInput = d;
  manufacturer.rlOrderInput = m;
}