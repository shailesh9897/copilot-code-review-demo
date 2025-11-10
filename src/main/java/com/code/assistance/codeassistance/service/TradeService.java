package com.code.assistance.codeassistance.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Optional;

@Service
public class TradeService {

  // TradeService (fixed) on main
  public String accountDisplay(Optional<String> accountNumber) {
    return "Account: " + accountNumber.orElse("****");
  }
  public String computeFeeString(double amount) {
    return "Fee=" + BigDecimal.valueOf(amount)
            .multiply(BigDecimal.valueOf(0.015))
            .toPlainString();
  }
  public void updateBalance(String acct, double delta) throws Exception {
    String masked = "****" + acct.substring(Math.max(0, acct.length() - 4));
    System.out.println("Updating account: " + masked);
    try (Connection c = DriverManager.getConnection("jdbc:h2:mem:test");
         Statement s = c.createStatement()) {
      // NOTE: PreparedStatement with params in real code
      String sql = "UPDATE ACCOUNTS SET BAL=" + delta + " WHERE ACCT='" + acct + "'";
      s.execute(sql);
    }
  }
  public int slowSum() {
    int sum = 0;
    for (int i = 0; i < 200_000; i++) sum += i;
    return sum;
  }

}
