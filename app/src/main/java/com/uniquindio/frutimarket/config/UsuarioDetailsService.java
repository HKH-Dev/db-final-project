package com.uniquindio.frutimarket.config;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private static final String SQL = """
            SELECT u.id_usuario, u.username, u.password_hash, u.nombre_completo, r.nombre AS rol, u.activo,
                   (SELECT c.id_cliente FROM cliente c WHERE c.id_usuario = u.id_usuario) AS id_cliente,
                   (SELECT ca.id_campesino FROM campesino ca WHERE ca.id_usuario = u.id_usuario) AS id_campesino,
                   (SELECT e.id_punto FROM empleado e WHERE e.id_usuario = u.id_usuario) AS id_punto
              FROM usuario u
              JOIN rol r ON r.id_rol = u.id_rol
             WHERE u.username = ? OR u.email = ?
            """;

    private final JdbcTemplate jdbc;

    public UsuarioDetailsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UsuarioSesion loadUserByUsername(String username) {
        List<UsuarioSesion> r = jdbc.query(SQL, (rs, i) -> new UsuarioSesion(
                rs.getInt("id_usuario"), rs.getString("username"), rs.getString("password_hash"),
                rs.getString("nombre_completo"), rs.getString("rol"), rs.getBoolean("activo"),
                (Integer) rs.getObject("id_cliente"), (Integer) rs.getObject("id_campesino"),
                (Integer) rs.getObject("id_punto")), username.trim(), username.trim());
        if (r.isEmpty()) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        return r.getFirst();
    }

    /** Vuelve a leer el usuario (tras editar el perfil) */
    public UsuarioSesion recargar(int idUsuario) {
        String username = jdbc.queryForObject("SELECT username FROM usuario WHERE id_usuario = ?", String.class, idUsuario);
        return loadUserByUsername(username);
    }
}
