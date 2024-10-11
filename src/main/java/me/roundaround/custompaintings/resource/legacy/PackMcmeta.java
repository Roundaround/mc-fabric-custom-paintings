package me.roundaround.custompaintings.resource.legacy;

public record PackMcmeta(PackSubKey pack) {
  public record PackSubKey(Integer pack_format, String description) {
  }
}
