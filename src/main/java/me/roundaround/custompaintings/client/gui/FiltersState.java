package me.roundaround.custompaintings.client.gui;

import java.util.function.Predicate;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;

public class FiltersState implements Predicate<PaintingData> {
  private final Predicate<PaintingData> canStay;

  private String search = "";
  private String nameSearch = "";
  private String artistSearch = "";
  private boolean canStayOnly = false;
  private int minWidth = 1;
  private int maxWidth = 32;
  private int minHeight = 1;
  private int maxHeight = 32;

  public FiltersState(Predicate<PaintingData> canStay) {
    this.canStay = canStay;
  }

  @Override
  public boolean test(PaintingData paintingData) {
    if (this.canStayOnly && !this.canStay.test(paintingData)) {
      return false;
    }

    if (this.minWidth > paintingData.width()
        || this.maxWidth < paintingData.width()
        || this.minHeight > paintingData.height()
        || this.maxHeight < paintingData.height()) {
      return false;
    }

    String query = this.search.toLowerCase().replace(" ", "");
    String name = paintingData.name().toLowerCase().replace(" ", "");
    String artist = paintingData.artist().toLowerCase().replace(" ", "");

    if (!query.isEmpty()) {
      if (!name.contains(query) && !artist.contains(query)) {
        return false;
      }
    }

    String nameQuery = this.nameSearch.toLowerCase().replace(" ", "");
    if (!nameQuery.isEmpty() && !name.contains(nameQuery)) {
      return false;
    }

    String artistQuery = this.artistSearch.toLowerCase().replace(" ", "");
    if (!artistQuery.isEmpty() && !artist.contains(artistQuery)) {
      return false;
    }

    return true;
  }

  public boolean hasFilters() {
    return !this.search.isEmpty()
        || !this.nameSearch.isEmpty()
        || !this.artistSearch.isEmpty()
        || this.canStayOnly
        || this.minWidth > 1
        || this.maxWidth < 32
        || this.minHeight > 1
        || this.maxHeight < 32;
  }

  public String getSearch() {
    return this.search;
  }

  public String getNameSearch() {
    return this.nameSearch;
  }

  public String getArtistSearch() {
    return this.artistSearch;
  }

  public boolean getCanStayOnly() {
    return this.canStayOnly;
  }

  public int getMinWidth() {
    return this.minWidth;
  }

  public int getMaxWidth() {
    return this.maxWidth;
  }

  public int getMinHeight() {
    return this.minHeight;
  }

  public int getMaxHeight() {
    return this.maxHeight;
  }

  public void reset() {
    this.search = "";
    this.nameSearch = "";
    this.artistSearch = "";
    this.canStayOnly = false;
    this.minWidth = 1;
    this.maxWidth = 32;
    this.minHeight = 1;
    this.maxHeight = 32;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public void setNameSearch(String nameSearch) {
    this.nameSearch = nameSearch;
  }

  public void setArtistSearch(String artistSearch) {
    this.artistSearch = artistSearch;
  }

  public void setCanStayOnly(boolean canStayOnly) {
    this.canStayOnly = canStayOnly;
  }

  public void setMinWidth(int minWidth) {
    this.minWidth = minWidth;
  }

  public void setMaxWidth(int maxWidth) {
    this.maxWidth = maxWidth;
  }

  public void setMinHeight(int minHeight) {
    this.minHeight = minHeight;
  }

  public void setMaxHeight(int maxHeight) {
    this.maxHeight = maxHeight;
  }

  public void setExactWidth(int width) {
    this.minWidth = width;
    this.maxWidth = width;
  }

  public void setExactHeight(int height) {
    this.minHeight = height;
    this.maxHeight = height;
  }
}
