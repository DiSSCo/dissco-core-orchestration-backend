package eu.dissco.orchestration.backend.domain.jsonapi;

import lombok.Value;

@Value
public class JsonApiLinks {
  String self;
  String first;
  String next;
  String prev;

  public JsonApiLinks(int pageSize, int pageNum, boolean hasNext, String path) {
    String pn = "?pageNumber=";
    String ps = "&pageSize=";
    this.self = path + pn + pageNum + ps + pageSize;
    this.first = path + pn + "1" + ps + pageSize;
    this.prev = (pageNum <= 1) ? null : path + pn + (pageNum - 1) + ps + pageSize;
    this.next = (hasNext) ? path + pn + (pageNum + 1) + ps + pageSize : null;
  }

  public JsonApiLinks(String self){
    this.self = self;
    this.first = null;
    this.next = null;
    this.prev = null;
  }


}
