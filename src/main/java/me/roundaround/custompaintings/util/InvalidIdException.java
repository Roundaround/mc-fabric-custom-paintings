package me.roundaround.custompaintings.util;

public class InvalidIdException extends Exception {
  public InvalidIdException(String id) {
    super(message(id));
  }

  public InvalidIdException(String id, String resource) {
    super(message(id, resource));
  }

  public InvalidIdException(String id, Throwable cause) {
    super(message(id), cause);
  }

  public InvalidIdException(String id, String resource, Throwable cause) {
    super(message(id, resource), cause);
  }

  private static String message(String id) {
    return String.format("Invalid ID: \"%s\"", id);
  }

  private static String message(String id, String resource) {
    return String.format("Invalid ID for %s: \"%s\"", resource, id);
  }
}
