package questionario.service.excel;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import questionario.service.AdminService;
import questionario.service.AdminService.Lido;
import questionario.service.FiltroStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Excel da equipe. Um municipio: uma aba por tabela do documento. Consolidado: as mesmas abas, com uma linha por
 * municipio e por linha de tabela, e as colunas "Municipio" e "Status" na frente, prontas para filtro e tabela
 * dinamica. A primeira aba do consolidado lista os questionarios incluidos (e avisa os municipios repetidos).
 */
@Service
@RequiredArgsConstructor
public class ExcelService {

    public static final String TIPO = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int LARGURA_MAX = 60;
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AdminService admin;

    /** Arquivo e nome sugerido. */
    public record Arquivo(byte[] bytes, String nome) {
    }

    @Transactional(readOnly = true)
    public Arquivo porMunicipio(String publicId) {
        return gerar(Documentos.porMunicipio(admin.ler(publicId)));
    }

    @Transactional(readOnly = true)
    public Arquivo consolidado(FiltroStatus filtro) {
        return gerar(Documentos.consolidado(admin.lerPara(filtro), admin.municipiosRepetidos(), filtro.name()));
    }

    /** Tudo numa aba so: cada tabela com o seu titulo, os cabecalhos e as linhas, uma embaixo da outra. */
    private Arquivo gerar(Documentos doc) {
        try (Workbook wb = new XSSFWorkbook()) {
            Estilos estilos = new Estilos(wb);
            Sheet sheet = wb.createSheet("Questionário");
            int colunas = doc.tabelas().stream().mapToInt(t -> t.colunas().size()).max().orElse(1);
            int[] larguras = new int[colunas];
            java.util.Arrays.fill(larguras, 12);

            int linha = 0;
            Row titulo = sheet.createRow(linha++);
            titulo.setHeightInPoints(26);
            Cell tc = titulo.createCell(0);
            tc.setCellValue(doc.titulo());
            tc.setCellStyle(estilos.titulo);
            Row sub = sheet.createRow(linha++);
            Cell sc = sub.createCell(0);
            sc.setCellValue(doc.subtitulo());
            sc.setCellStyle(estilos.texto);
            linha++;

            for (Tabela t : doc.tabelas()) {
                Row secao = sheet.createRow(linha++);
                Cell cs = secao.createCell(0);
                cs.setCellValue(t.aba());
                cs.setCellStyle(estilos.secao);
                for (int c = 1; c < t.colunas().size(); c++) secao.createCell(c).setCellStyle(estilos.secao);

                Row cab = sheet.createRow(linha++);
                cab.setHeightInPoints(32);
                for (int c = 0; c < t.colunas().size(); c++) {
                    Cell cell = cab.createCell(c);
                    cell.setCellValue(t.colunas().get(c));
                    cell.setCellStyle(estilos.cabecalho);
                    larguras[c] = Math.max(larguras[c], Math.min(LARGURA_MAX, t.colunas().get(c).length() / 2 + 4));
                }
                if (t.linhas().isEmpty()) {
                    Cell vazio = sheet.createRow(linha++).createCell(0);
                    vazio.setCellValue("Nenhuma linha preenchida.");
                    vazio.setCellStyle(estilos.texto);
                }
                for (List<Object> valores : t.linhas()) {
                    Row row = sheet.createRow(linha++);
                    for (int c = 0; c < t.colunas().size(); c++) {
                        Object v = c < valores.size() ? valores.get(c) : null;
                        int tamanho = preencher(row.createCell(c), v, estilos);
                        larguras[c] = Math.max(larguras[c], Math.min(LARGURA_MAX, tamanho + 2));
                    }
                }
                linha++; // linha em branco entre as tabelas
            }
            for (int c = 0; c < colunas; c++) sheet.setColumnWidth(c, larguras[c] * 256);
            return new Arquivo(bytes(wb), doc.nomeArquivo() + ".xlsx");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Poe o valor na celula com o formato certo; devolve o tamanho do texto, para a largura da coluna. */
    private static int preencher(Cell cell, Object v, Estilos e) {
        switch (v) {
            case null -> {
                cell.setCellStyle(e.texto);
                return 0;
            }
            case Integer i -> {
                cell.setCellValue(i);
                cell.setCellStyle(e.inteiro);
                return String.valueOf(i).length();
            }
            case BigDecimal d -> {
                cell.setCellValue(d.doubleValue());
                cell.setCellStyle(e.decimal);
                return d.toPlainString().length();
            }
            case Reais r -> {
                cell.setCellValue(r.valor().doubleValue());
                cell.setCellStyle(e.reais);
                return 14;
            }
            case LocalDate d -> {
                cell.setCellValue(d);
                cell.setCellStyle(e.data);
                return 10;
            }
            case LocalDateTime d -> {
                cell.setCellValue(d.format(DATA_HORA));
                cell.setCellStyle(e.texto);
                return 16;
            }
            default -> {
                String texto = v.toString();
                cell.setCellValue(texto);
                cell.setCellStyle(texto.length() > 40 ? e.textoLongo : e.texto);
                return texto.length();
            }
        }
    }

    private static byte[] bytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private static final class Estilos {
        final CellStyle titulo, secao, cabecalho, texto, textoLongo, inteiro, decimal, reais, data;

        Estilos(Workbook wb) {
            DataFormat formato = wb.createDataFormat();
            Font negrito = wb.createFont();
            negrito.setBold(true);
            negrito.setColor(IndexedColors.WHITE.getIndex());

            Font grande = wb.createFont();
            grande.setBold(true);
            grande.setFontHeightInPoints((short) 16);
            titulo = wb.createCellStyle();
            titulo.setFont(grande);
            Font verde = wb.createFont();
            verde.setBold(true);
            verde.setFontHeightInPoints((short) 12);
            verde.setColor(IndexedColors.DARK_GREEN.getIndex());
            secao = wb.createCellStyle();
            secao.setFont(verde);
            secao.setBorderBottom(BorderStyle.MEDIUM);
            secao.setBottomBorderColor(IndexedColors.DARK_GREEN.getIndex());

            cabecalho = wb.createCellStyle();
            cabecalho.setFont(negrito);
            cabecalho.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cabecalho.setWrapText(true);
            cabecalho.setVerticalAlignment(VerticalAlignment.CENTER);

            texto = base(wb);
            textoLongo = base(wb);
            textoLongo.setWrapText(true);
            inteiro = base(wb);
            inteiro.setDataFormat(formato.getFormat("#,##0"));
            inteiro.setAlignment(HorizontalAlignment.RIGHT);
            decimal = base(wb);
            decimal.setDataFormat(formato.getFormat("#,##0.00"));
            reais = base(wb);
            reais.setDataFormat(formato.getFormat("\"R$\" #,##0.00"));
            data = base(wb);
            data.setDataFormat(formato.getFormat("dd/mm/yyyy"));
            data.setAlignment(HorizontalAlignment.CENTER);
        }

        private static CellStyle base(Workbook wb) {
            CellStyle s = wb.createCellStyle();
            s.setVerticalAlignment(VerticalAlignment.TOP);
            s.setBorderBottom(BorderStyle.HAIR);
            return s;
        }
    }
}
