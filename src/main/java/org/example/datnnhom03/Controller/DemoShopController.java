package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Repository.SanPhamChiTietRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ShopController {

    private final SanPhamChiTietRepository spctRepo;

    public ShopController(SanPhamChiTietRepository spctRepo) {
        this.spctRepo = spctRepo;
    }

    // 1️⃣ SHOP – show 4 products
    @GetMapping("/shop")
    public String shop(Model model) {
        model.addAttribute(
                "products",
                spctRepo.findTop4ByTrangThai("Hoạt động")
        );
        return "shop/index";
    }

    // 2️⃣ PRODUCT DETAIL
    @GetMapping("/shop/product/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {
        model.addAttribute(
                "product",
                spctRepo.findById(id).orElseThrow()
        );
        return "shop/detail";
    }
}
