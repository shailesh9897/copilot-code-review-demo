package com.code.assistance.codeassistance.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Optional;

@Service
public class TradeService {

  // INTENTIONAL ISSUES:
  // 1) Optional.get() without check (NPE risk)
  // 2) BigDecimal(double) precision issue
  // 3) Raw SQL concat (SQL injection risk) + resource leak
  // 4) PII-style logging (printing full account)
  // 5) Inefficient boxing in loop

  public String accountDisplay(Optional<String> accountNumber) {
    return "Account: " + accountNumber.get(); // <-- unsafe Optional.get()
  }

  public String computeFeeString(double amount) {
    BigDecimal fee = new BigDecimal(amount * 0.015); // <-- BigDecimal(double)
    return "Fee=" + fee.toString();
  }

  public void updateBalance(String acct, double delta) throws Exception {
    System.out.println("Updating account: " + acct); // <-- avoid logging full acct in real code
    Connection c = DriverManager.getConnection("jdbc:h2:mem:test"); // <-- not closed
    Statement s = c.createStatement();
    String sql = "UPDATE ACCOUNTS SET BAL=" + delta + " WHERE ACCT='" + acct + "'"; // <-- concat
    s.execute(sql);
  }

  public int slowSum() {
    int sum = 0;
    for (int i = 0; i < 200_000; i++) {
      sum += Integer.valueOf(i).intValue(); // <-- needless boxing
    }
    return sum;
  }
}
