package com.code.assistance.codeassistance.controller;

import com.code.assistance.codeassistance.service.TradeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HelloController {

  private final TradeService tradeService;

  public HelloController(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @GetMapping("/hello")
  public String hello(@RequestParam(defaultValue = "world") String name) {
    return "Hello, " + name;
  }

  @GetMapping("/fee")
  public String fee(@RequestParam double amount) {
    return tradeService.computeFeeString(amount);
  }
}
