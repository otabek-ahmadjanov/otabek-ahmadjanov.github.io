package uz.syncoder.generator.render;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import uz.syncoder.generator.model.Language;

import java.nio.file.Path;
import java.util.Map;

public class Templates {

    private final TemplateEngine engine;
    private final Messages messages;

    public Templates(Path templatesDir, Messages messages) {
        this.messages = messages;
        this.engine = new TemplateEngine();
        engine.addTemplateResolver(resolver(templatesDir, ".html", TemplateMode.HTML, 1));
        engine.setMessageResolver(new PropertiesMessageResolver(messages));
        engine.setLinkBuilder(new StaticLinkBuilder());
    }

    private FileTemplateResolver resolver(Path templatesDir, String suffix, TemplateMode mode, int order) {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix(templatesDir.toAbsolutePath() + "/");
        resolver.setSuffix(suffix);
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setCheckExistence(true);
        resolver.setOrder(order);
        return resolver;
    }

    public Messages messages() {
        return messages;
    }

    public String render(String template, Language language, Map<String, Object> model) {
        return engine.process(template, new Context(language.locale(), model));
    }
}
