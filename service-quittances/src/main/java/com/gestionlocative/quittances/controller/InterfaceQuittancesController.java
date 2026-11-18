package com.gestionlocative.quittances.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InterfaceQuittancesController {

    @GetMapping({"/", "/quittances"})
    public String afficherInterface() {
        return "quittances";
    }
}