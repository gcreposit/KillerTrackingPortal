package com.example.killertrackingportal.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping(path = "/trackUser")
public class PageController {


    @GetMapping("/adminPanel")
    public String goToPanel(Model theModel) {

        return "tracker/track_dashboard";
    }
}

