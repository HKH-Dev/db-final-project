package com.uniquindio.frutimarket.cuenta;

import com.uniquindio.frutimarket.config.UsuarioDetailsService;
import com.uniquindio.frutimarket.config.UsuarioSesion;
import com.uniquindio.frutimarket.crud.ErroresBd;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Login, registro de clientes, perfil de usuario y notificaciones / recordatorios. */
@Controller
public class CuentaController {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern USUARIO = Pattern.compile("^[a-z0-9._]{4,40}$");
    private static final Pattern CLAVE = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final UsuarioDetailsService usuarios;

    public CuentaController(JdbcTemplate jdbc, PasswordEncoder encoder, UsuarioDetailsService usuarios) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.usuarios = usuarios;
    }

    @GetMapping("/login")
    public String login() {
        return "cuenta/login";
    }

    // ------------------------------------------------------------------ registro
    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("v", Map.of());
        model.addAttribute("municipios", municipios());
        return "cuenta/registro";
    }

    @PostMapping("/registro")
    @Transactional
    public String registrar(@RequestParam Map<String, String> f, Model model, RedirectAttributes ra) {
        Map<String, String> err = new LinkedHashMap<>();
        String user = f.getOrDefault("username", "").trim().toLowerCase();
        String email = f.getOrDefault("email", "").trim().toLowerCase();
        String tel = f.getOrDefault("telefono", "").replaceAll("[\\s-]", "");
        String doc = f.getOrDefault("numero_documento", "").trim();
        if (f.getOrDefault("nombre", "").isBlank()) err.put("nombre", "Escriba su nombre completo o la razón social");
        if (!USUARIO.matcher(user).matches()) err.put("username", "Entre 4 y 40 caracteres: minúsculas, números, punto o guion bajo");
        if (!EMAIL.matcher(email).matches()) err.put("email", "Correo no válido");
        if (!tel.matches("^[0-9]{7,10}$")) err.put("telefono", "Escriba entre 7 y 10 dígitos");
        if (!doc.matches("^[0-9]{5,10}(-[0-9])?$")) err.put("numero_documento", "Solo números (NIT con guion y dígito de verificación)");
        if (f.getOrDefault("direccion", "").isBlank()) err.put("direccion", "Escriba la dirección de entrega");
        if (!CLAVE.matcher(f.getOrDefault("clave", "")).matches()) err.put("clave", "Mínimo 8 caracteres con letras y números");
        if (!f.getOrDefault("clave", "").equals(f.get("clave2"))) err.put("clave2", "Las contraseñas no coinciden");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE username = ?", Integer.class, user) > 0) err.put("username", "Ese usuario ya existe");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE email = ?", Integer.class, email) > 0) err.put("email", "Ya hay una cuenta con ese correo");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM cliente WHERE numero_documento = ?", Integer.class, doc) > 0) err.put("numero_documento", "Ya hay un cliente con ese documento");
        if (!err.isEmpty()) {
            model.addAttribute("v", f);
            model.addAttribute("errores", err);
            model.addAttribute("municipios", municipios());
            return "cuenta/registro";
        }
        String nombre = f.get("nombre").trim();
        try {
            jdbc.update("INSERT INTO usuario (id_rol, username, password_hash, nombre_completo, email, telefono, activo, fecha_registro) VALUES (4, ?, ?, ?, ?, ?, 1, NOW())",
                    user, encoder.encode(f.get("clave")), nombre, email, tel);
            Integer idUsuario = jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE username = ?", Integer.class, user);
            jdbc.update("""
                    INSERT INTO cliente (id_usuario, id_municipio, tipo_cliente, tipo_documento, numero_documento, nombre, telefono, email, direccion, fecha_registro, activo)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURDATE(), 1)""",
                    idUsuario, Integer.valueOf(f.get("id_municipio")), f.get("tipo_cliente"), f.get("tipo_documento"), doc, nombre, tel, email, f.get("direccion").trim());
            jdbc.update("INSERT INTO notificacion (id_usuario, tipo, titulo, mensaje, fecha_programada) VALUES (?, 'SISTEMA', ?, ?, NOW())",
                    idUsuario, "¡Bienvenido a FrutiMarket!", "Ya puedes hacer pedidos de fruta fresca del Quindío y suscribirte a una canasta periódica.");
        } catch (DataAccessException e) {
            model.addAttribute("v", f);
            model.addAttribute("error", ErroresBd.mensaje(e, false));
            model.addAttribute("municipios", municipios());
            return "cuenta/registro";
        }
        ra.addFlashAttribute("ok", "Cuenta creada. Ya puede iniciar sesión con el usuario «" + user + "».");
        return "redirect:/login";
    }

    // ------------------------------------------------------------------ perfil
    @GetMapping("/perfil")
    public String perfil(@AuthenticationPrincipal UsuarioSesion u, Model model) {
        model.addAttribute("perfil", jdbc.queryForMap("""
                SELECT u.username, u.nombre_completo, u.email, u.telefono, u.fecha_registro, u.ultimo_acceso, r.nombre AS rol, r.descripcion AS rol_desc
                  FROM usuario u JOIN rol r ON r.id_rol = u.id_rol WHERE u.id_usuario = ?""", u.id()));
        if (u.idCliente() != null) {
            model.addAttribute("cliente", jdbc.queryForMap("SELECT c.*, m.nombre AS municipio FROM cliente c JOIN municipio m ON m.id_municipio = c.id_municipio WHERE c.id_cliente = ?", u.idCliente()));
            model.addAttribute("municipios", municipios());
        }
        return "cuenta/perfil";
    }

    @PostMapping("/perfil")
    @Transactional
    public String guardarPerfil(@AuthenticationPrincipal UsuarioSesion u, @RequestParam Map<String, String> f, RedirectAttributes ra) {
        String email = f.getOrDefault("email", "").trim().toLowerCase();
        String tel = f.getOrDefault("telefono", "").replaceAll("[\\s-]", "");
        if (f.getOrDefault("nombre_completo", "").isBlank() || !EMAIL.matcher(email).matches() || !tel.matches("^[0-9]{7,10}$")) {
            ra.addFlashAttribute("error", "Revise el nombre, el correo y el teléfono (7 a 10 dígitos).");
            return "redirect:/perfil";
        }
        try {
            jdbc.update("UPDATE usuario SET nombre_completo = ?, email = ?, telefono = ? WHERE id_usuario = ?", f.get("nombre_completo").trim(), email, tel, u.id());
            if (u.idCliente() != null) {
                jdbc.update("UPDATE cliente SET nombre = ?, email = ?, telefono = ?, direccion = ?, id_municipio = ? WHERE id_cliente = ?",
                        f.get("nombre_completo").trim(), email, tel, f.getOrDefault("direccion", "").trim(), Integer.valueOf(f.get("id_municipio")), u.idCliente());
            }
        } catch (DataAccessException e) {
            ra.addFlashAttribute("error", ErroresBd.mensaje(e, false));
            return "redirect:/perfil";
        }
        refrescarSesion(u);
        ra.addFlashAttribute("ok", "Perfil actualizado.");
        return "redirect:/perfil";
    }

    @PostMapping("/perfil/clave")
    public String cambiarClave(@AuthenticationPrincipal UsuarioSesion u, @RequestParam String actual, @RequestParam String nueva,
                               @RequestParam String confirmar, RedirectAttributes ra) {
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE id_usuario = ?", String.class, u.id());
        if (!encoder.matches(actual, hash)) {
            ra.addFlashAttribute("error", "La contraseña actual no es correcta.");
        } else if (!CLAVE.matcher(nueva).matches()) {
            ra.addFlashAttribute("error", "La nueva contraseña debe tener mínimo 8 caracteres con letras y números.");
        } else if (!nueva.equals(confirmar)) {
            ra.addFlashAttribute("error", "La confirmación no coincide con la nueva contraseña.");
        } else {
            jdbc.update("UPDATE usuario SET password_hash = ? WHERE id_usuario = ?", encoder.encode(nueva), u.id());
            refrescarSesion(u);
            ra.addFlashAttribute("ok", "Contraseña cambiada.");
        }
        return "redirect:/perfil";
    }

    // ------------------------------------------------------------------ notificaciones y recordatorios
    @GetMapping("/notificaciones")
    public String notificaciones(@AuthenticationPrincipal UsuarioSesion u, @RequestParam(defaultValue = "todas") String ver, Model model) {
        String filtro = switch (ver) {
            case "pendientes" -> " AND leida = 0 AND fecha_programada <= NOW()";
            case "programadas" -> " AND fecha_programada > NOW()";
            default -> " AND fecha_programada <= NOW()";
        };
        model.addAttribute("ver", ver);
        model.addAttribute("lista", jdbc.queryForList("SELECT * FROM notificacion WHERE id_usuario = ?" + filtro
                + " ORDER BY fecha_programada " + ("programadas".equals(ver) ? "ASC" : "DESC") + " LIMIT 100", u.id()));
        model.addAttribute("programadas", jdbc.queryForObject("SELECT COUNT(*) FROM notificacion WHERE id_usuario = ? AND fecha_programada > NOW()", Integer.class, u.id()));
        return "cuenta/notificaciones";
    }

    @PostMapping("/notificaciones/recordatorio")
    public String crearRecordatorio(@AuthenticationPrincipal UsuarioSesion u, @RequestParam String titulo, @RequestParam String mensaje,
                                    @RequestParam String cuando, RedirectAttributes ra) {
        try {
            LocalDateTime fecha = LocalDateTime.parse(cuando);
            if (titulo.isBlank() || titulo.length() > 100 || mensaje.length() > 400) {
                throw new IllegalArgumentException();
            }
            jdbc.update("INSERT INTO notificacion (id_usuario, tipo, titulo, mensaje, fecha_programada) VALUES (?, 'RECORDATORIO', ?, ?, ?)",
                    u.id(), titulo.trim(), mensaje.isBlank() ? titulo.trim() : mensaje.trim(), fecha);
            ra.addFlashAttribute("ok", "Recordatorio programado.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Revise el título (máx. 100 caracteres) y la fecha del recordatorio.");
        }
        return "redirect:/notificaciones?ver=programadas";
    }

    @PostMapping("/notificaciones/{id}/leida")
    public String marcarLeida(@AuthenticationPrincipal UsuarioSesion u, @PathVariable int id, @RequestParam(defaultValue = "todas") String ver) {
        jdbc.update("UPDATE notificacion SET leida = 1 WHERE id_notificacion = ? AND id_usuario = ?", id, u.id());
        return "redirect:/notificaciones?ver=" + ver;
    }

    @PostMapping("/notificaciones/leer-todas")
    public String leerTodas(@AuthenticationPrincipal UsuarioSesion u) {
        jdbc.update("UPDATE notificacion SET leida = 1 WHERE id_usuario = ? AND fecha_programada <= NOW()", u.id());
        return "redirect:/notificaciones";
    }

    @PostMapping("/notificaciones/{id}/eliminar")
    public String eliminarNotificacion(@AuthenticationPrincipal UsuarioSesion u, @PathVariable int id, @RequestParam(defaultValue = "todas") String ver) {
        jdbc.update("DELETE FROM notificacion WHERE id_notificacion = ? AND id_usuario = ?", id, u.id());
        return "redirect:/notificaciones?ver=" + ver;
    }

    private List<Map<String, Object>> municipios() {
        return jdbc.queryForList("SELECT id_municipio, nombre, departamento FROM municipio ORDER BY departamento DESC, nombre");
    }

    private void refrescarSesion(UsuarioSesion u) {
        UsuarioSesion nuevo = usuarios.recargar(u.id());
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(nuevo, null, nuevo.getAuthorities()));
        SecurityContextHolder.setContext(ctx);
        var attrs = (org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        new org.springframework.security.web.context.HttpSessionSecurityContextRepository()
                .saveContext(ctx, attrs.getRequest(), attrs.getResponse());
    }
}
