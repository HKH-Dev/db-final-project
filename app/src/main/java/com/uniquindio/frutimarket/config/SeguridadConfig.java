package com.uniquindio.frutimarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, JdbcTemplate jdbc) throws Exception {
        http.authorizeHttpRequests(a -> a
                        .requestMatchers("/login", "/registro", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/crud/usuarios/**", "/crud/roles/**", "/crud/empleados/**").hasRole("ADMIN")
                        .requestMatchers("/crud/**", "/consultas/**", "/reportes/**").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/planes/**").hasRole("CLIENTE")
                        .requestMatchers("/mis-lotes/**").hasRole("CAMPESINO")
                        .anyRequest().authenticated())
                .formLogin(f -> f.loginPage("/login")
                        .successHandler((req, res, auth) -> {
                            UsuarioSesion u = (UsuarioSesion) auth.getPrincipal();
                            jdbc.update("UPDATE usuario SET ultimo_acceso = NOW() WHERE id_usuario = ?", u.id());
                            res.sendRedirect(req.getContextPath() + "/");
                        })
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(l -> l.logoutUrl("/salir").logoutSuccessUrl("/login?salir").permitAll())
                .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> {
                    // formulario vencido (token CSRF de una sesion anterior): volver a mostrarlo
                    if (ex instanceof org.springframework.security.web.csrf.CsrfException) {
                        boolean login = req.getRequestURI().equals("/login");
                        res.sendRedirect(login ? "/login?expirada" : "/?expirada");
                        return;
                    }
                    req.getRequestDispatcher("/error/403").forward(req, res);
                }));
        return http.build();
    }
}
