package com.uniquindio.frutimarket.crud;

import com.uniquindio.frutimarket.config.UsuarioSesion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Registro, modificacion, eliminacion y consulta de todas las entidades del catalogo. */
@Controller
@RequestMapping("/crud/{clave}")
public class CrudController {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern TELEFONO = Pattern.compile("^[0-9]{7,10}$");
    private static final DateTimeFormatter DT_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final CatalogoEntidades catalogo;
    private final CrudRepositorio repo;
    private final PasswordEncoder encoder;

    public CrudController(CatalogoEntidades catalogo, CrudRepositorio repo, PasswordEncoder encoder) {
        this.catalogo = catalogo;
        this.repo = repo;
        this.encoder = encoder;
    }

    private Entidad entidad(String clave, UsuarioSesion u) {
        Entidad e = catalogo.buscar(clave).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (e.isSoloAdmin() && !u.esAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return e;
    }

    @GetMapping
    public String listar(@PathVariable String clave, @RequestParam(defaultValue = "") String q,
                         @RequestParam(required = false) String orden, @RequestParam(defaultValue = "false") boolean desc,
                         @RequestParam(defaultValue = "1") int pagina, @AuthenticationPrincipal UsuarioSesion u, Model model) {
        Entidad e = entidad(clave, u);
        model.addAttribute("e", e);
        model.addAttribute("q", q);
        model.addAttribute("orden", orden);
        model.addAttribute("desc", desc);
        model.addAttribute("pag", repo.listar(e, q, orden, desc, Math.max(1, pagina), 15));
        return "crud/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable String clave, @PathVariable long id, @AuthenticationPrincipal UsuarioSesion u, Model model) {
        Entidad e = entidad(clave, u);
        if (e.getUrlDetalle() != null) {
            return "redirect:" + e.getUrlDetalle().replace("{id}", String.valueOf(id));
        }
        model.addAttribute("e", e);
        model.addAttribute("fila", repo.fila(e, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        model.addAttribute("id", id);
        return "crud/detalle";
    }

    @GetMapping("/nuevo")
    public String nuevo(@PathVariable String clave, @AuthenticationPrincipal UsuarioSesion u, Model model) {
        Entidad e = entidad(clave, u);
        if (!e.isCrear()) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
        }
        if (e.getUrlCrear() != null) {
            return "redirect:" + e.getUrlCrear();
        }
        Map<String, Object> v = new HashMap<>();
        for (Campo c : e.getCampos()) {
            if (c.tipo() == Campo.Tipo.BOOLEANO && c.columna().equals("activo")) {
                v.put(c.columna(), "1");
            }
            if (c.tipo() == Campo.Tipo.FECHA && (c.columna().startsWith("fecha_registro") || c.columna().startsWith("fecha_vinc")
                    || c.columna().equals("fecha_ingreso") || c.columna().equals("fecha_inicio") || c.columna().equals("fecha_cosecha"))) {
                v.put(c.columna(), LocalDate.now().toString());
            }
        }
        return formulario(e, null, v, Map.of(), model);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String clave, @PathVariable long id, @AuthenticationPrincipal UsuarioSesion u, Model model) {
        Entidad e = entidad(clave, u);
        Map<String, Object> reg = repo.registro(e, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Map<String, Object> v = new HashMap<>();
        for (Campo c : e.getCampos()) {
            Object x = reg.get(c.columna());
            v.put(c.columna(), aTextoFormulario(c, x));
        }
        return formulario(e, id, v, Map.of(), model);
    }

    @PostMapping("/guardar")
    public String guardar(@PathVariable String clave, @RequestParam(required = false) Long id,
                          @RequestParam MultiValueMap<String, String> form, @AuthenticationPrincipal UsuarioSesion u,
                          Model model, RedirectAttributes ra) {
        Entidad e = entidad(clave, u);
        if (id == null ? !e.isCrear() || e.getUrlCrear() != null : !e.isEditar()) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
        }
        Map<String, Object> valores = new LinkedHashMap<>();
        Map<String, String> errores = new LinkedHashMap<>();
        Map<String, Object> crudo = new HashMap<>();
        for (Campo c : e.getCampos()) {
            String raw = form.getFirst(c.columna());
            crudo.put(c.columna(), raw);
            try {
                Object val = convertir(c, raw, id == null);
                if (val != OMITIR) {
                    valores.put(c.columna(), val);
                }
            } catch (IllegalArgumentException ex) {
                errores.put(c.columna(), ex.getMessage());
            }
        }
        if (!errores.isEmpty()) {
            model.addAttribute("error", "Revise los campos marcados.");
            return formulario(e, id, crudo, errores, model);
        }
        try {
            long nuevoId = id;
            if (id == null) {
                nuevoId = repo.insertar(e, valores);
                ra.addFlashAttribute("ok", "Se registró el " + e.getSingular() + " correctamente.");
            } else {
                repo.actualizar(e, id, valores);
                ra.addFlashAttribute("ok", "Se guardaron los cambios del " + e.getSingular() + ".");
            }
            return "redirect:/crud/" + clave + "/" + nuevoId;
        } catch (DataAccessException ex) {
            model.addAttribute("error", ErroresBd.mensaje(ex, false));
            return formulario(e, id, crudo, errores, model);
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String clave, @PathVariable long id, @AuthenticationPrincipal UsuarioSesion u, RedirectAttributes ra) {
        Entidad e = entidad(clave, u);
        if (!e.isEliminar()) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
        }
        try {
            repo.eliminar(e, id);
            ra.addFlashAttribute("ok", "Se eliminó el " + e.getSingular() + " #" + id + ".");
            return "redirect:/crud/" + clave;
        } catch (DataAccessException ex) {
            ra.addFlashAttribute("error", ErroresBd.mensaje(ex, true));
            return "redirect:/crud/" + clave + "/" + id;
        }
    }

    private String formulario(Entidad e, Long id, Map<String, Object> valores, Map<String, String> errores, Model model) {
        model.addAttribute("e", e);
        model.addAttribute("id", id);
        model.addAttribute("v", valores);
        model.addAttribute("errores", errores);
        model.addAttribute("opciones", repo.opciones(e));
        return "crud/form";
    }

    private static final Object OMITIR = new Object();

    private Object convertir(Campo c, String raw, boolean creando) {
        String s = raw == null ? "" : raw.trim();
        if (c.tipo() == Campo.Tipo.BOOLEANO) {
            return raw != null;
        }
        if (c.tipo() == Campo.Tipo.CLAVE) {
            if (s.isEmpty() && !creando) {
                return OMITIR;
            }
            if (s.length() < 8) {
                throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
            }
            return encoder.encode(s);
        }
        if (s.isEmpty()) {
            if (c.requerido()) {
                throw new IllegalArgumentException("Este campo es obligatorio");
            }
            return null;
        }
        if (c.max() > 0 && s.length() > c.max()) {
            throw new IllegalArgumentException("Máximo " + c.max() + " caracteres");
        }
        try {
            return switch (c.tipo()) {
                case EMAIL -> {
                    if (!EMAIL.matcher(s).matches()) {
                        throw new IllegalArgumentException("Correo no válido");
                    }
                    yield s.toLowerCase();
                }
                case TELEFONO -> {
                    String dig = s.replaceAll("[\\s-]", "");
                    if (!TELEFONO.matcher(dig).matches()) {
                        throw new IllegalArgumentException("Escriba entre 7 y 10 dígitos");
                    }
                    yield dig;
                }
                case ENTERO, FK -> {
                    long n = Long.parseLong(s);
                    minimo(c, n);
                    yield n;
                }
                case DECIMAL, MONEDA -> {
                    BigDecimal n = new BigDecimal(s);
                    minimo(c, n.doubleValue());
                    yield n;
                }
                case FECHA -> LocalDate.parse(s);
                case FECHA_HORA -> LocalDateTime.parse(s);
                case OPCIONES -> {
                    if (!c.opciones().contains(s)) {
                        throw new IllegalArgumentException("Opción no válida");
                    }
                    yield s;
                }
                default -> s;
            };
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Escriba un número válido");
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("Fecha no válida");
        }
    }

    private static void minimo(Campo c, double n) {
        if (c.minimo() != null && n < c.minimo()) {
            throw new IllegalArgumentException("Debe ser mayor o igual a " + (c.minimo() % 1 == 0 ? String.valueOf(c.minimo().longValue()) : c.minimo()));
        }
    }

    private static Object aTextoFormulario(Campo c, Object x) {
        if (x == null) {
            return null;
        }
        if (c.tipo() == Campo.Tipo.CLAVE) {
            return null;
        }
        if (c.tipo() == Campo.Tipo.BOOLEANO) {
            return com.uniquindio.frutimarket.comun.Fmt.verdadero(x) ? "1" : null;
        }
        if (x instanceof BigDecimal b) {
            return b.stripTrailingZeros().toPlainString();
        }
        if (x instanceof java.sql.Timestamp t) {
            return t.toLocalDateTime().format(DT_INPUT);
        }
        if (x instanceof LocalDateTime t) {
            return t.format(DT_INPUT);
        }
        return x.toString();
    }
}
