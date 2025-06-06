package com.example.killertrackingportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {




    public IndexController() {
    }

    @GetMapping(path="/")
    public String goToHomePage(){

        return "redirect:/nhp/dashboard";
    }


}
