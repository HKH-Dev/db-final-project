package com.uniquindio.frutimarket.config;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Usuario autenticado. idCliente / idCampesino se llenan si el usuario esta vinculado a uno. */
public record UsuarioSesion(int id, String username, String passwordHash, String nombre, String rol, boolean activo,
                            Integer idCliente, Integer idCampesino, Integer idPunto) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }

    public boolean esGestion() {
        return "ADMIN".equals(rol) || "OPERADOR".equals(rol);
    }

    public boolean esCliente() {
        return "CLIENTE".equals(rol);
    }

    public boolean esCampesino() {
        return "CAMPESINO".equals(rol);
    }

    public String iniciales() {
        String[] p = nombre.trim().split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[1].substring(0, 1) : "")).toUpperCase();
    }
}
