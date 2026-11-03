package com.uniquindio.frutimarket.reportes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/consultas")
public class ConsultaController {

    public record Resultado(Consulta.Seccion seccion, List<Map<String, Object>> filas, Map<String, java.math.BigDecimal> totales) {
    }

    private final CatalogoConsultas catalogo;
    private final EjecutorSql ejecutor;

    public ConsultaController(CatalogoConsultas catalogo, EjecutorSql ejecutor) {
        this.catalogo = catalogo;
        this.ejecutor = ejecutor;
    }

    @GetMapping
    public String indice(Model model) {
        model.addAttribute("consultas", catalogo.todas());
        return "consultas/indice";
    }

    @GetMapping("/{clave}")
    public String ver(@PathVariable String clave, @RequestParam Map<String, String> params, Model model) {
        Consulta c = catalogo.buscar(clave).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<EjecutorSql.Filtro> filtros = ejecutor.filtros(c.parametros(), params);
        MapSqlParameterSource p = ejecutor.parametros(filtros);
        List<Resultado> resultados = new ArrayList<>();
        for (Consulta.Seccion s : c.secciones()) {
            List<Map<String, Object>> filas = ejecutor.ejecutar(s.sql(), p);
            resultados.add(new Resultado(s, filas, ejecutor.totales(s.columnas(), filas)));
        }
        model.addAttribute("c", c);
        model.addAttribute("filtros", filtros);
        model.addAttribute("resultados", resultados);
        model.addAttribute("consultas", catalogo.todas());
        return "consultas/ver";
    }
}
