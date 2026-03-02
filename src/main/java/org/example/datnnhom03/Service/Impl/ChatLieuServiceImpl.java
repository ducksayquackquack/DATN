package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.ChatLieu;
import org.example.datnnhom03.Repository.ChatLieuRepository;
import org.example.datnnhom03.Service.ChatLieuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatLieuServiceImpl implements ChatLieuService {

    @Autowired
    private ChatLieuRepository chatLieuRepository;

    @Override
    public ChatLieu create(ChatLieu chatLieu) {
        chatLieu.setNgayTao(LocalDateTime.now());
        chatLieu.setNgaySua(LocalDateTime.now());
        return chatLieuRepository.save(chatLieu);
    }

    @Override
    public ChatLieu update(Integer id, ChatLieu chatLieu) {
        ChatLieu cl = chatLieuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu"));

        cl.setMaChatLieu(chatLieu.getMaChatLieu());
        cl.setTenChatLieu(chatLieu.getTenChatLieu());
        cl.setTrangThai(chatLieu.getTrangThai());
        cl.setNgaySua(LocalDateTime.now());

        return chatLieuRepository.save(cl);
    }

    @Override
    public void delete(Integer id) {
        ChatLieu cl = chatLieuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu"));

        cl.setTrangThai("Ngừng hoạt động");
        chatLieuRepository.save(cl);
    }

    @Override
    public ChatLieu findById(Integer id) {
        return chatLieuRepository.findById(id).orElse(null);
    }

    @Override
    public List<ChatLieu> findAll() {
        return chatLieuRepository.findByTrangThai("Hoạt động");
    }
}