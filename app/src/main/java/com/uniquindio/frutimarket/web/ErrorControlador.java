package com.uniquindio.frutimarket.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorControlador implements ErrorController {

    @RequestMapping("/error/403")
    public String prohibido(Model model) {
        model.addAttribute("codigo", 403);
        return "error";
    }

    @RequestMapping("/error")
    public String error(HttpServletRequest req, Model model) {
        Object s = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        model.addAttribute("codigo", s == null ? 500 : s);
        return "error";
    }
}
