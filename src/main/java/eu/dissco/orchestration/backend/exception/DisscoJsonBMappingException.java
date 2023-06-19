package eu.dissco.orchestration.backend.exception;

import org.springframework.dao.DataAccessException;

public class DisscoJsonBMappingException extends DataAccessException {

  public DisscoJsonBMappingException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
