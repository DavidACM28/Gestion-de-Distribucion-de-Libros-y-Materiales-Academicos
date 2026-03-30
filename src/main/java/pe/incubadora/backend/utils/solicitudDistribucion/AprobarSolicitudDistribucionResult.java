package pe.incubadora.backend.utils.solicitudDistribucion;

public enum AprobarSolicitudDistribucionResult {
    SOLICITUD_NOT_FOUND,
    ITEMS_EMPTY,
    DETALLE_NOT_FOUND,
    DETALLE_NOT_BELONG_TO_SOLICITUD,
    CANTIDAD_APROBADA_NOT_VALID,
    ESTADO_INVALIDO,
    STOCK_INSUFFICIENT,
    UPDATED
}
