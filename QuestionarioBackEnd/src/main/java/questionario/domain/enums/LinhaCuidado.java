package questionario.domain.enums;

/**
 * Bloco 2, item 2.2: linhas de cuidado avaliadas de 1 a 5, na ordem do documento. {@code OUTRA} é a linha aberta
 * ("Outra: ____"), cuja descrição vai em campo próprio. Com {@code ddl-auto=update}, mudar este enum exige ajuste
 * manual das linhas já gravadas; por isso o conjunto segue o documento e não deve crescer sem necessidade.
 */
public enum LinhaCuidado {
    CARDIOVASCULAR_AVC,
    TRAUMA_ORTOPEDIA,
    ONCOLOGIA,
    DIAGNOSTICO_IMAGEM,
    QUEIMADOS,
    ATENCAO_PRIMARIA_ESF,
    GESTACAO_PARTO_NASCIMENTO,
    SAUDE_CRIANCA_PEDIATRIA,
    UTI_ADULTO,
    CLINICA_MEDICA_RETAGUARDA,
    CIRURGIAS_ELETIVAS_MEDIA,
    SAUDE_MENTAL,
    NEFROLOGIA_TRS,
    OFTALMOLOGIA,
    DOENCAS_CRONICAS,
    REABILITACAO_PCD,
    PALIATIVOS_ATENCAO_DOMICILIAR,
    OUTRA
}
