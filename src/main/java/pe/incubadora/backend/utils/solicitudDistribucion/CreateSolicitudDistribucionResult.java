package pe.incubadora.backend.utils.solicitudDistribucion;

public enum CreateSolicitudDistribucionResult {
    SEDE_NOT_FOUND,
    SEDE_NOT_ACTIVE,
    MATERIAL_NOT_FOUND,
    SOLICITUD_DUPLICADA,
    PERIODO_NOT_VALID,
    PRIORIDAD_NOT_VALID,
    ITEMS_EMPTY,
    MATERIAL_DUPLICATE,
    CANTIDAD_SOLICITADA_NOT_VALID,
    CREATED
}
