package ma.ahmedbenidar.benchmark.a.dto;

import java.util.List;

/**
 * Enveloppe de pagination commune aux trois variantes : meme nom de champs,
 * meme structure, afin que les plans JMeter puissent utiliser le meme corps de
 * reponse et le meme extracteur JSON quelle que soit l'implementation.
 */
public class PageVue<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PageVue() {
    }

    public PageVue(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) ((totalElements + size - 1) / size) : 0;
    }

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
