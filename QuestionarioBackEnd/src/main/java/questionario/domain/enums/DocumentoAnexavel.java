package questionario.domain.enums;

/**
 * Bloco 8, item 8.2: documentos que o município pode anexar. {@link #PLANILHA_COLETA} é a exceção: não é do 8.2, é a
 * planilha de coleta que o município baixa, preenche e devolve no Bloco 1 (um arquivo só, que substitui o anterior).
 */
public enum DocumentoAnexavel {
    PLANO_MUNICIPAL_PAS,
    RELATORIO_FILA_REGULACAO,
    RELATORIO_PERMANENCIA_UPA,
    ESTUDOS_DEMANDA_REPRIMIDA,
    RESOLUCOES_COFINANCIAMENTO,
    CONTRATOS_HOSPITAIS_PRIVADOS,
    PLANILHA_COLETA;

    /** Os documentos que o 8.2 oferece (todos menos a planilha do Bloco 1). */
    public boolean doItem82() {
        return this != PLANILHA_COLETA;
    }
}
