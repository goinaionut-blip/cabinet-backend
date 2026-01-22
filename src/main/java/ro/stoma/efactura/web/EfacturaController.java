package ro.stoma.efactura.web;

import ro.stoma.efactura.service.EfacturaService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/efactura")
public class EfacturaController {
  private static final Logger log = LoggerFactory.getLogger(EfacturaController.class);
  private final EfacturaService efacturaService;

  public EfacturaController(EfacturaService efacturaService) {
    this.efacturaService = efacturaService;
  }

  @PostMapping(value = "/upload", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
  public ResponseEntity<String> upload(@RequestBody byte[] xml) {
    log.info("eFactura upload request size={} bytes", xml == null ? 0 : xml.length);
    String index = efacturaService.uploadInvoice(xml);
    return ResponseEntity.ok(index);
  }

  @GetMapping("/status/{index}")
  public ResponseEntity<String> status(@PathVariable("index") String index) {
    log.info("eFactura status request index={}", index);
    return ResponseEntity.ok(efacturaService.getStatus(index));
  }

  @GetMapping("/messages")
  public ResponseEntity<String> messages(@RequestParam(value = "days", required = false) Integer days) {
    log.info("eFactura messages request days={}", days);
    return ResponseEntity.ok(efacturaService.listMessages(days));
  }

  @GetMapping(value = "/download/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> download(@PathVariable("id") String id) {
    log.info("eFactura download request id={}", id);
    byte[] payload = efacturaService.download(id);
    return ResponseEntity.ok(payload);
  }

  @GetMapping("/cif")
  public ResponseEntity<String> cif() {
    return ResponseEntity.ok(efacturaService.getCif());
  }
}
