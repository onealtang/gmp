package com.tang.ads.gmp.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by oneal on 15/8/24.
 */
@RestController
public class AuthController {

    @RequestMapping("/")
    public String handleToken(String code) {
        return "code: " + code;
    }
}
