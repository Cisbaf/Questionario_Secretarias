package questionario.service.excel;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import questionario.service.AdminService;
import questionario.service.FiltroStatus;
import questionario.service.excel.ExcelService.Arquivo;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * PDF das mesmas tabelas do Excel (um municipio ou o consolidado): A4 deitado, cada tabela com o seu titulo, uma
 * embaixo da outra. As fontes padrao do PDF cobrem os acentos do portugues.
 */
@Service
@RequiredArgsConstructor
public class PdfService {

    public static final String TIPO = "application/pdf";
    private static final Color VERDE = new Color(0x1D, 0x4A, 0x3A);
    private static final Color CINZA = new Color(0xF1, 0xF3, 0xF2);

    private final AdminService admin;

    @Transactional(readOnly = true)
    public Arquivo porMunicipio(String publicId) {
        return gerar(Documentos.porMunicipio(admin.ler(publicId)));
    }

    @Transactional(readOnly = true)
    public Arquivo consolidado(FiltroStatus filtro) {
        return gerar(Documentos.consolidado(admin.lerPara(filtro), admin.municipiosRepetidos(), filtro.name()));
    }

    private Arquivo gerar(Documentos doc) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document pdf = new Document(new com.lowagie.text.Rectangle(PageSize.A4.getHeight(), PageSize.A4.getWidth()), 28, 28, 32, 36);
        try {
            PdfWriter.getInstance(pdf, out);
            Font rodape = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            HeaderFooter footer = new HeaderFooter(new Phrase("CISBAF · " + doc.titulo() + " · página ", rodape), true);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setBorder(Rectangle.NO_BORDER);
            pdf.setFooter(footer);
            pdf.open();

            pdf.add(new Paragraph(doc.titulo(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, VERDE)));
            Paragraph sub = new Paragraph(doc.subtitulo(), FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY));
            sub.setSpacingAfter(10);
            pdf.add(sub);

            for (Tabela t : doc.tabelas()) {
                Paragraph titulo = new Paragraph(t.aba(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, VERDE));
                titulo.setSpacingBefore(12);
                titulo.setSpacingAfter(4);
                titulo.setKeepTogether(true);
                pdf.add(titulo);
                pdf.add(tabela(t));
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("Não foi possível gerar o PDF", e);
        } finally {
            if (pdf.isOpen()) pdf.close();
        }
        return new Arquivo(out.toByteArray(), doc.nomeArquivo() + ".pdf");
    }

    private static PdfPTable tabela(Tabela t) {
        int n = Math.max(1, t.colunas().size());
        PdfPTable tabela = new PdfPTable(n);
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);
        Font cab = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font corpo = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        for (String coluna : t.colunas()) {
            PdfPCell cell = new PdfPCell(new Phrase(coluna, cab));
            cell.setBackgroundColor(VERDE);
            cell.setPadding(4);
            tabela.addCell(cell);
        }
        List<List<Object>> linhas = t.linhas();
        if (linhas.isEmpty()) {
            PdfPCell vazio = new PdfPCell(new Phrase("Nenhuma linha preenchida.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)));
            vazio.setColspan(n);
            vazio.setPadding(4);
            tabela.addCell(vazio);
        }
        for (int i = 0; i < linhas.size(); i++) {
            List<Object> linha = linhas.get(i);
            for (int c = 0; c < n; c++) {
                Object v = c < linha.size() ? linha.get(c) : null;
                PdfPCell cell = new PdfPCell(new Phrase(Formato.valor(v), corpo));
                cell.setPadding(4);
                if (i % 2 == 1) cell.setBackgroundColor(CINZA);
                if (v instanceof Number || v instanceof Reais) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                tabela.addCell(cell);
            }
        }
        return tabela;
    }
}
