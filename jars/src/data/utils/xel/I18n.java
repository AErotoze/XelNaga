package data.utils.xel;

import com.fs.starfarer.api.Global;

/**
 * 有助于将文本外部化到 strings.json 以便进行翻译。
 */
public class I18n {
    private String categoty;

    public I18n(String category) {
        this.categoty = category;
    }

    public String get(String id) {
        return Global.getSettings().getString(categoty, id);
    }

    public String format(String id, Object... args) {
        return String.format(get(id), args);
    }
}
