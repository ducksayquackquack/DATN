package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.ChatLieu;
import org.example.datnnhom03.Service.ChatLieuService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat-lieu")
@CrossOrigin("*")
public class ChatLieuApiController {

    private final ChatLieuService service;

    public ChatLieuApiController(ChatLieuService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChatLieu> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ChatLieu getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public ChatLieu create(@RequestBody ChatLieu chatLieu) {
        return service.create(chatLieu);
    }

    @PutMapping("/{id}")
    public ChatLieu update(@PathVariable Integer id,
                           @RequestBody ChatLieu chatLieu) {

        chatLieu.setId(id);
        return service.update(id, chatLieu);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}