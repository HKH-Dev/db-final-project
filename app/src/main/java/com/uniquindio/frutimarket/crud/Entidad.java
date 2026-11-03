package com.uniquindio.frutimarket.crud;

import com.uniquindio.frutimarket.comun.Columna;
import java.util.ArrayList;
import java.util.List;

/**
 * Definicion de una tabla administrable. sqlLista debe devolver la columna "id" (la llave)
 * y las columnas que se muestran; se usa para el listado, la busqueda y la ficha de detalle.
 */
public class Entidad {

    private final String clave;
    private final String tabla;
    private final String pk;
    private final String titulo;
    private final String singular;
    private final String icono;
    private final String grupo;
    private String descripcion = "";
    private String sqlLista;
    private String ordenDefecto = "id DESC";
    private final List<Columna> columnas = new ArrayList<>();
    private final List<String> enLista = new ArrayList<>();
    private final List<Campo> campos = new ArrayList<>();
    private boolean crear = true;
    private boolean editar = true;
    private boolean eliminar = true;
    private String urlCrear;
    private String urlDetalle;
    private boolean soloAdmin;

    public Entidad(String clave, String tabla, String pk, String titulo, String singular, String icono, String grupo) {
        this.clave = clave;
        this.tabla = tabla;
        this.pk = pk;
        this.titulo = titulo;
        this.singular = singular;
        this.icono = icono;
        this.grupo = grupo;
    }

    public Entidad descripcion(String d) { this.descripcion = d; return this; }

    public Entidad lista(String sql) { this.sqlLista = sql; return this; }

    public Entidad orden(String o) { this.ordenDefecto = o; return this; }

    /** Columna que aparece en el listado y en la ficha */
    public Entidad col(Columna c) { columnas.add(c); enLista.add(c.clave()); return this; }

    /** Columna que solo aparece en la ficha de detalle */
    public Entidad det(Columna c) { columnas.add(c); return this; }

    public Entidad campo(Campo c) { campos.add(c); return this; }

    public Entidad sinCrear() { crear = false; return this; }

    public Entidad sinEditar() { editar = false; return this; }

    public Entidad sinEliminar() { eliminar = false; return this; }

    public Entidad crearEn(String url) { urlCrear = url; return this; }

    public Entidad detalleEn(String url) { urlDetalle = url; return this; }

    public Entidad soloAdmin() { soloAdmin = true; return this; }

    public String getClave() { return clave; }
    public String getTabla() { return tabla; }
    public String getPk() { return pk; }
    public String getTitulo() { return titulo; }
    public String getSingular() { return singular; }
    public String getIcono() { return icono; }
    public String getGrupo() { return grupo; }
    public String getDescripcion() { return descripcion; }
    public String getSqlLista() { return sqlLista; }
    public String getOrdenDefecto() { return ordenDefecto; }
    public List<Columna> getColumnas() { return columnas; }
    public List<Columna> getColumnasLista() { return columnas.stream().filter(c -> enLista.contains(c.clave())).toList(); }
    public List<Campo> getCampos() { return campos; }
    public boolean isCrear() { return crear; }
    public boolean isEditar() { return editar && !campos.isEmpty(); }
    public boolean isEliminar() { return eliminar; }
    public String getUrlCrear() { return urlCrear; }
    /** Plantilla de url con {id} para entidades con pantalla propia (pedidos) */
    public String getUrlDetalle() { return urlDetalle; }
    public boolean isSoloAdmin() { return soloAdmin; }
}
