package ro.cabinet.backend.sign;

import java.io.InputStream;

import org.springframework.core.io.Resource;

public interface StorageService {
  StorageResult saveIncoming(String documentId, String filename, InputStream inputStream);

  Resource loadIncoming(String path);

  StorageResult saveSigned(String documentId, String filename, InputStream inputStream);

  Resource loadSigned(String path);

  record StorageResult(String path, String sha256) {
  }
}
