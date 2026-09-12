package uz.syncoder.generator.render.view;

import java.util.List;

public record ArticleView(
        String slug,
        String url,
        String title,
        String summary,
        String contentHtml,
        String coverImage,
        String publishedDate,
        int readingMinutes,
        List<TagView> tags) {
}
