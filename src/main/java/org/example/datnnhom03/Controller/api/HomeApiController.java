package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Service.HomeService;
import org.example.datnnhom03.dto.home.HomeResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/home")
public class HomeApiController {

    private final HomeService homeService;

    public HomeApiController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public ResponseEntity<HomeResponseDTO> getHomeData() {
        return ResponseEntity.ok(homeService.getHomeData());
    }
}