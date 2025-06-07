package com.example.killertrackingportal.service;

import com.example.killertrackingportal.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    List<User> getAllUsers(Integer hour);
}
