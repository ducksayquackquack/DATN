package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.ChatLieu;

import java.util.List;

public interface ChatLieuService {

    ChatLieu create(ChatLieu chatLieu);

    ChatLieu update(Integer id, ChatLieu chatLieu);

    void delete(Integer id);

    ChatLieu findById(Integer id);

    List<ChatLieu> findAll();
}