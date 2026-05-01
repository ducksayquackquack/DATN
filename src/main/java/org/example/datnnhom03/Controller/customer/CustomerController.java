package org.example.datnnhom03.Controller.customer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomerController {

    @GetMapping("/customer/home")
    public String home() {
        return "customer/home";
    }
}
