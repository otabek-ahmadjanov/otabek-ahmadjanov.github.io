package uz.syncoder.generator.render;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.ILinkBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

public class StaticLinkBuilder implements ILinkBuilder {

    @Override
    public String getName() {
        return "static";
    }

    @Override
    public Integer getOrder() {
        return 1;
    }

    @Override
    public String buildLink(IExpressionContext context, String base, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return base;
        }
        StringJoiner query = new StringJoiner("&");
        parameters.forEach((name, value) -> query.add(name + "=" + encode(value)));
        return base + (base.contains("?") ? "&" : "?") + query;
    }

    private String encode(Object value) {
        return value == null ? "" : URLEncoder.encode(value.toString(), StandardCharsets.UTF_8);
    }
}
