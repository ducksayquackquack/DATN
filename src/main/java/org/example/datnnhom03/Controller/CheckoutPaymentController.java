package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Model.PTTT;
import org.example.datnnhom03.Repository.PTTTRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/checkout")
public class CheckoutPaymentController {

    private final PTTTRepository ptttRepo;

    public CheckoutPaymentController(PTTTRepository ptttRepo) {
        this.ptttRepo = ptttRepo;
    }

    @GetMapping("/payment/{hoaDonId}")
    public String choosePayment(
            @PathVariable Integer hoaDonId,
            Model model
    ) {
        List<PTTT> methods = ptttRepo.findByTrangThai("Hoạt động");

        model.addAttribute("hoaDonId", hoaDonId);
        model.addAttribute("methods", methods);

        return "pttt/payment";
    }
}
