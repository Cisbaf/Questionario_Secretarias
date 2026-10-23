package questionario.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import questionario.api.dto.AdminDetalhe;
import questionario.api.dto.AdminResumo;
import questionario.service.AdminService;
import questionario.service.FiltroStatus;
import questionario.service.excel.ExcelService;
import questionario.service.excel.PdfService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/questionarios")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService service;
    private final ExcelService excel;
    private final PdfService pdf;

    @GetMapping
    public List<AdminResumo> listar(@RequestParam(required = false) String status) {
        return service.listar(FiltroStatus.de(status, FiltroStatus.TODOS));
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> consolidado(@RequestParam(required = false) String status) {
        return arquivo(excel.consolidado(FiltroStatus.de(status, FiltroStatus.ENVIADO)));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> consolidadoPdf(@RequestParam(required = false) String status) {
        return arquivo(pdf.consolidado(FiltroStatus.de(status, FiltroStatus.ENVIADO)), PdfService.TIPO);
    }

    @GetMapping("/{publicId}")
    public AdminDetalhe detalhe(@PathVariable String publicId) {
        return service.detalhe(publicId);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String publicId) {
        service.excluir(publicId);
    }

    @GetMapping("/{publicId}/excel")
    public ResponseEntity<byte[]> porMunicipio(@PathVariable String publicId) {
        return arquivo(excel.porMunicipio(publicId));
    }

    @GetMapping("/{publicId}/pdf")
    public ResponseEntity<byte[]> porMunicipioPdf(@PathVariable String publicId) {
        return arquivo(pdf.porMunicipio(publicId), PdfService.TIPO);
    }

    private static ResponseEntity<byte[]> arquivo(ExcelService.Arquivo a) {
        return arquivo(a, ExcelService.TIPO);
    }

    private static ResponseEntity<byte[]> arquivo(ExcelService.Arquivo a, String tipo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.nome(), StandardCharsets.UTF_8).build().toString())
                .body(a.bytes());
    }
}
