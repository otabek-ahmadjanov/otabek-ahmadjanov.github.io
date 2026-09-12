package uz.syncoder.generator.render;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.IMessageResolver;
import uz.syncoder.generator.model.Language;

public class PropertiesMessageResolver implements IMessageResolver {

    private final Messages messages;

    public PropertiesMessageResolver(Messages messages) {
        this.messages = messages;
    }

    @Override
    public String getName() {
        return "properties";
    }

    @Override
    public Integer getOrder() {
        return 1;
    }

    @Override
    public String resolveMessage(ITemplateContext context, Class<?> origin, String key, Object[] parameters) {
        return messages.get(languageOf(context), key, parameters == null ? new Object[0] : parameters);
    }

    @Override
    public String createAbsentMessageRepresentation(
            ITemplateContext context, Class<?> origin, String key, Object[] parameters) {
        return "??" + key + "??";
    }

    private Language languageOf(ITemplateContext context) {
        return "en".equals(context.getLocale().getLanguage()) ? Language.EN : Language.RU;
    }
}
