package com.code.assistance.codeassistance.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Optional;

public class BadPatternsDemo {
  public String showAccount(Optional<String> acct) {
    return acct.get(); // unsafe Optional.get()
  }
  public BigDecimal calcFee(double amt) {
    return new BigDecimal(amt * 0.015); // BigDecimal(double)
  }
  public void update(String acct, double delta) throws Exception {
    System.out.println("Updating account: " + acct); // PII in logs
    Connection c = DriverManager.getConnection("jdbc:h2:mem:test"); // not closed
    Statement s = c.createStatement();
    String sql = "UPDATE ACCOUNTS SET BAL=" + delta + " WHERE ACCT='" + acct + "'"; // raw concat SQL
    s.execute(sql);
  }
  public int slow() {
    int sum = 0 ;
    for (int i = 0; i < 100000; i++) {
      sum += Integer.valueOf(i).intValue(); // unnecessary boxing
    }
    return sum;
  }
}