package me.roundaround.custompaintings.resource.file.json;

public record PackMcmeta(PackSubKey pack) {
  public record PackSubKey(Integer pack_format, String description) {
  }
}
