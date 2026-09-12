package uz.syncoder.generator.render.view;

public record PaginationView(int page, int totalPages, String prevUrl, String nextUrl) {

    public boolean multiPage() {
        return totalPages > 1;
    }
}
