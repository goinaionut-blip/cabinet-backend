package ro.cabinet.backend.sign;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Arrays;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocalTempStorageService implements StorageService {
  private static final byte[] PDF_MAGIC = new byte[] {'%', 'P', 'D', 'F', '-'};

  private final Path incomingDir;
  private final Path signedDir;

  public LocalTempStorageService(SignProperties properties) {
    SignProperties.Storage storage = properties.getStorage();
    String baseDir = storage.getBaseDir();
    String incoming = storage.getIncomingDir();
    String signed = storage.getSignedDir();
    if ((incoming == null || incoming.isBlank()) && baseDir != null && !baseDir.isBlank()) {
      incoming = Path.of(baseDir, "incoming").toString();
    }
    if ((signed == null || signed.isBlank()) && baseDir != null && !baseDir.isBlank()) {
      signed = Path.of(baseDir, "signed").toString();
    }
    if (incoming == null || incoming.isBlank()) {
      incoming = "./data/tmp/incoming";
    }
    if (signed == null || signed.isBlank()) {
      signed = "./data/tmp/signed";
    }
    this.incomingDir = Path.of(incoming);
    this.signedDir = Path.of(signed);
    try {
      Files.createDirectories(this.incomingDir);
      Files.createDirectories(this.signedDir);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to initialize sign storage directories", ex);
    }
  }

  @Override
  public StorageResult saveIncoming(String documentId, String filename, InputStream inputStream) {
    String safeName = sanitizeFilename(filename);
    Path path = incomingDir.resolve(documentId + "-" + safeName);
    String sha256 = savePdfWithHash(inputStream, path);
    return new StorageResult(path.toString(), sha256);
  }

  @Override
  public Resource loadIncoming(String path) {
    return loadFile(path);
  }

  @Override
  public StorageResult saveSigned(String documentId, String filename, InputStream inputStream) {
    String safeName = sanitizeFilename(filename);
    Path path = signedDir.resolve(documentId + "-" + safeName);
    String sha256 = savePdfWithHash(inputStream, path);
    return new StorageResult(path.toString(), sha256);
  }

  @Override
  public Resource loadSigned(String path) {
    return loadFile(path);
  }

  private Resource loadFile(String path) {
    if (path == null || path.isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fisier lipsa");
    }
    Path filePath = Path.of(path);
    if (!Files.exists(filePath)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fisier lipsa");
    }
    return new FileSystemResource(filePath);
  }

  private String savePdfWithHash(InputStream inputStream, Path targetPath) {
    try (InputStream validated = validatePdfHeader(inputStream)) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (DigestInputStream dis = new DigestInputStream(validated, digest)) {
        Files.copy(dis, targetPath, StandardCopyOption.REPLACE_EXISTING);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException | IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nu pot salva fisierul");
    }
  }

  private InputStream validatePdfHeader(InputStream inputStream) throws IOException {
    byte[] header = inputStream.readNBytes(PDF_MAGIC.length);
    if (header.length < PDF_MAGIC.length) {
      inputStream.close();
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fisier PDF invalid");
    }
    if (!Arrays.equals(header, PDF_MAGIC)) {
      inputStream.close();
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fisier PDF invalid");
    }
    return new SequenceInputStream(new ByteArrayInputStream(header), inputStream);
  }

  private String sanitizeFilename(String value) {
    if (value == null || value.isBlank()) {
      return "document.pdf";
    }
    String sanitized = value.replaceAll("[\\r\\n]", "_");
    sanitized = sanitized.replace("/", "_").replace("\\\\", "_");
    return sanitized;
  }
}
