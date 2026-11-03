package com.uniquindio.frutimarket.web;

import com.uniquindio.frutimarket.comun.Fmt;
import com.uniquindio.frutimarket.config.UsuarioSesion;
import com.uniquindio.frutimarket.crud.CatalogoEntidades;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Datos que usan todas las paginas: usuario, menu, campana de notificaciones y formato. */
@ControllerAdvice(basePackages = "com.uniquindio.frutimarket")
public class ModeloGlobal {

    private final JdbcTemplate jdbc;
    private final CatalogoEntidades catalogo;
    private final Fmt fmt;

    public ModeloGlobal(JdbcTemplate jdbc, CatalogoEntidades catalogo, Fmt fmt) {
        this.jdbc = jdbc;
        this.catalogo = catalogo;
        this.fmt = fmt;
    }

    @ModelAttribute
    public void agregar(Model model, HttpServletRequest req) {
        model.addAttribute("fmt", fmt);
        model.addAttribute("ruta", req.getRequestURI());
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof UsuarioSesion u) {
            model.addAttribute("yo", u);
            model.addAttribute("menuEntidades", catalogo.porGrupo(u.esAdmin()));
            model.addAttribute("pendientes", jdbc.queryForObject(
                    "SELECT COUNT(*) FROM notificacion WHERE id_usuario = ? AND leida = 0 AND fecha_programada <= NOW()", Integer.class, u.id()));
        }
    }
}
