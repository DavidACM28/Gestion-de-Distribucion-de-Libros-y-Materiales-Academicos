package pe.incubadora.backend.utils.movimientoInventario;

public enum CreateAjusteMovimientoInventarioResult {
    MATERIAL_NOT_FOUND,
    LOTE_NOT_FOUND,
    LOTE_MATERIAL_CONFLICT,
    LOTE_NOT_VALID,
    TIPO_AJUSTE_NOT_VALID,
    STOCK_INSUFFICIENT,
    CREATED
}
