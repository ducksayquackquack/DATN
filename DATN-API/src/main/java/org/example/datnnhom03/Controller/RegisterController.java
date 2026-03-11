package org.example.datnnhom03.Controller;

import jakarta.validation.Valid;
import org.example.datnnhom03.Service.RegisterService;
import org.example.datnnhom03.dto.RegisterDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterDTO registerDTO,
                           BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        boolean success = registerService.register(registerDTO);

        if (!success) {
            bindingResult.rejectValue(
                    "email",
                    "email.exists",
                    "Email đã được sử dụng"
            );
            return "register";
        }

        return "redirect:/login?registerSuccess";
    }

}