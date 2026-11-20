package curso.flowable.dto;

public class SolicitudCurso {

    private Integer empresaId;

    private String cuit;
    private String empresa;
    private String contacto;
    private String email;
    private String telefono;

    private String tematica;
    private String descripcion;
    private Integer cantidadParticipantes;
    private String fechasPreferidas;

    public Integer empresaId() {
        return empresaId;
    }

    public void setEmpresaId(Integer empresaId) {
        this.empresaId = empresaId;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String tematica() {
        return tematica;
    }

    public void setTematica(String tematica) {
        this.tematica = tematica;
    }

    public String descripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer cantidadParticipantes() {
        return cantidadParticipantes;
    }

    public void setCantidadParticipantes(Integer cantidadParticipantes) {
        this.cantidadParticipantes = cantidadParticipantes;
    }

    public String fechasPreferidas() {
        return fechasPreferidas;
    }

    public void setFechasPreferidas(String fechasPreferidas) {
        this.fechasPreferidas = fechasPreferidas;
    }
}
